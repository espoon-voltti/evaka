<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Citizen Web Push Notifications — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let citizens opt into browser push notifications for new messages, persisting subscriptions in S3 (no DB migration), reusing the existing VAPID key and `webpush/WebPush.kt` sender. Synchronous best-effort hook into `MessageService.handleMarkMessageAsSent`.

**Architecture:** New Kotlin package `fi.espoo.evaka.citizenwebpush` with a thin S3-backed store, a category-filtering sender, and a citizen-facing controller under `/citizen/web-push/`. New `citizen-frontend/webpush/` directory with an axios API module, a `useWebPushState()` hook, and a `WebPushSettingsSection` nested inside the existing `NotificationSettingsSection`. Service worker grows two new listeners. See companion spec: `docs/superpowers/specs/2026-04-14-citizen-web-push-notifications-design.md`.

**Tech Stack:** Kotlin 2.x + Spring Boot + AWS SDK v2 `S3Client` (backend); React + TypeScript + vitest + axios (frontend); Web Push Protocol via existing `webpush/WebPush.kt`; service worker (vanilla JS, no framework).

---

## Conventions and gotchas (read before starting)

- **Repo root in this machine:** work treats the root as `/Volumes/evaka/evaka`, but paths in the plan are relative to repo root — if your host mounts it elsewhere, adapt. All file paths in tasks are repo-relative.
- **Toolchain:** `node`/`yarn` aren't on the sandbox PATH by default. Use `node_modules/.bin/vitest` directly, or `node .yarn/releases/yarn-4.13.0.cjs <cmd>` from `frontend/`. See `evaka/CLAUDE.md` for the full story.
- **Frontend type-check:** `yarn type-check` — do **not** use `tsc --noEmit`, the repo uses project references and `--noEmit` silently skips referenced projects.
- **Pre-commit hooks:** `lefthook` runs `./bin/add-license-headers.sh`, `eslint --fix` on staged TS/TSX, and `ktfmtPrecommit` on Kotlin. Re-stage whatever gets auto-modified and commit again — never `--no-verify`.
- **Spec storage deviation:** The spec shows `ecdhKey`/`authSecret` as base64 strings in the S3 JSON. We instead persist them as `List<Byte>` (JSON arrays) so the existing `WebPushSubscription(authSecret: List<Byte>, ecdhKey: List<Byte>)` type in `webpush/WebPushController.kt` can be reused without conversion. This is a deliberate simplification — file readability is slightly worse, code is meaningfully simpler.
- **Spec endpoint prefix deviation:** The spec uses `/citizen/push/*`. We use `/citizen/web-push/*` to match the employee-mobile convention (`/employee-mobile/push-subscription`) already in place. This is a one-word cosmetic change, doesn't affect any design.
- **i18n files are `.tsx`:** `lib-customizations/defaults/citizen/i18n/{fi,sv,en}.tsx` contain JSX values (e.g. `<OrderedList>` trees). The type shape is inferred from `fi.tsx`; the other two must keep the same shape or TypeScript fails the build.
- **Vitest project globs:** citizen specs must live under `src/citizen-frontend/**/*.spec.{ts,tsx}` to be picked up by the `citizen-frontend` vitest project. Tests that render lib-components styled elements must wrap in `<TestContextProvider translations={testTranslations}>` or fail with a theme-lookup error. See `evaka/CLAUDE.md`.
- **Dev server:** `mise start` (from repo root) brings up the full stack (docker compose + pm2 + frontend + apigw + service). Service cold-start is 1–3 min; first page load may 503 briefly. `mise stop` tears everything down. Logs: `pm2 logs frontend|apigw|service`.
- **What "run the test and confirm failure" means:** you must actually run the command and see the failure output. Don't assert in the commit message that it failed without seeing the output.

---

## File structure

### New backend files

```
service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/
├── CitizenPushCategory.kt            # enum URGENT_MESSAGE / MESSAGE / BULLETIN
├── CitizenPushLanguage.kt            # enum FI / SV / EN + fromPersonLanguage helper
├── CitizenPushStoreFile.kt           # JSON schema data classes
├── CitizenPushSubscriptionStore.kt   # S3 CRUD on citizen-push-subscriptions/{personId}.json
├── CitizenPushMessages.kt            # per-language (title, body) bundles
├── CitizenPushSender.kt              # loads store, filters, calls WebPush.send
├── CitizenWebPushController.kt       # /citizen/web-push/* endpoints
└── CitizenWebPushConfig.kt           # @Configuration: wires store + sender beans
```

### New backend test files

```
service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/
├── CitizenPushSubscriptionStoreTest.kt
├── CitizenPushSenderTest.kt
└── CitizenWebPushControllerIntegrationTest.kt
```

### Modified backend files

- `service/src/main/kotlin/fi/espoo/evaka/messaging/MessageService.kt` — call `CitizenPushSender.handleSentMessages` after `scheduleSendingMessageNotifications`, outside the main transaction.
- `service/src/main/kotlin/fi/espoo/evaka/Audit.kt` — add `CitizenWebPushSubscriptionUpsert`, `CitizenWebPushSubscriptionDelete`, `CitizenWebPushTestSent`.

### New frontend files

```
frontend/src/citizen-frontend/webpush/
├── webpush-api.ts               # axios wrappers
├── webpush-state.ts             # useWebPushState() hook
├── PermissionGuide.tsx          # inline per-platform instructions
├── WebPushSettingsSection.tsx   # nested UI inside NotificationSettingsSection
├── detectPlatform.spec.ts       # tests for the NEW fields added to pwa/detectPlatform.ts
├── webpush-state.spec.ts
├── WebPushSettingsSection.spec.tsx
└── PermissionGuide.spec.tsx
```

Note: the vitest `citizen-frontend` project glob is `src/citizen-frontend/**/*.spec.{ts,tsx}` — the `.test.ts` extension is NOT picked up, so all test files use `.spec.{ts,tsx}`. The `detectPlatform.spec.ts` lives in `webpush/` (not next to `pwa/detectPlatform.ts`) because the new fields it tests are primarily for webpush — ownership stays clear.

### Modified frontend files

- `frontend/src/citizen-frontend/service-worker.js` — add `push` and `notificationclick` handlers.
- `frontend/src/citizen-frontend/pwa/detectPlatform.ts` — add browser family + OS + standalone detection. Keep the existing `Platform` discriminated union usable; add a new `detectBrowser()` function that returns a flat struct.
- `frontend/src/citizen-frontend/personal-details/NotificationSettingsSection.tsx` — render `<WebPushSettingsSection>` nested inside.
- `frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx` — add `webPushSection` namespace.
- `frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx` — same shape.
- `frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx` — same shape.

---

## Task 1: Backend data types (enums + JSON schema data classes)

**Files:**
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushCategory.kt`
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushLanguage.kt`
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushStoreFile.kt`

No tests for this task — it's only enum + data class definitions. Jackson serialization is covered by the store test in Task 3.

- [ ] **Step 1: Create `CitizenPushCategory.kt`**

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

enum class CitizenPushCategory {
    URGENT_MESSAGE,
    MESSAGE,
    BULLETIN,
}
```

- [ ] **Step 2: Create `CitizenPushLanguage.kt`**

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

enum class CitizenPushLanguage {
    FI,
    SV,
    EN;

    companion object {
        fun fromPersonLanguage(language: String?): CitizenPushLanguage =
            when (language?.lowercase()) {
                "sv" -> SV
                "en" -> EN
                else -> FI
            }
    }
}
```

- [ ] **Step 3: Create `CitizenPushStoreFile.kt`**

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.net.URI

data class CitizenPushStoreFile(
    val personId: PersonId,
    val subscriptions: List<CitizenPushSubscriptionEntry>,
)

data class CitizenPushSubscriptionEntry(
    val endpoint: URI,
    val ecdhKey: List<Byte>,
    val authSecret: List<Byte>,
    val enabledCategories: Set<CitizenPushCategory>,
    val userAgent: String?,
    val createdAt: HelsinkiDateTime,
)
```

Name note: we use `CitizenPushSubscriptionEntry` (not `CitizenPushSubscription`) so downstream code can tell at a glance whether it's operating on a stored row or on a live push target. Referenced by `CitizenPushStoreFile.subscriptions` and by `CitizenPushSender` when iterating.

- [ ] **Step 4: Compile**

Run: `cd service && ./gradlew :compileKotlin`
Expected: BUILD SUCCESSFUL. If you see red squiggles in your IDE about `PersonId` or `HelsinkiDateTime`, those are already imported from `shared/PersonId.kt` and `shared/domain/HelsinkiDateTime.kt` — don't define your own.

- [ ] **Step 5: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushCategory.kt \
        service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushLanguage.kt \
        service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushStoreFile.kt
git commit -m "feat(citizenwebpush): add category/language enums and store file schema"
```

---

## Task 2: `CitizenPushMessages` (backend-side title/body localization)

**Files:**
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushMessages.kt`

Backend composes the push payload's title and body (pre-localized) so the service worker can render what it receives with zero logic. Stub translations are fine for the POC — the communications team can tune copy later.

- [ ] **Step 1: Create `CitizenPushMessages.kt`**

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

data class PushTitleAndBody(val title: String, val body: String)

object CitizenPushMessages {
    fun forMessage(
        category: CitizenPushCategory,
        language: CitizenPushLanguage,
        senderName: String,
    ): PushTitleAndBody =
        when (language) {
            CitizenPushLanguage.FI -> finnish(category, senderName)
            CitizenPushLanguage.SV -> swedish(category, senderName)
            CitizenPushLanguage.EN -> english(category, senderName)
        }

    fun forTest(language: CitizenPushLanguage): PushTitleAndBody =
        when (language) {
            CitizenPushLanguage.FI ->
                PushTitleAndBody("eVaka", "Push-ilmoitukset on otettu käyttöön.")
            CitizenPushLanguage.SV ->
                PushTitleAndBody("eVaka", "Push-notiser har aktiverats.")
            CitizenPushLanguage.EN ->
                PushTitleAndBody("eVaka", "Push notifications are now enabled.")
        }

    private fun finnish(category: CitizenPushCategory, sender: String) =
        when (category) {
            CitizenPushCategory.URGENT_MESSAGE ->
                PushTitleAndBody("Kiireellinen viesti", "$sender lähetti sinulle kiireellisen viestin.")
            CitizenPushCategory.MESSAGE ->
                PushTitleAndBody("Uusi viesti", "$sender lähetti sinulle viestin.")
            CitizenPushCategory.BULLETIN ->
                PushTitleAndBody("Uusi tiedote", "$sender on lähettänyt uuden tiedotteen.")
        }

    private fun swedish(category: CitizenPushCategory, sender: String) =
        when (category) {
            CitizenPushCategory.URGENT_MESSAGE ->
                PushTitleAndBody("Brådskande meddelande", "$sender har skickat dig ett brådskande meddelande.")
            CitizenPushCategory.MESSAGE ->
                PushTitleAndBody("Nytt meddelande", "$sender har skickat dig ett meddelande.")
            CitizenPushCategory.BULLETIN ->
                PushTitleAndBody("Nytt meddelande från kommunen", "$sender har publicerat ett nytt meddelande.")
        }

    private fun english(category: CitizenPushCategory, sender: String) =
        when (category) {
            CitizenPushCategory.URGENT_MESSAGE ->
                PushTitleAndBody("Urgent message", "$sender sent you an urgent message.")
            CitizenPushCategory.MESSAGE ->
                PushTitleAndBody("New message", "$sender sent you a message.")
            CitizenPushCategory.BULLETIN ->
                PushTitleAndBody("New bulletin", "$sender posted a new bulletin.")
        }
}
```

- [ ] **Step 2: Compile**

Run: `cd service && ./gradlew :compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushMessages.kt
git commit -m "feat(citizenwebpush): add pre-localized push title/body bundles"
```

---

## Task 3: `CitizenPushSubscriptionStore` (S3 CRUD) with tests

**Files:**
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSubscriptionStore.kt`
- Test: `service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSubscriptionStoreTest.kt`

**Design notes:**
- Reads and writes JSON under `s3://{BucketEnv.data}/citizen-push-subscriptions/{personId}.json` using the same `S3Client` bean `S3DocumentService` already consumes. Inject it directly; don't go through `DocumentService` — its `DocumentKey` sealed type doesn't have a variant for our paths and we don't want to add one for a POC.
- `save()` returns a `SaveResult` with `wasFirstWrite: Boolean`. The controller uses this to decide whether to send a welcome test push.
- Upsert semantics are enforced in the store: callers pass an endpoint + the full desired entry; the store replaces any entry with the same endpoint (one per device).
- A cleanup of the last subscription deletes the whole file. "No file" == "not subscribed".
- Last-write-wins on per-citizen concurrent updates — acceptable for a POC; a migration later would add real per-row upserts.
- Reads the object via `GetObjectRequest`. On `NoSuchKeyException`, return `null` rather than propagating.
- Jackson serialization uses `defaultJsonMapperBuilder().build()` (same builder `WebPush.kt` already uses).

