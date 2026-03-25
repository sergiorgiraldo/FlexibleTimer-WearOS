# FlexibleTimer-WearOS

> A timer for WearOS with sequential and group modes

## Use cases

Sequential mode: you can setup several timers which will run in sequence. Use it for exercises, to follow steps in a recipe, to follow activities in a art project, etc

Group mode: you can setup several timers which will run in parallel. Use it to track a set of dishes in your kitcken, to follow several colleagues doing exercises at the same time, etc

* Sets can be saved for later reuse and also edited

## Screenshots

<img src="https://raw.githubusercontent.com/sergiorgiraldo/FlexibleTimer-WearOS/refs/heads/main/Docs/i1.jpeg" width="200" /> <img src="https://raw.githubusercontent.com/sergiorgiraldo/FlexibleTimer-WearOS/refs/heads/main/Docs/i2.jpeg" width="200" /> <img src="https://raw.githubusercontent.com/sergiorgiraldo/FlexibleTimer-WearOS/refs/heads/main/Docs/i3.jpeg" width="200" /> 

<img src="https://raw.githubusercontent.com/sergiorgiraldo/FlexibleTimer-WearOS/refs/heads/main/Docs/i4.jpeg" width="200" /> <img src="https://raw.githubusercontent.com/sergiorgiraldo/FlexibleTimer-WearOS/refs/heads/main/Docs/i5.jpeg" width="200" /> <img src="https://raw.githubusercontent.com/sergiorgiraldo/FlexibleTimer-WearOS/refs/heads/main/Docs/i6.jpeg" width="200" />

<img src="https://raw.githubusercontent.com/sergiorgiraldo/FlexibleTimer-WearOS/refs/heads/main/Docs/i7.jpeg" width="200" />

## Project

### Structure

The project is a standard Gradle/Android Studio project. Key files:

| File | Purpose |
|---|---|
| `data/model/Models.kt` | `TimerEntry`, `SavedSequence`, `TimerRunState` sealed class |
| `data/repository/` | Room database, DAO, `SequenceRepository` |
| `service/TimerService.kt` | Foreground service — all timer logic, vibration patterns |
| `presentation/sequential/SequentialViewModel.kt` | Sequential state + service calls |
| `presentation/group/GroupViewModel.kt` | Group state + service calls |
| `ui/screens/` | All Wear Compose screens (Home, Menus, New, Saved, Running) |
| `MainActivity.kt` | SwipeDismissableNavHost wiring all screens together |
| `src/test/` | 5 test files, ~40 unit tests covering ViewModels, repository, models, timer logic |

### Key design decisions

* TimerService is a foreground service exposing a StateFlow<TimerRunState> as a process-wide singleton — the UI reacts to it from anywhere in the nav graph

* Double-tap uses Compose detectTapGestures(onDoubleTap) on the running screens
  
* Vibration: 3 long pulse on start, 2 long pulse after each non-final sequential timer, 3 long pulses at the end

* Group layout: 2-column symmetric grid for 4 timers, stacked for 2 or 3 timers

* Persistence: Room + Hilt DI; "Saved" button is conditionally shown only when items exist

### Build guide

`docs/BuildGuide.md`

Covers prerequisites (JDK 17, Android Studio, SDK), running unit tests, building debug and signed release APKs, and common errors.

### Deploy guide

`docs/DeployGuide.md` 

Covers enabling Developer Options on the watch, Wi-Fi ADB install (no cable needed), USB cable fallback, verifying install, updating the app, and an ADB quick reference.

### Signing the apk

key is in password manager:

to encode: 

```base64 -i flexibletimer.jks```

and store raw in Password Manager

to decode:

```base64 --decode -i fromPwdMgr -o flexibletimer.jks```
