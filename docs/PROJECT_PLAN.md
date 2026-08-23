# DevCollab Project Plan

## Product goal

DevCollab begins with the VOIS GitHub user-search assignment and evolves into an
evidence-based developer collaboration discovery application. It uses public GitHub
repositories, languages, topics, contributors, and recent public activity to explain
why a developer may be relevant. Repository evidence must never be presented as
verified professional skill or expertise.

The implementation uses Kotlin, Jetpack Compose, Material 3, MVVM, unidirectional
data flow, coroutines, `StateFlow`, Retrofit/OkHttp, Room, Paging 3, Firebase
Authentication with GitHub OAuth, Cloud Firestore, and meaningful automated tests.

## Delivery principles

- Complete and protect the standalone VOIS baseline before advanced features.
- Work in focused, buildable phases and do not carry broken code forward.
- Explain each important Android/Kotlin concept and data flow as it is introduced.
- Keep UI, presentation, domain rules, and data access responsibilities separate.
- Add abstractions and packages only when a current feature needs them.
- Treat GitHub numeric user ID as the stable GitHub identity; login may change.
- Keep GitHub/Firebase credentials and tokens out of source control.
- Use deterministic, testable matching and show the evidence behind every match.

## Architecture

```text
Compose UI
  renders immutable state and emits events
        |
        v
ViewModel
  owns screen state and coordinates actions
        |
        v
Repository / pure domain rules
  maps data, selects sources, applies cache/fallback policy
   /              |              \
  v               v               v
GitHub API     Firebase        Room
public data    app-owned       local/cache
               remote data     data
```

### Layer boundaries

- **UI:** Compose screens/components. No HTTP, SQL, Firestore, or matching logic.
- **Presentation:** Screen ViewModels exposing immutable `StateFlow` UI state.
- **Domain:** Stable models and pure matching/request-transition business rules.
- **Data:** Remote/local sources, mappers, and repository implementations.
- **Core:** Shared platform/UI utilities only after more than one feature needs them.

The project remains a single Gradle app module unless measured complexity or build
performance later justifies modularization.

## Data flow

```text
UI event
  -> ViewModel
  -> repository/domain operation
  -> GitHub, Firestore, or Room
  -> mapped result with freshness/error metadata
  -> immutable UI state
  -> lifecycle-aware Compose collection and recomposition
```

Offline GitHub flow:

```text
GitHub request fails
  -> query Room
  -> cached data: show it with timestamp/stale indication and Retry
  -> no cached data: show a typed error and Retry
```

Significant screens must cover initial, loading, success, empty, error, cached/offline,
pagination-loading, and pagination-error states where applicable.

## Source-of-truth ownership

| Data | Source of truth | Room responsibility |
|---|---|---|
| GitHub identity/profile | GitHub | Cache/offline copy |
| Search results | GitHub | Last-search snapshot |
| Repositories/topics/languages | GitHub | Bounded cache |
| Contributors/activity | GitHub | Optional short-lived evidence cache |
| Authentication session | Firebase Auth | None beyond Firebase SDK persistence |
| App member/collaboration profile | Firestore | Optional offline read cache later |
| Collaboration requests/status | Firestore | Optional offline presentation only |
| Recent searches/recently viewed | Room | Authoritative local data |
| Saved GitHub developers | Room initially | Authoritative unless sync is added later |

## Target package direction

Packages are introduced only when their phase begins.

```text
com.mohamedamr.devcollab
|-- app/
|   |-- MainActivity.kt
|   |-- DevCollabApp.kt
|   `-- navigation/
|-- feature/
|   |-- discover/
|   |-- developerprofile/
|   |-- saved/
|   |-- auth/
|   |-- myprofile/
|   |-- requests/
|   `-- projectdiscovery/
|-- domain/
|   |-- model/
|   |-- matching/
|   `-- collaboration/
|-- data/
|   |-- github/
|   |   |-- remote/
|   |   |-- local/
|   |   |-- mapper/
|   |   `-- repository/
|   `-- collaboration/
|       |-- firebase/
|       `-- repository/
|-- database/
|   |-- dao/
|   `-- entity/
`-- core/
    |-- designsystem/
    |-- network/
    |-- common/
    `-- testing/