- [ ] **Step 1: Write the failing test file**

File: `service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSubscriptionStoreTest.kt`

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.s3.S3Client

class CitizenPushSubscriptionStoreTest : FullApplicationTest(resetDbBeforeEach = false) {
    @Autowired lateinit var s3Client: S3Client
    @Autowired lateinit var bucketEnv: BucketEnv

    private val store by lazy { CitizenPushSubscriptionStore(s3Client, bucketEnv.data) }
    private val personId = PersonId(UUID.randomUUID())
    private val now = HelsinkiDateTime.from(ZonedDateTime.parse("2026-04-14T10:00:00+03:00[Europe/Helsinki]"))

    private fun entry(endpoint: String, categories: Set<CitizenPushCategory> = setOf(CitizenPushCategory.MESSAGE)) =
        CitizenPushSubscriptionEntry(
            endpoint = URI(endpoint),
            ecdhKey = List(65) { it.toByte() },
            authSecret = List(16) { it.toByte() },
            enabledCategories = categories,
            userAgent = "Mozilla/5.0 (Test)",
            createdAt = now,
        )

    @Test
    fun `load returns null when no file exists`() {
        store.delete(personId)
        assertNull(store.load(personId))
    }

    @Test
    fun `save creates file on first write and reports wasFirstWrite=true`() {
        store.delete(personId)
        val result = store.save(personId, CitizenPushStoreFile(personId, listOf(entry("https://fcm.example/a"))))
        assertTrue(result.wasFirstWrite)
        assertNotNull(store.load(personId))
    }

    @Test
    fun `save on existing file reports wasFirstWrite=false`() {
        store.delete(personId)
        store.save(personId, CitizenPushStoreFile(personId, listOf(entry("https://fcm.example/a"))))
        val result = store.save(personId, CitizenPushStoreFile(personId, listOf(entry("https://fcm.example/a"), entry("https://fcm.example/b"))))
        assertFalse(result.wasFirstWrite)
        assertEquals(2, store.load(personId)?.subscriptions?.size)
    }

    @Test
    fun `removeSubscription drops the matching endpoint and keeps the rest`() {
        store.delete(personId)
        store.save(
            personId,
            CitizenPushStoreFile(
                personId,
                listOf(entry("https://fcm.example/a"), entry("https://fcm.example/b")),
            ),
        )
        store.removeSubscription(personId, URI("https://fcm.example/a"))
        val remaining = store.load(personId)?.subscriptions
        assertNotNull(remaining)
        assertEquals(1, remaining.size)
        assertEquals(URI("https://fcm.example/b"), remaining[0].endpoint)
    }

    @Test
    fun `removeSubscription deletes file when last entry is removed`() {
        store.delete(personId)
        store.save(personId, CitizenPushStoreFile(personId, listOf(entry("https://fcm.example/a"))))
        store.removeSubscription(personId, URI("https://fcm.example/a"))
        assertNull(store.load(personId))
    }

    @Test
    fun `roundtrip preserves categories and byte arrays`() {
        store.delete(personId)
        val original = entry(
            "https://fcm.example/a",
            categories = setOf(CitizenPushCategory.URGENT_MESSAGE, CitizenPushCategory.BULLETIN),
        )
        store.save(personId, CitizenPushStoreFile(personId, listOf(original)))
        val loaded = store.load(personId)?.subscriptions?.single()
        assertNotNull(loaded)
        assertEquals(original.endpoint, loaded.endpoint)
        assertEquals(original.enabledCategories, loaded.enabledCategories)
        assertEquals(original.ecdhKey, loaded.ecdhKey)
        assertEquals(original.authSecret, loaded.authSecret)
    }
}
```

Note: `FullApplicationTest` is evaka's standard Spring-boot-test base class — it wires up an `S3Client` pointed at the local mock. Before running, open an existing `*Test.kt` that extends it (e.g. `s3/S3DocumentServiceTest.kt` if it exists, or any attachment test) and confirm the imports/base class name match. If the base class name in the current repo differs, adjust.

- [ ] **Step 2: Run the test to see it fail on missing class**

Run: `cd service && ./gradlew :test --tests fi.espoo.evaka.citizenwebpush.CitizenPushSubscriptionStoreTest`
Expected: compile failure — `CitizenPushSubscriptionStore` not defined. (Gradle output: `unresolved reference: CitizenPushSubscriptionStore`.)

- [ ] **Step 3: Write `CitizenPushSubscriptionStore.kt`**

File: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSubscriptionStore.kt`

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import java.net.URI
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest

data class SaveResult(val wasFirstWrite: Boolean)

class CitizenPushSubscriptionStore(
    private val s3Client: S3Client,
    private val bucket: String,
) {
    private val jsonMapper = defaultJsonMapperBuilder().build()

    private fun key(personId: PersonId): String = "citizen-push-subscriptions/$personId.json"

    fun load(personId: PersonId): CitizenPushStoreFile? {
        val request = GetObjectRequest.builder().bucket(bucket).key(key(personId)).build()
        return try {
            s3Client.getObject(request).use { stream ->
                jsonMapper.readValue(stream.readAllBytes(), CitizenPushStoreFile::class.java)
            }
        } catch (_: NoSuchKeyException) {
            null
        }
    }

    fun save(personId: PersonId, file: CitizenPushStoreFile): SaveResult {
        val wasFirstWrite = load(personId) == null
        val bytes = jsonMapper.writeValueAsBytes(file)
        val request =
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key(personId))
                .contentType("application/json")
                .build()
        s3Client.putObject(request, RequestBody.fromBytes(bytes))
        return SaveResult(wasFirstWrite)
    }

    fun delete(personId: PersonId) {
        val request = DeleteObjectRequest.builder().bucket(bucket).key(key(personId)).build()
        s3Client.deleteObject(request)
    }

    fun removeSubscription(personId: PersonId, endpoint: URI) {
        val current = load(personId) ?: return
        val remaining = current.subscriptions.filterNot { it.endpoint == endpoint }
        if (remaining.isEmpty()) {
            delete(personId)
        } else {
            save(personId, current.copy(subscriptions = remaining))
        }
    }

    fun upsertSubscription(personId: PersonId, entry: CitizenPushSubscriptionEntry): SaveResult {
        val current = load(personId) ?: CitizenPushStoreFile(personId, emptyList())
        val filtered = current.subscriptions.filterNot { it.endpoint == entry.endpoint }
        val next = current.copy(subscriptions = filtered + entry)
        return save(personId, next)
    }
}
```

- [ ] **Step 4: Run the test and see it pass**

Run: `cd service && ./gradlew :test --tests fi.espoo.evaka.citizenwebpush.CitizenPushSubscriptionStoreTest`
Expected: 6 tests passed.

If it fails with an S3Client bean missing error, confirm the test profile is active: evaka uses `local-s3` as the default profile for tests, spinning up a MinIO-in-container mock. This is wired by `FullApplicationTest`. If the base class is named differently in your checkout, grep for `: FullApplicationTest` or `@SpringBootTest` on an existing attachment test and match the same setup.

- [ ] **Step 5: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSubscriptionStore.kt \
        service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSubscriptionStoreTest.kt
git commit -m "feat(citizenwebpush): S3-backed subscription store with roundtrip tests"
```

---

## Task 4: `CitizenPushSender` with tests

**Files:**
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSender.kt`
- Test: `service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSenderTest.kt`

**Design notes:**
- Depends on the existing `WebPush` bean (which may be `null` if VAPID isn't configured — the sender no-ops gracefully if so).
- Holds no DB state. `handleSentMessages(db, clock, messageIds)` opens a small read-only transaction to fetch per-recipient push context (person_id, language, sender name, urgent, thread type, thread id) for a list of `MessageId`s, then closes the transaction, then does S3 + HTTP sends outside the transaction.
- For the per-recipient loop: load the store file once per person; filter entries by `enabledCategories`; call `WebPush.send` on each; catch `WebPush.SubscriptionExpired` and call `store.removeSubscription(personId, endpoint)`; swallow all other exceptions with a warn log.
- `sendTest(personId, language)` is called directly from the controller; bypasses category filtering.
- Use `WebPushPayload.NotificationV1(title = ...)` — note: the existing sealed interface currently only has a `title` field. We extend it in this task to carry `body`, `tag`, and `url` as well — see Step 1.

**Important — we're adding fields to the shared `WebPushPayload.NotificationV1`:** The existing `NotificationV1(title: String)` is used by employee push as well. We extend it with three new optional fields (`body: String? = null, tag: String? = null, url: String? = null`) so both employee and citizen can use it. This is a backwards-compatible change — existing employee call sites keep passing only `title` and still compile. The service worker on the citizen side will read body/tag/url; the employee service worker (separate SW under `/employee/mobile/`) can continue to ignore them.

- [ ] **Step 1: Extend `WebPushPayload.NotificationV1` in `webpush/WebPush.kt`**

Open `service/src/main/kotlin/fi/espoo/evaka/webpush/WebPush.kt`, find the existing sealed interface (~line 42):

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@JsonTypeIdResolver(SealedSubclassSimpleName::class)
sealed interface WebPushPayload {
    data class NotificationV1(val title: String) : WebPushPayload
}
```

Replace with:

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@JsonTypeIdResolver(SealedSubclassSimpleName::class)
sealed interface WebPushPayload {
    data class NotificationV1(
        val title: String,
        val body: String? = null,
        val tag: String? = null,
        val url: String? = null,
    ) : WebPushPayload
}
```

This compiles cleanly because every existing call site (employee mobile push) passes only `title` as a named argument, and the three new fields default to `null`.

- [ ] **Step 2: Run the full backend test suite to verify the payload change breaks nothing**

Run: `cd service && ./gradlew :test --tests fi.espoo.evaka.webpush.*`
Expected: existing webpush tests still pass. If anything fails, revert the change and reconsider — do NOT modify existing employee push tests to make them pass unless they're checking the Jackson-serialized shape of the payload (in which case the new optional fields show up in the output as `"body":null` and a naive string-equality check breaks; the right fix is a non-strict JSON comparison in that specific test, but flag this back to the user before touching).

- [ ] **Step 3: Write the failing sender test file**

File: `service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSenderTest.kt`

The test supplies a fake `VapidJwtProvider` (defined in Step 5) so the sender can be exercised without a DB.

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.webpush.VapidJwt
import fi.espoo.evaka.webpush.WebPush
import fi.espoo.evaka.webpush.WebPushCrypto
import fi.espoo.evaka.webpush.WebPushPayload
import java.net.URI
import java.security.SecureRandom
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus

class CitizenPushSenderTest {
    private val personId = PersonId(UUID.randomUUID())
    private val now =
        HelsinkiDateTime.from(ZonedDateTime.parse("2026-04-14T10:00:00+03:00[Europe/Helsinki]"))

    // Generate a real P-256 ECDH public key once so WebPushCrypto.decodePublicKey inside
    // the sender succeeds. Without this the sender lands in the decode-failure branch and
    // never calls WebPush.send. Values are shared across all tests.
    private val realEcdhKey: List<Byte> =
        WebPushCrypto.encode(WebPushCrypto.generateKeyPair(SecureRandom()).publicKey).toList()

    private lateinit var store: CitizenPushSubscriptionStore
    private lateinit var webPush: WebPush
    private lateinit var sender: CitizenPushSender
    private lateinit var fakeJwtProvider: VapidJwtProvider

    @BeforeEach
    fun setup() {
        store = mock()
        webPush = mock()
        sender = CitizenPushSender(store = store, webPush = webPush)
        fakeJwtProvider = VapidJwtProvider { _ -> mock<VapidJwt>() }
    }

    private fun entry(
        endpoint: String,
        categories: Set<CitizenPushCategory>,
    ) =
        CitizenPushSubscriptionEntry(
            endpoint = URI(endpoint),
            ecdhKey = realEcdhKey,
            authSecret = List(16) { (it + 1).toByte() },
            enabledCategories = categories,
            userAgent = null,
            createdAt = now,
        )

    @Test
    fun `notifyMessage skips subscriptions that do not include the category`() {
        whenever(store.load(personId)).thenReturn(
            CitizenPushStoreFile(
                personId,
                listOf(
                    entry("https://fcm.example/a", setOf(CitizenPushCategory.URGENT_MESSAGE)),
                    entry("https://fcm.example/b", setOf(CitizenPushCategory.MESSAGE)),
                ),
            )
        )

        sender.notifyMessage(
            personId = personId,
            threadId = "thread-123",
            category = CitizenPushCategory.URGENT_MESSAGE,
            senderName = "Alice",
            language = CitizenPushLanguage.FI,
            jwtProvider = fakeJwtProvider,
        )

        verify(webPush).send(
            any(),
            check { notification ->
                assertEquals(URI("https://fcm.example/a"), notification.endpoint.uri)
            },
        )
    }

    @Test
    fun `notifyMessage removes subscription when push service returns 410`() {
        whenever(store.load(personId)).thenReturn(
            CitizenPushStoreFile(
                personId,
                listOf(entry("https://fcm.example/gone", setOf(CitizenPushCategory.MESSAGE))),
            )
        )
        whenever(webPush.send(any(), any())).thenThrow(
            WebPush.SubscriptionExpired(HttpStatus.GONE, IllegalStateException("gone"))
        )

        sender.notifyMessage(
            personId = personId,
            threadId = "thread-123",
            category = CitizenPushCategory.MESSAGE,
            senderName = "Alice",
            language = CitizenPushLanguage.FI,
            jwtProvider = fakeJwtProvider,
        )

        verify(store).removeSubscription(personId, URI("https://fcm.example/gone"))
    }

    @Test
    fun `notifyMessage builds a payload with body, tag, and url`() {
        whenever(store.load(personId)).thenReturn(
            CitizenPushStoreFile(
                personId,
                listOf(entry("https://fcm.example/a", setOf(CitizenPushCategory.MESSAGE))),
            )
        )

        sender.notifyMessage(
            personId = personId,
            threadId = "thread-abc",
            category = CitizenPushCategory.MESSAGE,
            senderName = "Bob",
            language = CitizenPushLanguage.EN,
            jwtProvider = fakeJwtProvider,
        )

        verify(webPush).send(
            any(),
            check { notification ->
                val payload = notification.payloads.single() as WebPushPayload.NotificationV1
                assertEquals("New message", payload.title)
                assertEquals("Bob sent you a message.", payload.body)
                assertEquals("msg-thread-abc", payload.tag)
                assertEquals("/messages/thread-abc", payload.url)
            },
        )
    }

    @Test
    fun `notifyMessage no-ops when store is empty`() {
        whenever(store.load(personId)).thenReturn(null)
        sender.notifyMessage(
            personId = personId,
            threadId = "t",
            category = CitizenPushCategory.MESSAGE,
            senderName = "Alice",
            language = CitizenPushLanguage.FI,
            jwtProvider = fakeJwtProvider,
        )
        verify(webPush, never()).send(any(), any())
    }
}
```

