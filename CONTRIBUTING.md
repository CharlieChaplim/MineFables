
# Development Guidelines

## General Principles

- Prioritize maintainability, performance and modularity.
- Implement features incrementally.
- Do not invent gameplay mechanics or rules that have not been specified.
- Avoid unnecessary abstractions and premature optimization.
- Explain important architectural decisions.

## Architecture

- Keep gameplay logic separate from rendering and UI.
- Gameplay-critical operations must be server-authoritative.
- Use player attachments for appropriate player-specific persistent data.
- Use SavedData for appropriate world-global persistent data.
- Avoid duplicating shared systems between characters.
- Keep character-specific mechanics isolated when appropriate.

## Code Quality

- Use Java 21.
- Follow consistent Java naming conventions.
- Keep classes and methods focused on clearly defined responsibilities.
- Avoid unnecessary dependencies.
- Document non-obvious architectural decisions.

## Git Workflow

- Keep main stable and buildable.
- Create a separate branch for each feature or bug fix.
- Keep commits focused on one coherent change.
- Compile and test before merging.
- Never commit credentials, personal configuration or generated files.

## Testing

- Test gameplay behavior in the development client.
- Verify server-side behavior for networked mechanics.
- Add automated tests when appropriate.
- Do not consider a feature complete merely because it compiles.
