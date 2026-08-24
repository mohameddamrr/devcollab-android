# DevCollab Architecture

## Why MVVM

Compose observes immutable UI state from a ViewModel. The ViewModel receives events and calls repository contracts; repositories hide Retrofit, Room, and Firestore.

```text
User -> Composable -> ViewModel -> Repository -> data source
          ^             |                        |
          +-------------+---- immutable StateFlow+
```

Without these boundaries, recomposition could repeat data work, UI would mix with business logic, and tests would require Android/network/database infrastructure.

## Packages

- `app/`: application graph, activity, navigation, and shell
- `feature/`: screens, UI state, ViewModels, and components
- `domain/model/`: source-independent concepts
- `domain/repository/`: contracts used by ViewModels
- `domain/matching/`: pure evidence aggregation/ranking
- `domain/collaboration/`: pure request transitions
- `data/github/`: Retrofit, DTOs, mapping, Paging, and repository
- `data/local/`: Room database, DAOs, entities, and migrations
- `data/firebase/`: authentication and Firestore implementations

Manual constructor injection keeps this internship-sized graph visible. Hilt becomes worthwhile only if graph size/scoping creates a real maintenance problem.

## Models

- DTO: mirrors external JSON.
- Entity: represents a Room row and persistence metadata.
- Domain model: source-independent app concept.
- UI state: presentation fields such as loading, selected tab, and messages.

Separation is used where it protects a boundary, without duplicating every tiny object.

## Search and offline flow

```text
query -> debounce -> SearchViewModel -> DeveloperRepository -> Paging/GitHub
                                              |
                                              +-> ordered Room snapshot
```

Paging owns page keys and initial/append states. Room stores the last successful query and ordered results, so restoration does not depend on ViewModel memory. When remote access fails, cached data is labelled stale; without cache, the UI exposes Retry.

## Profile efficiency

Search cards use the lightweight search response. Full `/users/{username}`, repository, and activity data load only when a profile is opened. Optional fields remain nullable and are hidden when absent. This prevents one detail call per visible result.

## Evidence discovery

```text
requirements -> bounded repository search -> bounded contributors
 -> deduplicate by GitHub ID -> aggregate evidence -> deterministic rank
```

Fan-out limits protect GitHub rate limits. Full profiles load only on selection. The UI explains relevant projects/technologies instead of presenting an unexplained percentage or claiming expertise.

## Firebase identity and privacy

The immutable GitHub numeric ID links GitHub identity to the Firebase user because logins can change.

```text
appUsers/{firebaseUid}          owner collaboration profile
publicMembers/{githubUserId}   minimal authenticated member lookup
collaborationRequests/{id}     sender/receiver-scoped request
```

The public directory contains only lookup information and availability. Optional contact information stays in the protected owner profile. Client validation improves feedback; Firestore rules are the security boundary.

## Request state machine

```text
Pending --receiver--> Accepted | Declined
Pending --sender----> Cancelled
```

Only Pending can transition. Terminal states cannot change again. Pure Kotlin and Firestore rules enforce the same constraints.

## Source-of-truth rules

- GitHub: public profiles, repositories, contributors, events
- Firestore: app profiles and collaboration requests
- Room: saved developers/local behavior and GitHub cache
- Firebase SDK: authentication session; tokens are not copied into Room

## Compose and lifecycle

ViewModels keep `MutableStateFlow` private and expose read-only state. Immutable `copy()` creates updates. Composables collect lifecycle-aware; recomposition redraws state rather than triggering arbitrary network work. `remember` is for recomposition-local retention, `rememberSaveable` for supported recreation, and `LaunchedEffect` for explicitly keyed side effects.

## Security checklist

- Never commit OAuth secrets, tokens, service accounts, or `google-services.json`.
- Publish and emulator-test `firestore.rules`.
- Compare immutable GitHub IDs for identity.
- Expose only intentionally supplied contact data.
- Bound API fan-out and handle rate limits.
- Validate request transitions in domain code and server rules.
