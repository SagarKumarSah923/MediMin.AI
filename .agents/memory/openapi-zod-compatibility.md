---
name: OpenAPI and Zod compatibility
description: A workspace-specific constraint when adding API schemas and regenerating Orval output.
---

When extending OpenAPI contracts in this workspace, prefer schema constructs that generate helpers supported by the pinned Zod 3 runtime. In particular, integer and email formats can make Orval emit Zod 4-only top-level helpers.

**Why:** The code generator currently emits `zod.int()` and `zod.email()` for those constructs, while the workspace resolves `zod` to v3.25.76. Codegen completes but the chained library typecheck fails.

**How to apply:** Use numeric bounds without `integer` and plain strings for email-like fields unless the generator/runtime versions are upgraded together.