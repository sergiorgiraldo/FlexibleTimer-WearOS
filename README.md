# FlexibleTimer-WearOS

> A timer for WearOS with sequential and group modes

## Use cases

Sequential mode: you can setup several timers which will run in sequence. Use it for exercises, to follow steps in a recipe, to follow activities in a art project, etc

Group mode: you can setup several timers which will run in parallel. Use it to track a set of dishes in your kitcken, to follow several colleagues doing exercises at the same time, etc

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
  
* Vibration: 1 short pulse on start, 1 short pulse after each non-final sequential timer, 3 short pulses at the end

* Group layout: 2-column symmetric grid (works for 2, 3, and 4 timers)

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

to decode:
```base64 --decode > flexibletimer.jks