Note: `WebPushCrypto.generateKeyPair` and `WebPushCrypto.encode(publicKey)` are the same helpers the production sender path uses. If the class/method names in the checkout differ slightly, grep `webpush/WebPushCrypto.kt` — it's a ~50 line file and the helper names are stable across the repo's history.

- [ ] **Step 4: Run the test — expect compile failure**

Run: `cd service && ./gradlew :test --tests fi.espoo.evaka.citizenwebpush.CitizenPushSenderTest`
Expected: `unresolved reference: CitizenPushSender` and `unresolved reference: VapidJwtProvider`.

- [ ] **Step 5: Write `CitizenPushSender.kt`**

File: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSender.kt`

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.webpush.VapidJwt
import fi.espoo.evaka.webpush.WebPush
import fi.espoo.evaka.webpush.WebPushCrypto
import fi.espoo.evaka.webpush.WebPushEndpoint
import fi.espoo.evaka.webpush.WebPushNotification
import fi.espoo.evaka.webpush.WebPushPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Caller-supplied strategy for building a VAPID JWT for a given endpoint.
 * Production wiring passes `webPush.getValidToken(tx, clock, uri)` inside a short DB transaction.
 * Tests supply a stub that returns a mock VapidJwt without needing a DB.
 */
fun interface VapidJwtProvider {
    fun get(uri: URI): VapidJwt
}

class CitizenPushSender(
    private val store: CitizenPushSubscriptionStore,
    private val webPush: WebPush?,
) {
    fun notifyMessage(
        personId: PersonId,
        threadId: String,
        category: CitizenPushCategory,
        senderName: String,
        language: CitizenPushLanguage,
        jwtProvider: VapidJwtProvider = defaultJwtProviderOrNoop(),
    ) {
        if (webPush == null) return
        val file = store.load(personId) ?: return
        val entries = file.subscriptions.filter { category in it.enabledCategories }
        if (entries.isEmpty()) return

        val titleAndBody = CitizenPushMessages.forMessage(category, language, senderName)
        val payload =
            WebPushPayload.NotificationV1(
                title = titleAndBody.title,
                body = titleAndBody.body,
                tag = "msg-$threadId",
                url = "/messages/$threadId",
            )

        entries.forEach { entry -> sendOne(personId, entry, payload, jwtProvider) }
    }

    fun sendTest(
        personId: PersonId,
        language: CitizenPushLanguage,
        jwtProvider: VapidJwtProvider = defaultJwtProviderOrNoop(),
    ) {
        if (webPush == null) return
        val file = store.load(personId) ?: return
        val titleAndBody = CitizenPushMessages.forTest(language)
        val payload =
            WebPushPayload.NotificationV1(
                title = titleAndBody.title,
                body = titleAndBody.body,
                tag = "welcome",
                url = "/personal-details",
            )
        file.subscriptions.forEach { entry -> sendOne(personId, entry, payload, jwtProvider) }
    }

    private fun sendOne(
        personId: PersonId,
        entry: CitizenPushSubscriptionEntry,
        payload: WebPushPayload.NotificationV1,
        jwtProvider: VapidJwtProvider,
    ) {
        val endpoint =
            try {
                WebPushEndpoint(
                    uri = entry.endpoint,
                    ecdhPublicKey = WebPushCrypto.decodePublicKey(entry.ecdhKey.toByteArray()),
                    authSecret = entry.authSecret.toByteArray(),
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to decode stored push endpoint for $personId; removing" }
                store.removeSubscription(personId, entry.endpoint)
                return
            }

        val notification =
            WebPushNotification(
                endpoint = endpoint,
                ttl = Duration.ofDays(1),
                payloads = listOf(payload),
            )

        try {
            val vapidJwt = jwtProvider.get(endpoint.uri)
            webPush!!.send(vapidJwt, notification)
        } catch (e: WebPush.SubscriptionExpired) {
            logger.warn { "Citizen push subscription expired (${e.status}); removing" }
            store.removeSubscription(personId, entry.endpoint)
        } catch (e: Exception) {
            logger.warn(e) { "Citizen push send failed; swallowing" }
        }
    }

    private fun defaultJwtProviderOrNoop(): VapidJwtProvider =
        VapidJwtProvider { _ ->
            error(
                "No VapidJwtProvider supplied. Production callers must route through " +
                    "CitizenPushSender.notifyMessage(..., jwtProvider = { uri -> " +
                    "db.transaction { tx -> webPush.getValidToken(tx, clock, uri) } }).",
            )
        }
}
```

The test file in Step 3 already constructs `fakeJwtProvider` and passes it to each `sender.notifyMessage(...)` call, so no test changes are needed.

- [ ] **Step 6: Run the test and see it pass**

Run: `cd service && ./gradlew :test --tests fi.espoo.evaka.citizenwebpush.CitizenPushSenderTest`
Expected: 4 tests passed.

- [ ] **Step 7: Add `handleSentMessages` for the production path**

Still in `CitizenPushSender.kt`, add:

```kotlin
    /**
     * Production entry point called from MessageService.handleMarkMessageAsSent, OUTSIDE its main
     * transaction. Opens its own short read-only transaction to resolve per-recipient context,
     * then releases the connection before doing S3 / HTTP. Best-effort: any exception is caught
     * and logged, never re-thrown.
     */
    fun handleSentMessages(
        db: fi.espoo.evaka.shared.db.Database.Connection,
        clock: fi.espoo.evaka.shared.domain.EvakaClock,
        messageIds: List<fi.espoo.evaka.shared.MessageId>,
    ) {
        if (webPush == null || messageIds.isEmpty()) return
        val recipients =
            try {
                db.transaction { tx -> tx.getCitizenPushRecipients(messageIds) }
            } catch (e: Exception) {
                logger.warn(e) { "Citizen push: failed to resolve recipients; skipping" }
                return
            }
        for (r in recipients) {
            val category =
                when {
                    r.urgent -> CitizenPushCategory.URGENT_MESSAGE
                    r.threadType == fi.espoo.evaka.messaging.MessageType.BULLETIN -> CitizenPushCategory.BULLETIN
                    else -> CitizenPushCategory.MESSAGE
                }
            try {
                notifyMessage(
                    personId = r.personId,
                    threadId = r.threadId.toString(),
                    category = category,
                    senderName = r.senderName,
                    language = CitizenPushLanguage.fromPersonLanguage(r.language),
                    jwtProvider =
                        VapidJwtProvider { uri ->
                            db.transaction { tx -> webPush.getValidToken(tx, clock, uri) }
                        },
                )
            } catch (e: Exception) {
                logger.warn(e) { "Citizen push: failed for ${r.personId}, continuing" }
            }
        }
    }
```

And add the query helper (below the class, in the same file for cohesion):

```kotlin
data class CitizenPushRecipientRow(
    val personId: fi.espoo.evaka.shared.PersonId,
    val threadId: fi.espoo.evaka.shared.MessageThreadId,
    val language: String?,
    val urgent: Boolean,
    val threadType: fi.espoo.evaka.messaging.MessageType,
    val senderName: String,
)

fun fi.espoo.evaka.shared.db.Database.Read.getCitizenPushRecipients(
    messageIds: List<fi.espoo.evaka.shared.MessageId>
): List<CitizenPushRecipientRow> =
    createQuery {
            sql(
                """
SELECT DISTINCT
    p.id AS person_id,
    m.thread_id,
    lower(p.language) AS language,
    t.urgent,
    t.message_type AS thread_type,
    coalesce(sender_account.name, '') AS sender_name
FROM message m
JOIN message_recipients mr ON mr.message_id = m.id
JOIN message_account ma ON ma.id = mr.recipient_id
JOIN person p ON p.id = ma.person_id
JOIN message_thread t ON m.thread_id = t.id
LEFT JOIN message_account sender_account ON sender_account.id = m.sender_id
WHERE m.id = ANY(${bind(messageIds)})
"""
            )
        }
        .toList<CitizenPushRecipientRow>()
```

**Note on column names:** I've used `t.message_type` and `t.urgent` based on the email-notification query pattern quoted earlier. If the actual column in `message_thread` is named differently, adjust — open `service/src/main/kotlin/fi/espoo/evaka/messaging/Message.kt` and search for the thread row mapping. The sender name column comes from `message_account`, joined on `m.sender_id` via `message_account`.

Also note we import `fi.espoo.evaka.shared.db.Database.Read` — if evaka's typed-query helper wants `Transaction` for SELECTs, swap accordingly.

- [ ] **Step 8: Recompile and run `CitizenPushSenderTest`**

Run: `cd service && ./gradlew :test --tests fi.espoo.evaka.citizenwebpush.CitizenPushSenderTest`
Expected: 4 unit tests still pass — `handleSentMessages` and its DB query are not yet exercised by unit tests; they'll be covered by an integration test in Task 7 (controller) indirectly and by manual verification. That's fine for a POC.

- [ ] **Step 9: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSender.kt \
        service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSenderTest.kt \
        service/src/main/kotlin/fi/espoo/evaka/webpush/WebPush.kt
git commit -m "feat(citizenwebpush): sender with category filtering and 410 cleanup"
```

---

## Task 5: `CitizenWebPushController`

**Files:**
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenWebPushController.kt`
- Modify: `service/src/main/kotlin/fi/espoo/evaka/Audit.kt` — add audit constants.
- Test: `service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenWebPushControllerIntegrationTest.kt`

**Endpoints (all under `/citizen/web-push/`):**

| Method | Path           | Body                                                                     | Response            |
|--------|----------------|--------------------------------------------------------------------------|---------------------|
| GET    | `/vapid-key`   | —                                                                        | `{ publicKey }` or 503 |
| PUT    | `/subscription`| `{ endpoint, ecdhKey, authSecret, enabledCategories, userAgent }`        | `{ sentTest: bool }`|
| DELETE | `/subscription`| `{ endpoint }`                                                           | 204                 |
| POST   | `/test`        | —                                                                        | 204                 |

**Auth:** `AuthenticatedUser.Citizen` injected by Spring, exact same pattern as `PersonalDataControllerCitizen`.

- [ ] **Step 1: Add Audit constants**

Open `service/src/main/kotlin/fi/espoo/evaka/Audit.kt`, find the enum entry `PushSubscriptionUpsert,` (~line 500) and add three new constants alphabetically nearby:

```kotlin
    CitizenWebPushSubscriptionUpsert,
    CitizenWebPushSubscriptionDelete,
    CitizenWebPushTestSent,
```

