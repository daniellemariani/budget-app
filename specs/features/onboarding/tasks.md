# Onboarding — Tasks

**Version:** 0.3.0
**Status:** Draft
**Phase:** 1 (Android)
**Owner:** Danielle Mariani
**Created at:** 2026-05-31
**Last Updated:** 2026-05-31

---

## Overview

This file defines the implementation task breakdown for the Onboarding feature. Tasks are organized into groups that follow the natural dependency order of the Android layered architecture: foundation first, then data, then logic, then UI, then tests.

Each task specifies its requirements coverage, acceptance criteria, dependencies on other tasks, files to create, and an effort estimate (S = ~1–2h, M = ~2–4h, L = ~4–8h).

**Group execution order:**

| Group | Name | Can start when |
|---|---|---|
| 0 | Project Foundation | — |
| 1 | Database Foundation | TSK-ON-01 done |
| 2 | Data Layer | TSK-ON-05 done |
| 3 | Constants & Resources | TSK-ON-01 done (parallel with Group 2) |
| 4 | Navigation Shell | TSK-ON-10, TSK-ON-14 done |
| 5 | ViewModel | TSK-ON-10 done |
| 6 | Shared Components | TSK-ON-14, TSK-ON-15 done |
| 7 | Screens | TSK-ON-16, TSK-ON-17, Groups 4–6 done |
| 8 | Testing | Groups 2 and 5 done |

---

## Task Summary

| ID | Title | Group | Phase | Effort | Status |
|---|---|---|---|---|---|
| TSK-ON-01 | Create Android project skeleton | Project Foundation | 1 | M | Not Started |
| TSK-ON-02 | Set up base package structure | Project Foundation | 1 | S | Not Started |
| TSK-ON-03 | Configure Material 3 theme | Project Foundation | 1 | S | Not Started |
| TSK-ON-04 | Set up Hilt application module | Project Foundation | 1 | S | Not Started |
| TSK-ON-05 | Define Room database and Workspace entity | Database Foundation | 1 | M | Not Started |
| TSK-ON-06 | Define Category entity and DAO | Database Foundation | 1 | M | Not Started |
| TSK-ON-07 | Define Account entity, DAO, and AccountType enum | Database Foundation | 1 | M | Not Started |
| TSK-ON-08 | Implement OnboardingLocalDataSource | Data Layer | 1 | S | Not Started |
| TSK-ON-09 | Implement OnboardingRepository interface and implementation | Data Layer | 1 | S | Not Started |
| TSK-ON-10 | Implement InitializeWorkspaceUseCase | Data Layer | 1 | M | Not Started |
| TSK-ON-11 | Implement SaveDisplayNameUseCase | Data Layer | 1 | S | Not Started |
| TSK-ON-12 | Implement CreateAccountUseCase | Data Layer | 1 | M | Not Started |
| TSK-ON-13 | Create PreferenceKeys constants | Constants & Resources | 1 | S | Not Started |
| TSK-ON-13B | Implement PreferencesDataSource | Constants & Resources | 1 | S | Not Started |
| TSK-ON-14 | Define onboarding string resources | Constants & Resources | 1 | S | Not Started |
| TSK-ON-15 | Implement OnboardingActivity and NavGraph shell | Navigation Shell | 1 | M | Not Started |
| TSK-ON-16 | Implement OnboardingViewModel | ViewModel | 1 | L | Not Started |
| TSK-ON-17 | Implement AccountFormFields shared composable | Shared Components | 1 | M | Not Started |
| TSK-ON-18 | Implement OnboardingPageIndicator and OnboardingSlide | Shared Components | 1 | S | Not Started |
| TSK-ON-19 | Implement AccountSavedDialog component | Shared Components | 1 | S | Not Started |
| TSK-ON-20 | Implement FeatureSlidesScreen | Screens | 1 | M | Not Started |
| TSK-ON-21 | Implement SetYourNameScreen | Screens | 1 | M | Not Started |
| TSK-ON-22 | Implement AddAnAccountScreen | Screens | 1 | L | Not Started |
| TSK-ON-23 | Unit tests for OnboardingViewModel | Testing | 1 | M | Not Started |
| TSK-ON-24 | Unit tests for use cases | Testing | 1 | M | Not Started |
| TSK-ON-25 | Integration tests for Room (onboarding) | Testing | 1 | M | Not Started |

---

## Task Format

Each task follows this structure:

```
**TSK-ON-XX — Title**
- Effort: S / M / L
- Phase: 1
- Group: Group name
- Requirements: RQ-ON-XX, ...
- Acceptance Criteria: AC-ON-XX, ...
- Status: Not Started / Done
- Depends on: TSK-ON-XX, ... / None
- Creates:
  - full/path/to/File.kt
- Details:
  What to implement, key constraints, and any non-obvious decisions.
```

---

## Group 0 — Project Foundation

This group creates the Android project skeleton. No onboarding-specific code is written here — only the base project configuration and shared infrastructure that all features depend on. This group is a prerequisite for everything else.

---

**TSK-ON-01 — Create Android project skeleton**
- Effort: M
- Phase: 1
- Group: Project Foundation
- Requirements: —
- Acceptance Criteria: —
- Status: Not Started
- Depends on: None
- Creates:
  - `android/build.gradle.kts` (root)
  - `android/settings.gradle.kts`
  - `android/gradle/libs.versions.toml` (version catalog)
  - `android/app/build.gradle.kts`
  - `android/app/src/main/AndroidManifest.xml`
  - `android/app/src/main/java/com/dmariani/capital/app/MainActivity.kt`
  - `android/app/src/main/java/com/dmariani/capital/app/CapitalApp.kt`
  - `android/gradle/wrapper/gradle-wrapper.properties`
- Details:
  Create a new Android project using Android Studio (Empty Activity template as base, then clean up generated boilerplate). Use the following configuration:

  **Version catalog (`libs.versions.toml`):**
  ```toml
  [versions]
  agp = "9.1.1"
  kotlin = "2.3.0"
  compileSdk = "36"
  targetSdk = "36"
  minSdk = "23"
  composeBom = "2026.05.00"
  hilt = "2.56"
  room = "2.7.1"
  lifecycle = "2.9.0"
  navigationCompose = "2.9.0"
  coroutines = "1.10.2"

  [libraries]
  compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
  compose-ui = { group = "androidx.compose.ui", name = "ui" }
  compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
  compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
  compose-material3 = { group = "androidx.compose.material3", name = "material3" }
  compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
  activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.10.1" }
  navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
  hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
  hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
  hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
  room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
  room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
  room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
  lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
  lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
  kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
  androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.16.0" }
  junit = { group = "junit", name = "junit", version = "4.13.2" }
  kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
  room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

  [plugins]
  android-application = { id = "com.android.application", version.ref = "agp" }
  kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
  kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
  hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
  ksp = { id = "com.google.devtools.ksp", version = "2.3.0-1.0.29" }
  ```

  **`app/build.gradle.kts` key config:**
  ```kotlin
  android {
      namespace = "com.dmariani.capital"
      compileSdk = 36
      defaultConfig {
          applicationId = "com.dmariani.capital"
          minSdk = 23
          targetSdk = 36
          versionCode = 1
          versionName = "1.0.0"
      }
      buildFeatures { compose = true }
  }
  ```

  The `applicationId` and package name are `com.dmariani.capital`. To rename if needed in the future: use Android Studio's refactor tool (Refactor → Rename package) and update `applicationId` in `build.gradle.kts`. This affects both the package structure and the Play Store identifier.

  `CapitalApp.kt` is the Application class annotated with `@HiltAndroidApp`. Register it in `AndroidManifest.xml` via `android:name=".app.CapitalApp"`.

  `MainActivity.kt` is a minimal empty `ComponentActivity` for now — it will be fleshed out in later tasks. Annotate with `@AndroidEntryPoint`.

