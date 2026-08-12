# MediMin AI

MediMin AI is a calm, privacy-minded health companion for symptom guidance, guided check-ins, AI conversations, and personal wellness context.

## Run & Operate

- `bash services/medimin-spring/run-dev.sh` — run the Spring Boot microservices locally
- `mvn -f services/medimin-spring/pom.xml clean package -DskipTests` — build all Spring services
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from the OpenAPI spec
- Required secret: `MONGODB_URI` — MongoDB connection string
- Optional env: `MONGODB_DATABASE` — database name when the URI does not include one (defaults to `medimin`)
- Optional secret: `OPENAI_API_KEY` — enables live LLM responses; local agent fallbacks remain available

## Stack

- pnpm workspaces, Node.js 24, TypeScript 5.9
- API: Spring Boot 3 microservices
- DB: MongoDB + Spring Data MongoDB
- AI: dedicated Spring Boot agent service with server-side OpenAI calls and safety fallbacks
- API codegen: Orval (from OpenAPI spec)
- Build: Maven + Spring Boot Maven Plugin

## Where things live

- `artifacts/medimin-ai/src/App.tsx` — complete responsive patient workspace and routes
- `artifacts/medimin-ai/src/index.css` — MediMin visual tokens, typography, responsive behavior, and motion
- `services/medimin-spring/gateway` — public `/api` gateway consumed by React
- `services/medimin-spring/profile-service` — MongoDB-backed patient profile service
- `services/medimin-spring/care-service` — MongoDB-backed assessments, symptoms, conversations, messages, and dashboard service
- `services/medimin-spring/agent-service` — LLM/agent orchestration, structured symptom analysis, and safe fallbacks
- `services/medimin-spring/common` — shared Java API records between services
- `lib/api-spec/openapi.yaml` — source-of-truth API contract
- `lib/db/src/schema/medimin.ts` — PostgreSQL tables and typed Drizzle models

## Architecture decisions

- API contracts are defined in OpenAPI first, then generated hooks and Zod validators are used by the frontend and server.
- The first build uses a single seeded patient workspace so the product is immediately understandable; user-scoped auth can be layered onto the MongoDB documents.
- AI calls happen only in the agent service. If the provider is unavailable, health flows remain usable and return a cautious, non-diagnostic fallback.
- The product deliberately separates symptom organization and reflection from diagnosis or emergency care.

## Product

The app includes a dashboard snapshot, health assessment history and creation, symptom analysis, AI conversations, a profile context view, and a trust/safety page. The Spring services persist the profile, assessments, symptom checks, conversations, and messages in MongoDB.

## User preferences

The user prefers a complete frontend-to-backend product structure based on the uploaded MediMin AI brief.

## Gotchas

- Run `pnpm --filter @workspace/api-spec run codegen` after any OpenAPI change.
- Keep `OPENAI_API_KEY` server-side; never expose it in frontend code or logs.

## Pointers

- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details
