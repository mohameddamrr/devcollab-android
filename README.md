# DevCollab

DevCollab is an Android application that searches for GitHub developers, displays
paginated results, and opens a detailed public profile. It currently represents the
complete standalone VOIS internship-assignment baseline and now includes the rich
public GitHub profile delivered in Phase 8. Later phases will add authenticated
collaboration features.

## VOIS baseline features

- Kotlin and Jetpack Compose with Material 3
- GitHub user search using `GET /search/users`
- Debounced search with manual keyboard/search-button submission
- Result cards containing avatar, login, numeric GitHub ID, and account type
- Developer details using `GET /users/{username}`
- Paging 3 pagination with loading, error, retry, and end-of-results handling
- Room persistence for the last successful query and its ordered results
- Automatic restoration after the app is closed and reopened
- Offline cached fallback with a visible stale-data message
- Unit, networking, Room, Paging, and Compose UI tests

## Rich GitHub profile features

- Overview, Repositories, and Activity tabs
- Public repository cards with external GitHub links
- Incremental repository loading plus local search and Recent/Popular/Name sorting
- Top-repository overview based on stars among repositories loaded so far
- Repository-language summary based on primary-language repository counts
- Recent public GitHub activity and a bounded “Most Active Recently” repository summary
- Pull-to-refresh with independent profile, repository, and activity states

## Architecture

The baseline follows MVVM, repository boundaries, and unidirectional data flow:

```text
Compose UI
    | user events
    v
ViewModel -- immutable StateFlow --> Compose UI
    |
    v
DeveloperRepository
    |
    +-- GitHub remote data source (Retrofit + OkHttp)
    |
    +-- RemoteMediator --> Room DAO --> PagingSource
```

The UI does not call Retrofit or Room. ViewModels own screen state and send work to the
repository. GitHub remains authoritative for public developer data; Room stores an
offline snapshot and is the Paging source displayed by Compose.

The detailed product plan and architecture decisions are in
[`docs/PROJECT_PLAN.md`](docs/PROJECT_PLAN.md).

## Requirements

- Android Studio with JDK 11 or newer supported by the configured Android Gradle Plugin
- Android SDK matching the versions declared in `app/build.gradle.kts`
- An emulator or Android device running API 24 or newer
- Internet access for fresh GitHub searches

No API secret is required for the current unauthenticated GitHub baseline. GitHub's
unauthenticated REST API rate limit still applies. Do not add access tokens or secrets
to source control.

## Run the app

1. Clone the repository and open it in Android Studio.
2. Allow Gradle sync to finish.
3. Start an emulator or connect an Android device.
4. Select the `app` run configuration and press **Run**.
5. Search for a GitHub login or term such as `android`.

To verify offline restoration, complete a search, close the app, disable the device's
network connection, and reopen it. The previous query and cached results should appear
with a cached-data banner. Uninstalling the app or clearing its storage deletes Room.

## Tests

Run JVM unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Run Room, Paging, and Compose instrumented tests on a connected emulator/device:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Run the baseline validation set:

```powershell
.\gradlew.bat testDebugUnitTest connectedDebugAndroidTest assembleDebug lintDebug
```

## Current limitations

- GitHub API requests are unauthenticated and therefore have a relatively low rate limit.
- Cached search results may become stale and are clearly labelled when used offline.
- Public GitHub data is evidence of public activity, not proof of professional skill.
- Repository-language percentages count loaded repositories; they are not byte-level
  language statistics or proficiency scores.
- Activity uses up to 30 recent public GitHub events and is not lifetime contribution
  history. GitHub may delay public events and does not expose private activity.
- Authentication, collaboration profiles, evidence-based discovery, saved developers,
  and collaboration requests belong to later project phases.