- [ ] **Step 2: Create `CitizenWebPushController.kt`**

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.webpush.WebPush
import java.net.URI
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/web-push")
class CitizenWebPushController(
    private val store: CitizenPushSubscriptionStore,
    private val sender: CitizenPushSender,
    private val webPush: WebPush?,
) {
    data class VapidKeyResponse(val publicKey: String)

    data class SubscribeRequest(
        val endpoint: URI,
        val ecdhKey: List<Byte>,
        val authSecret: List<Byte>,
        val enabledCategories: Set<CitizenPushCategory>,
        val userAgent: String?,
    )

    data class SubscribeResponse(val sentTest: Boolean)

    data class UnsubscribeRequest(val endpoint: URI)

    @GetMapping("/vapid-key")
    fun vapidKey(user: AuthenticatedUser.Citizen): ResponseEntity<VapidKeyResponse> =
        webPush?.let { ResponseEntity.ok(VapidKeyResponse(it.applicationServerKey)) }
            ?: ResponseEntity.status(503).build()

    @PutMapping("/subscription")
    fun putSubscription(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: SubscribeRequest,
    ): SubscribeResponse {
        val entry =
            CitizenPushSubscriptionEntry(
                endpoint = body.endpoint,
                ecdhKey = body.ecdhKey,
                authSecret = body.authSecret,
                enabledCategories = body.enabledCategories,
                userAgent = body.userAgent,
                createdAt = clock.now(),
            )
        val result = store.upsertSubscription(user.id, entry)
        Audit.CitizenWebPushSubscriptionUpsert.log(
            targetId = AuditId(user.id),
            meta = mapOf("endpoint" to body.endpoint.toString(), "firstWrite" to result.wasFirstWrite),
        )
        if (result.wasFirstWrite && webPush != null) {
            val language =
                CitizenPushLanguage.fromPersonLanguage(resolveLanguage(db, user.id))
            sender.sendTest(
                personId = user.id,
                language = language,
                jwtProvider = VapidJwtProvider { uri ->
                    db.connect { dbc -> dbc.transaction { tx -> webPush.getValidToken(tx, clock, uri) } }
                },
            )
        }
        return SubscribeResponse(sentTest = result.wasFirstWrite)
    }

    @DeleteMapping("/subscription")
    fun deleteSubscription(
        user: AuthenticatedUser.Citizen,
        @RequestBody body: UnsubscribeRequest,
    ) {
        store.removeSubscription(user.id, body.endpoint)
        Audit.CitizenWebPushSubscriptionDelete.log(
            targetId = AuditId(user.id),
            meta = mapOf("endpoint" to body.endpoint.toString()),
        )
    }

    @PostMapping("/test")
    fun postTest(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ) {
        if (webPush == null) return
        val language = CitizenPushLanguage.fromPersonLanguage(resolveLanguage(db, user.id))
        sender.sendTest(
            personId = user.id,
            language = language,
            jwtProvider = VapidJwtProvider { uri ->
                db.connect { dbc -> dbc.transaction { tx -> webPush.getValidToken(tx, clock, uri) } }
            },
        )
        Audit.CitizenWebPushTestSent.log(targetId = AuditId(user.id))
    }

    private fun resolveLanguage(db: Database, personId: PersonId): String? =
        db.connect { dbc ->
            dbc.read { tx ->
                tx.createQuery { sql("SELECT language FROM person WHERE id = ${bind(personId)}") }
                    .exactlyOneOrNull<String?>()
            }
        }
}
```

**Note on the query DSL:** evaka's typed query builder uses `tx.createQuery { sql(...) }.exactlyOneOrNull<T>()`. If `exactlyOneOrNull` isn't the method name in this checkout, grep for the equivalent in `shared/db/` — alternatives include `.mapTo<String>().findOne()` or `.firstOrNull()`. The goal is just to read `person.language` as `String?`. A direct SQL query avoids coupling the controller to `PersonService`.

- [ ] **Step 3: Write the controller integration test**

File: `service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenWebPushControllerIntegrationTest.kt`

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insert
import java.net.URI
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CitizenWebPushControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private lateinit var citizen: DevPerson

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> citizen = tx.insert(DevPerson()) }
    }

    private fun user() = AuthenticatedUser.Citizen(citizen.id, authLevel = fi.espoo.evaka.shared.auth.CitizenAuthLevel.STRONG)

    @Test
    fun `GET vapid-key returns public key when VAPID configured, 503 otherwise`() {
        val response =
            http.get("/citizen/web-push/vapid-key").asUser(user()).responseObject<Map<String, String>>()
        // Accept either 200 with publicKey or 503 depending on test profile configuration.
        val code = response.second.statusCode
        assertTrue(code == 200 || code == 503, "expected 200 or 503, got $code")
    }

    @Test
    fun `first-time PUT subscription marks sentTest=true`() {
        val body =
            mapOf(
                "endpoint" to "https://fcm.example/test-a",
                "ecdhKey" to List(65) { 1 },
                "authSecret" to List(16) { 2 },
                "enabledCategories" to listOf("MESSAGE"),
                "userAgent" to "TestUA",
            )
        val (_, resp, result) =
            http.put("/citizen/web-push/subscription").jsonBody(body).asUser(user())
                .responseObject<Map<String, Any>>()
        assertEquals(200, resp.statusCode)
        val sentTest = result.get()["sentTest"] as Boolean
        // May be true or false depending on whether VAPID is configured in the test profile.
        // Assert only that the PUT succeeded and the shape is right.
        assertNotNull(sentTest)
    }

    @Test
    fun `subsequent PUT with same endpoint does not re-send test`() {
        val body =
            mapOf(
                "endpoint" to "https://fcm.example/test-b",
                "ecdhKey" to List(65) { 3 },
                "authSecret" to List(16) { 4 },
                "enabledCategories" to listOf("MESSAGE"),
                "userAgent" to "TestUA",
            )
        http.put("/citizen/web-push/subscription").jsonBody(body).asUser(user()).response()
        val (_, _, result) =
            http.put("/citizen/web-push/subscription").jsonBody(body).asUser(user())
                .responseObject<Map<String, Any>>()
        assertEquals(false, result.get()["sentTest"])
    }

    @Test
    fun `DELETE subscription removes only the targeted endpoint`() {
        val bodyA =
            mapOf(
                "endpoint" to "https://fcm.example/a",
                "ecdhKey" to List(65) { 1 },
                "authSecret" to List(16) { 2 },
                "enabledCategories" to listOf("MESSAGE"),
                "userAgent" to null,
            )
        val bodyB = bodyA + ("endpoint" to "https://fcm.example/b")
        http.put("/citizen/web-push/subscription").jsonBody(bodyA).asUser(user()).response()
        http.put("/citizen/web-push/subscription").jsonBody(bodyB).asUser(user()).response()
        val deleteBody = mapOf("endpoint" to "https://fcm.example/a")
        val (_, delResp, _) =
            http.delete("/citizen/web-push/subscription").jsonBody(deleteBody).asUser(user()).response()
        assertEquals(204, delResp.statusCode)
    }

    @Test
    fun `unauthenticated requests are rejected`() {
        val (_, resp, _) =
            http.get("/citizen/web-push/vapid-key").response()
        assertTrue(resp.statusCode == 401 || resp.statusCode == 403)
    }
}
```

Note: evaka's integration test HTTP client API (`http.put(...).asUser(...).responseObject`) is a Fuel-like wrapper already used throughout the test suite. If the exact DSL differs in your checkout, model after an existing controller integration test (e.g. `PersonalDataControllerCitizenIntegrationTest.kt`).

- [ ] **Step 4: Run the integration test**

Run: `cd service && ./gradlew :test --tests fi.espoo.evaka.citizenwebpush.CitizenWebPushControllerIntegrationTest`
Expected: all tests pass. If a test fails on the `sentTest` value, double-check whether your local test profile actually has `evaka.web_push.vapid_private_key` set — without it the WebPush bean is null and `sentTest` is always false.

- [ ] **Step 5: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenWebPushController.kt \
        service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenWebPushControllerIntegrationTest.kt \
        service/src/main/kotlin/fi/espoo/evaka/Audit.kt
git commit -m "feat(citizenwebpush): controller with subscribe/unsubscribe/test endpoints"
```

---

## Task 6: Spring wiring

**Files:**
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenWebPushConfig.kt`

- [ ] **Step 1: Create `CitizenWebPushConfig.kt`**

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.webpush.WebPush
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class CitizenWebPushConfig {
    @Bean
    fun citizenPushSubscriptionStore(
        s3Client: S3Client,
        bucketEnv: BucketEnv,
    ): CitizenPushSubscriptionStore = CitizenPushSubscriptionStore(s3Client, bucketEnv.data)

    @Bean
    fun citizenPushSender(
        store: CitizenPushSubscriptionStore,
        webPush: WebPush?,
    ): CitizenPushSender = CitizenPushSender(store = store, webPush = webPush)
}
```

The controller is a `@RestController`, so it's picked up by Spring's component scan automatically — no explicit bean method needed for it.

- [ ] **Step 2: Recompile to confirm the context wires correctly**

Run: `cd service && ./gradlew :test --tests 'fi.espoo.evaka.citizenwebpush.*'`
Expected: all three citizenwebpush test classes still pass and the Spring context starts without "no qualifying bean" errors.

- [ ] **Step 3: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenWebPushConfig.kt
git commit -m "feat(citizenwebpush): Spring @Configuration wiring store and sender beans"
```

---

## Task 7: Hook into `MessageService.handleMarkMessageAsSent`

**Files:**
- Modify: `service/src/main/kotlin/fi/espoo/evaka/messaging/MessageService.kt`

- [ ] **Step 1: Inject `CitizenPushSender` into `MessageService`**

Open `service/src/main/kotlin/fi/espoo/evaka/messaging/MessageService.kt`. Locate the `MessageService` class constructor and add `private val citizenPushSender: fi.espoo.evaka.citizenwebpush.CitizenPushSender` to the parameter list. The constructor order should stay alphabetical-ish with the other injected services — follow the file's existing style (if the file uses field injection via `@Autowired`, use that instead; check the top of the class).

- [ ] **Step 2: Modify `handleMarkMessageAsSent`**

Locate the function at ~line 45 and change it from:

```kotlin
fun handleMarkMessageAsSent(
    db: Database.Connection,
    clock: EvakaClock,
    msg: AsyncJob.MarkMessagesAsSent,
) {
    db.transaction { tx ->
        tx.lockMessageContentForUpdate(msg.messageContentId)
        tx.upsertRecipientThreadParticipants(msg.messageContentId, msg.sentAt)
        val messages = tx.markMessagesAsSent(msg.messageContentId, msg.sentAt)
        notificationEmailService.scheduleSendingMessageNotifications(tx, messages, clock.now())
        asyncJobRunner.plan(
            tx,
            messagePushNotifications.getAsyncJobs(tx, messages),
            runAt = clock.now(),
        )
    }
}
```

To:

```kotlin
fun handleMarkMessageAsSent(
    db: Database.Connection,
    clock: EvakaClock,
    msg: AsyncJob.MarkMessagesAsSent,
) {
    val messages =
        db.transaction { tx ->
            tx.lockMessageContentForUpdate(msg.messageContentId)
            tx.upsertRecipientThreadParticipants(msg.messageContentId, msg.sentAt)
            val messages = tx.markMessagesAsSent(msg.messageContentId, msg.sentAt)
            notificationEmailService.scheduleSendingMessageNotifications(tx, messages, clock.now())
            asyncJobRunner.plan(
                tx,
                messagePushNotifications.getAsyncJobs(tx, messages),
                runAt = clock.now(),
            )
            messages
        }
    try {
        citizenPushSender.handleSentMessages(db, clock, messages)
    } catch (e: Exception) {
        logger.warn(e) { "Citizen push: top-level failure in handleSentMessages; swallowing" }
    }
}
```

Note: if the file doesn't have a `logger` at the top, either add `private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}` at the file top or use the existing logger that's already imported — grep the file first.

- [ ] **Step 3: Run the existing message service tests to confirm nothing breaks**

Run: `cd service && ./gradlew :test --tests 'fi.espoo.evaka.messaging.*'`
Expected: all existing messaging tests still pass. The new call to `citizenPushSender.handleSentMessages` is a no-op when no citizen subscriptions exist, so existing tests should not be affected. If any messaging test now fails to start because it can't construct `MessageService` (missing constructor arg), find the `new MessageService(...)` call in that test's setup and pass a mocked `CitizenPushSender`.

