# MediMin AI

MediMin AI is a calm, privacy-minded health companion for symptom guidance, guided check-ins, AI conversations, and personal wellness context.

## Run & Operate

- `pnpm --filter @workspace/api-server run dev` — run the API server (port 5000)
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from the OpenAPI spec
- `pnpm --filter @workspace/db run push` — push DB schema changes (dev only)
- Required env: `DATABASE_URL` — Postgres connection string
- Required secret for live assistant responses: `OPENAI_API_KEY`

## Stack

- pnpm workspaces, Node.js 24, TypeScript 5.9
- API: Express 5
- DB: PostgreSQL + Drizzle ORM
- Validation: Zod (`zod/v4`), `drizzle-zod`
- API codegen: Orval (from OpenAPI spec)
- Build: esbuild (CJS bundle)

## Where things live

- `artifacts/medimin-ai/src/App.tsx` — complete responsive patient workspace and routes
- `artifacts/medimin-ai/src/index.css` — MediMin visual tokens, typography, responsive behavior, and motion
- `artifacts/api-server/src/routes/medimin.ts` — dashboard, assessment, symptom, conversation, and profile API routes
- `artifacts/api-server/src/lib/medimin-ai.ts` — server-side AI calls and safe health-information fallbacks
- `lib/api-spec/openapi.yaml` — source-of-truth API contract
- `lib/db/src/schema/medimin.ts` — PostgreSQL tables and typed Drizzle models

## Architecture decisions

- API contracts are defined in OpenAPI first, then generated hooks and Zod validators are used by the frontend and server.
- The first build uses a single seeded patient workspace so the product is immediately understandable; user-scoped auth can be layered onto the same tables.
- AI calls happen only on the server. If the provider is unavailable, health flows remain usable and return a cautious, non-diagnostic fallback.
- The product deliberately separates symptom organization and reflection from diagnosis or emergency care.

## Product

The app includes a dashboard snapshot, health assessment history and creation, symptom analysis, AI conversations, a profile context view, and a trust/safety page. The backend persists the profile, assessments, symptom checks, conversations, and messages in PostgreSQL.

## User preferences

The user prefers a complete frontend-to-backend product structure based on the uploaded MediMin AI brief.

## Gotchas

- Run `pnpm --filter @workspace/api-spec run codegen` after any OpenAPI change.
- Keep `OPENAI_API_KEY` server-side; never expose it in frontend code or logs.

## Pointers

- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details
