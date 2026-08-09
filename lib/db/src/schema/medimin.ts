import { date, integer, jsonb, pgTable, serial, text, timestamp } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod/v4";

export const profilesTable = pgTable("medimin_profiles", {
  id: serial("id").primaryKey(),
  firstName: text("first_name").notNull(),
  lastName: text("last_name").notNull(),
  email: text("email").notNull(),
  dateOfBirth: date("date_of_birth", { mode: "string" }).notNull(),
  bloodType: text("blood_type").notNull().default("Not added"),
  allergies: text("allergies").array().notNull().default([]),
  conditions: text("conditions").array().notNull().default([]),
  medicationCount: integer("medication_count").notNull().default(0),
});

export const assessmentsTable = pgTable("medimin_assessments", {
  id: serial("id").primaryKey(),
  title: text("title").notNull(),
  status: text("status").notNull().default("in progress"),
  summary: text("summary").notNull().default(""),
  score: integer("score").notNull().default(0),
  answers: jsonb("answers").$type<Record<string, string>>().notNull().default({}),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
  updatedAt: timestamp("updated_at", { withTimezone: true }).notNull().defaultNow(),
});

export const symptomChecksTable = pgTable("medimin_symptom_checks", {
  id: serial("id").primaryKey(),
  symptoms: text("symptoms").array().notNull(),
  duration: text("duration").notNull(),
  severity: text("severity").notNull(),
  notes: text("notes").notNull().default(""),
  possibleCauses: text("possible_causes").array().notNull(),
  guidance: text("guidance").array().notNull(),
  urgency: text("urgency").notNull(),
  disclaimer: text("disclaimer").notNull(),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
});

export const conversationsTable = pgTable("medimin_conversations", {
  id: serial("id").primaryKey(),
  title: text("title").notNull(),
  preview: text("preview").notNull().default("A new conversation with MediMin AI"),
  messageCount: integer("message_count").notNull().default(0),
  updatedAt: timestamp("updated_at", { withTimezone: true }).notNull().defaultNow(),
});

export const messagesTable = pgTable("medimin_messages", {
  id: serial("id").primaryKey(),
  conversationId: integer("conversation_id").notNull(),
  role: text("role").notNull(),
  content: text("content").notNull(),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
});

export const insertProfileSchema = createInsertSchema(profilesTable).omit({ id: true });
export const insertAssessmentSchema = createInsertSchema(assessmentsTable).omit({ id: true, createdAt: true, updatedAt: true });
export const insertSymptomCheckSchema = createInsertSchema(symptomChecksTable).omit({ id: true, createdAt: true });
export const insertConversationSchema = createInsertSchema(conversationsTable).omit({ id: true, updatedAt: true });
export const insertMessageSchema = createInsertSchema(messagesTable).omit({ id: true, createdAt: true });

export type Profile = typeof profilesTable.$inferSelect;
export type Assessment = typeof assessmentsTable.$inferSelect;
export type SymptomCheck = typeof symptomChecksTable.$inferSelect;
export type Conversation = typeof conversationsTable.$inferSelect;
export type Message = typeof messagesTable.$inferSelect;
export type InsertProfile = z.infer<typeof insertProfileSchema>;
export type InsertAssessment = z.infer<typeof insertAssessmentSchema>;
export type InsertSymptomCheck = z.infer<typeof insertSymptomCheckSchema>;
export type InsertConversation = z.infer<typeof insertConversationSchema>;
export type InsertMessage = z.infer<typeof insertMessageSchema>;