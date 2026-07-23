# ENDRMS — Full Project Architecture

Companion to `ENDRMS-backend-architecture.md` (backend detail lives there).
This doc covers how the backend, web app, mobile app, and infra fit together
as one system, and how the repos/folders should be organized end to end.

The system diagram above shows the runtime shape: Next.js web + Flutter
mobile → Nginx → Spring Boot modular monolith → PostgreSQL/PostGIS, Redis,
MinIO. Everything below is about how that gets built and shipped.

## 1. Repo strategy: monorepo

One repository, with backend/web/mobile/infra as top-level workspaces.
Simpler day-to-day for a team building all three together, and it makes
the API-contract sync in §4 easier since a single PR can update the
backend controller, regenerate the client, and update the consuming
frontend code together.

```
endrms/
├── backend/         # Spring Boot — see ENDRMS-backend-architecture.md
├── web/               # Next.js
├── mobile/            # Flutter
├── infra/             # docker-compose, k8s manifests, nginx, monitoring
├── contracts/         # published openapi.json + generated clients — see §4
└── .github/workflows/ # CI, path-filtered per workspace — see §6
```

The internal structure of each workspace below (§2–§5) is unchanged
whether it's a folder in this monorepo or its own repo — only the
top-level wrapper changes. So if you ever do want to split a workspace
out later (e.g. mobile goes to a separate contractor), it's a `git
subtree split`, not a rewrite.

## 2. `web/` (Next.js)

Feature-organized to mirror the backend's modules — this matters because
whoever's working a feature end-to-end shouldn't have to relearn a
different taxonomy on each side of the stack.

```
web/
├── package.json
├── next.config.ts
├── src/
│   ├── app/                        # Next.js App Router
│   │   ├── (public)/               # unauthenticated: search, property details
│   │   │   ├── properties/
│   │   │   └── page.tsx
│   │   ├── (tenant)/               # tenant-only routes, role-guarded
│   │   │   ├── applications/
│   │   │   ├── agreements/
│   │   │   └── payments/
│   │   ├── (owner)/                # property owner routes
│   │   │   ├── properties/
│   │   │   └── applications/
│   │   ├── (government)/           # gov officer dashboard — verification, approvals
│   │   │   ├── verification/
│   │   │   └── reports/
│   │   ├── (admin)/                 # system admin
│   │   ├── auth/                    # login, register, password recovery
│   │   └── layout.tsx
│   │
│   ├── features/                    # business logic per domain, mirrors backend modules
│   │   ├── identity/                 # auth state, role checks
│   │   ├── property/
│   │   │   ├── api.ts                # calls generated API client
│   │   │   ├── hooks.ts              # data-fetching hooks
│   │   │   └── components/
│   │   ├── verification/
│   │   ├── rental/
│   │   ├── agreement/
│   │   ├── payment/
│   │   └── search/
│   │
│   ├── components/                   # shared/presentational UI, not feature-specific
│   ├── lib/
│   │   ├── api-client/                # generated from OpenAPI spec — see §4
│   │   ├── auth/                      # JWT storage/refresh handling
│   │   └── utils/
│   └── middleware.ts                  # route guards by role (RBAC on the frontend too)
└── public/
```

**Still open:** data-fetching/state library for `features/*/hooks.ts`.
Not decided yet, so I've left that file as a placeholder rather than
guessing — TanStack Query is the common default pairing with Next.js for
server-state (properties, applications, etc.), but SWR or plain fetch +
Server Components are both reasonable too depending on how much
client-side interactivity each screen needs. Let me know when you've
picked one and I'll fill in the actual hook implementations.

## 3. `mobile/` (Flutter, Riverpod)

Feature-first + layered within each feature (data / domain /
presentation) — this is the standard pattern for Flutter apps past
toy-app size, and it maps cleanly onto the same backend modules again.
State management is Riverpod throughout.

```
mobile/
├── pubspec.yaml
├── lib/
│   ├── main.dart
│   ├── app/
│   │   ├── router.dart              # go_router, role-based route guards
│   │   └── theme.dart
│   ├── core/
│   │   ├── network/                  # Dio client, interceptors (JWT attach/refresh)
│   │   ├── storage/                  # secure token storage
│   │   ├── error/
│   │   └── widgets/                  # shared UI components
│   ├── features/
│   │   ├── identity/
│   │   │   ├── data/                 # API client (generated), repositories
│   │   │   ├── domain/               # entities, use cases
│   │   │   └── presentation/         # screens + Riverpod providers
│   │   ├── property/
│   │   │   ├── data/ domain/ presentation/
│   │   ├── search/
│   │   ├── rental/
│   │   ├── agreement/
│   │   ├── payment/
│   │   └── notification/
│   └── generated/                    # OpenAPI-generated Dart client — see §4
└── test/
```

Each feature's `presentation/` layer holds its Riverpod providers
(`propertyListProvider`, `agreementDetailProvider`, etc.) alongside the
screens that consume them — keeps state scoped to the feature that owns
it rather than one global provider file.

## 4. Keeping backend, web, and mobile in sync: the API contract

Being in one repo doesn't remove the need for a real contract between
Java, TypeScript, and Dart — it just makes keeping it in sync easier,
since a single PR can touch the controller, the regenerated client, and
the consuming frontend code together.

1. **Spring Boot is the source of truth.** springdoc-openapi (already in
   the backend dependency list) auto-generates `openapi.json` from the
   actual controllers/DTOs — not hand-maintained.
2. **Publish it into `contracts/`** — a CI step regenerates
   `contracts/openapi.json` whenever `backend/` changes, and fails the
   build if `web/` or `mobile/` weren't regenerated to match (a simple
   "does the generated client differ from what's committed" check).
3. **Generate clients from it, don't hand-write them:**
   - Web: `openapi-typescript` → typed fetch client in `web/src/lib/api-client/`
   - Mobile: `openapi-generator` (Dart client) → `mobile/lib/generated/`
4. This turns "the mobile app broke because the backend renamed a field"
   from a runtime bug into a compile-time error in the generated client —
   worth setting up early rather than retrofitting later.

## 5. `infra/`

```
infra/
├── docker-compose.yml          # full local stack: backend + web + postgres + redis + minio + nginx
├── nginx/
│   ├── nginx.conf
│   └── conf.d/
├── monitoring/
│   ├── prometheus/
│   └── grafana/
│       └── dashboards/
├── k8s/                        # once you outgrow docker-compose — see open question below
│   ├── base/
│   └── overlays/{dev,staging,prod}/
└── ci/
    └── shared-workflows/         # reusable CI steps if using GitHub Actions
```

## 6. CI/CD (sketch — needs your input, see below)

Monorepo means one thing to get right immediately: **path-filtered
workflows**, or every PR triggers a full backend+web+mobile build even
if it only touched one line of Dart. Assuming GitHub Actions (swap the
syntax if you're on GitLab CI or Azure DevOps — the stages are the same):

```
# .github/workflows/backend.yml — triggers only on changes under backend/**
# .github/workflows/web.yml      — triggers only on changes under web/**
# .github/workflows/mobile.yml   — triggers only on changes under mobile/**
# .github/workflows/contracts.yml — triggers on backend/** changes,
#                                    regenerates contracts/ and fails
#                                    the build if web/mobile clients are stale

on:
  push:
    paths: ['backend/**']
jobs:
  lint-and-test:      # unit tests; Testcontainers-based integration tests for backend
  build:                # mvn package / next build / flutter build
  docker-build-push:    # backend + web only; tag with git SHA
  deploy:                # triggered on merge to main → staging; manual/tag → prod
```

Mobile adds a separate release lane (Play Store / App Store builds via
`flutter build appbundle` / `flutter build ipa`, likely gated behind
manual approval rather than auto-deploy per merge).

## 7. Environments

Standard three-tier setup fits what the SRS describes (NFR-INFRA-001's
hybrid/multi-provider requirement, 99.9% uptime target):

| Environment | Purpose | Notes |
|---|---|---|
| `dev` | Local + shared dev instance | docker-compose is enough |
| `staging` | Pre-prod, government + QA sign-off before features go live | Should mirror prod topology |
| `prod` | Addis Ababa launch, scaling to national | Needs the HA/load-balancing tactics from NFR-AVAIL-001 |

## 8. Still open — genuinely needs your input, not guessable from the SRS

Resolved so far: monorepo (§1), Flutter state management → Riverpod (§3).

1. **Deployment target** — carried over from the backend doc: bare
   Docker on VMs, managed Kubernetes, or a specific cloud (the SRS
   insists on infra-agnostic, but you still need a first target to build
   the actual `k8s/` overlays or decide to skip Kubernetes for now).
2. **CI/CD platform** — GitHub Actions assumed above; confirm or correct.
3. **Web state/data-fetching library** (§2) — not yet decided; the
   `features/*/hooks.ts` files are placeholders until this is picked.
