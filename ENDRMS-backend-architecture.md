# ENDRMS Backend Architecture & Folder Structure

Based on the SRS (ADR-001 through ADR-008). This expands the SRS's high-level
module sketch into a concrete, buildable project layout.

## 1. Locked-in decisions (from the SRS — not up for debate)

| Decision | Choice | Source |
|---|---|---|
| Language/Framework | Java + Spring Boot | ADR-001 |
| Architecture style | **Modular Monolith** (not microservices, not traditional monolith) | ADR-005 |
| Database | PostgreSQL + PostGIS | ADR-004 |
| API style | REST, versioned (`/api/v1/...`), standard `{success, data, message}` envelope | ADR-006 |
| Auth | JWT (access + refresh) via Spring Security, BCrypt hashing | ADR-007 |
| Identity model | **One user account, many roles (RBAC)** — not one account per role | ADR-008 |
| Caching | Redis | NFR-PERF-001 |
| Containerization | Docker (infra-agnostic — must run on-prem, AWS, Azure, gov servers) | NFR-INFRA-001 |
| Monitoring | Prometheus + Grafana | NFR-AVAIL-001 |
| Reverse proxy | Nginx | Executive Summary |
| Object storage | Something S3-compatible (images must NOT live in Postgres) | NFR-PERF-001 |
| Build tool | **Maven** | your choice |
| Object storage provider | **MinIO** (self-hosted, S3-compatible) | your choice |
| Schema migrations | **Flyway** (chosen for you — simpler SQL-first workflow, plays well with PostGIS-specific SQL and the need for a readable, linear migration history for audit purposes) | your choice |

## 1a. Core Maven dependencies

```xml
<dependencies>
    <!-- Web / API -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>

    <!-- Security -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId></dependency>
    <!-- + jjwt-impl, jjwt-jackson at runtime scope -->

    <!-- Persistence -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId></dependency>
    <dependency><groupId>net.postgis</groupId><artifactId>postgis-jdbc</artifactId></dependency> <!-- PostGIS -->
    <dependency><groupId>org.hibernate</groupId><artifactId>hibernate-spatial</artifactId></dependency>

    <!-- Migrations -->
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>

    <!-- Caching -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-cache</artifactId></dependency>

    <!-- Object storage (MinIO, S3-compatible client) -->
    <dependency><groupId>io.minio</groupId><artifactId>minio</artifactId></dependency>

    <!-- API docs -->
    <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId></dependency>

    <!-- Mapping -->
    <dependency><groupId>org.mapstruct</groupId><artifactId>mapstruct</artifactId></dependency>

    <!-- Monitoring -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>io.micrometer</groupId><artifactId>micrometer-registry-prometheus</artifactId></dependency>

    <!-- Testing -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>postgresql</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>junit-jupiter</artifactId><scope>test</scope></dependency>
</dependencies>
```

Note: `postgis-jdbc` + `hibernate-spatial` versions need to be pinned to
match your Postgres/PostGIS versions — left exact version numbers out since
they should be resolved against whichever Postgres/PostGIS image version
you settle on for prod, not guessed here.

## 2. What "Modular Monolith" means for the codebase

One deployable Spring Boot artifact, but internally organized as **strongly
bounded modules** that talk to each other through explicit interfaces, not
by reaching into each other's repositories/entities. This is what makes the
later "migrate to microservices if needed" path (mentioned in ADR-005)
actually feasible instead of theoretical.

Two rules that make this real, not just folders with good intentions:

1. **A module's `repository` and `entity` packages are package-private
   (or at least never imported) outside that module.** Other modules only
   call a module's public `Service` interface (or its `api` sub-package —
   see below). This is the single most important rule — skipping it is how
   "modular monoliths" quietly become "big balls of mud" in 6 months.