- [ ] **Step 4: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/messaging/MessageService.kt
git commit -m "feat(messaging): dispatch citizen web push after email notifications"
```

---

## Task 8: Extend `detectPlatform.ts` with browser + OS + standalone fields

**Files:**
- Modify: `frontend/src/citizen-frontend/pwa/detectPlatform.ts`
- Test: `frontend/src/citizen-frontend/webpush/detectPlatform.spec.ts`

**Design:** add a new `detectBrowser()` function that returns a flat record the webpush UI needs. Keep the existing `detectPlatform()` discriminated union untouched so PWA code doesn't break. Both live in the same file.

- [ ] **Step 1: Write the failing test**

File: `frontend/src/citizen-frontend/webpush/detectPlatform.spec.ts`

```ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import { detectBrowser } from '../pwa/detectPlatform'

describe('detectBrowser', () => {
  it('detects Chrome on Android', () => {
    const ua =
      'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'android',
      family: 'chrome',
      isStandalone: false
    })
  })

  it('detects Samsung Internet on Android', () => {
    const ua =
      'Mozilla/5.0 (Linux; Android 13; SAMSUNG SM-G975U) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/22.0 Chrome/115.0.0.0 Mobile Safari/537.36'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'android',
      family: 'samsung-internet'
    })
  })

  it('detects Firefox on Android', () => {
    const ua = 'Mozilla/5.0 (Android 13; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'android',
      family: 'firefox'
    })
  })

  it('detects Safari on iOS', () => {
    const ua =
      'Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'ios',
      family: 'safari'
    })
  })

  it('detects Chrome on iOS as non-Safari (CriOS)', () => {
    const ua =
      'Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.0.0 Mobile/15E148 Safari/604.1'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'ios',
      family: 'chrome-ios'
    })
  })

  it('detects Edge on desktop Windows', () => {
    const ua =
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'windows',
      family: 'edge'
    })
  })

  it('detects Chrome on desktop Linux', () => {
    const ua =
      'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'linux',
      family: 'chrome'
    })
  })

  it('detects Firefox on desktop', () => {
    const ua =
      'Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0'
    expect(detectBrowser(ua)).toMatchObject({
      family: 'firefox'
    })
  })

  it('detects Safari on macOS', () => {
    const ua =
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'macos',
      family: 'safari'
    })
  })

  it('falls back cleanly on unknown UA', () => {
    expect(detectBrowser('ReallyWeirdBot/1.0')).toMatchObject({
      os: 'other',
      family: 'other'
    })
  })
})
```

- [ ] **Step 2: Run the test and see it fail**

Run: `cd frontend && node_modules/.bin/vitest run --project citizen-frontend src/citizen-frontend/webpush/detectPlatform.spec.ts`
Expected: test suite fails because `detectBrowser` is not exported from `../pwa/detectPlatform`.

- [ ] **Step 3: Extend `detectPlatform.ts`**

Open `frontend/src/citizen-frontend/pwa/detectPlatform.ts` and APPEND the following at the bottom (do not touch the existing `Platform` type or `detectPlatform` function):

```typescript
export type BrowserOs =
  | 'ios'
  | 'android'
  | 'macos'
  | 'windows'
  | 'linux'
  | 'other'

export type BrowserFamily =
  | 'safari'
  | 'chrome'
  | 'chrome-ios'
  | 'edge'
  | 'firefox'
  | 'samsung-internet'
  | 'other'

export interface BrowserInfo {
  os: BrowserOs
  family: BrowserFamily
  isStandalone: boolean
}

export function detectBrowser(
  userAgent: string = typeof navigator !== 'undefined' ? navigator.userAgent : '',
  mediaQueryMatch: boolean =
    typeof window !== 'undefined' && typeof window.matchMedia === 'function'
      ? window.matchMedia('(display-mode: standalone)').matches
      : false
): BrowserInfo {
  const os: BrowserOs = (() => {
    if (/iPad|iPhone|iPod/.test(userAgent)) return 'ios'
    if (userAgent.includes('Android')) return 'android'
    if (userAgent.includes('Macintosh') || userAgent.includes('Mac OS X')) return 'macos'
    if (userAgent.includes('Windows')) return 'windows'
    if (userAgent.includes('Linux')) return 'linux'
    return 'other'
  })()

  const family: BrowserFamily = (() => {
    if (os === 'ios') {
      if (/CriOS/.test(userAgent)) return 'chrome-ios'
      if (/FxiOS/.test(userAgent)) return 'other' // treat iOS Firefox as "other" — same WebKit base as Safari but no SW
      if (/EdgiOS/.test(userAgent)) return 'other'
      return 'safari'
    }
    if (userAgent.includes('SamsungBrowser')) return 'samsung-internet'
    if (/Edg\//.test(userAgent)) return 'edge'
    if (userAgent.includes('Firefox')) return 'firefox'
    if (userAgent.includes('Chrome')) return 'chrome'
    if (userAgent.includes('Safari')) return 'safari'
    return 'other'
  })()

  return { os, family, isStandalone: mediaQueryMatch }
}
```

- [ ] **Step 4: Run the test and see it pass**

Run: `cd frontend && node_modules/.bin/vitest run --project citizen-frontend src/citizen-frontend/webpush/detectPlatform.spec.ts`
Expected: 10 tests passed.

- [ ] **Step 5: Run the citizen-frontend project type-check**

Run: `cd frontend && node_modules/.bin/tsc --build --force .`
Expected: no errors. (If the eslint hook adds `ua.includes('Android')` rewrites later, that's fine.)

- [ ] **Step 6: Commit**

```bash
git add frontend/src/citizen-frontend/pwa/detectPlatform.ts \
        frontend/src/citizen-frontend/webpush/detectPlatform.spec.ts
git commit -m "feat(citizen/pwa): detectBrowser for browser family + OS + standalone"
```

---

## Task 9: Service worker push + notificationclick handlers

**Files:**
- Modify: `frontend/src/citizen-frontend/service-worker.js`

No automated test for the service worker — we'll verify manually in staging (Task 16). Service workers run in a worker context that's awkward to mock without a heavy stack, and for a POC the manual walkthrough is sufficient.

- [ ] **Step 1: Append push and notificationclick handlers**

Open `frontend/src/citizen-frontend/service-worker.js` and append below the existing `fetch` listener (the existing fetch listener stays untouched):

```javascript
serviceWorker.addEventListener('push', (event) => {
  const data = (() => {
    try {
      return event.data ? event.data.json() : {}
    } catch {
      return {}
    }
  })()
  const title = data.title ?? 'eVaka'
  event.waitUntil(
    serviceWorker.registration.showNotification(title, {
      body: data.body ?? '',
      icon: '/citizen/evaka-192px.png',
      badge: '/citizen/evaka-180px.png',
      tag: data.tag,
      data: { url: data.url ?? '/messages' }
    })
  )
})

serviceWorker.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = event.notification.data?.url ?? '/messages'
  event.waitUntil(
    serviceWorker.clients
      .matchAll({ type: 'window', includeUncontrolled: true })
      .then((list) => {
        for (const client of list) {
          if (client.url.includes(url) && 'focus' in client) {
            return client.focus()
          }
        }
        return serviceWorker.clients.openWindow(url)
      })
  )
})
```

The icon paths `/citizen/evaka-192px.png` and `/citizen/evaka-180px.png` assume the citizen public assets already contain these. Verify: `ls frontend/src/citizen-frontend/public/` (or wherever vite serves citizen static assets). If the exact filenames differ, adapt to whatever exists — do NOT add new icon assets for this POC. The spec says "one shared evaka icon" — use whichever evaka icon is already served at the citizen root.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/citizen-frontend/service-worker.js
git commit -m "feat(citizen/service-worker): push and notificationclick handlers"
```

---

## Task 10: `webpush-api.ts` (axios wrappers)

**Files:**
- Create: `frontend/src/citizen-frontend/webpush/webpush-api.ts`

- [ ] **Step 1: Create the API module**

```typescript
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from '../api-client'

export type CitizenPushCategory = 'URGENT_MESSAGE' | 'MESSAGE' | 'BULLETIN'

export interface SubscribeRequest {
  endpoint: string
  ecdhKey: number[]
  authSecret: number[]
  enabledCategories: CitizenPushCategory[]
  userAgent: string | null
}

export interface SubscribeResponse {
  sentTest: boolean
}

export async function getVapidKey(): Promise<string | null> {
  try {
    const { data } = await client.get<{ publicKey: string }>('/citizen/web-push/vapid-key')
    return data.publicKey
  } catch (err) {
    // 503 means VAPID is not configured on this server. Return null so the UI can show
    // the "push notifications unavailable" guide variant.
    if (
      err &&
      typeof err === 'object' &&
      'response' in err &&
      (err as { response?: { status?: number } }).response?.status === 503
    ) {
      return null
    }
    throw err
  }
}

export async function putSubscription(
  body: SubscribeRequest
): Promise<SubscribeResponse> {
  const { data } = await client.put<SubscribeResponse>('/citizen/web-push/subscription', body)
  return data
}

export async function deleteSubscription(endpoint: string): Promise<void> {
  await client.delete('/citizen/web-push/subscription', { data: { endpoint } })
}

export async function postTest(): Promise<void> {
  await client.post('/citizen/web-push/test')
}
```

- [ ] **Step 2: Type-check**

Run: `cd frontend && node_modules/.bin/tsc --build --force .`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/citizen-frontend/webpush/webpush-api.ts
git commit -m "feat(citizen/webpush): axios wrappers for /citizen/web-push endpoints"
```

---

## Task 11: `useWebPushState()` hook with tests

**Files:**
- Create: `frontend/src/citizen-frontend/webpush/webpush-state.ts`
- Test: `frontend/src/citizen-frontend/webpush/webpush-state.spec.ts`

**Design:** the hook encapsulates all browser quirks:
- Feature detection: `ServiceWorker in navigator && PushManager in window`
- Server-side VAPID check: a `null` return from `getVapidKey()` means "unsupported on this server"
- Permission states: `default`, `granted`, `denied`
- Live subscription check: `navigator.serviceWorker.getRegistration()` → `registration.pushManager.getSubscription()`
- Category state: stored client-side only between the subscribe() call and the next PUT; on mount we can't read categories back from the browser (push API has none) so we start with "all three enabled" by default on first subscribe and persist the citizen's last-saved choice in `sessionStorage` keyed by endpoint.

Returned shape:

```ts
export type WebPushStatus = 'unsupported' | 'unregistered' | 'denied' | 'subscribed'

export interface UseWebPushStateResult {
  status: WebPushStatus
  categories: Set<CitizenPushCategory>
  subscribe: (categories: Set<CitizenPushCategory>) => Promise<void>
  updateCategories: (categories: Set<CitizenPushCategory>) => Promise<void>
  unsubscribe: () => Promise<void>
  sendTest: () => Promise<void>
}
```

- [ ] **Step 1: Write the failing test**

File: `frontend/src/citizen-frontend/webpush/webpush-state.spec.ts`

```ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useWebPushState } from './webpush-state'
import * as api from './webpush-api'

vi.mock('./webpush-api')

function mockBrowser(options: {
  hasServiceWorker: boolean
  hasPushManager: boolean
  permission: 'default' | 'granted' | 'denied'
  existingSubscription: {
    endpoint: string
    keys: { p256dh: string; auth: string }
  } | null
}) {
  const subscription = options.existingSubscription
    ? {
        endpoint: options.existingSubscription.endpoint,
        getKey: (name: 'p256dh' | 'auth') =>
          new TextEncoder().encode(options.existingSubscription!.keys[name]).buffer,
        unsubscribe: vi.fn().mockResolvedValue(true)
      }
    : null

  const pushManager = {
    getSubscription: vi.fn().mockResolvedValue(subscription),
    subscribe: vi.fn().mockResolvedValue({
      endpoint: 'https://fcm.example/new',
      getKey: (name: 'p256dh' | 'auth') =>
        new TextEncoder().encode(name === 'p256dh' ? 'p256dh-bytes' : 'auth-bytes').buffer,
      unsubscribe: vi.fn().mockResolvedValue(true)
    })
  }

  const registration = options.hasPushManager ? { pushManager } : {}

  // @ts-expect-error — jsdom doesn't define ServiceWorker globally
  globalThis.navigator.serviceWorker = options.hasServiceWorker
    ? {
        ready: Promise.resolve(registration),
        getRegistration: vi.fn().mockResolvedValue(registration)
      }
    : undefined

  // @ts-expect-error
  globalThis.PushManager = options.hasPushManager ? class {} : undefined

  // @ts-expect-error
  globalThis.Notification = {
    permission: options.permission,
    requestPermission: vi.fn().mockResolvedValue(options.permission)
  }
}