---

**TSK-ON-02 — Set up base package structure**
- Effort: S
- Phase: 1
- Group: Project Foundation
- Requirements: —
- Acceptance Criteria: —
- Status: Not Started
- Depends on: TSK-ON-01
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/core/ui/` (directory)
  - `android/app/src/main/java/com/dmariani/capital/core/domain/` (directory)
  - `android/app/src/main/java/com/dmariani/capital/core/data/` (directory)
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/` (directory)
  - `android/app/src/main/java/com/dmariani/capital/feature/dashboard/` (directory, empty)
  - `android/app/src/main/java/com/dmariani/capital/feature/accounts/` (directory, empty)
  - `android/app/src/main/java/com/dmariani/capital/feature/transactions/` (directory, empty)
  - `android/app/src/main/java/com/dmariani/capital/feature/budgets/` (directory, empty)
  - `android/app/src/main/java/com/dmariani/capital/feature/goals/` (directory, empty)
  - `android/app/src/main/java/com/dmariani/capital/feature/categories/` (directory, empty)
  - `android/app/src/main/java/com/dmariani/capital/feature/merchants/` (directory, empty)
  - `android/app/src/main/java/com/dmariani/capital/feature/transfers/` (directory, empty)
- Details:
  Create the full feature and core directory skeleton as defined in `ARCHITECTURE.md`. Directories are empty at this stage — no files yet. The onboarding feature directory will be populated in subsequent tasks. Other feature directories are created now to avoid structural changes later.

---

**TSK-ON-03 — Configure Material 3 theme**
- Effort: S
- Phase: 1
- Group: Project Foundation
- Requirements: —
- Acceptance Criteria: —
- Status: Not Started
- Depends on: TSK-ON-01, TSK-ON-02
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/core/ui/theme/Color.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/ui/theme/Type.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/ui/theme/Theme.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/ui/theme/Spacing.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/ui/theme/Radius.kt`
  - `android/app/src/main/res/font/inter_variable.xml` (downloadable font descriptor)
- Details:
  Implement the design token system from `specs/design/design.md` as a Material 3 `ColorScheme` and custom theme extensions.

  **`Color.kt`** — define light and dark color values for all tokens in `design.md`:
  - Light: `AccentPrimary = Color(0xFF0D7377)`, `AccentOn = Color(0xFFFFFFFF)`, `BackgroundPrimary = Color(0xFFF7F8FA)`, `SurfaceCard = Color(0xFFFFFFFF)`, `SurfaceAlt = Color(0xFFF1F3F5)`, `BorderDefault = Color(0xFFE2E8F0)`, `TextPrimary = Color(0xFF111827)`, `TextSecondary = Color(0xFF6B7280)`, `SemanticSuccess = Color(0xFF16A34A)`, `SemanticError = Color(0xFFDC2626)`, `SemanticWarning = Color(0xFFD97706)`, `SemanticInfo = Color(0xFF2563EB)`
  - Dark: `AccentPrimary = Color(0xFF2EB5AC)`, `AccentOn = Color(0xFF0A2E2D)`, `BackgroundPrimary = Color(0xFF111827)`, `SurfaceCard = Color(0xFF1F2937)`, `SurfaceAlt = Color(0xFF1C2434)`, `BorderDefault = Color(0xFF374151)`, `TextPrimary = Color(0xFFF3F4F6)`, `TextSecondary = Color(0xFF9CA3AF)`, `SemanticSuccess = Color(0xFF4ADE80)`, `SemanticError = Color(0xFFF87171)`, `SemanticWarning = Color(0xFFFBBF24)`, `SemanticInfo = Color(0xFF60A5FA)`

  **`Type.kt`** — define `Typography` using Inter font with the scale from `design.md` (Display 32sp/600, Headline 22sp/600, Title 17sp/600, Body 15sp/400, Label 13sp/500, Caption 11sp/400). Use `FontFamily` with the Inter downloadable font.

  **`Theme.kt`** — define `AppTheme` composable that applies `MaterialTheme` with the correct `ColorScheme` (light/dark) and `Typography`. Map Budget App tokens to Material 3 roles as defined in `design.md` (Platform Adaptation section).

  **`Spacing.kt`** — define a `Spacing` data class with all spacing tokens (xxs=2dp, xs=4dp, sm=8dp, md=16dp, lg=24dp, xl=32dp, xxl=48dp) and a `LocalSpacing` `CompositionLocal`.

  **`Radius.kt`** — define a `Radius` data class with all radius tokens (sm=6dp, md=10dp, lg=16dp, xl=24dp) and a `LocalRadius` `CompositionLocal`.

  Inter is loaded as a downloadable font via the Google Fonts provider — do not bundle the font file as an asset (prefer smaller APK size). Define the font descriptor in `res/font/inter_variable.xml`.

---

**TSK-ON-04 — Set up Hilt application module**
- Effort: S
- Phase: 1
- Group: Project Foundation
- Requirements: —
- Acceptance Criteria: —
- Status: Not Started
- Depends on: TSK-ON-01, TSK-ON-02
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/core/data/di/DatabaseModule.kt`
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/di/OnboardingModule.kt`
- Details:
  **`DatabaseModule.kt`:** Hilt `@Module` installed in `SingletonComponent`. Created as a placeholder at this stage — no bindings yet. Room database and DAO providers will be added in TSK-ON-05, TSK-ON-06, and TSK-ON-07 as each entity is defined. Annotate with `@Module` and `@InstallIn(SingletonComponent::class)`.

  **`OnboardingModule.kt`:** Hilt `@Module` installed in `SingletonComponent`. Created as a placeholder at this stage. As each Repository is created in Group 2, its `@Binds` declaration must be added here. The pattern to follow for every repository binding:
  ```kotlin
  @Binds
  abstract fun bindOnboardingRepository(
      impl: OnboardingRepositoryImpl
  ): OnboardingRepository
  ```
  Use cases that require `@ApplicationContext` (e.g. `SaveDisplayNameUseCase`) are annotated accordingly. This module is abstract (uses `@Binds`) — concrete `@Provides` functions for DAOs live in `DatabaseModule` instead. Annotate with `@Module` and `@InstallIn(SingletonComponent::class)`.

---

## Group 1 — Database Foundation

This group sets up Room and all entities needed by onboarding (Workspace, Category, Account). Full schema is defined in `specs/technical/data-model.md`.

---

**TSK-ON-05 — Define Room database and Workspace entity**
- Effort: M
- Phase: 1
- Group: Database Foundation
- Requirements: RQ-ON-27, RQ-ON-29, RQ-ON-30
- Acceptance Criteria: AC-ON-01, AC-ON-19
- Status: Not Started
- Depends on: TSK-ON-04
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/core/data/AppDatabase.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/domain/Workspace.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/data/WorkspaceEntity.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/data/WorkspaceDao.kt`
- Details:
  **`Workspace.kt` (domain model):** Pure Kotlin data class, no Room annotations. Fields: `id: String`, `name: String`, `baseCurrency: String`, `createdAt: Long`, `updatedAt: Long`, `deletedAt: Long?`, `lastSyncedAt: Long?`, `syncStatus: String?`.

  **`WorkspaceEntity.kt` (Room entity):** Annotated with `@Entity(tableName = "workspaces")`. All fields match `data-model.md` schema. Primary key: `id` (TEXT). Store amounts as INTEGER, timestamps as INTEGER (Unix UTC). `syncStatus` and `lastSyncedAt` are nullable (null in Phase 1).

  **`WorkspaceDao.kt`:** Interface annotated with `@Dao`. Methods: `insertWorkspace(entity: WorkspaceEntity)`, `getFirstWorkspace(): WorkspaceEntity?` (used for idempotency check). Both suspend functions.

  **`AppDatabase.kt`:** `@Database` class listing all entities (start with `WorkspaceEntity` — others added as they are defined in later tasks). `version = 1`. Provide via Hilt in `DatabaseModule` as a singleton using `Room.databaseBuilder(context, AppDatabase::class.java, "capital.db")`. Export schema to `schemas/` directory for migration tracking: set `exportSchema = true` and configure `room.schemaLocation` in `build.gradle.kts`.

  **Update `DatabaseModule.kt`:** Add the following providers at the end of this task:
  ```kotlin
  @Provides
  fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
      Room.databaseBuilder(context, AppDatabase::class.java, "capital.db")
          .build()

  @Provides
  fun provideWorkspaceDao(db: AppDatabase): WorkspaceDao = db.workspaceDao()
  ```