2. **Cross-module communication for anything asynchronous/side-effecty
   (e.g. "when a property is approved, send a notification and update the
   tax record") goes through in-process domain events**, not direct service
   calls chained together. Spring's `ApplicationEventPublisher` /
   `@TransactionalEventListener` is enough — no message broker needed at
   this stage.

## 3. Top-level project structure

```
endrms-backend/
├── pom.xml                        # or build.gradle.kts — see open question #1
├── docker-compose.yml             # local dev: postgres+postgis, redis, app
├── Dockerfile
├── .env.example
├── README.md
│
├── src/main/java/et/gov/endrms/
│   ├── EndrmsApplication.java
│   │
│   ├── shared/                    # "shared kernel" — see §4
│   │   ├── config/
│   │   ├── security/
│   │   ├── exception/
│   │   ├── response/              # standard API envelope
│   │   ├── audit/                 # cross-cutting audit trail (FR-GOV-003, NFR-AUD-001)
│   │   ├── event/                 # base domain event classes
│   │   ├── validation/
│   │   └── util/
│   │
│   ├── identity/                  # renamed from "authentication"+"users" — see note below
│   │   ├── api/                   # interfaces other modules are allowed to call
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   ├── security/              # JWT filter, token provider (module-specific parts)
│   │   └── config/
│   │
│   ├── property/                  # FR-PROP-*
│   │   ├── api/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   └── config/
│   │
│   ├── verification/              # FR-GOV-001, FR-GOV-002 (government review workflow)
│   │   ├── controller/ service/ repository/ entity/ dto/
│   │
│   ├── search/                    # FR-SEARCH-* — PostGIS queries live here, reads-only
│   │   ├── controller/ service/ repository/ dto/
│   │
│   ├── rental/                    # FR-RENT-* — applications, tenant selection
│   │   ├── controller/ service/ repository/ entity/ dto/
│   │
│   ├── agreement/                 # digital rental agreements + gov approval (FR-GOV-004)
│   │   ├── controller/ service/ repository/ entity/ dto/
│   │
│   ├── payment/                   # FR-PAY-*
│   │   ├── controller/ service/ repository/ entity/ dto/
│   │   └── provider/              # adapter interface + implementations per gateway
│   │
│   ├── taxation/                  # FR-TAX-*
│   │   ├── controller/ service/ repository/ entity/ dto/
│   │
│   ├── notification/              # FR-NOTIF (5.8) — listens to domain events from other modules
│   │   ├── service/ repository/ entity/ dto/
│   │   └── channel/                # email/SMS/push adapters
│   │
│   ├── reporting/                 # 5.9 — mostly read-side, may use projections/views
│   │   ├── controller/ service/ dto/
│   │
│   ├── audit/                     # FR-GOV-003 / NFR-AUD-001 as its own queryable module
│   │   ├── controller/ service/ repository/ entity/
│   │
│   └── integration/                # NFR-INT-001 — anti-corruption layer for external systems
│       ├── landregistry/          # future
│       ├── nationalid/            # future
│       ├── digitalsignature/      # future
│       └── config/
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-staging.yml
│   ├── application-prod.yml
│   └── db/migration/              # Flyway: V1__init.sql, V2__..., etc.
│
├── src/test/java/et/gov/endrms/
│   ├── <module>/                  # unit tests mirror main structure
│   └── integration/                # Testcontainers-based module + API tests
│
└── infra/
    ├── nginx/
    ├── prometheus/
    ├── grafana/
    └── k8s/                        # if/when you outgrow docker-compose
```

### Local dev `docker-compose.yml` sketch

```yaml
services:
  postgres:
    image: postgis/postgis:16-3.4      # pin exact tag once decided
    environment:
      POSTGRES_DB: endrms
      POSTGRES_USER: endrms
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes: [pgdata:/var/lib/postgresql/data]
    ports: ["5432:5432"]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    volumes: [miniodata:/data]
    ports: ["9000:9000", "9001:9001"]   # 9000 = API, 9001 = console

  app:
    build: .
    depends_on: [postgres, redis, minio]
    env_file: .env
    ports: ["8080:8080"]

volumes:
  pgdata:
  miniodata:
```

Flyway migrations run automatically on app startup against `postgres`
(default Spring Boot behavior once `flyway-core` is on the classpath and
`spring.flyway.enabled=true`). Migration files live in
`src/main/resources/db/migration/` as `V1__init_schema.sql`,
`V2__add_property_geom_index.sql`, etc. — one file per module's initial
table set is a reasonable starting convention (`V1__identity_schema.sql`,
`V2__property_schema.sql`...) so migration history stays traceable to a
module, which matters given the audit requirements in the SRS.

### Note on module naming
The SRS lists `authentication` and `users` as separate top-level modules.
I'd merge these into a single `identity` module. Reason: ADR-008 explicitly
says one account can carry multiple roles and identity is authenticated
once — splitting "login" from "who is this user" across two modules just
creates a chatty dependency between them for no benefit. If you'd rather
keep them literally as the SRS lists them, that's a one-word rename, not
a structural problem — happy to do it that way if there's a reason (e.g.
a diagram elsewhere in the org already refers to them separately).

