type ChatMessage = { role: "system" | "user" | "assistant"; content: string };

const SYSTEM_PROMPT =
  "You are MediMin AI, a careful health information assistant. Help users organize symptoms, questions, and next steps in plain, compassionate language. Never diagnose, never claim certainty, and always recommend a qualified clinician for concerning or persistent symptoms. If the user may be experiencing an emergency, tell them to contact local emergency services immediately. Keep responses concise and practical.";

export async function askMediMin(messages: ChatMessage[]): Promise<string> {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    return "I can help you organize what you are noticing, but the AI service is not connected yet. For now, write down when it started, what makes it better or worse, and any changes that concern you. A qualified clinician can help interpret the full picture.";
  }

  try {
    const response = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: "gpt-4o-mini",
        temperature: 0.2,
        max_tokens: 500,
        messages: [{ role: "system", content: SYSTEM_PROMPT }, ...messages],
      }),
    });

    if (!response.ok) {
      return "I’m having trouble reaching the assistant right now. If you are worried about a symptom, contact a qualified healthcare professional rather than waiting for a response.";
    }

    const payload = (await response.json()) as {
      choices?: Array<{ message?: { content?: string } }>;
    };
    return (
      payload.choices?.[0]?.message?.content?.trim() ??
      "I’m not sure how to respond to that yet. Could you share a little more about when it started and how it is changing?"
    );
  } catch {
    return "I’m having trouble reaching the assistant right now. If you are worried about a symptom, contact a qualified healthcare professional rather than waiting for a response.";
  }
}

export async function analyzeSymptoms(input: {
  symptoms: string[];
  duration: string;
  severity: string;
  notes?: string;
}): Promise<{ possibleCauses: string[]; guidance: string[]; urgency: string; disclaimer: string }> {
  const fallback = {
    possibleCauses: [
      "A temporary or self-limited illness",
      "Stress, sleep, hydration, or other everyday factors",
      "Something that may benefit from a clinician’s assessment if it persists",
    ],
    guidance: [
      "Notice whether the symptoms are improving, stable, or getting worse",
      "Rest, hydrate, and keep a short note of timing and triggers",
      "Contact a qualified clinician if symptoms persist or feel concerning",
    ],
    urgency: input.severity === "severe" ? "Seek care soon" : "Monitor mindfully",
    disclaimer:
      "This is general information, not a diagnosis. A clinician who knows your history can give you personalized medical advice.",
  };

  const response = await askMediMin([
    {
      role: "user",
      content: `Analyze these symptoms for a general health information summary. Return JSON only with keys possibleCauses (array of 3 short strings), guidance (array of 3 short strings), urgency (short string), disclaimer (short string). Do not diagnose. Symptoms: ${input.symptoms.join(", ")}. Duration: ${input.duration}. Severity: ${input.severity}. Notes: ${input.notes ?? "none"}`,
    },
  ]);

  try {
    const parsed = JSON.parse(response) as Partial<typeof fallback>;
    if (
      Array.isArray(parsed.possibleCauses) &&
      Array.isArray(parsed.guidance) &&
      typeof parsed.urgency === "string" &&
      typeof parsed.disclaimer === "string"
    ) {
      return {
        possibleCauses: parsed.possibleCauses.map(String).slice(0, 4),
        guidance: parsed.guidance.map(String).slice(0, 4),
        urgency: parsed.urgency,
        disclaimer: parsed.disclaimer,
      };
    }
  } catch {
    // Keep the safe structured fallback when the model does not return JSON.
  }

  return fallback;
}