---

**TSK-ON-06 — Define Category entity and DAO**
- Effort: M
- Phase: 1
- Group: Database Foundation
- Requirements: RQ-ON-28, RQ-ON-29, RQ-ON-30
- Acceptance Criteria: AC-ON-01, AC-ON-19
- Status: Not Started
- Depends on: TSK-ON-05
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/core/domain/Category.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/data/CategoryEntity.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/data/CategoryDao.kt`
- Details:
  **`Category.kt` (domain model):** Pure Kotlin data class. Fields: `id: String`, `workspaceId: String`, `name: String`, `icon: String?`, `isDefault: Boolean`, `isHidden: Boolean`, `createdAt: Long`, `updatedAt: Long`, `deletedAt: Long?`, `lastSyncedAt: Long?`, `syncStatus: String?`.

  **`CategoryEntity.kt`:** `@Entity(tableName = "categories", foreignKeys = [ForeignKey(entity = WorkspaceEntity::class, parentColumns = ["id"], childColumns = ["workspace_id"])])`. Add index on `workspace_id`. Store `isDefault` and `isHidden` as INTEGER (0/1).

  **`CategoryDao.kt`:** Methods: `insertCategories(entities: List<CategoryEntity>)` annotated with `@Transaction` (for atomic batch insert during seeding). `getCategoriesForWorkspace(workspaceId: String): List<CategoryEntity>` (used for idempotency check). Both suspend functions.

  Register `CategoryEntity` in `AppDatabase` entities list.

  **Update `DatabaseModule.kt`:** Add the following provider at the end of this task:
  ```kotlin
  @Provides
  fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
  ```

---

**TSK-ON-07 — Define Account entity, DAO, and AccountType enum**
- Effort: M
- Phase: 1
- Group: Database Foundation
- Requirements: RQ-ON-19, RQ-ON-20, RQ-ON-21, RQ-ON-22
- Acceptance Criteria: AC-ON-06, AC-ON-07, AC-ON-09, AC-ON-18
- Status: Not Started
- Depends on: TSK-ON-05
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/core/domain/Account.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/domain/AccountType.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/data/AccountEntity.kt`
  - `android/app/src/main/java/com/dmariani/capital/core/data/AccountDao.kt`
- Details:
  **`AccountType.kt`:** Kotlin enum: `CHECKING`, `SAVINGS`, `CASH`, `CREDIT_CARD`.

  **`Account.kt` (domain model):** Pure Kotlin data class. Fields: `id: String`, `workspaceId: String`, `name: String`, `type: AccountType`, `currencyCode: String`, `initialBalance: Long`, `creditLimit: Long?`, `isPinned: Boolean`, `pinnedAt: Long?`, `createdAt: Long`, `updatedAt: Long`, `deletedAt: Long?`, `lastSyncedAt: Long?`, `syncStatus: String?`.

  **`AccountEntity.kt`:** `@Entity(tableName = "accounts", foreignKeys = [ForeignKey(...WorkspaceEntity...)], indices = [Index("workspace_id"), Index("is_pinned", "pinned_at")])`. Store `type` as TEXT (enum name). Store `isPinned` as INTEGER (0/1). Add unique index on `(workspace_id, name)` where `deleted_at IS NULL` — enforce at the DAO query level since SQLite partial indexes require a workaround (filter in the DAO insert method).

  **`AccountDao.kt`:** Methods: `insertAccount(entity: AccountEntity)`, `getAccountByName(workspaceId: String, name: String): AccountEntity?` (used for duplicate name check). Both suspend functions.

  Register `AccountEntity` in `AppDatabase` entities list.

  **Update `DatabaseModule.kt`:** Add the following provider at the end of this task:
  ```kotlin
  @Provides
  fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
  ```

---

## Group 2 — Data Layer

This group implements the Repository, LocalDataSource, and Use Cases for onboarding. All business logic for data operations lives here.

---

