# AI-Powered Support Ticket Assistant

Backend service using Java 22, Spring Boot, REST APIs, PostgreSQL, Flyway, and a configurable LLM provider.

## Features
- REST ticket creation, retrieval, and listing
- Validation and UUID ticket IDs
- Asynchronous LLM analysis
- `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` states
- `LlmProvider` abstraction with mock and OpenAI-compatible implementations
- Structured LLM output validation before persistence
- LLM/provider failure handling
- Prompt-injection mitigation
- PostgreSQL + Flyway
- Swagger/OpenAPI
- Pagination/filtering
- Docker Compose
- Actuator health/info/metrics
- Unit and REST integration tests

## Technology
| Area | Technology |
|---|---|
| Language | Java 22 |
| Framework | Spring Boot |
| API | REST / Spring Web |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Migration | Flyway |
| Build | Maven |
| API docs | Springdoc OpenAPI / Swagger |
| Async | Spring `@Async` + transaction event listener |
| LLM | Mock or OpenAI-compatible provider |
| Tests | JUnit 5 / Mockito / Spring Boot Test |

## Architecture

```text
Client
  |
  v
REST Controller
  |
  v
TicketService -----> PostgreSQL
  |
  +-- TicketCreated event (AFTER_COMMIT)
                |
                v
        Async Analysis Worker
                |
                v
           LlmProvider
          /           \
       Mock       OpenAI-compatible
          \           /
           v         v
        Structured output
                |
                v
        Validate before storage
                |
                v
        PostgreSQL analysis
```

The LLM call is deliberately outside the database transaction. The ticket is committed first; the worker then performs analysis and uses short independent transactions for status/analysis persistence.

## Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL
- Git

The project can run without an external LLM subscription by using the mock provider.

## Database
Create:

```sql
CREATE DATABASE support_assistant;
```

The application uses the `support` schema and Flyway manages the tables.

## Configuration

### Mock mode (default/reviewer mode)

```text
LLM_PROVIDER=mock
```

No API key is required.

### External provider

```text
LLM_PROVIDER=openai
LLM_ENDPOINT=https://api.openai.com/v1/chat/completions
LLM_MODEL=gpt-4.1-mini
LLM_API_KEY=<your-api-key>
LLM_CONNECT_TIMEOUT_MS=3000
LLM_READ_TIMEOUT_MS=15000
LLM_MAX_ATTEMPTS=3
```

Never commit secrets.

## Run

PowerShell:

```powershell
$env:DB_USERNAME="support_user"
$env:DB_PASSWORD="<your-password>"
$env:LLM_PROVIDER="mock"

mvn spring-boot:run
```

Application:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI:

```text
http://localhost:8080/v3/api-docs
```

## API

### Create ticket

`POST /api/tickets`

```json
{
  "customerId": "CUST-101",
  "subject": "Unable to import a Word document",
  "description": "The import remains at 80% and eventually fails.",
  "priority": "HIGH",
  "product": "Import"
}
```

Creation returns the ticket while analysis is asynchronous. Initial status is normally `PENDING`.

### Retrieve ticket

`GET /api/tickets/{ticketId}`

After analysis:

```json
{
  "id": "ticket-uuid",
  "customerId": "CUST-101",
  "subject": "Unable to import a Word document",
  "description": "The import remains at 80% and eventually fails.",
  "priority": "HIGH",
  "product": "Import",
  "analysisStatus": "COMPLETED",
  "analysis": {
    "category": "IMPORT_FAILURE",
    "summary": "The customer is experiencing a document import failure.",
    "suggestedResponse": "We are investigating the import issue.",
    "recommendedTeam": "IMPORT_ENGINEERING",
    "confidence": 0.87
  }
}
```

Before completion, the ticket is returned with its current status and without a completed analysis.

### List tickets

`GET /api/tickets?page=0&size=20`

Filtering by customer/status is supported where exposed by the controller.

## Failure handling
- Invalid request → HTTP 400
- Ticket not found → HTTP 404
- Provider failure/timeout → `FAILED`
- Empty/malformed/unexpected LLM output → `FAILED`
- Confidence outside `0..1` → rejected
- Original ticket remains available when analysis fails

## LLM abstraction

```text
LlmProvider
├── MockLlmProvider
└── OpenAiCompatibleLlmProvider
```

This keeps ticket business logic independent of a specific provider.

## Structured output validation
Before persistence:
- category must be present
- summary must be present
- suggested response must be present
- recommended team must be present
- confidence must be finite and between `0` and `1`

## Prompt injection and sensitive information
Ticket subject/description are untrusted data. The prompt instructs the model to treat ticket content as data, ignore embedded instructions, not reveal system prompts/secrets, and not invent unsupported facts.

`customerId` is stored by the application but is intentionally not sent to the LLM because it is not required for analysis.

These are mitigations, not guarantees that prompt injection is impossible.

## Testing

```powershell
mvn clean test
```

Tests cover ticket creation/validation, successful analysis, invalid LLM output, provider failure, missing tickets, confidence validation, and REST integration.

## Docker

```powershell
docker compose up --build
```

## Production gaps
The assessment implementation intentionally remains small. For production, consider:
- durable queue/outbox instead of in-process `@Async`
- authentication/authorization
- correlation IDs and distributed tracing
- richer LLM latency/failure metrics
- durable retries and dead-letter handling
- managed secret storage
- provider-specific privacy/retention controls
- concurrency protection for analysis regeneration

## Assumptions
- Ticket persistence is independent of analysis success.
- Analysis is eventually consistent.
- Ticket content is untrusted.
- Mock mode is sufficient for reviewer execution.
- PostgreSQL is the persistence mechanism.
- Current async processing is appropriate for assessment scope.

## AI tool disclosure
AI assistance was used for requirements review, architecture discussion, debugging, test-coverage review, and documentation. Submitted code was reviewed and tested by the candidate. The candidate can explain the submitted implementation and design decisions.

## Approximate time spent
Record the actual time before submission:

```text
Approximate time spent: <ENTER ACTUAL TIME>
```

## Submission checklist
- [ ] `mvn clean test` passes
- [ ] Mock mode runs without an external API subscription
- [ ] POST → PENDING → PROCESSING → COMPLETED verified
- [ ] Invalid request → 400 verified
- [ ] Unknown ticket → 404 verified
- [ ] Swagger works
- [ ] No secrets committed
- [ ] `.env` ignored
- [ ] `target/` and IDE files ignored
- [ ] README complete
- [ ] Git repository contains source and tests