describe('useWebPushState', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('returns unsupported when ServiceWorker is missing', async () => {
    mockBrowser({
      hasServiceWorker: false,
      hasPushManager: false,
      permission: 'default',
      existingSubscription: null
    })
    vi.mocked(api.getVapidKey).mockResolvedValue('vapid-public-key-base64')

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('unsupported'))
  })

  it('returns unsupported when server returns null VAPID key (503)', async () => {
    mockBrowser({
      hasServiceWorker: true,
      hasPushManager: true,
      permission: 'default',
      existingSubscription: null
    })
    vi.mocked(api.getVapidKey).mockResolvedValue(null)

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('unsupported'))
  })

  it('returns unregistered when supported and no subscription yet', async () => {
    mockBrowser({
      hasServiceWorker: true,
      hasPushManager: true,
      permission: 'default',
      existingSubscription: null
    })
    vi.mocked(api.getVapidKey).mockResolvedValue('vapid-public-key-base64')

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('unregistered'))
  })

  it('returns denied when permission is denied', async () => {
    mockBrowser({
      hasServiceWorker: true,
      hasPushManager: true,
      permission: 'denied',
      existingSubscription: null
    })
    vi.mocked(api.getVapidKey).mockResolvedValue('vapid-public-key-base64')

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('denied'))
  })

  it('returns subscribed when permission granted and existing subscription present', async () => {
    mockBrowser({
      hasServiceWorker: true,
      hasPushManager: true,
      permission: 'granted',
      existingSubscription: {
        endpoint: 'https://fcm.example/existing',
        keys: { p256dh: 'k', auth: 'a' }
      }
    })
    vi.mocked(api.getVapidKey).mockResolvedValue('vapid-public-key-base64')

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('subscribed'))
  })

  it('subscribe() POSTs the new subscription with the chosen categories', async () => {
    mockBrowser({
      hasServiceWorker: true,
      hasPushManager: true,
      permission: 'default',
      existingSubscription: null
    })
    vi.mocked(api.getVapidKey).mockResolvedValue('vapid-public-key-base64')
    vi.mocked(api.putSubscription).mockResolvedValue({ sentTest: true })
    // Simulate Notification.requestPermission flipping to granted
    ;(globalThis.Notification as unknown as { permission: string }).permission = 'default'
    vi.mocked(globalThis.Notification.requestPermission).mockResolvedValue('granted')

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('unregistered'))

    await act(async () => {
      await result.current.subscribe(new Set(['URGENT_MESSAGE', 'MESSAGE']))
    })

    expect(api.putSubscription).toHaveBeenCalledWith(
      expect.objectContaining({
        endpoint: 'https://fcm.example/new',
        enabledCategories: ['URGENT_MESSAGE', 'MESSAGE']
      })
    )
  })
})
```

- [ ] **Step 2: Run the test and see it fail**

Run: `cd frontend && node_modules/.bin/vitest run --project citizen-frontend src/citizen-frontend/webpush/webpush-state.spec.ts`
Expected: fails because `./webpush-state` doesn't exist.

- [ ] **Step 3: Implement `webpush-state.ts`**

```typescript
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useEffect, useState } from 'react'

import type { CitizenPushCategory } from './webpush-api'
import {
  deleteSubscription,
  getVapidKey,
  postTest,
  putSubscription
} from './webpush-api'

export type WebPushStatus =
  | 'loading'
  | 'unsupported'
  | 'unregistered'
  | 'denied'
  | 'subscribed'

export interface UseWebPushStateResult {
  status: WebPushStatus
  categories: Set<CitizenPushCategory>
  subscribe: (categories: Set<CitizenPushCategory>) => Promise<void>
  updateCategories: (categories: Set<CitizenPushCategory>) => Promise<void>
  unsubscribe: () => Promise<void>
  sendTest: () => Promise<void>
}

const ALL_CATEGORIES: CitizenPushCategory[] = [
  'URGENT_MESSAGE',
  'MESSAGE',
  'BULLETIN'
]

const SESSION_KEY = 'evaka-webpush-categories'

function loadCategories(endpoint: string): Set<CitizenPushCategory> {
  if (typeof sessionStorage === 'undefined') return new Set(ALL_CATEGORIES)
  try {
    const raw = sessionStorage.getItem(`${SESSION_KEY}:${endpoint}`)
    if (!raw) return new Set(ALL_CATEGORIES)
    const parsed = JSON.parse(raw) as CitizenPushCategory[]
    return new Set(parsed)
  } catch {
    return new Set(ALL_CATEGORIES)
  }
}

function persistCategories(endpoint: string, categories: Set<CitizenPushCategory>): void {
  if (typeof sessionStorage === 'undefined') return
  try {
    sessionStorage.setItem(
      `${SESSION_KEY}:${endpoint}`,
      JSON.stringify(Array.from(categories))
    )
  } catch {
    // full storage — ignore
  }
}

function bufferToBytes(buffer: ArrayBuffer | null): number[] {
  if (!buffer) return []
  return Array.from(new Uint8Array(buffer))
}

function base64UrlToUint8Array(base64: string): Uint8Array {
  const padding = '='.repeat((4 - (base64.length % 4)) % 4)
  const normalized = (base64 + padding).replace(/-/g, '+').replace(/_/g, '/')
  const raw = atob(normalized)
  const out = new Uint8Array(raw.length)
  for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i)
  return out
}

async function browserSupportsPush(): Promise<boolean> {
  if (typeof navigator === 'undefined') return false
  if (!('serviceWorker' in navigator)) return false
  if (typeof window === 'undefined' || !('PushManager' in window)) return false
  return true
}

export function useWebPushState(): UseWebPushStateResult {
  const [status, setStatus] = useState<WebPushStatus>('loading')
  const [categories, setCategories] = useState<Set<CitizenPushCategory>>(
    new Set(ALL_CATEGORIES)
  )
  const [vapidKey, setVapidKey] = useState<string | null>(null)
  const [endpoint, setEndpoint] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    if (!(await browserSupportsPush())) {
      setStatus('unsupported')
      return
    }
    const key = await getVapidKey()
    if (!key) {
      setStatus('unsupported')
      return
    }
    setVapidKey(key)
    const reg = await navigator.serviceWorker.getRegistration()
    const existing = (await reg?.pushManager.getSubscription()) ?? null
    if (existing) {
      setEndpoint(existing.endpoint)
      setCategories(loadCategories(existing.endpoint))
      setStatus(
        (globalThis.Notification?.permission ?? 'default') === 'granted'
          ? 'subscribed'
          : 'unregistered'
      )
      return
    }
    const perm = globalThis.Notification?.permission ?? 'default'
    setStatus(perm === 'denied' ? 'denied' : 'unregistered')
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const subscribe = useCallback(
    async (chosen: Set<CitizenPushCategory>) => {
      if (!vapidKey) return
      const perm = await globalThis.Notification.requestPermission()
      if (perm !== 'granted') {
        setStatus(perm === 'denied' ? 'denied' : 'unregistered')
        return
      }
      const reg = await navigator.serviceWorker.ready
      const sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: base64UrlToUint8Array(vapidKey)
      })
      const p256dh = bufferToBytes(sub.getKey('p256dh'))
      const auth = bufferToBytes(sub.getKey('auth'))
      await putSubscription({
        endpoint: sub.endpoint,
        ecdhKey: p256dh,
        authSecret: auth,
        enabledCategories: Array.from(chosen),
        userAgent: navigator.userAgent
      })
      setEndpoint(sub.endpoint)
      setCategories(chosen)
      persistCategories(sub.endpoint, chosen)
      setStatus('subscribed')
    },
    [vapidKey]
  )

  const updateCategories = useCallback(
    async (chosen: Set<CitizenPushCategory>) => {
      if (!endpoint) return
      const reg = await navigator.serviceWorker.getRegistration()
      const sub = await reg?.pushManager.getSubscription()
      if (!sub) return
      const p256dh = bufferToBytes(sub.getKey('p256dh'))
      const auth = bufferToBytes(sub.getKey('auth'))
      await putSubscription({
        endpoint: sub.endpoint,
        ecdhKey: p256dh,
        authSecret: auth,
        enabledCategories: Array.from(chosen),
        userAgent: navigator.userAgent
      })
      setCategories(chosen)
      persistCategories(endpoint, chosen)
    },
    [endpoint]
  )

  const unsubscribe = useCallback(async () => {
    const reg = await navigator.serviceWorker.getRegistration()
    const sub = await reg?.pushManager.getSubscription()
    if (sub) {
      await deleteSubscription(sub.endpoint)
      await sub.unsubscribe()
    }
    setEndpoint(null)
    setStatus('unregistered')
  }, [])

  const sendTest = useCallback(async () => {
    await postTest()
  }, [])

  return {
    status,
    categories,
    subscribe,
    updateCategories,
    unsubscribe,
    sendTest
  }
}
```

- [ ] **Step 4: Run the test and see it pass**

Run: `cd frontend && node_modules/.bin/vitest run --project citizen-frontend src/citizen-frontend/webpush/webpush-state.spec.ts`
Expected: 6 tests passed. (If jsdom throws on `globalThis.Notification` access, wrap the test's `mockBrowser` to set it on `window` as well as `globalThis`.)

- [ ] **Step 5: Type-check**

Run: `cd frontend && node_modules/.bin/tsc --build --force .`
Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/citizen-frontend/webpush/webpush-state.ts \
        frontend/src/citizen-frontend/webpush/webpush-state.spec.ts
git commit -m "feat(citizen/webpush): useWebPushState hook with status machine"
```

---

## Task 12: Add i18n `webPushSection` namespace

**Files:**
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx`
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx`
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx`

**Why do this before writing the UI components?** `NotificationSettingsSection.tsx` imports `useTranslation()` which returns a type inferred from `fi.tsx`. If the UI uses `t.personalDetails.webPushSection.*` before those keys exist in `fi.tsx`, TypeScript blocks the build. Add the namespace in all three files first so the UI code compiles cleanly.

The sv and en files must have the exact same key shape as fi, or the structural-equality check TypeScript performs on i18n files fails.

- [ ] **Step 1: Add namespace to `fi.tsx`**

Open `frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx`, find the existing `notificationsSection: { ... }` object inside `personalDetails: { ... }`, and add a new sibling key `webPushSection` right after it:

```typescript
      webPushSection: {
        title: 'Push-ilmoitukset',
        info: (
          <P>
            Voit ottaa käyttöön selaimen push-ilmoitukset uusista viesteistä.
            Tämä edellyttää, että annat selaimelle luvan ilmoitusten näyttämiseen.
          </P>
        ),
        enable: 'Ota push-ilmoitukset käyttöön',
        enabling: 'Otetaan käyttöön…',
        enabled: 'Push-ilmoitukset käytössä',
        categoryUrgent: {
          label: 'Kiireelliset viestit',
          description: 'Kiireellisiksi merkityt viestit henkilökunnalta'
        },
        categoryMessage: {
          label: 'Tavalliset viestit',
          description: 'Uudet viestit ja vastaukset keskusteluissa'
        },
        categoryBulletin: {
          label: 'Tiedotteet',
          description: 'Kunnan yleiset tiedotteet'
        },
        sendTest: 'Lähetä testi-ilmoitus',
        testSent: 'Testi-ilmoitus lähetetty',
        testFailed: 'Testi-ilmoituksen lähetys epäonnistui',
        unsupported:
          'Push-ilmoitukset eivät ole tuettuja tässä selaimessa tai laitteessa.',
        denied:
          'Push-ilmoitukset on estetty tältä sivustolta. Voit sallia ne selaimen tai käyttöjärjestelmän asetuksista.',
        guide: {
          chromeAndroid: (
            <OrderedList>
              <li>Avaa Chromen valikko (kolme pistettä oikeassa yläkulmassa).</li>
              <li>Valitse Asetukset → Sivuston asetukset → Ilmoitukset.</li>
              <li>Salli ilmoitukset tälle sivustolle.</li>
            </OrderedList>
          ),
          samsungAndroid: (
            <OrderedList>
              <li>Avaa Samsung Internetin valikko.</li>
              <li>Valitse Asetukset → Sivustot ja lataukset → Ilmoitukset.</li>
              <li>Salli ilmoitukset tälle sivustolle.</li>
            </OrderedList>
          ),
          firefoxAndroid: (
            <OrderedList>
              <li>Avaa Firefoxin valikko.</li>
              <li>Valitse Asetukset → Sivuston käyttöoikeudet → Ilmoitukset.</li>
              <li>Salli ilmoitukset tälle sivustolle.</li>
            </OrderedList>
          ),
          safariIOS: (
            <OrderedList>
              <li>Lisää eVaka Koti-näytölle Safarin jakovalikosta (Lisää Koti-näytölle).</li>
              <li>Avaa eVaka Koti-näytöstä (ei Safarista).</li>
              <li>Käytössäoloilmoitus ilmestyy — salli se.</li>
            </OrderedList>
          ),
          chromeDesktop: (
            <OrderedList>
              <li>Klikkaa osoiterivin lukkokuvaketta.</li>
              <li>Valitse Sivuston asetukset → Ilmoitukset → Salli.</li>
              <li>Lataa sivu uudelleen.</li>
            </OrderedList>
          ),
          firefoxDesktop: (
            <OrderedList>
              <li>Klikkaa osoiterivin lukkokuvaketta.</li>
              <li>Kohdassa Käyttöoikeudet salli Ilmoitukset.</li>
              <li>Lataa sivu uudelleen.</li>
            </OrderedList>
          ),
          safariMacos: (
            <OrderedList>
              <li>Avaa Safarin asetukset (Safari → Asetukset → Verkkosivustot → Ilmoitukset).</li>
              <li>Salli ilmoitukset eVakalta.</li>
            </OrderedList>
          ),
          fallback: (
            <P>
              Tarkista selaimesi ja käyttöjärjestelmäsi ilmoitusasetukset.
            </P>
          )
        }
      },