**TSK-ON-08 — Implement OnboardingLocalDataSource**
- Effort: S
- Phase: 1
- Group: Data Layer
- Requirements: RQ-ON-27, RQ-ON-28, RQ-ON-30
- Acceptance Criteria: AC-ON-01, AC-ON-19
- Status: Not Started
- Depends on: TSK-ON-05, TSK-ON-06, TSK-ON-07
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/data/local/OnboardingLocalDataSource.kt`
- Details:
  Class injected with `WorkspaceDao`, `CategoryDao`, and `AccountDao` via constructor. Wraps DAO calls — no business logic here.

  Methods:
  - `suspend fun getFirstWorkspace(): WorkspaceEntity?` — delegates to `WorkspaceDao`
  - `suspend fun insertWorkspace(entity: WorkspaceEntity)` — delegates to `WorkspaceDao`
  - `suspend fun getCategoriesForWorkspace(workspaceId: String): List<CategoryEntity>` — delegates to `CategoryDao`
  - `suspend fun insertCategories(entities: List<CategoryEntity>)` — delegates to `CategoryDao` (transactional)
  - `suspend fun getAccountByName(workspaceId: String, name: String): AccountEntity?` — delegates to `AccountDao`
  - `suspend fun insertAccount(entity: AccountEntity)` — delegates to `AccountDao`

---

**TSK-ON-09 — Implement OnboardingRepository interface and implementation**
- Effort: S
- Phase: 1
- Group: Data Layer
- Requirements: RQ-ON-27, RQ-ON-28, RQ-ON-30
- Acceptance Criteria: AC-ON-01
- Status: Not Started
- Depends on: TSK-ON-08
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/domain/OnboardingRepository.kt`
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/data/OnboardingRepositoryImpl.kt`
- Details:
  **`OnboardingRepository.kt`:** Interface with all methods needed by use cases. Mirror the `OnboardingLocalDataSource` surface at the domain level — but use domain models (`Workspace`, `Category`, `Account`), not entities.

  **`OnboardingRepositoryImpl.kt`:** Implements the interface. Injects `OnboardingLocalDataSource`. Responsible for mapping between domain models and Room entities. Mappers are defined as extension functions in the same file (e.g. `WorkspaceEntity.toDomain()`, `Workspace.toEntity()`).

  **Update `OnboardingModule.kt`:** Add the `@Binds` declaration so Hilt knows to inject `OnboardingRepositoryImpl` wherever `OnboardingRepository` is required:
  ```kotlin
  @Binds
  abstract fun bindOnboardingRepository(
      impl: OnboardingRepositoryImpl
  ): OnboardingRepository
  ```
  Without this binding, `InitializeWorkspaceUseCase` and `CreateAccountUseCase` cannot be injected.

---

**TSK-ON-10 — Implement InitializeWorkspaceUseCase**
- Effort: M
- Phase: 1
- Group: Data Layer
- Requirements: RQ-ON-27, RQ-ON-28, RQ-ON-29, RQ-ON-30
- Acceptance Criteria: AC-ON-01, AC-ON-10, AC-ON-19, AC-ON-20
- Status: Not Started
- Depends on: TSK-ON-09
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/domain/InitializeWorkspaceUseCase.kt`
- Details:
  Single `suspend operator fun invoke(): Result<Unit>`.

  Logic:
  1. Call `repository.getFirstWorkspace()`
  2. If a Workspace already exists → return `Result.success(Unit)` immediately (idempotent — RQ-ON-30)
  3. If no Workspace exists:
     a. Generate a UUID v4 for the Workspace id (`UUID.randomUUID().toString()`)
     b. Create a `Workspace` domain object: name = `"Personal"`, baseCurrency = `"USD"`, createdAt/updatedAt = current UTC milliseconds converted to Unix seconds
     c. Call `repository.insertWorkspace(workspace)`
     d. Build the list of 20 default `Category` domain objects from the canonical list in `requirements.md` (RQ-ON-28). Each gets a UUID v4 id, the new workspace's id, `isDefault = true`, `isHidden = false`
     e. Call `repository.insertCategories(categories)` — wrapped in a single transaction
  4. Return `Result.success(Unit)` on success
  5. Catch all exceptions → return `Result.failure(exception)`

  The 20 default categories are defined as a private constant list inside this use case — not hardcoded in the ViewModel or UI.

---

**TSK-ON-11 — Implement SaveDisplayNameUseCase**
- Effort: S
- Phase: 1
- Group: Data Layer
- Requirements: RQ-ON-12, RQ-ON-15
- Acceptance Criteria: AC-ON-05, AC-ON-10
- Status: Not Started
- Depends on: TSK-ON-13B (PreferencesDataSource must exist)
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/domain/SaveDisplayNameUseCase.kt`
- Details:
  Injects `PreferencesDataSource` via constructor (not `@ApplicationContext context` directly — all SharedPreferences access is centralized through `PreferencesDataSource`). Single `fun invoke(name: String)` (not suspend — SharedPreferences write with `apply()` is fire-and-forget).

  Logic:
  1. Trim `name`
  2. Call `preferencesDataSource.saveDisplayName(trimmedName)`
  3. `PreferencesDataSource.saveDisplayName` handles the atomic write of both `DISPLAY_NAME` and `ONBOARDING_COMPLETED` keys.

---

**TSK-ON-12 — Implement CreateAccountUseCase**
- Effort: M
- Phase: 1
- Group: Data Layer
- Requirements: RQ-ON-19, RQ-ON-21, RQ-ON-22, RQ-ON-26
- Acceptance Criteria: AC-ON-06, AC-ON-07, AC-ON-16, AC-ON-18
- Status: Not Started
- Depends on: TSK-ON-09
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/domain/CreateAccountUseCase.kt`
- Details:
  Single `suspend operator fun invoke(params: CreateAccountParams): Result<Unit>`.

  ```kotlin
  data class CreateAccountParams(
      val workspaceId: String,
      val name: String,
      val type: AccountType,
      val currencyCode: String,
      val initialBalanceCents: Long,
      val creditLimitCents: Long?
  )
  ```

  Logic:
  1. Check for duplicate name: call `repository.getAccountByName(workspaceId, name.trim())`
  2. If found → return `Result.failure(DuplicateAccountNameException())`
  3. Build `Account` domain object with UUID v4 id, `isPinned = false`, `pinnedAt = null`, current UTC timestamps
  4. Call `repository.insertAccount(account)`
  5. Catch all other exceptions → return `Result.failure(exception)`

  Define `DuplicateAccountNameException` as a local class in this file. The ViewModel maps it to the appropriate error string.

  Amount validation (max value check) is performed in the ViewModel before calling this use case — not here. This use case trusts that inputs are already validated.

---

## Group 3 — Constants & Resources

This group defines all constants and string resources. Can be developed in parallel with Group 2.

---

