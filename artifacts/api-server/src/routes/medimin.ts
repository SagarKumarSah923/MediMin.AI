import { Router, type IRouter } from "express";
import { desc, eq } from "drizzle-orm";
import {
  db,
  assessmentsTable,
  conversationsTable,
  messagesTable,
  profilesTable,
  symptomChecksTable,
} from "@workspace/db";
import {
  CreateAssessmentBody,
  CreateAssessmentResponse,
  CreateConversationBody,
  CreateConversationResponse,
  CreateSymptomCheckBody,
  CreateSymptomCheckResponse,
  GetAssessmentParams,
  GetAssessmentResponse,
  GetDashboardResponse,
  GetProfileResponse,
  ListAssessmentsResponse,
  ListConversationsResponse,
  ListMessagesParams,
  ListMessagesResponse,
  SendMessageBody,
  SendMessageParams,
  SendMessageResponse,
} from "@workspace/api-zod";
import { analyzeSymptoms, askMediMin } from "../lib/medimin-ai";

const router: IRouter = Router();

function profileResponse(profile: typeof profilesTable.$inferSelect) {
  return GetProfileResponse.parse(profile);
}

router.get("/profile", async (_req, res): Promise<void> => {
  const [profile] = await db.select().from(profilesTable).limit(1);
  if (!profile) {
    res.status(404).json({ error: "Profile not found" });
    return;
  }
  res.json(profileResponse(profile));
});

router.get("/dashboard", async (_req, res): Promise<void> => {
  const [profile] = await db.select().from(profilesTable).limit(1);
  if (!profile) {
    res.status(404).json({ error: "Profile not found" });
    return;
  }
  const [assessments, conversations, symptoms] = await Promise.all([
    db.select().from(assessmentsTable).orderBy(desc(assessmentsTable.createdAt)),
    db.select().from(conversationsTable).orderBy(desc(conversationsTable.updatedAt)),
    db.select().from(symptomChecksTable).orderBy(desc(symptomChecksTable.createdAt)),
  ]);
  const latestAssessment = assessments[0] ?? null;
  const activity = [
    ...assessments.map((item) => ({
      id: item.id,
      kind: "assessment",
      title: "Completed a health check-in",
      detail: item.title,
      occurredAt: item.createdAt,
    })),
    ...conversations.map((item) => ({
      id: 10000 + item.id,
      kind: "conversation",
      title: "Talked with MediMin AI",
      detail: item.title,
      occurredAt: item.updatedAt,
    })),
    ...symptoms.map((item) => ({
      id: 20000 + item.id,
      kind: "symptom",
      title: "Reviewed a symptom check",
      detail: item.symptoms.join(", "),
      occurredAt: item.createdAt,
    })),
  ]
    .sort((a, b) => b.occurredAt.getTime() - a.occurredAt.getTime())
    .slice(0, 6);
  const score = latestAssessment?.score ?? 0;
  res.json(
    GetDashboardResponse.parse({
      profile,
      metrics: [
        {
          label: "Check-ins completed",
          value: String(assessments.length),
          detail: assessments.length ? "Keep noticing the patterns" : "Your first one is a few minutes",
          tone: "teal",
        },
        {
          label: "Last check-in",
          value: latestAssessment ? `${score}/100` : "Not yet",
          detail: latestAssessment ? "A reflection, not a diagnosis" : "Start whenever you are ready",
          tone: "sun",
        },
      ],
      latestAssessment,
      recentActivity: activity,
      healthScore: score,
      healthScoreLabel: latestAssessment ? "A useful snapshot" : "Build your baseline",
    }),
  );
});

router.get("/assessments", async (_req, res): Promise<void> => {
  const assessments = await db.select().from(assessmentsTable).orderBy(desc(assessmentsTable.createdAt));
  res.json(ListAssessmentsResponse.parse(assessments));
});

