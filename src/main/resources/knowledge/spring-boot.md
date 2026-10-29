# Spring Boot and dependency injection

Spring Boot configures an application from dependencies and properties. Spring manages objects called beans in an application context. Constructor dependency injection makes required dependencies explicit and supports testing without starting the entire application. Annotate a service with @Service and inject its collaborators through the constructor. Avoid field injection.

A @RestController exposes HTTP endpoints. @GetMapping retrieves data; @PostMapping creates a resource or performs an action; @DeleteMapping removes a resource. Return 201 Created with a Location header when creating resources and 204 No Content for successful deletion. Validate request bodies with @Valid and constraints such as @NotBlank and @Size. Return 400 for invalid input and 404 for missing resources.

Practice: build a task API with a controller, service, and repository. Add integration tests for creation, validation, listing, and deletion. Keep HTTP details in controllers and business rules in services.