**TSK-ON-13 — Create PreferenceKeys constants**
- Effort: S
- Phase: 1
- Group: Constants & Resources
- Requirements: RQ-ON-12, BR-ON-01, BR-ON-03
- Acceptance Criteria: AC-ON-05, AC-ON-10
- Status: Not Started
- Depends on: TSK-ON-02
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/core/data/PreferenceKeys.kt`
- Details:
  ```kotlin
  object PreferenceKeys {
      const val ONBOARDING_COMPLETED = "onboarding_completed"
      const val DISPLAY_NAME = "display_name"
  }
  ```
  Located in `core/data/` — SharedPreferences is a data-layer concern and these keys are read outside of onboarding (e.g. Dashboard greeting reads `DISPLAY_NAME`). No other constants file should define raw SharedPreferences key strings.

---

**TSK-ON-13B — Implement PreferencesDataSource**
- Effort: S
- Phase: 1
- Group: Constants & Resources
- Requirements: RQ-ON-12, BR-ON-01, BR-ON-03
- Acceptance Criteria: AC-ON-05, AC-ON-10
- Status: Not Started
- Depends on: TSK-ON-13
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/core/data/PreferencesDataSource.kt`
- Details:
  Centralized wrapper for all SharedPreferences access in the app. Any feature that reads or writes SharedPreferences must do so through this class — never by accessing SharedPreferences directly. This prevents scattered access patterns and makes testing straightforward via injection of a fake.

  ```kotlin
  class PreferencesDataSource @Inject constructor(
      @ApplicationContext private val context: Context
  ) {
      private val prefs by lazy {
          context.getSharedPreferences("capital_prefs", Context.MODE_PRIVATE)
      }

      fun saveDisplayName(name: String) {
          prefs.edit {
              putString(PreferenceKeys.DISPLAY_NAME, name)
              putBoolean(PreferenceKeys.ONBOARDING_COMPLETED, true)
          }
      }

      fun getDisplayName(): String? =
          prefs.getString(PreferenceKeys.DISPLAY_NAME, null)

      fun isOnboardingCompleted(): Boolean =
          prefs.getBoolean(PreferenceKeys.ONBOARDING_COMPLETED, false)
  }
  ```

  `saveDisplayName` writes both keys atomically in a single `edit { }` block — consistent with the atomic write requirement in RQ-ON-12.

  **Update `OnboardingModule.kt`:** Add a `@Provides` function for `PreferencesDataSource`:
  ```kotlin
  @Provides
  fun providePreferencesDataSource(
      @ApplicationContext context: Context
  ): PreferencesDataSource = PreferencesDataSource(context)
  ```

  **Impact on `SaveDisplayNameUseCase` (TSK-ON-11):** Inject `PreferencesDataSource` instead of `@ApplicationContext context: Context` directly. Call `preferencesDataSource.saveDisplayName(trimmedName)`.

  **Impact on `OnboardingActivity` (TSK-ON-15):** Inject `PreferencesDataSource` via field injection (`@Inject lateinit var preferencesDataSource: PreferencesDataSource`) and call `preferencesDataSource.isOnboardingCompleted()` instead of reading SharedPreferences directly.

---

**TSK-ON-14 — Define onboarding string resources**
- Effort: S
- Phase: 1
- Group: Constants & Resources
- Requirements: RQ-ON-03, RQ-ON-05, RQ-ON-06, RQ-ON-10, RQ-ON-14, RQ-ON-18, RQ-ON-23, RQ-ON-24
- Acceptance Criteria: AC-ON-01 through AC-ON-20
- Status: Not Started
- Depends on: TSK-ON-01
- Creates:
  - `android/app/src/main/res/values/strings.xml`
- Details:
  Define all string resources as specified in the Constants & Resources section of `specs/features/onboarding/design.md`. No hardcoded strings in any Kotlin or Composable file — all copy must reference `R.string.*`.

  Key strings to define (full list in `design.md`):
  - Feature Slides: titles, messages, Skip, Get Started
  - Set Your Name: hint, privacy copy, Continue
  - Add an Account: encouraging copy (with `%s` placeholder for app name), field labels, account type options, Save, Skip for now
  - Account Saved Dialog: title, body, Add Another Account, Go to Home
  - Validation errors: duplicate name, save failed, max amount, credit limit zero
  - Initialization error: title, body, Retry

  Also define `app_name` string here — used throughout the app.

---

## Group 4 — Navigation Shell

This group wires up `OnboardingActivity`, the `NavHost`, route constants, and back navigation overrides.

---

**TSK-ON-15 — Implement OnboardingActivity and NavGraph shell**
- Effort: M
- Phase: 1
- Group: Navigation Shell
- Requirements: RQ-ON-01, RQ-ON-07, RQ-ON-08, RQ-ON-16, RQ-ON-25, BR-ON-01, BR-ON-08
- Acceptance Criteria: AC-ON-10, AC-ON-11, AC-ON-12, AC-ON-13, AC-ON-14, AC-ON-15
- Status: Not Started
- Depends on: TSK-ON-03, TSK-ON-13B
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/ui/OnboardingActivity.kt`
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/ui/OnboardingNavGraph.kt`
- Details:
  **`MainActivity.kt` (update placeholder):** At this stage `MainActivity` must be an explicit empty placeholder — a blank white screen. It is not yet implemented. Add a comment inside the file:
  ```kotlin
  // TODO: Replace with Dashboard screen when the Dashboard feature is implemented.
  ```
  Set content to an empty `Box(modifier = Modifier.fillMaxSize().background(Color.White))` — nothing else. Annotate with `@AndroidEntryPoint`. This makes the Hilt graph valid and the app launchable for manual testing of the onboarding flow end-to-end.

  **`OnboardingActivity.kt`:**
  - Annotate with `@AndroidEntryPoint`
  - Inject `PreferencesDataSource` via field injection: `@Inject lateinit var preferencesDataSource: PreferencesDataSource`
  - On `onCreate`: call `preferencesDataSource.isOnboardingCompleted()` — no direct SharedPreferences access
  - If `true`: start `MainActivity` via `Intent`, call `finish()`, return immediately
  - If `false`: set content to `AppTheme { OnboardingNavGraph(onOnboardingComplete = { startActivity(...); finish() }) }`
  - Register in `AndroidManifest.xml` as the launcher Activity. `MainActivity` is no longer the launcher.
  - Set `android:theme` to a splash/no-action-bar theme to avoid a brief toolbar flash on launch.

  **`OnboardingNavGraph.kt`:**
  - Define route constants:
    ```kotlin
    object OnboardingRoutes {
        const val FEATURE_SLIDES = "feature_slides"
        const val SET_YOUR_NAME = "set_your_name"
        const val ADD_AN_ACCOUNT = "add_an_account"
    }
    ```
  - Define `NavHost` starting at `OnboardingRoutes.FEATURE_SLIDES`
  - Collect `OnboardingSideEffect` from the shared `OnboardingViewModel` (obtained via `hiltViewModel()` at the NavGraph level and passed down)
  - Side effect collection:
    - `NavigateToSetYourName` → `navController.navigate(OnboardingRoutes.SET_YOUR_NAME)`
    - `NavigateToAddAnAccount` → `navController.navigate(OnboardingRoutes.ADD_AN_ACCOUNT)`
    - `NavigateToHome` → call `onOnboardingComplete()`
  - Screen placeholders (empty composables) for all three routes — screens are implemented in Group 7

---

## Group 5 — ViewModel

---

