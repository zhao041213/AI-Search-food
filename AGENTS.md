# AGENTS.md

## Project Context

This repository is a graduation project: ingredient recognition and intelligent recipe recommendation based on multimodal large models.

Current stack:

- Java 17
- Spring Boot 3
- Maven
- DeepSeek API integration
- Static frontend under `src/main/resources/static`

## Core Working Rules

- Only change files that are directly related to the current task.
- Do not refactor unrelated code.
- Preserve the existing project structure and coding style unless a task requires a focused change.
- Prefer small, reviewable changes over broad edits.
- Read the surrounding code before editing.
- Do not overwrite user changes. If user changes affect the task, work with them.
- All frontend pages must use Chinese as the primary visible language. English copy should only remain for code identifiers, API values, or unavoidable third-party terms.

## Required Flow For Code Changes

Before changing code:

- Inspect the relevant files and tests.
- State a short plan when the change is not trivial.
- Keep the scope tied to the requested feature, fix, or documentation update.

After changing code:

- Review the diff before reporting completion.
- Run relevant verification when practical.
- For this Maven project, prefer:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" test
```

- If verification cannot be run, explain the exact reason.
- Ask the user whether to commit the branch and push it to GitHub.

## Review Requirement

Every completed modification must be reviewed before the final response.

The review should check:

- Whether the change matches the user's request.
- Whether unrelated files or behavior were changed.
- Whether obvious bugs, broken tests, formatting issues, or encoding issues were introduced.
- Whether the change needs additional tests.

## Rollback Requirement

After every modification, treat the current pre-change state as the previous version.

If the user says the Chinese rollback phrase U+56DE U+9000, or says "rollback",
rollback the last modification made by the agent:

- Revert only the agent's most recent changes.
- Do not revert unrelated user changes.
- Check the diff after rollback.
- Report what was reverted.

## Git And GitHub Rules

- Do not commit unless the user explicitly approves.
- Do not push to GitHub unless the user explicitly approves.
- Do not merge branches unless the user explicitly approves.
- After each completed modification, ask whether the user wants the branch committed and pushed to GitHub.
- Keep commit messages concise and task-focused when commits are approved.

## API Key Handling

The user may temporarily provide a DeepSeek API key for local debugging or verification.

Default handling:

- Use temporary environment variables when possible.
- Do not write API keys into source code, README files, committed config files, tests, or generated artifacts by default.
- Do not commit API keys to GitHub.
- If a task truly requires writing an API key to a local file, state the target path and risk first, then wait for explicit confirmation.

## Project-Specific Notes

- Keep DeepSeek configuration compatible with environment variables such as `DEEPSEEK_API_KEY` and `DEEPSEEK_MODEL`.
- Keep backend endpoints and frontend behavior aligned.
- For API behavior changes, update or add focused controller/service tests when practical.
- For UI changes, verify that the static page still works from the Spring Boot app.
- Avoid introducing unnecessary frameworks, build tools, or large dependencies.
