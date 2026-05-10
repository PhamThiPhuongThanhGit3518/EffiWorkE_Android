# AI AGENT GUIDELINES: EFFIWORK ANDROID (MVVM + MVI)

## 1. Project Context

* **Project Name:** EffiWork_Android
* **Platform:** Android (Kotlin)
* **Architecture:** MVVM + MVI Pattern (State, Effect, Intent)
* **UI Framework:** Jetpack Compose + Material 3 + Hilt

---

## 2. Directory Structure (Strict Adherence)

The Agent must follow this folder structure exactly:

```text
EffiWork_Android/app/src/main/java/com/phuongthanh/effiwork_android/
├── api/                    # Retrofit interfaces
├── data/
│   ├── local/              # Room/DataStore
│   ├── model/
│   │   ├── request/        # Request DTOs
│   │   └── response/       # Response DTOs
│   └── repository/         # Repository Implementations
├── di/                     # Hilt Modules
├── ui/
│   ├── common/             # Reusable components
│   ├── screen/             # Feature-based screens (home, login, projects, etc.)
│   └── theme/              # Colors, Type, Theme
└── viewmodel/              # Feature-based ViewModels (State, Effect, Intent)
```

---

# 3. Workflow (Planner → Developer → Tester)

## Phase 1: PLANNER (Pre-coding Analysis)

Before writing any business logic, the Agent must output:

### File Checklist

List all files to be created/modified.

### MVI Definition

Define:

* UiState
* UiEffect
* UiIntent

for the feature.

### Strings

List all new keys to be added to:

```text
res/values/strings.xml
```

### API Requirements

Identify necessary Retrofit endpoints.

---

## Phase 2: DEVELOPER (Implementation)

### ViewModel

* Use `MutableStateFlow` for State.
* Use `Channel` for Effects.

### Repository

* All network/local calls must use `Flow`.
* Emit:

```kotlin
Result.Success
Result.Error
```

### UI

* 100% Jetpack Compose.
* Use:

```kotlin
MaterialTheme.colorScheme
```

### Logic

* NO heavy logic inside Composables.
* Sorting/filtering/business logic must stay inside ViewModel.

---

## Phase 3: TESTER (Mandatory)

For every new screen/feature, the Agent MUST generate:

### Unit Test (`test/`)

Test:

* ViewModel state transitions
* Repository logic

Use:

* MockK

### UI Test (`androidTest/`)

Create Compose UI tests to verify:

* UI elements display correctly
* User interactions trigger proper Intent

Examples:

```kotlin
Verify CircularProgressIndicator shows when isLoading = true
```

```kotlin
Verify clicking a button triggers the correct Intent
```

---

# 4. UI Rules & Coding Standards

## 4.1 No Hard-coded Strings

❌ Bad:

```kotlin
Text("Login")
```

✅ Good:

```kotlin
Text(stringResource(R.string.btn_login))
```

---

## 4.2 Theme & Preview

Requirements:

* Support both Light and Dark mode.
* Every Composable must include:

   * `@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)`
   * `@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)`

---

## 4.3 Edge-to-Edge & Layout

Use:

```kotlin
systemBarsPadding()
```

or:

```kotlin
Scaffold(contentWindowInsets = ...)
```

### TabRow Customization

* Indicator width: `32.dp`
* Indicator aligned center
* Unselected tab alpha: `0.6f`

---

## 4.4 Concurrency

Requirements:

* Network/DB operations MUST use:

```kotlin
Dispatchers.IO
```

* Use:

```kotlin
viewModelScope.launch
```

---

# 5. Required Pre-commit Verification

Before finishing, the Agent must confirm:

* [ ] Is the architecture MVVM (Data -> Repository -> ViewModel -> UI)?
* [ ] Are all strings in `strings.xml`?
* [ ] Does it have `@Preview` for Dark Mode?
* [ ] Is there an `androidTest` for UI and `test` for ViewModel?
* [ ] No `java.io.File` or blocking calls on the Main Thread?