**TSK-ON-16 — Implement OnboardingViewModel**
- Effort: L
- Phase: 1
- Group: ViewModel
- Requirements: RQ-ON-01, RQ-ON-10, RQ-ON-11, RQ-ON-12, RQ-ON-19, RQ-ON-20, RQ-ON-21, RQ-ON-22, RQ-ON-23, RQ-ON-26, BR-ON-02, BR-ON-07
- Acceptance Criteria: AC-ON-04, AC-ON-05, AC-ON-06, AC-ON-07, AC-ON-09, AC-ON-16, AC-ON-17, AC-ON-18
- Status: Not Started
- Depends on: TSK-ON-10, TSK-ON-11, TSK-ON-12
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/ui/OnboardingViewModel.kt`
- Details:
  `@HiltViewModel` class. Injects `InitializeWorkspaceUseCase`, `SaveDisplayNameUseCase`, `CreateAccountUseCase`.

  Exposes:
  - `uiState: StateFlow<OnboardingUiState>` — full state as defined in `design.md`
  - `sideEffects: Flow<OnboardingSideEffect>` — via `Channel(Channel.BUFFERED).receiveAsFlow()`

  **`OnboardingUiState`** (defined in same file or companion):
  ```kotlin
  data class OnboardingUiState(
      val isInitializing: Boolean = true,
      val initializationError: Boolean = false,
      val displayName: String = "",
      val isDisplayNameValid: Boolean = false,
      val accountName: String = "",
      val accountType: AccountType = AccountType.CHECKING,
      val initialBalance: String = "",
      val creditLimit: String = "",
      val isAccountFormValid: Boolean = false,
      val accountNameError: String? = null,
      val initialBalanceError: String? = null,
      val creditLimitError: String? = null,
      val accountSaveError: String? = null,
      val showAccountSavedDialog: Boolean = false,
      val savedAccountsCount: Int = 0
  )
  ```

  **`OnboardingEvent`** and **`OnboardingSideEffect`** sealed classes as defined in `design.md`.

  `init` block: launch `InitializeWorkspaceUseCase` on `viewModelScope`. On success: set `isInitializing = false`. On failure: set `initializationError = true, isInitializing = false`.

  **Display Name validation:** `isDisplayNameValid = displayName.trim().length in 2..30`. Recomputed on every `DisplayNameChanged` event.

  **Account form validation:** `isAccountFormValid` is `true` when: `accountName.trim()` is non-empty and ≤ 100 chars, `initialBalance` parses to a valid value ≥ 0 and ≤ 999,999,999 cents, and if `accountType == CREDIT_CARD` then `creditLimit` parses to > 0 and ≤ 999,999,999 cents.

  **`ContinueWithName`:** trim name, call `SaveDisplayNameUseCase`, emit `NavigateToAddAnAccount`.

  **`SaveAccount`:** validate amounts, call `CreateAccountUseCase`, handle `DuplicateAccountNameException` → set `accountNameError`, handle other failures → set `accountSaveError`, on success → set `showAccountSavedDialog = true`, increment `savedAccountsCount`.

  **`AddAnotherAccount`:** clear form fields, reset type to `CHECKING`, set `showAccountSavedDialog = false`.

  **`GoToHome`:** set `showAccountSavedDialog = false`, emit `NavigateToHome`.

  **`InitializationRetried`:** set `initializationError = false`, set `isInitializing = true`, then re-launch `InitializeWorkspaceUseCase` on `viewModelScope`. On success: set `isInitializing = false`. On failure: set `initializationError = true, isInitializing = false`. This mirrors the `init` block logic and must be extracted into a private `fun initialize()` function called from both `init` and `InitializationRetried` handling to avoid duplication.

---

## Group 6 — Shared Components

This group implements composables that are shared across screens or reused in other features.

---

**TSK-ON-17 — Implement AccountFormFields shared composable**
- Effort: M
- Phase: 1
- Group: Shared Components
- Requirements: RQ-ON-19, RQ-ON-20, RQ-ON-21
- Acceptance Criteria: AC-ON-09
- Status: Not Started
- Depends on: TSK-ON-03, TSK-ON-14
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/core/ui/components/AccountFormFields.kt`
- Details:
  Stateless composable as defined in `design.md` (Shared Components section). Accepts all field values and change callbacks as parameters. Holds no internal state.

  Credit Limit field is rendered only when `accountType == AccountType.CREDIT_CARD`. This conditional is inside the composable — not the caller's responsibility.

  Currency field is a read-only `OutlinedTextField` with `enabled = false` and a visually dimmed style (`color.text.secondary` text, `color.surface.alt` background).

  Initial Balance and Credit Limit use a numeric keyboard (`KeyboardType.Number`). Apply a `VisualTransformation` to format the raw numeric input as a currency string (e.g. display `$1,234.56` while the backing state holds `"123456"` as a cents string). The exact `VisualTransformation` implementation may be defined as a helper class in the same file.

  Field labels come from `stringResource(R.string.*)` — no hardcoded strings.

  Apply design tokens from `design.md` (Add an Account — Design Tokens Applied section): `OutlinedTextField` with `radius.md` corners, `color.accent.primary` focused border, `color.text.secondary` label, `color.semantic.error` for inline errors.

---

**TSK-ON-18 — Implement OnboardingPageIndicator and OnboardingSlide components**
- Effort: S
- Phase: 1
- Group: Shared Components
- Requirements: RQ-ON-02, RQ-ON-03, RQ-ON-04
- Acceptance Criteria: AC-ON-02, AC-ON-03
- Status: Not Started
- Depends on: TSK-ON-03, TSK-ON-14
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/ui/components/OnboardingPageIndicator.kt`
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/ui/components/OnboardingSlide.kt`
- Details:
  **`OnboardingPageIndicator`:** Row of `count` dots (8dp each). Active dot: filled circle, `color.accent.on`. Inactive: `color.accent.on` at 40% alpha. Gap between dots: `spacing.sm` (8dp). Parameters: `count: Int`, `activePage: Int`, `modifier`.

  **`OnboardingSlide`:** Column layout. Parameters: `@DrawableRes illustration: Int`, `title: String`, `message: String`, `modifier`. Title uses Headline typography (22sp/600, `color.accent.on`). Message uses Body typography (15sp/400, `color.accent.on`). Illustration is an `Image` composable centered and filling the majority of available vertical space using `weight(1f)` in the column. Horizontal padding on text: `spacing.lg` (24dp).

---

