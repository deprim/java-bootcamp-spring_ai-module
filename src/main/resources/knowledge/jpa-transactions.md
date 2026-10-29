# JPA, SQLite, and transactions

JPA maps Java entities to relational tables. Mark an entity with @Entity and its key with @Id. A repository provides persistence operations. Keep both sides of a bidirectional relationship synchronized. Cascade operations allow a conversation aggregate to own its messages. Specify ordering explicitly when loading a message history.

@Transactional defines a unit of work. A runtime exception normally rolls back database changes made inside the transaction. A user message and its assistant reply should either both persist or neither persist when generation fails. Use a read-only transaction for queries. Calling an external model inside a transaction keeps the exchange atomic but holds a connection while waiting; for higher concurrency, use a separate pending-message workflow.

SQLite is an embedded database useful for a local demo. It permits limited concurrent writing. This project uses Hibernate's community SQLite dialect. Disable open-in-view and map entities to DTOs while the service transaction is active. Practice: test that a failed AI exchange leaves the stored conversation unchanged.