```

## Dependency plan

Exact mutually compatible stable versions are verified immediately before each
Gradle change. Version declarations remain centralized in `libs.versions.toml`.

- Foundation: Compose BOM, Material 3, Navigation Compose, Lifecycle Compose/ViewModel.
- Networking: Retrofit, OkHttp, kotlinx.serialization, serialization converter.
- Images: Coil Compose.
- Persistence/paging: Room, KSP, Paging runtime/Compose, optional Room-Paging bridge.
- Backend: Firebase BOM, main `firebase-auth` and `firebase-firestore` modules.
- Testing: JUnit, coroutine-test, Room test support, Paging tests, Compose UI tests,
  MockWebServer/fakes, and Firebase Emulator/rules tests.
- DI: constructor/manual wiring first; reconsider Hilt when the object graph warrants it.

## Specialized agent ownership

1. **Orchestrator/Tech Lead:** requirements, milestones, integration, validation,
   progress, and architecture consistency.
2. **Android Architecture:** MVVM boundaries, packages, UDF/StateFlow, navigation,
   lifecycle, repository contracts, and DI decisions.
3. **Compose/UI:** Material 3 screens, reusable components, accessibility, layouts,
   navigation UI, and complete visual state handling.
4. **GitHub/Data:** REST endpoints, Retrofit/OkHttp, DTOs, errors, rate limits,
   pagination, contributors, activity, and bounded API use.
5. **Matching/Discovery:** deterministic aggregation, deduplication, ranking,
   evidence generation, and matching tests.
6. **Firebase/Auth:** GitHub OAuth, member profiles, Firestore schema, requests,
   rules, and credential safety.
7. **Room/Offline:** entities, DAOs, last-search restoration, cache timestamps,
   recent/saved data, and source-of-truth boundaries.
8. **Testing/QA:** meaningful unit/integration/UI tests, edge cases, and regression checks.
9. **Security/Quality:** privacy, secrets, Firestore rules, coroutine/lifecycle issues,
   nullability, architecture violations, and API efficiency.
10. **Git/Documentation:** commits, README, diagrams, setup, limitations, and
    interview/presentation readiness.

### Orchestration

```text
Milestone and acceptance criteria
  -> responsible specialist proposes contract/data shape
  -> architecture validation
  -> explicitly owned, non-overlapping implementation
  -> build and tests
  -> QA and security review as appropriate
  -> orchestrator reviews integrated diff
  -> explanation and user checkpoint
  -> stable Git commit
```

Parallel work is used only for independent areas. Dependency order is preserved; for
example, repository contracts precede ViewModels and state contracts precede UI work.

## Initial Room proposal

Only the mandatory last-search persistence is introduced initially:

```text
LastSearchEntity
  id = 1
  query
  updatedAt
  totalCount

CachedSearchResultEntity
  normalizedQuery
  position
  githubUserId
  login
  avatarUrl
  accountType
  githubUrl
  cachedAt
```

The result key includes normalized query and GitHub ID, with an index on query and
position. First-page replacement is transactional; appended pages extend the ordered
snapshot. Later migrations may add recent searches, recently viewed developers, saved
developers, profile cache, and bounded repository/activity caches.

## Initial Firestore proposal

Firestore is not implemented until its phase.

```text
members/{firebaseUid}
  githubUserId, githubLogin, displayName, avatarUrl
  collaborationBio, available, interests, collaborationTypes
  remotePreference, optionalLocation, optionalContact
  createdAt, updatedAt

githubIdentityLinks/{githubUserId}
  firebaseUid, createdAt

collaborationRequests/{requestId}
  senderUid, receiverUid
  senderGithubUserId, receiverGithubUserId
  projectName, projectDescription, technologies
  collaborationType, requestedRole, expectedCommitment
  message, evidenceReasons, status
  createdAt, updatedAt