**TSK-ON-19 — Implement AccountSavedDialog component**
- Effort: S
- Phase: 1
- Group: Shared Components
- Requirements: RQ-ON-23
- Acceptance Criteria: AC-ON-06, AC-ON-07
- Status: Not Started
- Depends on: TSK-ON-03, TSK-ON-14
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/ui/components/AccountSavedDialog.kt`
- Details:
  Material 3 `AlertDialog` composable. Parameters: `onAddAnother: () -> Unit`, `onGoToHome: () -> Unit`.

  Title: `stringResource(R.string.onboarding_dialog_title)` — Title typography (17sp/600).
  Body: `stringResource(R.string.onboarding_dialog_body)` — Body typography (15sp/400, `color.text.secondary`).
  "Go to Home" → filled `Button` with `color.accent.primary`.
  "Add Another Account" → `TextButton` with `color.accent.primary` text.

  Dialog corner radius: `radius.xl` (24dp). Elevation: `elevation.lg` (8dp).

---

## Group 7 — Screens

This group implements the three onboarding screen composables. Each screen is wired to `OnboardingViewModel` and observes `uiState`.

---

**TSK-ON-20 — Implement FeatureSlidesScreen**
- Effort: M
- Phase: 1
- Group: Screens
- Requirements: RQ-ON-01, RQ-ON-02, RQ-ON-03, RQ-ON-04, RQ-ON-05, RQ-ON-06, RQ-ON-07, RQ-ON-08
- Acceptance Criteria: AC-ON-02, AC-ON-03, AC-ON-11, AC-ON-13
- Status: Not Started
- Depends on: TSK-ON-15, TSK-ON-16, TSK-ON-18
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/ui/screens/FeatureSlidesScreen.kt`
  - `android/app/src/main/res/drawable/ic_slide_1.xml` (placeholder illustration)
  - `android/app/src/main/res/drawable/ic_slide_2.xml` (placeholder illustration)
  - `android/app/src/main/res/drawable/ic_slide_3.xml` (placeholder illustration)
- Details:
  Full-screen composable. Background: `color.accent.primary`. No `Scaffold` — custom layout.

  Top: App logo PNG image, centered, top padding `spacing.xl` (32dp).

  `HorizontalPager` with `PagerState(pageCount = 3)`. Each page renders `OnboardingSlide` with the corresponding illustration, title, and message from `strings.xml`. Slide transition animation: 250ms horizontal slide, follows swipe direction.

  Below pager content: `OnboardingPageIndicator(count = 3, activePage = pagerState.currentPage)`. Top margin: `spacing.lg` (24dp).

  Action area at the bottom:
  - On pages 0 and 1: "Skip" `TextButton` aligned to bottom-right, inset `spacing.md` from right and bottom edges
  - On page 2: "Get Started" filled `Button` centered, horizontal inset `spacing.md`, bottom padding `spacing.lg`. Skip is hidden.

  Back navigation: `BackHandler` registered only when `pagerState.currentPage == 0` → `(context as Activity).finish()`. On pages 1 and 2, no `BackHandler` — swiping back within the pager is handled natively by `HorizontalPager`.

  Show initialization loading state (full-screen centered `CircularProgressIndicator` on accent background) when `uiState.isInitializing == true`.

  Show initialization error state when `uiState.initializationError == true` — full-screen error composable with icon, title, body, and Retry button. Retry dispatches `OnboardingEvent.InitializationRetried`.

  On Skip or Get Started: dispatch `OnboardingEvent.SlidesCompleted` → ViewModel emits `NavigateToSetYourName` side effect.

---

