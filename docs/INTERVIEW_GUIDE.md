# Interview and Demo Guide

## Product story

The original task was a GitHub user-search app. I completed it with Kotlin, Compose, details navigation, Paging 3, Room restoration, and tests, then evolved it into DevCollab. DevCollab discovers collaborators using transparent public repository/contribution evidence. Firebase GitHub OAuth identifies registered members and Firestore securely stores collaboration profiles and requests.

## Explain the data flow

1. Compose emits an event.
2. The ViewModel coordinates work and updates immutable `StateFlow`.
3. A repository hides GitHub, Firestore, or Room.
4. DTOs/entities map to source-independent domain models.
5. Compose collects lifecycle-aware and recomposes.

The key decision is ownership: GitHub owns public evidence, Firestore owns collaboration data, and Room owns local persistence/cache behavior.

## Concepts to explain

- `data class`: immutable value/state holder with equality and `copy()`.
- Null safety: nullable GitHub fields use `?.`, `?:`, and `let`, not `!!`.
- `suspend`/coroutines: asynchronous work without blocking the main thread.
- `viewModelScope`: cancels ViewModel work when it is cleared.
- `Flow`/`StateFlow`: streams; StateFlow always has a current value.
- Repository: prevents ViewModels knowing HTTP, SQL, or Firestore details.
- DTO/entity/domain: external JSON, database row, and app concept.
- Recomposition: redraw caused by observed state changes, not a place for direct I/O.
- Paging: incremental loading with distinct initial and append states.
- OAuth: GitHub proves identity without the app handling a password.
- Firestore rules: server authorization; client checks alone are insufficient.

## Honest tradeoffs

- Manual DI keeps this graph understandable; Hilt can be added if complexity warrants it.
- Discovery is bounded, so it is relevant sampling rather than exhaustive ranking.
- Primary-language counts are explainable but less precise than byte-level calls per repo.
- Saved developers are local in V1.
- There is no chat; contact remains voluntary.

## Manual release checklist

- Search/debounce/no-results/error/retry and append loading/retry.
- Profile tabs, refresh, external links, and share sheet.
- Search, force-close, disable network, relaunch, and verify cached restoration.
- GitHub sign-in cancellation/success/restoration/sign-out.
- Complete/edit profile and verify current user says View My Profile.
- Verify GitHub-only profiles cannot receive requests.
- With two registered accounts, send, accept, decline, cancel, and reject repeated transitions.
- Save/unsave and verify persistence.
- Check small/large layouts, font scaling, accessibility, and dark theme.

## Screenshot checklist

Use real configured data; never fabricate evidence.

- Discover results
- Profile Overview and Repositories
- Activity / Most Active Recently
- Offline cached banner
- Requirements discovery and “Why this developer?”
- Collaboration profile
- Received and Sent requests
- Saved developers

Before publishing, remove private contact details, emails, tokens, and unintended account data.