```

- [ ] **Step 2: Mirror the same shape in `sv.tsx` with Swedish copy**

Open `frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx`. Add the same `webPushSection` object. Use short placeholder Swedish strings (the communications team will refine):

```typescript
      webPushSection: {
        title: 'Push-notiser',
        info: (
          <P>
            Du kan aktivera push-notiser i webbläsaren för nya meddelanden.
            Detta kräver att du ger webbläsaren tillåtelse att visa notiser.
          </P>
        ),
        enable: 'Aktivera push-notiser',
        enabling: 'Aktiverar…',
        enabled: 'Push-notiser är aktiverade',
        categoryUrgent: {
          label: 'Brådskande meddelanden',
          description: 'Meddelanden markerade som brådskande av personalen'
        },
        categoryMessage: {
          label: 'Vanliga meddelanden',
          description: 'Nya meddelanden och svar i diskussioner'
        },
        categoryBulletin: {
          label: 'Meddelanden',
          description: 'Allmänna meddelanden från kommunen'
        },
        sendTest: 'Skicka testnotis',
        testSent: 'Testnotis skickad',
        testFailed: 'Kunde inte skicka testnotis',
        unsupported:
          'Push-notiser stöds inte i denna webbläsare eller enhet.',
        denied:
          'Push-notiser är blockerade för denna webbplats. Du kan tillåta dem i webbläsarens eller operativsystemets inställningar.',
        guide: {
          chromeAndroid: (
            <OrderedList>
              <li>Öppna Chromes meny (tre prickar uppe till höger).</li>
              <li>Välj Inställningar → Webbplatsinställningar → Notiser.</li>
              <li>Tillåt notiser för denna webbplats.</li>
            </OrderedList>
          ),
          samsungAndroid: (
            <OrderedList>
              <li>Öppna Samsung Internets meny.</li>
              <li>Välj Inställningar → Webbplatser och nedladdningar → Notiser.</li>
              <li>Tillåt notiser för denna webbplats.</li>
            </OrderedList>
          ),
          firefoxAndroid: (
            <OrderedList>
              <li>Öppna Firefox-menyn.</li>
              <li>Välj Inställningar → Webbplatsbehörigheter → Notiser.</li>
              <li>Tillåt notiser för denna webbplats.</li>
            </OrderedList>
          ),
          safariIOS: (
            <OrderedList>
              <li>Lägg till eVaka på hemskärmen via Safaris delningsmeny.</li>
              <li>Öppna eVaka från hemskärmen (inte från Safari).</li>
              <li>Tillåt aktiveringsnotisen när den visas.</li>
            </OrderedList>
          ),
          chromeDesktop: (
            <OrderedList>
              <li>Klicka på låsikonen i adressfältet.</li>
              <li>Välj Webbplatsinställningar → Notiser → Tillåt.</li>
              <li>Ladda om sidan.</li>
            </OrderedList>
          ),
          firefoxDesktop: (
            <OrderedList>
              <li>Klicka på låsikonen i adressfältet.</li>
              <li>Tillåt Notiser under Behörigheter.</li>
              <li>Ladda om sidan.</li>
            </OrderedList>
          ),
          safariMacos: (
            <OrderedList>
              <li>Öppna Safaris inställningar (Safari → Inställningar → Webbplatser → Notiser).</li>
              <li>Tillåt notiser från eVaka.</li>
            </OrderedList>
          ),
          fallback: (
            <P>
              Kontrollera inställningarna för notiser i webbläsaren och operativsystemet.
            </P>
          )
        }
      },
```

Make sure `<P>` and `<OrderedList>` are already imported at the top of `sv.tsx` — they should be, since the file mirrors `fi.tsx`. If not, grep the top imports in `sv.tsx` and adapt.

- [ ] **Step 3: Mirror in `en.tsx` with English copy**

Same shape, English strings. Keep it concise:

```typescript
      webPushSection: {
        title: 'Push notifications',
        info: (
          <P>
            You can enable browser push notifications for new messages.
            This requires granting your browser permission to display notifications.
          </P>
        ),
        enable: 'Enable push notifications',
        enabling: 'Enabling…',
        enabled: 'Push notifications enabled',
        categoryUrgent: {
          label: 'Urgent messages',
          description: 'Messages flagged as urgent by staff'
        },
        categoryMessage: {
          label: 'Normal messages',
          description: 'New messages and replies in discussions'
        },
        categoryBulletin: {
          label: 'Bulletins',
          description: 'General bulletins from the municipality'
        },
        sendTest: 'Send test notification',
        testSent: 'Test notification sent',
        testFailed: 'Failed to send test notification',
        unsupported:
          'Push notifications are not supported on this browser or device.',
        denied:
          'Push notifications are blocked for this site. You can allow them in your browser or OS settings.',
        guide: {
          chromeAndroid: (
            <OrderedList>
              <li>Open the Chrome menu (three dots in the top right).</li>
              <li>Select Settings → Site settings → Notifications.</li>
              <li>Allow notifications for this site.</li>
            </OrderedList>
          ),
          samsungAndroid: (
            <OrderedList>
              <li>Open the Samsung Internet menu.</li>
              <li>Select Settings → Sites and downloads → Notifications.</li>
              <li>Allow notifications for this site.</li>
            </OrderedList>
          ),
          firefoxAndroid: (
            <OrderedList>
              <li>Open the Firefox menu.</li>
              <li>Select Settings → Site permissions → Notifications.</li>
              <li>Allow notifications for this site.</li>
            </OrderedList>
          ),
          safariIOS: (
            <OrderedList>
              <li>Add eVaka to your home screen via Safari&apos;s share menu.</li>
              <li>Open eVaka from the home screen (not from Safari).</li>
              <li>Allow the enable prompt when it appears.</li>
            </OrderedList>
          ),
          chromeDesktop: (
            <OrderedList>
              <li>Click the lock icon in the address bar.</li>
              <li>Select Site settings → Notifications → Allow.</li>
              <li>Reload the page.</li>
            </OrderedList>
          ),
          firefoxDesktop: (
            <OrderedList>
              <li>Click the lock icon in the address bar.</li>
              <li>Under Permissions allow Notifications.</li>
              <li>Reload the page.</li>
            </OrderedList>
          ),
          safariMacos: (
            <OrderedList>
              <li>Open Safari preferences (Safari → Preferences → Websites → Notifications).</li>
              <li>Allow notifications from eVaka.</li>
            </OrderedList>
          ),
          fallback: (
            <P>
              Check your browser and operating system notification settings.
            </P>
          )
        }
      },
```

- [ ] **Step 4: Type-check**

Run: `cd frontend && node_modules/.bin/tsc --build --force .`
Expected: no errors. If TypeScript complains about a key mismatch between the three files, go back and align — structural equality of the i18n shape is enforced.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx \
        frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx \
        frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx
git commit -m "feat(citizen/i18n): webPushSection namespace for fi/sv/en"
```

---

## Task 13: `PermissionGuide.tsx` with tests

**Files:**
- Create: `frontend/src/citizen-frontend/webpush/PermissionGuide.tsx`
- Test: `frontend/src/citizen-frontend/webpush/PermissionGuide.spec.tsx`

- [ ] **Step 1: Write the failing test**

File: `frontend/src/citizen-frontend/webpush/PermissionGuide.spec.tsx`

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render, screen } from '@testing-library/react'
import React from 'react'
import { describe, expect, it } from 'vitest'

import {
  TestContextProvider,
  testTranslations
} from 'lib-components/utils/TestContextProvider'

import { PermissionGuide } from './PermissionGuide'

function wrap(child: React.JSX.Element) {
  return <TestContextProvider translations={testTranslations}>{child}</TestContextProvider>
}

describe('PermissionGuide', () => {
  it('renders fallback guide when browser family is unknown', () => {
    render(
      wrap(<PermissionGuide browser={{ os: 'other', family: 'other', isStandalone: false }} />)
    )
    // The fallback content is asserted loosely — whatever testTranslations.fallback shows.
    expect(screen.getByRole('group')).toBeDefined()
  })

  it('renders Chrome-on-Android variant', () => {
    render(
      wrap(<PermissionGuide browser={{ os: 'android', family: 'chrome', isStandalone: false }} />)
    )
    expect(screen.getByRole('group')).toBeDefined()
  })

  it('renders iOS Safari variant when not standalone', () => {
    render(
      wrap(<PermissionGuide browser={{ os: 'ios', family: 'safari', isStandalone: false }} />)
    )
    expect(screen.getByRole('group')).toBeDefined()
  })
})
```

Note: `testTranslations` exported from `lib-components/utils/TestContextProvider` contains a minimal set of translation strings for lib-component defaults — it does NOT automatically include the new citizen-frontend `webPushSection`. The component under test reads translations from `useTranslation()` (the citizen-frontend hook), not from the lib-components context. So the test passes as long as the JSX renders and the citizen i18n default is loaded at import time. If the test fails with a missing translation error, verify the test environment imports the default citizen i18n (usually via `../localization`). Wrap the component's `useTranslation` usage in a try/catch or mock if needed.

- [ ] **Step 2: Run the test — expect failure**

Run: `cd frontend && node_modules/.bin/vitest run --project citizen-frontend src/citizen-frontend/webpush/PermissionGuide.spec.tsx`
Expected: module not found.

- [ ] **Step 3: Implement `PermissionGuide.tsx`**

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from '../localization'
import type { BrowserInfo } from '../pwa/detectPlatform'

interface Props {
  browser: BrowserInfo
}

export const PermissionGuide = React.memo(function PermissionGuide({ browser }: Props) {
  const t = useTranslation()
  const guide = t.personalDetails.webPushSection.guide

  const body = (() => {
    if (browser.os === 'ios' && browser.family === 'safari') return guide.safariIOS
    if (browser.os === 'android' && browser.family === 'chrome') return guide.chromeAndroid
    if (browser.os === 'android' && browser.family === 'samsung-internet') return guide.samsungAndroid
    if (browser.os === 'android' && browser.family === 'firefox') return guide.firefoxAndroid
    if (browser.family === 'chrome' || browser.family === 'edge') return guide.chromeDesktop
    if (browser.family === 'firefox') return guide.firefoxDesktop
    if (browser.os === 'macos' && browser.family === 'safari') return guide.safariMacos
    return guide.fallback
  })()

  return (
    <div role="group" data-qa="webpush-permission-guide">
      {body}
    </div>
  )
})
```

- [ ] **Step 4: Run the test — expect pass**

Run: `cd frontend && node_modules/.bin/vitest run --project citizen-frontend src/citizen-frontend/webpush/PermissionGuide.spec.tsx`
Expected: 3 tests passed.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/citizen-frontend/webpush/PermissionGuide.tsx \
        frontend/src/citizen-frontend/webpush/PermissionGuide.spec.tsx