**TSK-ON-21 — Implement SetYourNameScreen**
- Effort: M
- Phase: 1
- Group: Screens
- Requirements: RQ-ON-09, RQ-ON-10, RQ-ON-11, RQ-ON-12, RQ-ON-14, RQ-ON-15, RQ-ON-16
- Acceptance Criteria: AC-ON-04, AC-ON-05, AC-ON-14, AC-ON-17
- Status: Not Started
- Depends on: TSK-ON-15, TSK-ON-16
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/ui/screens/SetYourNameScreen.kt`
- Details:
  Full-screen composable. Background: `color.background.primary`. No `Scaffold`.

  Top: App logo PNG, centered, top padding `spacing.xl` (32dp). Bottom margin below logo: `spacing.xxl` (48dp).

  Display Name `BasicTextField` (not `OutlinedTextField` — transparent background with bottom border only):
  - Font: Inter, 28sp, weight 400
  - Text color: `color.text.primary`
  - Background: transparent
  - Bottom border: `color.border.default` at 1dp (unfocused), `color.accent.primary` at 1dp (focused)
  - `maxLength = 30` enforced via `visualTransformation` or `onValueChange` filter — silently reject input beyond 30 chars
  - Horizontal padding: `spacing.md`

  Privacy copy `Text` below the field: `stringResource(R.string.onboarding_name_privacy_copy)`, Caption typography (11sp/400), `color.text.secondary`, top margin `spacing.sm`.

  Continue `Button` pinned to the bottom, full-width minus `spacing.md` horizontal insets, bottom padding `spacing.lg`:
  - Enabled: `uiState.isDisplayNameValid == true`
  - Disabled: 38% alpha on background and text (standard Material 3 disabled state)

  Back navigation: `BackHandler` → `(context as Activity).finish()`.

  On Continue: dispatch `OnboardingEvent.ContinueWithName`.
  On `DisplayNameChanged`: dispatch event on every keystroke.

---

**TSK-ON-22 — Implement AddAnAccountScreen**
- Effort: L
- Phase: 1
- Group: Screens
- Requirements: RQ-ON-17, RQ-ON-18, RQ-ON-19, RQ-ON-20, RQ-ON-21, RQ-ON-22, RQ-ON-23, RQ-ON-24, RQ-ON-25, RQ-ON-26
- Acceptance Criteria: AC-ON-06, AC-ON-07, AC-ON-08, AC-ON-09, AC-ON-12, AC-ON-15, AC-ON-16, AC-ON-18
- Status: Not Started
- Depends on: TSK-ON-15, TSK-ON-16, TSK-ON-17, TSK-ON-19
- Creates:
  - `android/app/src/main/java/com/dmariani/capital/feature/onboarding/ui/screens/AddAnAccountScreen.kt`
- Details:
  Full-screen composable. Background: `color.background.primary`. Scrollable `Column` to handle small screens with the Credit Limit field visible.

  Top: App logo PNG, centered, top padding `spacing.xl`. Below logo: encouraging copy `Text` (`color.text.secondary`, Body typography, centered), bottom margin `spacing.lg`.

  `AccountFormFields` composable wired to ViewModel state:
  - Pass all field values from `uiState`
  - Pass all change callbacks dispatching the corresponding `OnboardingEvent`
  - Pass error strings from `uiState` (`accountNameError`, `initialBalanceError`, `creditLimitError`)
  - `currencyCode` = `"USD"` hardcoded in Phase 1 (workspace base currency)

  Save `Button` below the form, full-width minus insets:
  - Enabled: `uiState.isAccountFormValid`
  - On click: dispatch `OnboardingEvent.SaveAccount`
  - Display `uiState.accountSaveError` as inline error text below the button if non-null

  "Skip for now" `TextButton` below Save, centered, bottom padding `spacing.lg`. Always enabled. On click: dispatch `OnboardingEvent.GoToHome`.

  `AccountSavedDialog` shown when `uiState.showAccountSavedDialog == true`:
  - `onAddAnother`: dispatch `OnboardingEvent.AddAnotherAccount`
  - `onGoToHome`: dispatch `OnboardingEvent.GoToHome`

  Back navigation: `BackHandler` → `(context as Activity).finish()`.

---

## Group 8 — Testing

---

**TSK-ON-23 — Unit tests for OnboardingViewModel**
- Effort: M
- Phase: 1
- Group: Testing
- Requirements: RQ-ON-10, RQ-ON-11, RQ-ON-12, RQ-ON-19, RQ-ON-20, RQ-ON-21, RQ-ON-22, RQ-ON-23, RQ-ON-26
- Acceptance Criteria: AC-ON-04, AC-ON-05, AC-ON-06, AC-ON-07, AC-ON-09, AC-ON-16, AC-ON-17, AC-ON-18
- Status: Not Started
- Depends on: TSK-ON-16
- Creates:
  - `android/app/src/test/java/com/dmariani/capital/feature/onboarding/OnboardingViewModelTest.kt`
- Details:
  Use `kotlinx-coroutines-test` and `TestCoroutineDispatcher`. Mock use cases with fakes (not Mockito — prefer hand-written fakes for simplicity).

  Test cases:
  - `displayName blank → isDisplayNameValid = false`
  - `displayName 1 char → isDisplayNameValid = false`
  - `displayName 2 chars → isDisplayNameValid = true`
  - `displayName 30 chars → isDisplayNameValid = true`
  - `displayName cleared after valid → isDisplayNameValid = false`
  - `displayName whitespace only → isDisplayNameValid = false` (after trim)
  - `isAccountFormValid false when name empty`
  - `isAccountFormValid false when initialBalance invalid`
  - `isAccountFormValid true with valid Checking fields`
  - `isAccountFormValid false for CreditCard when creditLimit missing`
  - `isAccountFormValid true for CreditCard with all fields`
  - `accountType changed to CREDIT_CARD → creditLimit visible in form state`
  - `accountType changed away from CREDIT_CARD → creditLimit cleared`
  - `SaveAccount success → showAccountSavedDialog = true, savedAccountsCount incremented`
  - `SaveAccount duplicate name → accountNameError set`
  - `AddAnotherAccount → form cleared, showAccountSavedDialog = false`
  - `ContinueWithName → NavigateToAddAnAccount side effect emitted`
  - `GoToHome → NavigateToHome side effect emitted`
  - `InitializationRetried → isInitializing = true, re-runs use case`

---

**TSK-ON-24 — Unit tests for use cases**
- Effort: M
- Phase: 1
- Group: Testing
- Requirements: RQ-ON-27, RQ-ON-28, RQ-ON-29, RQ-ON-30, RQ-ON-12
- Acceptance Criteria: AC-ON-01, AC-ON-19, AC-ON-20
- Status: Not Started
- Depends on: TSK-ON-10, TSK-ON-11, TSK-ON-12
- Creates:
  - `android/app/src/test/java/com/dmariani/capital/feature/onboarding/InitializeWorkspaceUseCaseTest.kt`
  - `android/app/src/test/java/com/dmariani/capital/feature/onboarding/SaveDisplayNameUseCaseTest.kt`
  - `android/app/src/test/java/com/dmariani/capital/feature/onboarding/CreateAccountUseCaseTest.kt`
- Details:
  **`InitializeWorkspaceUseCaseTest`:**
  - Creates Workspace and exactly 20 Categories when DB is empty
  - Second invocation is a no-op (idempotent) — no new records created
  - Returns `Result.failure` when repository throws

  **`SaveDisplayNameUseCaseTest`:**
  - Writes `DISPLAY_NAME` and `ONBOARDING_COMPLETED` to SharedPreferences
  - Trims leading/trailing whitespace before writing
  - Uses `PreferenceKeys` constants — verify key names match

  **`CreateAccountUseCaseTest`:**
  - Happy path: account inserted with correct field values (cents conversion)
  - Duplicate name: returns `Result.failure(DuplicateAccountNameException)`
  - Boundary: `initialBalance = 0` → accepted
  - Boundary: `initialBalance = 999_999_999` → accepted
  - Boundary: `creditLimit = 0` → this is caught in ViewModel validation, not use case; document explicitly

---

**TSK-ON-25 — Integration tests for Room (onboarding)**
- Effort: M
- Phase: 1
- Group: Testing
- Requirements: RQ-ON-27, RQ-ON-28, RQ-ON-30
- Acceptance Criteria: AC-ON-01, AC-ON-19, AC-ON-20
- Status: Not Started
- Depends on: TSK-ON-10, TSK-ON-12
- Creates:
  - `android/app/src/androidTest/java/com/dmariani/capital/feature/onboarding/OnboardingRoomIntegrationTest.kt`
- Details:
  Use `Room.inMemoryDatabaseBuilder` for a clean database per test. Tests run on an Android device or emulator (instrumented tests).

  Test cases:
  - `InitializeWorkspaceUseCase` creates exactly 1 Workspace and exactly 20 Categories
  - Second call to `InitializeWorkspaceUseCase` leaves DB unchanged (idempotent)
  - `CreateAccountUseCase` persists Account with correct `workspace_id`
  - Duplicate Account name with same `workspace_id` is rejected (unique constraint)
  - Account with `workspace_id` pointing to non-existent Workspace fails (foreign key)
  - `savedAccountsCount` reflects actual persisted count after multiple inserts

---

## Changelog

| Version | Date | Author | Notes |
|---|---|---|---|
| 0.1.0 | 2026-05-31 | Danielle Mariani | Initial draft. 25 tasks across 9 groups including Group 0 (Project Foundation). Versions: AGP 9.1.1, Kotlin 2.3.0, Gradle 9.4.1, Compose BOM 2026.05.00, compileSdk/targetSdk 36, minSdk 23. App ID: com.dmariani.budgetapp (original placeholder). |
| 0.2.0 | 2026-05-31 | Danielle Mariani | Add Task Summary table (ID, title, group, phase, effort, status). Add Status field to every task definition and Task Format section. All tasks initialised to Not Started. |
| 0.3.0 | 2026-05-31 | Danielle Mariani | TSK-ON-04: expand to create OnboardingModule.kt and document @Binds pattern for repositories. TSK-ON-05/06/07: add explicit DatabaseModule updates for provideWorkspaceDao, provideCategoryDao, provideAccountDao. TSK-ON-09: add @Binds registration step in OnboardingModule. TSK-ON-13B (new): implement PreferencesDataSource as centralized SharedPreferences wrapper in core/data/. TSK-ON-11: update to inject PreferencesDataSource instead of Context directly. TSK-ON-15: update to inject PreferencesDataSource; clarify MainActivity is a temporary empty placeholder with TODO comment. TSK-ON-16: add explicit InitializationRetried event handling; extract initialize() private function to avoid duplication. Summary table updated with TSK-ON-13B row. |
| 0.4.0 | 2026-06-05 | Danielle Mariani | Rename app package from com.dmariani.budgetapp to com.dmariani.capital throughout all file paths and code snippets. Rename BudgetApp.kt to CapitalApp.kt and update AndroidManifest reference. Rename BudgetAppTheme to AppTheme. Rename SharedPreferences file name from "budget_app_prefs" to "capital_prefs". Remove "temporary" note from applicationId — com.dmariani.capital is the confirmed package name. Rename Room database from "budget_app.db" to "capital.db" in TSK-ON-05. |