# Local Ollama Tutor Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task by task. Track progress below.

**Goal:** Complete the assignment with a working local chatbot, lexical RAG, and a three-agent study-plan API.

**Architecture:** A Spring AI Ollama gateway serves existing persistent chat and a new tutor service. SQLite notes feed deterministic chunk retrieval; three separate role-specific calls produce a reviewed study plan.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring AI 2.0.2, Ollama gemma3:1b, SQLite, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-29-local-ollama-tutor-design.md`

## Global constraints

- English only; local Ollama at `http://localhost:11434`; default `gemma3:1b`.
- Preserve existing chat URLs and frontend behavior.
- Normal tests do not require Ollama. Live tests are opt-in.
- This supplied directory has no usable Git repository; work in place without commits.

## Review focus

- Model unavailable or empty response: HTTP 502 and no partial chat persistence.
- Conversation reload: deterministic user/assistant ordering and complete history.
- Empty or unrelated retrieval: no invented sources or unnecessary model call.
- Long notes and punctuation-only queries: bounded chunks and deterministic empty results.
- Agent handoffs: all three real calls, correct order, grounded context, failure propagation.

## Task 1: Complete local chat

Files: `pom.xml`, `.env.example`, `application.yaml`, `client/OllamaChatClient.java`,
`client/AiModelException.java`, `config/AiProperties.java`, `client/ConversationMessages.java`,
`ChatService.java`, `GlobalExceptionHandler.java`, `ChatController.java`.

Interfaces: `OllamaChatClient.complete(List<ChatMessage>) -> String` and
`generate(String systemPrompt, String userPrompt) -> String`.

- [x] Add BOM/starter and local settings; use the installed Java 21 runtime.
- [x] Write and run failing history/empty-reply tests and HTTP chat tests.
- [x] Implement model calls, role mapping, errors, and message URL alias.
- [x] Verify chat CRUD, validation, complete history, rollback, and reload order.

## Task 2: Local knowledge retrieval

Files: `knowledge/KnowledgeDocument.java`, `KnowledgeRepository.java`,
`KnowledgeService.java`, `KnowledgeController.java`, `KnowledgeSeed.java`,
`KnowledgeDtos.java`, `src/main/resources/knowledge/*.md`.

Interfaces: `KnowledgeService.search(String query, int limit) -> List<Source>`;
sources contain stable IDs, document identity/title, chunk content, and score.

- [x] Write and run failing ranking, chunking, no-match, and knowledge API tests.
- [x] Implement persistent notes, bounded overlapping chunks, lexical scoring,
      validation, missing-document handling, and initial course notes.
- [x] Verify CRUD, relevant snippets, punctuation queries, and deletion behavior.

## Task 3: Grounded tutor and multi-agent workflow

Files: `tutor/TutorDtos.java`, `TutorService.java`, `TutorController.java`.

Interfaces: `POST /api/tutor/ask -> {answer,sources}` and
`POST /api/tutor/study-plan -> {plan,stages,sources}` with named stage outputs.

- [x] Write and run failing tests for citation prompts, no evidence, ordered
      researcher/tutor/reviewer handoffs, validation, and stage failure.
- [x] Implement retrieval-augmented Q&A and three distinct model calls.
- [x] Verify stage outputs and final plan through HTTP integration tests.

## Task 4: Real runtime acceptance and documentation

Files: `README.md`, `scripts/smoke-test.py`, live test profile/tests,
`.gitignore`, and optional project-local Ollama setup script.

- [x] Install the official Ollama runtime locally and run `ollama --version`.
- [x] Start the API, pull `gemma3:1b`, and run a manual model prompt.
- [x] Run `./mvnw verify` and the opt-in live acceptance checks.
- [x] Document setup, endpoints, workflow, limitations, and actual results.
- [x] Review the full implementation and resolve material findings.

## Execution record

- Initial inspection: six TODOs; unfinished OpenRouter client cannot compile;
  Ollama absent; Java 21 available alongside default Java 17.
- Choice: retain Boot 4.1.1 with verified compatible Spring AI 2.0.2.
- Choice: implement in the supplied directory; Git metadata is unavailable.
- Task 1 complete: chat protocol/persistence tests passed; full ordered history,
  legacy URL compatibility, empty-response handling, and rollback verified.
- Task 2 complete: knowledge CRUD and retrieval tests passed, including long
  notes, missing evidence, validation, deletion, and readable creation Location.
- Task 3 complete: grounded Q&A and three role-specific model calls passed unit
  and HTTP tests; research and draft handoffs and failure propagation verified.
- Task 4 complete: official Ollama 0.40.13 installed project-locally, model
  `gemma3:1b` pulled, CLI/manual generation verified, API running on 11434.
- Final independent review: no critical backend findings. Fixed the browser
  conversation-switching race and draft recovery; four Node regression tests
  observed failing before the fix and passing after it. Added GET for a newly
  created note's Location; its regression test observed 405 before the fix.
- Final verification: `./mvnw -Pollama-live verify` passed with 16 ordinary tests
  and one real-model acceptance test; `node --test src/test/js/app.test.cjs`
  passed all four browser tests. Real-model acceptance verified an inserted
  `ORBIT-42` fact and all three agent outputs. Packaged-app smoke test passed;
  generated outputs are saved locally in `.local/acceptance.json`.
- Review boundaries: probabilistic model quality, lexical recall, full-history
  context limits, and SQLite concurrency remain documented demo limitations.