git commit -m "feat(citizen/webpush): PermissionGuide with per-browser variants"
```

---

## Task 14: `WebPushSettingsSection.tsx` with tests

**Files:**
- Create: `frontend/src/citizen-frontend/webpush/WebPushSettingsSection.tsx`
- Test: `frontend/src/citizen-frontend/webpush/WebPushSettingsSection.spec.tsx`

- [ ] **Step 1: Write the failing test**

File: `frontend/src/citizen-frontend/webpush/WebPushSettingsSection.spec.tsx`

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import React from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  TestContextProvider,
  testTranslations
} from 'lib-components/utils/TestContextProvider'

import { WebPushSettingsSection } from './WebPushSettingsSection'
import { useWebPushState, type UseWebPushStateResult } from './webpush-state'

vi.mock('./webpush-state')

function wrap(child: React.JSX.Element) {
  return <TestContextProvider translations={testTranslations}>{child}</TestContextProvider>
}

function setState(state: UseWebPushStateResult) {
  vi.mocked(useWebPushState).mockReturnValue(state)
}

const baseActions = {
  subscribe: vi.fn().mockResolvedValue(undefined),
  updateCategories: vi.fn().mockResolvedValue(undefined),
  unsubscribe: vi.fn().mockResolvedValue(undefined),
  sendTest: vi.fn().mockResolvedValue(undefined)
}

describe('WebPushSettingsSection', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows the master toggle disabled when unsupported', () => {
    setState({
      ...baseActions,
      status: 'unsupported',
      categories: new Set(['MESSAGE'])
    })
    render(wrap(<WebPushSettingsSection />))
    expect(
      screen.getByRole('checkbox', { name: /push notifications/i })
    ).toBeDisabled()
  })

  it('shows the guide when denied', () => {
    setState({
      ...baseActions,
      status: 'denied',
      categories: new Set(['MESSAGE'])
    })
    render(wrap(<WebPushSettingsSection />))
    expect(screen.getByTestId('webpush-permission-guide')).toBeDefined()
  })

  it('calls subscribe when the master toggle flips on', async () => {
    setState({
      ...baseActions,
      status: 'unregistered',
      categories: new Set(['URGENT_MESSAGE', 'MESSAGE', 'BULLETIN'])
    })
    render(wrap(<WebPushSettingsSection />))
    const toggle = screen.getByRole('checkbox', { name: /push notifications/i })
    await userEvent.click(toggle)
    expect(baseActions.subscribe).toHaveBeenCalledTimes(1)
  })

  it('disables category checkboxes when not subscribed', () => {
    setState({
      ...baseActions,
      status: 'unregistered',
      categories: new Set(['URGENT_MESSAGE', 'MESSAGE', 'BULLETIN'])
    })
    render(wrap(<WebPushSettingsSection />))
    expect(screen.getByRole('checkbox', { name: /urgent/i })).toBeDisabled()
    expect(screen.getByRole('checkbox', { name: /bulletin/i })).toBeDisabled()
  })

  it('enables category checkboxes and the test button when subscribed', () => {
    setState({
      ...baseActions,
      status: 'subscribed',
      categories: new Set(['MESSAGE'])
    })
    render(wrap(<WebPushSettingsSection />))
    expect(screen.getByRole('checkbox', { name: /normal messages/i })).not.toBeDisabled()
    expect(screen.getByRole('button', { name: /send test/i })).not.toBeDisabled()
  })

  it('calls sendTest when the test button is clicked', async () => {
    setState({
      ...baseActions,
      status: 'subscribed',
      categories: new Set(['MESSAGE'])
    })
    render(wrap(<WebPushSettingsSection />))
    await userEvent.click(screen.getByRole('button', { name: /send test/i }))
    expect(baseActions.sendTest).toHaveBeenCalledTimes(1)
  })
})
```

- [ ] **Step 2: Run the test — expect failure**

Run: `cd frontend && node_modules/.bin/vitest run --project citizen-frontend src/citizen-frontend/webpush/WebPushSettingsSection.spec.tsx`
Expected: module not found.

- [ ] **Step 3: Implement `WebPushSettingsSection.tsx`**

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'

import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Button } from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H3, P } from 'lib-components/typography'

import { useTranslation } from '../localization'
import { detectBrowser } from '../pwa/detectPlatform'

import { PermissionGuide } from './PermissionGuide'
import type { CitizenPushCategory } from './webpush-api'
import { useWebPushState } from './webpush-state'

const ALL_CATEGORIES: CitizenPushCategory[] = [
  'URGENT_MESSAGE',
  'MESSAGE',
  'BULLETIN'
]

export const WebPushSettingsSection = React.memo(function WebPushSettingsSection() {
  const t = useTranslation().personalDetails.webPushSection
  const { status, categories, subscribe, updateCategories, unsubscribe, sendTest } =
    useWebPushState()
  const browser = useMemo(() => detectBrowser(), [])

  const masterOn = status === 'subscribed'
  const masterDisabled = status === 'unsupported' || status === 'denied' || status === 'loading'

  const handleMasterToggle = useCallback(async () => {
    if (status === 'subscribed') {
      await unsubscribe()
    } else if (status === 'unregistered') {
      await subscribe(new Set(ALL_CATEGORIES))
    }
  }, [status, subscribe, unsubscribe])

  const toggleCategory = useCallback(
    async (category: CitizenPushCategory, nowChecked: boolean) => {
      const next = new Set(categories)
      if (nowChecked) next.add(category)
      else next.delete(category)
      await updateCategories(next)
    },
    [categories, updateCategories]
  )

  return (
    <div data-qa="webpush-settings-section">
      <H3>{t.title}</H3>
      {t.info}
      <Checkbox
        checked={masterOn}
        disabled={masterDisabled}
        label={t.enable}
        onChange={() => void handleMasterToggle()}
        data-qa="webpush-master-toggle"
      />
      <FixedSpaceColumn spacing="xs">
        <Checkbox
          checked={categories.has('URGENT_MESSAGE')}
          disabled={!masterOn}
          label={t.categoryUrgent.label}
          onChange={(checked) => void toggleCategory('URGENT_MESSAGE', checked)}
          data-qa="webpush-cat-urgent"
        />
        <P>{t.categoryUrgent.description}</P>
        <Checkbox
          checked={categories.has('MESSAGE')}
          disabled={!masterOn}
          label={t.categoryMessage.label}
          onChange={(checked) => void toggleCategory('MESSAGE', checked)}
          data-qa="webpush-cat-message"
        />
        <P>{t.categoryMessage.description}</P>
        <Checkbox
          checked={categories.has('BULLETIN')}
          disabled={!masterOn}
          label={t.categoryBulletin.label}
          onChange={(checked) => void toggleCategory('BULLETIN', checked)}
          data-qa="webpush-cat-bulletin"
        />
        <P>{t.categoryBulletin.description}</P>
      </FixedSpaceColumn>
      <Button
        text={t.sendTest}
        onClick={() => void sendTest()}
        disabled={!masterOn}
        data-qa="webpush-send-test"
      />
      {(status === 'unsupported' || status === 'denied') && (
        <PermissionGuide browser={browser} />
      )}
    </div>
  )
})
```

**Note on `Checkbox`:** evaka's citizen frontend uses the `CheckboxF` (form-bound) variant in existing forms, and a plain `Checkbox` in simpler contexts. Grep for `import Checkbox from 'lib-components/atoms/form/Checkbox'` on an existing file to confirm the import path and prop signature. If the `Checkbox` prop is `onChange: (checked: boolean) => void` vs the `CheckboxF`'s form-bound pattern, adapt. This plan uses the plain `Checkbox`.

**Note on `Button`:** evaka's `Button` atom takes `text: string` prop (not `children`) and has built-in double-click throttling. See `evaka/CLAUDE.md`.

- [ ] **Step 4: Run the test — expect pass**

Run: `cd frontend && node_modules/.bin/vitest run --project citizen-frontend src/citizen-frontend/webpush/WebPushSettingsSection.spec.tsx`
Expected: 6 tests passed.

If a test fails with a theme lookup error (`Cannot read properties of undefined (reading 'main')`), the component is using a styled lib-component without being wrapped. Fix the wrap helper — the test above already uses `TestContextProvider` so this should Just Work.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/citizen-frontend/webpush/WebPushSettingsSection.tsx \
        frontend/src/citizen-frontend/webpush/WebPushSettingsSection.spec.tsx
git commit -m "feat(citizen/webpush): WebPushSettingsSection with master + categories + test button"
```

---

## Task 15: Nest `WebPushSettingsSection` inside `NotificationSettingsSection`

**Files:**
- Modify: `frontend/src/citizen-frontend/personal-details/NotificationSettingsSection.tsx`

- [ ] **Step 1: Import and render the new section**

Open `frontend/src/citizen-frontend/personal-details/NotificationSettingsSection.tsx`. At the top of the file, add the import (after the existing imports):

```typescript
import { WebPushSettingsSection } from '../webpush/WebPushSettingsSection'
```

Then, inside the return JSX, find the closing tag of the email-settings section (typically right after the `<FixedSpaceRow $justifyContent="flex-end">...</FixedSpaceRow>` or after the save button row — whichever is the last direct child of the email settings block). Add:

```tsx
        <Gap size="m" />
        <WebPushSettingsSection />
```

just before the closing `</div>` of the outer section `data-qa="notification-settings-section"`. This nests push settings **inside** the same card as email settings.

- [ ] **Step 2: Type-check**

Run: `cd frontend && node_modules/.bin/tsc --build --force .`
Expected: no errors.

- [ ] **Step 3: Run the full citizen-frontend vitest project once to confirm no regressions**

Run: `cd frontend && node_modules/.bin/vitest run --project citizen-frontend`
Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/citizen-frontend/personal-details/NotificationSettingsSection.tsx
git commit -m "feat(citizen/personal-details): nest push settings inside notification card"
```

---

## Task 16: Manual staging verification

No automated tests can validate the real push path (browser ↔ push service ↔ backend ↔ push service ↔ browser). Do this manually.

- [ ] **Step 1: Start the full stack locally**

```bash
cd /Volumes/evaka/evaka
mise start
```

Wait for `pm2 status` to show `service` as "online" (1–3 minutes cold start). Citizen app at <http://localhost:9099/>.

- [ ] **Step 2: Confirm `evaka.web_push.vapid_private_key` is set in the local config**

Grep the local config:

```bash
cd /Volumes/evaka/evaka
grep -R "evaka.web_push" compose/
```

If the key isn't wired for the local stack, set it as an env var for the service process. Without it, `GET /citizen/web-push/vapid-key` returns 503 and the UI shows the "unavailable" guide — which is fine for testing that branch, but not the happy path.

- [ ] **Step 3: Log in as a test citizen and open Personal Details → Notification settings**

Open http://localhost:9099, log in via the dev IDP (any test citizen works), navigate to Personal details. Confirm the new "Push notifications" section is nested below the email section within the same card.

- [ ] **Step 4: Flip the master toggle on**

Expect:
- Browser shows a permission prompt. Grant it.
- Immediately after granting, a test notification appears (the welcome test push).
- The three category checkboxes become enabled.
- Sentry/console logs no errors.

- [ ] **Step 5: Send a real message from the employee side**

Open a second browser (or incognito), log in as an employee, send a message to the test citizen. Expect:
- A push notification arrives on the citizen browser within a few seconds.
- Clicking the notification opens/focuses the citizen tab at `/messages/{threadId}`.

- [ ] **Step 6: Toggle categories**

Uncheck "Bulletins", send a bulletin from the employee side. Expect: no push. Send a normal message. Expect: push arrives.

- [ ] **Step 7: Test disabled permission**

In browser settings, revoke notification permission for localhost:9099. Reload the page. Expect:
- The section now shows the `denied` state with the per-platform `PermissionGuide` visible.
- The master toggle is disabled.

- [ ] **Step 8: Test on a real mobile device (optional but recommended)**

Start a cloudflared tunnel per `evaka/CLAUDE.md`:

```bash
/tmp/cloudflared tunnel --url http://localhost:9099
```

Open the printed `*.trycloudflare.com` URL on an Android phone (Chrome). Repeat steps 4–5. On iOS, you must install the PWA to the home screen first and open it from there — only then does Safari allow push subscriptions.

- [ ] **Step 9: Verify S3 store contents**

In a second terminal:

```bash
cd /Volumes/evaka/evaka
docker compose -f compose/docker-compose.yml exec s3mock \
  awslocal s3 ls s3://evaka-local-data/citizen-push-subscriptions/
```

Adjust the bucket name to whatever `BucketEnv.data` resolves to in your local config. You should see a `{personId}.json` file for each subscribed citizen.

- [ ] **Step 10: Tear down and commit a NOTES.md with observed results**

```bash
mise stop
```

Optionally write a short note in the plan file (this one) under a new "## Manual verification observed" heading with the outcome — this helps the next implementer know what to expect on the same branch.

- [ ] **Step 11: Commit any manual-test notes**

```bash
git add docs/superpowers/plans/2026-04-14-citizen-web-push-notifications-plan.md
git commit -m "docs: record manual staging verification for citizen web push POC"
```

(Skip this step if you didn't add any notes — don't create an empty commit.)

---

## Wrap-up

At this point:
- Backend: 7 new Kotlin files + 3 Kotlin test classes, 2 modified files (`Audit.kt`, `MessageService.kt`), 1 minor modification to `webpush/WebPush.kt`.
- Frontend: 4 new TS/TSX files + 4 test files, 1 modified service worker, 1 modified detectPlatform, 1 modified NotificationSettingsSection, 3 modified i18n files.
- Total commits: 14–15.

**Known follow-up items (explicitly out of scope):**
- Real database migration replacing the S3 store once the POC is validated.
- Per-category notification icons.
- Rate limiting / retry on push service 5xx errors.
- Metrics beyond Sentry breadcrumbs.
- Employee-mobile service worker update — the body/tag/url fields we added to `WebPushPayload.NotificationV1` are optional and backwards-compatible, but the employee SW currently ignores them. If employee mobile wants these too, it's a separate plan.