## 4. Inside a typical module (e.g. `property/`)

```
property/
├── api/
│   └── PropertyLookupApi.java      # narrow interface exposed to other modules
├── controller/
│   └── PropertyController.java     # @RestController, /api/v1/properties
├── service/
│   ├── PropertyService.java        # interface
│   └── PropertyServiceImpl.java
├── repository/
│   └── PropertyRepository.java     # Spring Data JPA
├── entity/
│   ├── Property.java
│   └── PropertyStatus.java         # enum: DRAFT, PENDING_REVIEW, APPROVED, REJECTED, SUSPENDED
├── dto/
│   ├── request/
│   │   ├── CreatePropertyRequest.java
│   │   └── UpdatePropertyRequest.java
│   └── response/
│       └── PropertyResponse.java
├── mapper/
│   └── PropertyMapper.java         # MapStruct: entity <-> DTO
├── event/
│   ├── PropertySubmittedEvent.java
│   └── PropertyApprovedEvent.java  # consumed by notification, taxation modules
└── config/
    └── PropertyModuleConfig.java   # optional, module-scoped beans
```

`verification`, `rental`, `agreement`, `payment`, `taxation` all follow the
same shape. `notification`, `audit`, and `reporting` are lighter — mostly
`service` + `repository`, since they're largely event consumers or
read-models rather than owning a primary business workflow.

## 5. Why `shared/` and not a generic `common/` grab-bag

`shared/` is deliberately narrow — it should only ever contain things every
module genuinely needs (the response envelope, the exception hierarchy, the
JWT filter, base audit annotations). It's the one package every module is
allowed to depend on. If something in `shared/` starts encoding business
rules (e.g. a "PropertyStatus" enum), that's a sign it belongs in a module,
not here — this is the most common way modular monoliths erode over time.

## 6. Mapping SRS non-functionals to concrete choices

| NFR | Concrete implementation |
|---|---|
| NFR-PERF-001 (Redis caching) | `@Cacheable` on search/property-read paths in `search/` and `property/` |
| NFR-PERF-001 (image handling) | Object storage adapter in `shared/` or its own `media/` module — images never touch Postgres |
| NFR-REL-001 (transactions) | `@Transactional` boundaries at service layer; agreement approval flow is the canonical example from the SRS itself |
| NFR-REL-001 (backups) | Handled at infra level (managed Postgres or pg_dump cron + WAL archiving), not app code |
| NFR-AUD-001 | `shared/audit` — an `@Auditable`-style annotation + AOP aspect, or a `AuditLogService` called explicitly from each module's approval/decision points |
| NFR-SEC-* | Spring Security filter chain in `identity/security`, method-level `@PreAuthorize` per role in each controller |
| NFR-INT-001 | `integration/` module — nothing in `property`, `agreement`, etc. should ever call an external government API directly |