```

One central request collection is queried by sender or receiver. Receiver-only
transitions are `PENDING -> ACCEPTED|DECLINED`; sender-only transition is
`PENDING -> CANCELLED`. Terminal states cannot transition again. Rules must validate
ownership, allowed fields, immutable context, and query access. Globally race-free
GitHub identity linking may require a trusted Firebase function if authenticated token
claims cannot enforce it; that scope decision requires approval.

## Testing strategy

- JVM: mappers, query normalization, matching, evidence, freshness, request state machine.
- ViewModel: loading/success/empty/error/cached state sequences with fake repositories.
- Repository: remote success/cache write and remote failure/cache fallback.
- Room: ordered inserts, transactional replacement, query isolation, restoration.
- Paging: keys, append/retry, duplicates, errors, and end-of-data.
- Compose: a small set of meaningful state-rendering and user-flow tests.
- Firebase: Emulator Suite security-rule tests plus pure request-transition tests.
- Manual: process death, relaunch, airplane mode, rate limit, OAuth cancellation,
  logout, intents, back navigation, and device/layout checks.

## Git strategy

- Keep `main` buildable and never commit credentials or generated outputs.
- Use focused feature branches when useful and focused conventional commits.
- Run relevant build/tests before each stable checkpoint.
- Create a protected Phase 7 VOIS baseline checkpoint/tag.
- Keep later collaboration work from changing the baseline acceptance tests.

## Milestones

### Phase 0 - Discovery and planning

Inspect the project/toolchain/Git state; confirm requirements; define architecture,
agents, ownership, tests, risks, and completion criteria. **Status: complete.**

### Phase 1 - Android foundation

Verify the generated toolchain, align foundation dependencies, establish the Material 3
application/navigation shell and placeholder Discover, Requests, Saved, and Profile
destinations, then build and test. **Status: implementation and automated validation
complete, including installation and navigation verification on the emulator.**

### Phase 2 - GitHub networking

Add Internet permission, Retrofit/OkHttp/serialization, GitHub headers, DTOs, search and
detail endpoints, typed failures/rate-limit metadata, and focused API tests. **Status:
complete and committed.**

### Phase 3 - Core MVVM search

Build the first vertical slice: repository, immutable search state, ViewModel, search
UI, result cards, and initial/loading/success/empty/error behavior. **Status:
complete and committed. Search now also starts automatically after the user pauses
typing, using a rate-limit-conscious debounce.**

### Phase 4 - Developer details

Navigate from results, call `/users/{username}`, show nullable-safe rich details, and
support native open/share actions. Mandatory VOIS behavior is now functional.
**Status: implementation and local automated validation complete; awaiting commit
approval. The connected emulator suite is compiled, but the current emulator image
crashes its instrumentation process intermittently.**

### Phase 5 - Pagination

Use Paging 3 for first/next pages, loading, append error/retry, duplicate prevention,
and end-of-results. Begin with remote paging instead of an early `RemoteMediator`.

### Phase 6 - Room persistence

Persist and restore the last successful query and ordered results. Demonstrate full
app close/reopen behavior, then add recent searches/recently viewed where appropriate.

### Phase 7 - VOIS tests and protected checkpoint

Complete ViewModel, repository, DAO, and paging tests. Demonstrate and freeze the
standalone VOIS baseline before advanced product work.

### Phase 8 - Rich GitHub profile

Add Overview/Repositories/Activity tabs, top repositories, repository sorting/search,
recent events, bounded language summaries, pull-to-refresh, and caching.

### Phase 9 - GitHub authentication

Configure Firebase GitHub OAuth, handle pending/cancelled sign-in, establish current
user mapping and authenticated GitHub access, and add logout without exposing secrets.

### Phase 10 - Collaboration profiles

Add availability, collaboration bio/interests/types, remote preference, optional
location/contact information, and correct current-user behavior.

### Phase 11 - Developer discovery

Add project requirements and specific-repository discovery; perform bounded repository
and contributor calls, deduplicate by GitHub ID, rank deterministically, explain
evidence, paginate candidates, and thoroughly test aggregation.

### Phase 12 - Collaboration requests

Add contextual send/received/sent workflows and validated Pending, Accepted, Declined,
and Cancelled transitions with restrictive Firestore rules and emulator tests. No chat.

### Phase 13 - Saved developers and offline polish

Complete save/unsave behavior, Saved screen, timestamps, stale-cache communication,
offline indicators, retry paths, and cache policy polish.

### Phase 14 - QA and UI polish

Review accessibility, layouts, state coverage, navigation, device behavior, API/Firebase
failures, Room fallback, and request workflows.

### Phase 15 - Documentation and presentation

Complete README, screenshots, diagrams, architecture/API/cache/matching explanations,
setup and test instructions, limitations, clean history, and internship presentation.

## Mandatory Phase 7 checkpoint

- [ ] Kotlin
- [ ] Jetpack Compose
- [ ] GitHub Search Users API
- [ ] Search result list with ID, avatar, login, and useful fields
- [ ] Details navigation and GitHub User Details API
- [ ] Pagination with loading/error/retry/end handling
- [ ] Room last-search persistence and relaunch restoration
- [ ] Meaningful unit/integration tests
- [ ] Clean Git repository and setup documentation

## Key risks and mitigations

- **GitHub rate limits:** inspect headers, cache, bound fan-out, avoid blind retries.
- **Contributor cost:** cap repositories/contributor pages and deduplicate before profiles.
- **OAuth configuration:** verify redirects/SHA/provider settings; never ship secrets.
- **Firestore security:** validate ownership and state transitions in rules and emulator tests.
- **Offline complexity:** start with a clear last-search snapshot and explicit freshness.
- **API limits:** public data is incomplete; recent events are not lifetime history.
- **Evidence accuracy:** label languages/topics/contributions as evidence, not proficiency.
- **Scope:** preserve Phase 7 as an independently demonstrable baseline.

## Definition of done

### VOIS baseline

The app builds from a clean checkout, searches and paginates GitHub users, displays the
required fields, loads user details, restores the last search from Room after relaunch,
renders all relevant UI states, passes meaningful tests, contains no secrets, and has
clear setup/architecture documentation.

### Expanded product

Authentication and identity distinctions are correct; rich GitHub screens use bounded
and cached requests; discovery is deterministic and evidence-based; saved/recent data
persists; collaboration requests obey tested Firestore rules; offline/rate-limit states
are explicit; accessibility and critical flows pass QA; the Phase 7 suite remains green;
and the README enables setup, demonstration, and an honest interview explanation.

## Decision log

- 2026-08-23: Keep one Gradle app module initially.
- 2026-08-23: Use feature-first presentation packages and source-oriented data packages.
- 2026-08-23: Start with manual constructor DI; reconsider Hilt when complexity appears.
- 2026-08-23: Implement remote PagingSource before considering Room RemoteMediator.
- 2026-08-23: Persist an explicit ordered last-search snapshot for the VOIS requirement.
- 2026-08-23: Use GitHub numeric ID for identity and login as mutable routing/display data.
