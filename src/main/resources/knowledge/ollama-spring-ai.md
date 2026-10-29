# Local Ollama and Spring AI

Ollama runs language models locally. Start the server with ollama serve. Its default API host is http://localhost:11434. Pull a small model using ollama pull gemma3:1b, then test it with ollama run gemma3:1b. The local API does not require a cloud API key. The model must be downloaded before inference can work offline.

Spring AI's Ollama starter supplies a chat model and a ChatClient.Builder. Build a ChatClient, pass a system instruction and ordered user/assistant history, then call the model for text. Keep speaker roles intact. Configure spring.ai.ollama.base-url and spring.ai.ollama.chat.options.model. A system prompt controls the assistant's role; user messages carry the request.

Retrieval augmented generation (RAG) retrieves relevant note chunks before generation and supplies them as reference context. Cite returned source IDs and admit when notes lack the answer. Lexical retrieval matches words and can miss synonyms. A multi-agent workflow gives specialized model calls different roles and passes one stage's output to the next. Small local models benefit from concise prompts and bounded output.
