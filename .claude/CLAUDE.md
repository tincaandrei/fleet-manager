# Project Instructions for Claude

You are working on the Fleet Manager / DoccuFleet codebase.

## General rules

- Do not run any git commands.
- Do not rewrite the whole project.
- Work incrementally.
- Preserve existing functionality.
- Prefer shared components over duplicated logic.
- Ask before deleting files or changing API contracts.
- Do not introduce Flyway or Liquibase unless explicitly requested.
- Follow existing backend/frontend patterns.
- Do not fake frontend behavior that is not backed by APIs.

## Frontend rules

- Keep TypeScript strict.
- Keep the UI mobile-compatible.
- Use existing components/design system where possible.
- Improve loading, empty, success, and error states.
- Avoid excessive gradients, glassmorphism, oversized cards, and unnecessary animations.

## Backend rules

- Preserve existing role boundaries.
- Do not rely only on frontend authorization.
- Add backend authorization checks for SUPERADMIN and BUSINESS_ADMIN behavior.
- Add/update tests for security-sensitive changes.

## Validation

After meaningful changes, run the existing build/test commands if available.
Do not claim tests passed unless they actually ran successfully.