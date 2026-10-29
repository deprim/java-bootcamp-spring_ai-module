# Local Ollama Bootcamp Tutor

## Intent and requirements

Complete all six assignment TODOs and retain the existing browser chat and
SQLite conversations. Use local Ollama at `http://localhost:11434` by default,
with `gemma3:1b` and no API key. All project changes are in English.
Demonstrate additional endpoints, logic components/RAG, and a real multi-agent
workflow rather than a single prompt pretending to contain several agents.

## Approach

Use the existing Java 21, Spring Boot 4.1.1, JPA, MapStruct, and SQLite stack.
Add the Spring AI 2.0.2 BOM and native Ollama starter. Replace the unfinished
OpenRouter client and configuration with Ollama-specific names and a reusable
model gateway. Map persisted message roles into Spring AI messages.

An external MCP server would add process and tool-capable-model requirements.
A vector database would add storage and an embedding model. My selected
implementation uses local lexical RAG and explicit Java orchestration instead,
which meets the expansion requirements with only the requested small model.
These implementation choices were delegated by the user's request.

## API and behavior

- Preserve chat CRUD and `POST /api/chats/{id}/chatMessages`; add `/messages`
  as a conventional alias. Calls include the system prompt and ordered history.
- `GET/POST /api/knowledge/documents`, `DELETE /api/knowledge/documents/{id}`:
  persistent, validated notes with a title and content; seed a small English
  Java/Spring/Ollama knowledge base only on an empty database.
- `GET /api/knowledge/search?query=...&limit=...`: deterministic ranked chunks.
- `POST /api/tutor/ask` with `question`: retrieve up to three matching chunks,
  answer with citation IDs, and return sources. With no evidence, return an
  explicit no-evidence answer without calling the model.
- `POST /api/tutor/study-plan` with `topic`, `level`, and `days`: researcher
  extracts relevant facts from notes; tutor creates a paced plan using those
  facts; reviewer corrects the draft. Return each named stage's output, the
  final reviewed plan, and retrieved sources. Three distinct model calls use
  separate system roles and pass outputs to subsequent stages.

## Boundaries and failures

Validate blank, oversized, and out-of-range inputs. Bound source chunks and
generated output for small-model context. Treat notes as untrusted reference
data, not instructions. Return readable HTTP problem details for missing
records and upstream model failures. A failed chat exchange rolls back both
messages. Do not silently fall back to a cloud model or a fake answer.

Use lexical overlap ranking (not embeddings); note its limited semantic recall.
No arbitrary file access, code execution, or external tools are exposed to LLMs.
No extra infrastructure, authentication, or UI redesign is required.

## Verification

Unit tests cover role mapping, retrieval ranking and chunking, grounded answers,
and stage handoffs. HTTP/persistence tests cover chat CRUD, ordering, validation,
rollback, knowledge CRUD, retrieval, and workflow responses. Normal tests use a
controlled model boundary and isolated SQLite files. An opt-in live test profile
checks the actual local message endpoint, RAG, and three model calls. README
documents setup, example requests, the architecture, and observed verification.