router.post("/assessments", async (req, res): Promise<void> => {
  const parsed = CreateAssessmentBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }
  const score = Math.min(100, 58 + Object.keys(parsed.data.answers).length * 7);
  const [assessment] = await db
    .insert(assessmentsTable)
    .values({
      title: parsed.data.title,
      answers: parsed.data.answers,
      score,
      status: "complete",
      summary: "A thoughtful starting point for noticing how things have been feeling recently.",
    })
    .returning();
  res.status(201).json(CreateAssessmentResponse.parse(assessment));
});

router.get("/assessments/:id", async (req, res): Promise<void> => {
  const params = GetAssessmentParams.safeParse(req.params);
  if (!params.success) {
    res.status(400).json({ error: params.error.message });
    return;
  }
  const [assessment] = await db
    .select()
    .from(assessmentsTable)
    .where(eq(assessmentsTable.id, params.data.id));
  if (!assessment) {
    res.status(404).json({ error: "Assessment not found" });
    return;
  }
  res.json(GetAssessmentResponse.parse(assessment));
});

router.post("/symptom-checks", async (req, res): Promise<void> => {
  const parsed = CreateSymptomCheckBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }
  const analysis = await analyzeSymptoms(parsed.data);
  const [check] = await db
    .insert(symptomChecksTable)
    .values({ ...parsed.data, notes: parsed.data.notes ?? "", ...analysis })
    .returning();
  res.status(201).json(CreateSymptomCheckResponse.parse(check));
});

router.get("/conversations", async (_req, res): Promise<void> => {
  const conversations = await db.select().from(conversationsTable).orderBy(desc(conversationsTable.updatedAt));
  res.json(ListConversationsResponse.parse(conversations));
});

router.post("/conversations", async (req, res): Promise<void> => {
  const parsed = CreateConversationBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }
  const [conversation] = await db.insert(conversationsTable).values({ title: parsed.data.title }).returning();
  res.status(201).json(CreateConversationResponse.parse(conversation));
});

router.get("/conversations/:id/messages", async (req, res): Promise<void> => {
  const params = ListMessagesParams.safeParse(req.params);
  if (!params.success) {
    res.status(400).json({ error: params.error.message });
    return;
  }
  const messages = await db
    .select()
    .from(messagesTable)
    .where(eq(messagesTable.conversationId, params.data.id))
    .orderBy(messagesTable.createdAt);
  res.json(ListMessagesResponse.parse(messages));
});

router.post("/conversations/:id/messages", async (req, res): Promise<void> => {
  const params = SendMessageParams.safeParse(req.params);
  const body = SendMessageBody.safeParse(req.body);
  if (!params.success || !body.success) {
    res.status(400).json({ error: "Invalid conversation or message" });
    return;
  }
  const [conversation] = await db
    .select()
    .from(conversationsTable)
    .where(eq(conversationsTable.id, params.data.id));
  if (!conversation) {
    res.status(404).json({ error: "Conversation not found" });
    return;
  }
  const [userMessage] = await db
    .insert(messagesTable)
    .values({ conversationId: conversation.id, role: "user", content: body.data.content })
    .returning();
  const prior = await db
    .select()
    .from(messagesTable)
    .where(eq(messagesTable.conversationId, conversation.id))
    .orderBy(messagesTable.createdAt);
  const assistantContent = await askMediMin(
    prior.map((message) => ({
      role: message.role === "assistant" ? "assistant" : "user",
      content: message.content,
    })),
  );
  const [assistantMessage] = await db
    .insert(messagesTable)
    .values({ conversationId: conversation.id, role: "assistant", content: assistantContent })
    .returning();
  await db
    .update(conversationsTable)
    .set({
      preview: assistantContent,
      messageCount: prior.length + 1,
      updatedAt: new Date(),
    })
    .where(eq(conversationsTable.id, conversation.id));
  res.status(201).json(SendMessageResponse.parse({ userMessage, assistantMessage }));
});

export default router;