# Citizen Web Push Inline Reply Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let citizens reply inline from a new-message web push notification (Android Chrome inline text reply; graceful fallback to deep-linked reply form on other platforms), with IndexedDB-backed draft persistence so the typed reply survives session expiry and SAML login round-trip.

**Architecture:** The backend embeds a `replyAction` object on `WebPushPayload.NotificationV1` for non-bulletin message notifications (threadId + reply-all recipient account IDs + localized strings). The service worker declares a `type: 'text'` action on `showNotification`, POSTs the reply via session cookie + `x-evaka-csrf: 1` header on submit, and shows a local success/error ack. Draft text is saved to IndexedDB before the POST so it survives session expiry and the SAML round-trip; the thread view reads the draft on mount and prefills the reply form. No explicit feature detection — platforms without inline reply fall through naturally (no action button shown → user taps the notification body → thread opens; action button shown but no text input → empty `event.reply` → thread opens with focused reply form).

**Tech Stack:** Kotlin + Spring (backend), Jackson sealed-class JSON (push payload), PostgreSQL (reply recipient query via LATERAL JOIN + array_agg), React + wouter (citizen frontend), Vite `serviceWorker()` plugin (SW bundling), IndexedDB (draft store), vitest + fake-indexeddb (frontend unit tests), mockito-kotlin + JUnit 5 (backend unit tests).

**Scope:**
- ✅ Inline reply action on `MESSAGE` and `URGENT_MESSAGE` categories
- ✅ Localized success/error ack notifications from SW
- ✅ Draft persistence through session expiry via IDB
- ✅ Thread view reads + prefills draft on mount
- ✅ Graceful fallback on platforms without `type: 'text'` support
- ❌ NOT: a new backend endpoint (reuses existing `POST /citizen/messages/reply-to/{threadId}`)
- ❌ NOT: reply action on `BULLETIN` notifications (bulletins are one-way)
- ❌ NOT: draft persistence across device boundaries (IDB is origin+device-scoped)

---

## File Structure

### Backend (`service/src/main/kotlin/fi/espoo/evaka/`)

**Modify:**
- `webpush/WebPush.kt` — extend `NotificationV1` with optional `replyAction: NotificationReplyAction?`; add nested data class.
- `citizenwebpush/CitizenPushMessages.kt` — add `forReplyAction(language)` returning a `PushReplyActionStrings` bundle.
- `citizenwebpush/CitizenPushSender.kt` — extend `CitizenPushRecipientRow` with `replyRecipientAccountIds`; extend query with LATERAL JOIN computing reply-all recipient set; build `replyAction` in `notifyMessage` for non-bulletin categories when recipients non-empty.

### Frontend (`frontend/src/citizen-frontend/`)

**Create:**
- `webpush/draftStore.ts` — IndexedDB utility: `saveDraft`, `loadDraft`, `deleteDraft`, `purgeOldDrafts`. Used by both SW (via ES-module import in the Vite SW plugin bundle) and main app code.
- `webpush/draftStore.spec.ts` — vitest unit tests using `fake-indexeddb`.

**Modify:**
- `service-worker.js` — parse `replyAction` from payload; add `actions` array with `type: 'text'`; new `notificationclick` branch for `event.action === 'reply'` that saves draft to IDB, POSTs reply, and shows success/error ack.
- `messages/ThreadView.tsx` — on mount, load draft from IDB for current threadId, prefill reply content via `MessageContext.setReplyContent`, show a restored-from-notification hint, read `?focus=reply` query param to focus the textarea, and delete the draft on successful send.
- `lib-customizations/defaults/citizen/i18n/{fi,sv,en}.tsx` — add `personalDetails.webPushSection.restoredReplyDraft` string.

### Package config

**Modify:** `frontend/package.json` — add `fake-indexeddb` devDependency if not present (needed for draft-store tests in vitest + jsdom).

---

## Task 1: Verify post-login redirect preserves `/messages/:threadId`

**Purpose:** The entire error-recovery flow assumes that a citizen tapping `/messages/abc-123` while unauthenticated will land back at `/messages/abc-123` after suomi.fi SAML login. If this is broken we must fix it before building on top. The previous exploration suggests it already works via `RequireAuth` + `RelayState`, but we verify manually before committing any dependent code.

**Files:**
- Read-only: `frontend/src/citizen-frontend/RequireAuth.tsx`, `frontend/src/citizen-frontend/navigation/const.ts`

- [ ] **Step 1: Open the app with a deep-link while unauthenticated**

Start the dev stack (`mise start` + cloudflared tunnel if needed) and in an incognito window navigate directly to a real thread URL such as `/messages/<any-valid-thread-id>`. If you don't have one, use any random UUID like `/messages/00000000-0000-0000-0000-000000000000` — we only need to verify the redirect preserves the path, not that the thread exists.

- [ ] **Step 2: Confirm the `next=` param is set**

Verify the browser is redirected to `/login?next=%2Fmessages%2F<id>` by `RequireAuth.tsx`. This is the first half of the round-trip.

- [ ] **Step 3: Trigger suomi.fi strong login**

Click the suomi.fi login button. Verify the URL becomes `/api/citizen/auth/sfi/login?RelayState=%2Fmessages%2F<id>` (from `navigation/const.ts:getStrongLoginUri`).

- [ ] **Step 4: Complete SAML round-trip**

Pick any dummy-idp user and complete login. Verify the final URL is `/messages/<id>` (either the thread view, or a "thread not found" state if the id was fake — either is fine; we only care the route survived).

- [ ] **Step 5: Document the result**

If the round-trip works: proceed to Task 2. If it is broken: STOP, file this as a separate bug, fix it before continuing. Do not commit anything in this task — it is a manual verification gate only.

---

## Task 2: Extend `NotificationV1` with `replyAction` field

**Files:**
- Modify: `service/src/main/kotlin/fi/espoo/evaka/webpush/WebPush.kt:42-51`
- Test: `service/src/test/kotlin/fi/espoo/evaka/webpush/WebPushPayloadSerializationTest.kt` (create if missing; otherwise find an existing serialization test and extend it)

- [ ] **Step 1: Write the failing test**

If a serialization test file for `WebPushPayload` already exists (grep under `service/src/test/kotlin/fi/espoo/evaka/webpush/` for `NotificationV1` or `WebPushPayload`), add a test there. Otherwise create a new file:

```kotlin
// service/src/test/kotlin/fi/espoo/evaka/webpush/WebPushPayloadSerializationTest.kt
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class WebPushPayloadSerializationTest {
    private val mapper = defaultJsonMapperBuilder().build()

    @Test
    fun `NotificationV1 serializes replyAction`() {
        val threadId = MessageThreadId(UUID.randomUUID())
        val accountId = MessageAccountId(UUID.randomUUID())
        val payload: WebPushPayload =
            WebPushPayload.NotificationV1(
                title = "New message",
                body = "Alice sent you a message.",
                tag = "msg-$threadId",
                url = "/messages/$threadId",
                replyAction =
                    WebPushPayload.NotificationV1.ReplyAction(
                        threadId = threadId,
                        recipientAccountIds = setOf(accountId),
                        actionLabel = "Reply",
                        actionPlaceholder = "Type a reply…",
                        successTitle = "Reply sent",
                        successBody = "Your reply was delivered.",
                        errorTitle = "Reply not sent",
                        errorBody = "Open eVaka to retry.",
                    ),
            )

        val json = mapper.writeValueAsString(listOf(payload))
        val tree = mapper.readTree(json)

        assertEquals("NotificationV1", tree[0]["type"].asText())
        val ra = tree[0]["replyAction"]
        assertNotNull(ra)
        assertEquals(threadId.raw.toString(), ra["threadId"].asText())
        assertEquals(1, ra["recipientAccountIds"].size())
        assertEquals(accountId.raw.toString(), ra["recipientAccountIds"][0].asText())
        assertEquals("Reply", ra["actionLabel"].asText())
    }

    @Test
    fun `NotificationV1 omits replyAction when null`() {
        val payload: WebPushPayload =
            WebPushPayload.NotificationV1(
                title = "Bulletin",
                body = "A new bulletin was posted.",
                tag = "msg-abc",
                url = "/messages/abc",
                replyAction = null,
            )

        val json = mapper.writeValueAsString(listOf(payload))
        val tree = mapper.readTree(json)

        // `replyAction` should either be absent or a JSON null, not an empty object.
        assert(!tree[0].has("replyAction") || tree[0]["replyAction"].isNull) {
            "replyAction should be absent or null, was: ${tree[0]["replyAction"]}"
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run from `service/`:

```bash
./gradlew test --tests "fi.espoo.evaka.webpush.WebPushPayloadSerializationTest"
```

Expected: FAIL with "unresolved reference: ReplyAction" or similar compilation error.

- [ ] **Step 3: Implement the minimal change in `WebPush.kt`**

Replace the existing `NotificationV1` data class at `service/src/main/kotlin/fi/espoo/evaka/webpush/WebPush.kt:42-51` with:

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@JsonTypeIdResolver(SealedSubclassSimpleName::class)
sealed interface WebPushPayload {
    data class NotificationV1(
        val title: String,
        val body: String? = null,
        val tag: String? = null,
        val url: String? = null,
        val replyAction: ReplyAction? = null,
    ) : WebPushPayload {
        data class ReplyAction(
            val threadId: MessageThreadId,
            val recipientAccountIds: Set<MessageAccountId>,
            val actionLabel: String,
            val actionPlaceholder: String,
            val successTitle: String,
            val successBody: String,
            val errorTitle: String,
            val errorBody: String,
        )
    }
}
```

Add these imports to the top of `WebPush.kt` (below the existing imports — keep them alphabetically grouped):

```kotlin
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageThreadId
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew test --tests "fi.espoo.evaka.webpush.WebPushPayloadSerializationTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/webpush/WebPush.kt \
        service/src/test/kotlin/fi/espoo/evaka/webpush/WebPushPayloadSerializationTest.kt
git commit -m "feat(webpush): add replyAction field to NotificationV1 payload"
```

---

## Task 3: Add reply-action strings to `CitizenPushMessages`

**Files:**
- Modify: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushMessages.kt`
- Test: `service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushMessagesTest.kt` (create if not present)

- [ ] **Step 1: Write the failing test**

Create `service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushMessagesTest.kt`:

```kotlin
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class CitizenPushMessagesTest {
    @Test
    fun `forReplyAction returns Finnish strings`() {
        val s = CitizenPushMessages.forReplyAction(CitizenPushLanguage.FI)
        assertEquals("Vastaa", s.actionLabel)
        assertEquals("Kirjoita vastaus…", s.actionPlaceholder)
        assertEquals("Vastaus lähetetty", s.successTitle)
        assertTrue(s.successBody.isNotBlank())
        assertEquals("Vastauksen lähetys epäonnistui", s.errorTitle)
        assertTrue(s.errorBody.isNotBlank())
    }

    @Test
    fun `forReplyAction returns Swedish strings`() {
        val s = CitizenPushMessages.forReplyAction(CitizenPushLanguage.SV)
        assertEquals("Svara", s.actionLabel)
        assertEquals("Skriv ett svar…", s.actionPlaceholder)
        assertEquals("Svaret skickat", s.successTitle)
        assertTrue(s.successBody.isNotBlank())
        assertEquals("Svaret kunde inte skickas", s.errorTitle)
        assertTrue(s.errorBody.isNotBlank())
    }

    @Test
    fun `forReplyAction returns English strings`() {
        val s = CitizenPushMessages.forReplyAction(CitizenPushLanguage.EN)
        assertEquals("Reply", s.actionLabel)
        assertEquals("Type a reply…", s.actionPlaceholder)
        assertEquals("Reply sent", s.successTitle)
        assertTrue(s.successBody.isNotBlank())
        assertEquals("Reply not sent", s.errorTitle)
        assertTrue(s.errorBody.isNotBlank())
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

```bash
./gradlew test --tests "fi.espoo.evaka.citizenwebpush.CitizenPushMessagesTest"
```

Expected: FAIL — `forReplyAction` is unresolved.

- [ ] **Step 3: Add `PushReplyActionStrings` + `forReplyAction` to `CitizenPushMessages.kt`**

At the top of `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushMessages.kt`, alongside the existing `PushTitleAndBody` declaration, add:

```kotlin
data class PushReplyActionStrings(
    val actionLabel: String,
    val actionPlaceholder: String,
    val successTitle: String,
    val successBody: String,
    val errorTitle: String,
    val errorBody: String,
)
```

Then inside the `object CitizenPushMessages { ... }` block, add this function (next to `forMessage` and `forTest`):

```kotlin
fun forReplyAction(language: CitizenPushLanguage): PushReplyActionStrings =
    when (language) {
        CitizenPushLanguage.FI ->
            PushReplyActionStrings(
                actionLabel = "Vastaa",
                actionPlaceholder = "Kirjoita vastaus…",
                successTitle = "Vastaus lähetetty",
                successBody = "Vastauksesi on toimitettu.",
                errorTitle = "Vastauksen lähetys epäonnistui",
                errorBody = "Avaa eVaka ja yritä uudelleen.",
            )
        CitizenPushLanguage.SV ->
            PushReplyActionStrings(
                actionLabel = "Svara",
                actionPlaceholder = "Skriv ett svar…",
                successTitle = "Svaret skickat",
                successBody = "Ditt svar har levererats.",
                errorTitle = "Svaret kunde inte skickas",
                errorBody = "Öppna eVaka och försök igen.",
            )
        CitizenPushLanguage.EN ->
            PushReplyActionStrings(
                actionLabel = "Reply",
                actionPlaceholder = "Type a reply…",
                successTitle = "Reply sent",
                successBody = "Your reply was delivered.",
                errorTitle = "Reply not sent",
                errorBody = "Open eVaka to retry.",
            )
    }
```

- [ ] **Step 4: Run the test and verify it passes**

```bash
./gradlew test --tests "fi.espoo.evaka.citizenwebpush.CitizenPushMessagesTest"
```

Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushMessages.kt \
        service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushMessagesTest.kt
git commit -m "feat(citizenwebpush): add localized reply-action strings"
```

---

## Task 4: Extend `getCitizenPushRecipients` query with reply-all account IDs

**Files:**
- Modify: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSender.kt:177-209` (`CitizenPushRecipientRow` + `getCitizenPushRecipients`)
- Test: find the existing backend integration test that exercises the citizen push sender query and extend it. Grep for `getCitizenPushRecipients` under `service/src/integrationTest/kotlin/` and `service/src/test/kotlin/`. If an `CitizenPushSenderIntegrationTest` already exists, add to it. Otherwise create `service/src/integrationTest/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSenderIntegrationTest.kt` modeled on any existing `*IntegrationTest.kt` that uses `PureJdbiTest` or `FullApplicationTest`.

- [ ] **Step 1: Write the failing test**

Locate the existing integration test (or create the new file). Add this test case. It creates a three-party thread: Alice (sender, employee) → {Bob, Carol} (recipients, citizens). When the push recipients query is called for the `m.id` of Alice's message, Bob's row should list `{Alice, Carol}` as reply recipients and Carol's row should list `{Alice, Bob}`.

```kotlin
@Test
fun `getCitizenPushRecipients returns reply-all account ids excluding self`() {
    val employee = db.transaction { tx -> /* create employee + employee message_account */ }
    val alice = employee.accountId // message_account id
    val bob = db.transaction { tx -> /* create citizen + message_account */ }
    val carol = db.transaction { tx -> /* create citizen + message_account */ }

    val messageId = db.transaction { tx ->
        /* create thread with sender=alice, recipients={bob, carol}, one message */
    }

    val rows = db.read { it.getCitizenPushRecipients(listOf(messageId)) }

    assertEquals(2, rows.size)
    val bobRow = rows.first { it.personId == bobPersonId }
    val carolRow = rows.first { it.personId == carolPersonId }
    assertEquals(setOf(alice, carol), bobRow.replyRecipientAccountIds.toSet())
    assertEquals(setOf(alice, bob), carolRow.replyRecipientAccountIds.toSet())
}
```

**Note for implementer:** Look at the existing integration test helpers in the `messaging/` test package for message/thread creation (e.g. `MessageServiceIntegrationTest.kt`). Use those helpers rather than hand-crafting SQL. The query result row field must be named `replyRecipientAccountIds: List<MessageAccountId>`.

Also add a second test for the empty case: a thread with only one participant (the recipient themselves, defensive) → `replyRecipientAccountIds` is empty.

- [ ] **Step 2: Run the test and verify it fails**

```bash
./gradlew integrationTest --tests "*CitizenPushSenderIntegrationTest.getCitizenPushRecipients*"
```

Expected: FAIL — `replyRecipientAccountIds` unresolved on `CitizenPushRecipientRow`.

- [ ] **Step 3: Extend `CitizenPushRecipientRow`**

Replace the existing data class at `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSender.kt:177-184`:

```kotlin
data class CitizenPushRecipientRow(
    val personId: PersonId,
    val threadId: MessageThreadId,
    val language: String?,
    val urgent: Boolean,
    val threadType: MessageType,
    val senderName: String,
    val replyRecipientAccountIds: List<MessageAccountId>,
)
```

- [ ] **Step 4: Extend the SQL query**

Replace the existing `getCitizenPushRecipients` at `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSender.kt:186-209`:

```kotlin
fun Database.Read.getCitizenPushRecipients(
    messageIds: List<MessageId>
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
    m.sender_name,
    COALESCE(reply_participants.account_ids, ARRAY[]::uuid[]) AS reply_recipient_account_ids
FROM message m
JOIN message_recipients mr ON mr.message_id = m.id
JOIN message_account ma ON ma.id = mr.recipient_id
JOIN person p ON p.id = ma.person_id
JOIN message_thread t ON m.thread_id = t.id
LEFT JOIN LATERAL (
    SELECT array_agg(DISTINCT account_id) AS account_ids
    FROM (
        SELECT m2.sender_id AS account_id
        FROM message m2
        WHERE m2.thread_id = t.id
          AND m2.sender_id IS NOT NULL
          AND m2.sender_id <> ma.id
        UNION
        SELECT mr2.recipient_id AS account_id
        FROM message_recipients mr2
        JOIN message m2 ON m2.id = mr2.message_id
        WHERE m2.thread_id = t.id
          AND mr2.recipient_id <> ma.id
    ) _p
) reply_participants ON TRUE
WHERE m.id = ANY(${bind(messageIds)})
  AND t.is_copy IS FALSE
"""
            )
        }
        .toList<CitizenPushRecipientRow>()
```

Key points for the reviewer:
- The `LEFT JOIN LATERAL` lets each outer row compute its own per-recipient reply set (the `<> ma.id` filter uses the outer `ma.id`).
- `COALESCE` ensures empty sets become `[]` instead of `null`, so the Kotlin field stays `List` rather than `List?`.
- `DISTINCT` in `array_agg` + `UNION` handle any duplicate accounts that appear as both sender and recipient.
- `sender_id IS NOT NULL` guards the edge case where sender_id might be null on rare historical rows; safe to include.

- [ ] **Step 5: Run the test and verify it passes**

```bash
./gradlew integrationTest --tests "*CitizenPushSenderIntegrationTest.getCitizenPushRecipients*"
```

Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSender.kt \
        service/src/integrationTest/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSenderIntegrationTest.kt
git commit -m "feat(citizenwebpush): compute reply-all recipient ids in push query"
```

---

## Task 5: Wire `replyAction` into `notifyMessage` payload construction

**Files:**
- Modify: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSender.kt:38-61` (`notifyMessage`), 138-174 (`handleSentMessages`)
- Test: `service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSenderTest.kt` (the existing unit test file for the sender, which tests payload construction with mocked WebPush)

- [ ] **Step 1: Write the failing test**

Add two tests to the existing `CitizenPushSenderTest.kt` (or create the file if missing; mirror the style of existing sender unit tests that use mockito-kotlin):

```kotlin
@Test
fun `notifyMessage includes replyAction for MESSAGE category when recipients present`() {
    val threadId = MessageThreadId(UUID.randomUUID())
    val replyAcc = MessageAccountId(UUID.randomUUID())
    val payloadCaptor = argumentCaptor<WebPushPayload.NotificationV1>()
    whenever(store.load(any())).thenReturn(subscriptionFile(CitizenPushCategory.MESSAGE))

    sender.notifyMessage(
        personId = PersonId(UUID.randomUUID()),
        threadId = threadId.raw.toString(),
        category = CitizenPushCategory.MESSAGE,
        senderName = "Alice",
        language = CitizenPushLanguage.EN,
        replyRecipientAccountIds = listOf(replyAcc),
    )

    verify(webPush).send(any(), any(), payloadCaptor.capture(), any())
    val ra = payloadCaptor.firstValue.replyAction
    assertNotNull(ra)
    assertEquals(threadId, ra.threadId)
    assertEquals(setOf(replyAcc), ra.recipientAccountIds)
    assertEquals("Reply", ra.actionLabel)
    assertEquals("Reply sent", ra.successTitle)
}

@Test
fun `notifyMessage omits replyAction for BULLETIN category`() {
    val threadId = MessageThreadId(UUID.randomUUID())
    val payloadCaptor = argumentCaptor<WebPushPayload.NotificationV1>()
    whenever(store.load(any())).thenReturn(subscriptionFile(CitizenPushCategory.BULLETIN))

    sender.notifyMessage(
        personId = PersonId(UUID.randomUUID()),
        threadId = threadId.raw.toString(),
        category = CitizenPushCategory.BULLETIN,
        senderName = "Municipality",
        language = CitizenPushLanguage.EN,
        replyRecipientAccountIds = listOf(MessageAccountId(UUID.randomUUID())),
    )

    verify(webPush).send(any(), any(), payloadCaptor.capture(), any())
    assertNull(payloadCaptor.firstValue.replyAction)
}

@Test
fun `notifyMessage omits replyAction when reply recipients are empty`() {
    val threadId = MessageThreadId(UUID.randomUUID())
    val payloadCaptor = argumentCaptor<WebPushPayload.NotificationV1>()
    whenever(store.load(any())).thenReturn(subscriptionFile(CitizenPushCategory.MESSAGE))

    sender.notifyMessage(
        personId = PersonId(UUID.randomUUID()),
        threadId = threadId.raw.toString(),
        category = CitizenPushCategory.MESSAGE,
        senderName = "Alice",
        language = CitizenPushLanguage.EN,
        replyRecipientAccountIds = emptyList(),
    )

    verify(webPush).send(any(), any(), payloadCaptor.capture(), any())
    assertNull(payloadCaptor.firstValue.replyAction)
}
```

**Note:** If the existing test helper for building a `subscriptionFile` doesn't exist, create one locally that returns a `CitizenPushStoreFile` containing a single subscription with the given category enabled. Reuse the exact pattern used by the existing passing tests in this file.

- [ ] **Step 2: Run the test and verify it fails**

```bash
./gradlew test --tests "fi.espoo.evaka.citizenwebpush.CitizenPushSenderTest*replyAction*"
```

Expected: FAIL — `notifyMessage` doesn't accept `replyRecipientAccountIds`.

- [ ] **Step 3: Update `notifyMessage` signature and payload construction**

Replace the existing `notifyMessage` at `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSender.kt:38-61`:

```kotlin
fun notifyMessage(
    personId: PersonId,
    threadId: String,
    category: CitizenPushCategory,
    senderName: String,
    language: CitizenPushLanguage,
    replyRecipientAccountIds: List<MessageAccountId>,
    jwtProvider: VapidJwtProvider = defaultJwtProviderOrNoop(),
) {
    val wp = webPush ?: return
    val file = store.load(personId) ?: return
    val entries = file.subscriptions.filter { category in it.enabledCategories }
    if (entries.isEmpty()) return

    val titleAndBody = CitizenPushMessages.forMessage(category, language, senderName)
    val replyAction =
        if (
            category != CitizenPushCategory.BULLETIN && replyRecipientAccountIds.isNotEmpty()
        ) {
            val strings = CitizenPushMessages.forReplyAction(language)
            WebPushPayload.NotificationV1.ReplyAction(
                threadId = MessageThreadId(UUID.fromString(threadId)),
                recipientAccountIds = replyRecipientAccountIds.toSet(),
                actionLabel = strings.actionLabel,
                actionPlaceholder = strings.actionPlaceholder,
                successTitle = strings.successTitle,
                successBody = strings.successBody,
                errorTitle = strings.errorTitle,
                errorBody = strings.errorBody,
            )
        } else {
            null
        }
    val payload =
        WebPushPayload.NotificationV1(
            title = titleAndBody.title,
            body = titleAndBody.body,
            tag = "msg-$threadId",
            url = "/messages/$threadId",
            replyAction = replyAction,
        )

    entries.forEach { entry -> sendOne(personId, entry, payload, jwtProvider, wp) }
}
```

Add `import java.util.UUID` if not already imported.

- [ ] **Step 4: Update `handleSentMessages` to pass recipient ids through**

In the same file at `CitizenPushSender.kt:138-174`, inside the `for (r in recipients) { ... }` loop, change the `notifyMessage` call to pass the new field:

```kotlin
notifyMessage(
    personId = r.personId,
    threadId = r.threadId.toString(),
    category = category,
    senderName = r.senderName,
    language = CitizenPushLanguage.fromPersonLanguage(r.language),
    replyRecipientAccountIds = r.replyRecipientAccountIds,
    jwtProvider =
        VapidJwtProvider { uri ->
            db.transaction { tx -> webPush.getValidToken(tx, clock, uri) }
        },
)
```

- [ ] **Step 5: Run the tests and verify everything passes**

```bash
./gradlew test --tests "fi.espoo.evaka.citizenwebpush.CitizenPushSenderTest"
```

Expected: all sender unit tests PASS, including the three new `replyAction` tests.

Then also run the full `citizenwebpush` package:

```bash
./gradlew test --tests "fi.espoo.evaka.citizenwebpush.*"
```

Expected: all PASS — no regression in existing tests.

- [ ] **Step 6: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSender.kt \
        service/src/test/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSenderTest.kt
git commit -m "feat(citizenwebpush): embed replyAction in push payload for messages"
```

---

## Task 6: Add `fake-indexeddb` dev dependency and sanity-check vitest setup

**Files:**
- Modify: `frontend/package.json`

- [ ] **Step 1: Check if already installed**

Run from `frontend/`:

```bash
grep -E '"fake-indexeddb"' package.json || echo NOT_INSTALLED
```

If it prints `NOT_INSTALLED`, proceed with Step 2. Otherwise skip to Task 7.

- [ ] **Step 2: Add the dev dependency**

Run from `frontend/`:

```bash
yarn add -D fake-indexeddb
```

Expected: `package.json` now lists `"fake-indexeddb": "^6.x"` under `devDependencies` and `yarn.lock` updated.

- [ ] **Step 3: Commit**

```bash
git add frontend/package.json frontend/yarn.lock
git commit -m "chore(citizen-frontend): add fake-indexeddb for draft store tests"
```

---

## Task 7: Create `draftStore.ts` IndexedDB utility

**Files:**
- Create: `frontend/src/citizen-frontend/webpush/draftStore.ts`
- Test: `frontend/src/citizen-frontend/webpush/draftStore.spec.ts`

**Design:** A tiny wrapper around `indexedDB.open('evaka-citizen-webpush', 1)` with an object store `drafts` keyed by `threadId`. Values: `{ threadId, text, savedAt }`. Exposes four async functions: `saveDraft`, `loadDraft`, `deleteDraft`, `purgeOldDrafts`. No external deps; works in both the service worker (via self.indexedDB) and the main thread (via window.indexedDB). We bind to `globalThis.indexedDB` so the same module works in both contexts.

- [ ] **Step 1: Write the failing tests**

Create `frontend/src/citizen-frontend/webpush/draftStore.spec.ts`:

```typescript
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'fake-indexeddb/auto'
import { afterEach, describe, expect, it } from 'vitest'

import {
  deleteDraft,
  loadDraft,
  purgeOldDrafts,
  saveDraft
} from './draftStore'

async function resetDb() {
  await new Promise<void>((resolve, reject) => {
    const req = indexedDB.deleteDatabase('evaka-citizen-webpush')
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
    req.onblocked = () => resolve()
  })
}

describe('draftStore', () => {
  afterEach(async () => {
    await resetDb()
  })

  it('saves and loads a draft by threadId', async () => {
    await saveDraft('thread-a', 'Hello from notification')
    const draft = await loadDraft('thread-a')
    expect(draft).not.toBeNull()
    expect(draft!.threadId).toBe('thread-a')
    expect(draft!.text).toBe('Hello from notification')
    expect(typeof draft!.savedAt).toBe('number')
  })

  it('returns null when no draft exists', async () => {
    const draft = await loadDraft('nonexistent')
    expect(draft).toBeNull()
  })

  it('overwrites an existing draft for the same threadId', async () => {
    await saveDraft('thread-a', 'first')
    await saveDraft('thread-a', 'second')
    const draft = await loadDraft('thread-a')
    expect(draft!.text).toBe('second')
  })

  it('deletes a draft', async () => {
    await saveDraft('thread-a', 'hi')
    await deleteDraft('thread-a')
    expect(await loadDraft('thread-a')).toBeNull()
  })

  it('keeps drafts for other threads when deleting one', async () => {
    await saveDraft('thread-a', 'a')
    await saveDraft('thread-b', 'b')
    await deleteDraft('thread-a')
    expect(await loadDraft('thread-a')).toBeNull()
    expect((await loadDraft('thread-b'))!.text).toBe('b')
  })

  it('purges drafts older than the TTL', async () => {
    await saveDraft('old', 'stale')
    // Manually rewrite savedAt to simulate age
    await new Promise<void>((resolve, reject) => {
      const open = indexedDB.open('evaka-citizen-webpush', 1)
      open.onsuccess = () => {
        const db = open.result
        const tx = db.transaction('drafts', 'readwrite')
        const store = tx.objectStore('drafts')
        const get = store.get('old')
        get.onsuccess = () => {
          const value = get.result
          value.savedAt = Date.now() - 1000 * 60 * 60 * 24 * 30 // 30 days ago
          store.put(value)
        }
        tx.oncomplete = () => {
          db.close()
          resolve()
        }
        tx.onerror = () => reject(tx.error)
      }
      open.onerror = () => reject(open.error)
    })

    await saveDraft('fresh', 'fresh draft')
    await purgeOldDrafts(1000 * 60 * 60 * 24 * 7) // 7 days TTL

    expect(await loadDraft('old')).toBeNull()
    expect((await loadDraft('fresh'))!.text).toBe('fresh draft')
  })
})
```

- [ ] **Step 2: Run the test and verify it fails**

Run from `frontend/`:

```bash
yarn vitest run src/citizen-frontend/webpush/draftStore.spec.ts
```

Expected: FAIL — module `./draftStore` not found.

- [ ] **Step 3: Implement `draftStore.ts`**

Create `frontend/src/citizen-frontend/webpush/draftStore.ts`:

```typescript
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const DB_NAME = 'evaka-citizen-webpush'
const DB_VERSION = 1
const STORE = 'drafts'

export interface ReplyDraft {
  threadId: string
  text: string
  savedAt: number
}

function getIdb(): IDBFactory {
  // Works in both window and ServiceWorkerGlobalScope contexts
  const idb = (globalThis as unknown as { indexedDB?: IDBFactory }).indexedDB
  if (!idb) {
    throw new Error('IndexedDB is not available in this context')
  }
  return idb
}

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = getIdb().open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE, { keyPath: 'threadId' })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

function run<T>(
  mode: IDBTransactionMode,
  op: (store: IDBObjectStore) => IDBRequest<T> | null
): Promise<T | null> {
  return openDb().then(
    (db) =>
      new Promise<T | null>((resolve, reject) => {
        const tx = db.transaction(STORE, mode)
        const store = tx.objectStore(STORE)
        const req = op(store)
        let result: T | null = null
        if (req) {
          req.onsuccess = () => {
            result = req.result as T
          }
          req.onerror = () => reject(req.error)
        }
        tx.oncomplete = () => {
          db.close()
          resolve(result)
        }
        tx.onerror = () => {
          db.close()
          reject(tx.error)
        }
      })
  )
}

export async function saveDraft(threadId: string, text: string): Promise<void> {
  const entry: ReplyDraft = { threadId, text, savedAt: Date.now() }
  await run('readwrite', (store) => store.put(entry))
}

export async function loadDraft(threadId: string): Promise<ReplyDraft | null> {
  const result = await run<ReplyDraft | undefined>('readonly', (store) =>
    store.get(threadId)
  )
  return result ?? null
}

export async function deleteDraft(threadId: string): Promise<void> {
  await run('readwrite', (store) => store.delete(threadId))
}

export async function purgeOldDrafts(maxAgeMs: number): Promise<void> {
  const cutoff = Date.now() - maxAgeMs
  const db = await openDb()
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    const store = tx.objectStore(STORE)
    const cursorReq = store.openCursor()
    cursorReq.onsuccess = () => {
      const cursor = cursorReq.result
      if (!cursor) return
      const value = cursor.value as ReplyDraft
      if (value.savedAt < cutoff) {
        cursor.delete()
      }
      cursor.continue()
    }
    cursorReq.onerror = () => reject(cursorReq.error)
    tx.oncomplete = () => {
      db.close()
      resolve()
    }
    tx.onerror = () => {
      db.close()
      reject(tx.error)
    }
  })
}
```

- [ ] **Step 4: Run the tests and verify they pass**

```bash
yarn vitest run src/citizen-frontend/webpush/draftStore.spec.ts
```

Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/citizen-frontend/webpush/draftStore.ts \
        frontend/src/citizen-frontend/webpush/draftStore.spec.ts
git commit -m "feat(citizen-frontend): add IndexedDB draft store for push reply"
```

---

## Task 8: Service worker — declare `type: 'text'` reply action when payload carries `replyAction`

**Files:**
- Modify: `frontend/src/citizen-frontend/service-worker.js`

**Note on testing:** The service worker is plain JS served via a Vite plugin and is hard to unit-test directly. We rely on the draft-store unit tests (Task 7) + the manual device test (Task 12) to verify SW behavior. Do not write SW unit tests.

- [ ] **Step 1: Update the `push` handler to pass actions through**

Replace the `push` event listener at `frontend/src/citizen-frontend/service-worker.js:28-56` with:

```js
serviceWorker.addEventListener('push', (event) => {
  // Backend sends a JSON array of WebPushPayload entries (sealed class, tagged with
  // `type`), not a single object, so we pick the first NotificationV1 entry.
  const payload = (() => {
    try {
      const parsed = event.data ? event.data.json() : null
      const list = Array.isArray(parsed) ? parsed : parsed ? [parsed] : []
      return (
        list.find((p) => p && p.type === 'NotificationV1') ?? list[0] ?? {}
      )
    } catch {
      return {}
    }
  })()
  const title = payload.title ?? 'eVaka'
  const actions = []
  if (payload.replyAction) {
    actions.push({
      action: 'reply',
      type: 'text',
      title: payload.replyAction.actionLabel,
      placeholder: payload.replyAction.actionPlaceholder
    })
  }
  event.waitUntil(
    serviceWorker.registration.showNotification(title, {
      body: payload.body ?? '',
      icon: '/citizen/evaka-192px.png',
      // Android Chrome uses the alpha channel of the badge image to draw a
      // monochrome mask in the status bar, so the file is a transparent PNG
      // with just the "e" glyph opaque.
      badge: '/citizen/evaka-badge-72.png',
      tag: payload.tag,
      data: {
        url: payload.url ?? '/messages',
        replyAction: payload.replyAction ?? null
      },
      actions
    })
  )
})
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/citizen-frontend/service-worker.js
git commit -m "feat(citizen/service-worker): declare text reply action on push"
```

**Do not push/test yet** — Task 9 completes the click-handler side of this change. Commit here to keep the diff bite-sized.

---

## Task 9: Service worker — handle `reply` action click (POST reply + success/error ack + draft persistence)

**Files:**
- Modify: `frontend/src/citizen-frontend/service-worker.js`

**Important context for the implementer:**
- The reply endpoint is `POST /api/application/citizen/messages/reply-to/{threadId}` (the apigw prefix is `/api/application/` — verify by reading `frontend/src/citizen-frontend/api-client.ts` for the exact base URL and by grepping `frontend/src/citizen-frontend/messages/` for how the SPA currently calls `reply-to`).
- The CSRF header is `x-evaka-csrf: 1` (any value; presence-check only).
- `credentials: 'include'` is required to send the session cookie.
- Request body shape: `{ content: string, recipientAccountIds: string[] }`.
- The draft store (from Task 7) must be imported. Since vite's serviceWorker plugin bundles the SW as an ES module in prod and transforms it in dev, a static `import` at the top of the file should work. If the dev middleware fails to resolve the import, fall back to redefining the minimal `saveDraft`/`deleteDraft` inline using the same DB name/version/store as `draftStore.ts` — the two modules must be interoperable.

- [ ] **Step 1: Add the import (or inline the helpers if needed)**

At the top of `frontend/src/citizen-frontend/service-worker.js`, below the license header:

```js
import { saveDraft, deleteDraft } from './webpush/draftStore.ts'
```

**If the dev middleware or prod bundler rejects the TS import,** instead add the inline helpers at the top of the file (copy the minimal DB open + put/delete logic from `draftStore.ts`, keyed on `threadId`, same DB name `evaka-citizen-webpush` / version 1 / store `drafts`). Do NOT diverge from the draftStore schema — the main thread must read exactly what the SW wrote.

- [ ] **Step 2: Rewrite the `notificationclick` handler**

Replace the existing `notificationclick` listener at `frontend/src/citizen-frontend/service-worker.js:58-73` with:

```js
serviceWorker.addEventListener('notificationclick', (event) => {
  const notification = event.notification
  const data = notification.data ?? {}
  const url = data.url ?? '/messages'
  const replyAction = data.replyAction

  if (event.action === 'reply' && replyAction) {
    const reply = event.reply
    if (typeof reply === 'string' && reply.trim().length > 0) {
      notification.close()
      event.waitUntil(handleInlineReply(replyAction, reply.trim()))
      return
    }
    // Platform rendered the action as a plain button (no inline text input).
    // Fall through to opening the thread with the reply textarea focused.
    notification.close()
    event.waitUntil(openThreadForReply(replyAction.threadId))
    return
  }

  notification.close()
  event.waitUntil(openUrl(url))
})

async function handleInlineReply(replyAction, content) {
  const threadId = replyAction.threadId
  // Save the draft BEFORE attempting the POST so it survives session expiry
  // and the SAML login round-trip on error.
  try {
    await saveDraft(threadId, content)
  } catch (err) {
    // If IDB is unavailable we still attempt the POST; loss of draft on
    // failure is acceptable when IDB is broken.
    console.warn('Failed to persist reply draft', err)
  }

  try {
    const response = await fetch(
      `/api/application/citizen/messages/reply-to/${threadId}`,
      {
        method: 'POST',
        credentials: 'include',
        headers: {
          'content-type': 'application/json',
          'x-evaka-csrf': '1'
        },
        body: JSON.stringify({
          content,
          recipientAccountIds: replyAction.recipientAccountIds
        })
      }
    )
    if (!response.ok) throw new Error(`reply POST failed: ${response.status}`)
    await deleteDraft(threadId).catch(() => {})
    await serviceWorker.registration.showNotification(replyAction.successTitle, {
      body: replyAction.successBody,
      icon: '/citizen/evaka-192px.png',
      badge: '/citizen/evaka-badge-72.png',
      tag: `msg-${threadId}`,
      data: { url: `/messages/${threadId}` },
      actions: []
    })
  } catch (err) {
    console.warn('Reply POST failed', err)
    await serviceWorker.registration.showNotification(replyAction.errorTitle, {
      body: replyAction.errorBody,
      icon: '/citizen/evaka-192px.png',
      badge: '/citizen/evaka-badge-72.png',
      tag: `msg-reply-error-${threadId}`,
      requireInteraction: true,
      data: { url: `/messages/${threadId}?focus=reply` },
      actions: []
    })
  }
}

async function openThreadForReply(threadId) {
  return openUrl(`/messages/${threadId}?focus=reply`)
}

async function openUrl(url) {
  const list = await serviceWorker.clients.matchAll({
    type: 'window',
    includeUncontrolled: true
  })
  for (const client of list) {
    if (client.url.includes(url) && 'focus' in client) {
      return client.focus()
    }
  }
  return serviceWorker.clients.openWindow(url)
}
```

Design notes (for reviewer):
- The success ack reuses the original `tag` (`msg-${threadId}`) so it **replaces** the original reply prompt — no two notifications lingering for the same thread.
- The error ack uses a distinct tag (`msg-reply-error-${threadId}`) so it doesn't get swallowed by subsequent pushes, and sets `requireInteraction: true` so the user doesn't miss it.
- Both ack notifications carry empty `actions: []` so the user can't recursively "reply" from an ack.
- The draft is saved **before** the POST, deleted **after** success. On failure we leave the draft in IDB and deep-link to `/messages/{threadId}?focus=reply` so the thread view can restore it (Task 10).

- [ ] **Step 3: Commit**

```bash
git add frontend/src/citizen-frontend/service-worker.js
git commit -m "feat(citizen/service-worker): handle inline reply action"
```

---

## Task 10: Thread view — restore draft from IDB and honour `?focus=reply`

**Files:**
- Modify: `frontend/src/citizen-frontend/messages/ThreadView.tsx`
- Test: `frontend/src/citizen-frontend/messages/ThreadView.spec.tsx` (create if missing — there's likely an existing test file for ThreadView, extend that instead)

- [ ] **Step 1: Locate the current `ThreadView` component**

Open `frontend/src/citizen-frontend/messages/ThreadView.tsx` and identify:
- Where `threadId` is available (from props or `useParams`)
- The `useContext(MessageContext)` call exposing `setReplyContent`, `getReplyContent`
- The `MessageReplyEditor` JSX with props `value`, `onChange`

You will add a `useEffect` that runs once per threadId.

- [ ] **Step 2: Write the failing test**

Add this test to `ThreadView.spec.tsx` (or create it with imports mirroring the surrounding test files):

```typescript
import 'fake-indexeddb/auto'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, afterEach } from 'vitest'

import { saveDraft } from '../webpush/draftStore'
import { ThreadView } from './ThreadView'
// ... plus any providers / router wrappers used by existing tests in this file

async function resetDraftDb() {
  await new Promise<void>((resolve) => {
    const req = indexedDB.deleteDatabase('evaka-citizen-webpush')
    req.onsuccess = () => resolve()
    req.onerror = () => resolve()
    req.onblocked = () => resolve()
  })
}

describe('ThreadView draft restoration', () => {
  afterEach(resetDraftDb)

  it('prefills the reply textarea from IDB draft', async () => {
    await saveDraft('thread-xyz', 'Restored from notification')
    // render ThreadView with threadId='thread-xyz' inside whatever providers
    // the existing tests use (MessageContextProvider, Router, etc.)
    // ...
    await waitFor(() => {
      const textarea = screen.getByRole('textbox', { name: /reply/i })
      expect(textarea).toHaveValue('Restored from notification')
    })
    expect(screen.getByTestId('restored-draft-banner')).toBeInTheDocument()
  })

  it('shows no banner when no draft exists', async () => {
    // render without prior saveDraft
    // ...
    await waitFor(() => {
      expect(screen.queryByTestId('restored-draft-banner')).not.toBeInTheDocument()
    })
  })
})
```

**Note for implementer:** Mirror whatever provider wrapping the existing `ThreadView` tests already use. If no `ThreadView.spec.tsx` exists, create a minimal one — the component may require stubbing `MessageContext` and a thread data shape. Do not block on getting every provider right; if setup is too gnarly, reduce to a smaller component-level test that directly tests a new `useReplyDraftRestore(threadId)` hook you extract (see Step 4).

- [ ] **Step 3: Run the test and verify it fails**

```bash
yarn vitest run src/citizen-frontend/messages/ThreadView.spec.tsx
```

Expected: FAIL — draft not loaded, banner not rendered.

- [ ] **Step 4: Extract a `useReplyDraftRestore` hook**

Create `frontend/src/citizen-frontend/webpush/useReplyDraftRestore.ts`:

```typescript
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useState } from 'react'

import { deleteDraft, loadDraft, purgeOldDrafts } from './draftStore'

const DRAFT_TTL_MS = 1000 * 60 * 60 * 24 * 7 // 7 days

/**
 * On mount, loads any saved push-notification reply draft for `threadId` and
 * calls `onRestore` with the text. Also runs a best-effort purge of drafts
 * older than DRAFT_TTL_MS. Returns a boolean that flips true once a draft has
 * been restored so the UI can render a "restored from notification" hint.
 */
export function useReplyDraftRestore(
  threadId: string,
  onRestore: (text: string) => void
): { restored: boolean; clearRestored: () => void; discardDraft: () => void } {
  const [restored, setRestored] = useState(false)

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        await purgeOldDrafts(DRAFT_TTL_MS)
        const draft = await loadDraft(threadId)
        if (cancelled || !draft) return
        onRestore(draft.text)
        setRestored(true)
      } catch {
        // IDB unavailable — no draft to restore, not fatal.
      }
    })()
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [threadId])

  return {
    restored,
    clearRestored: () => setRestored(false),
    discardDraft: () => {
      void deleteDraft(threadId).catch(() => {})
      setRestored(false)
    }
  }
}
```

- [ ] **Step 5: Wire the hook into `ThreadView.tsx`**

In `frontend/src/citizen-frontend/messages/ThreadView.tsx`:

1. Add the import: `import { useReplyDraftRestore } from '../webpush/useReplyDraftRestore'`
2. Inside the component, after the existing `useContext(MessageContext)` call that gives you `setReplyContent`, add:

```tsx
const { restored, clearRestored, discardDraft } = useReplyDraftRestore(
  threadId,
  (text) => setReplyContent(threadId, text)
)
```

3. When the `MessageReplyEditor` successfully sends (find the existing success callback for the send action and extend it), call `discardDraft()` so the IDB entry is removed when the user finishes the reply via the UI.

4. Above the `MessageReplyEditor`, render a dismissible banner when `restored` is true. Use whatever existing banner/info-box component the citizen-frontend uses (grep for `InformationSection` or `AlertBox`). Example:

```tsx
{restored && (
  <AlertBox
    data-testid="restored-draft-banner"
    title={t.messages.restoredReplyDraft}
    onClose={clearRestored}
  />
)}
```

5. Read `?focus=reply` from the URL. The citizen router uses wouter — import `useSearch` from `wouter` (or mirror how other components in the citizen-frontend read query params). Example:

```tsx
import { useSearch } from 'wouter'
// ...
const search = useSearch()
const shouldFocusReply =
  new URLSearchParams(search).get('focus') === 'reply'
```

Pass `shouldFocusReply` to `MessageReplyEditor` as a prop (e.g. `autoFocus={shouldFocusReply || restored}`) — note that `MessageReplyEditor` already takes `autoFocus` per the earlier exploration (`lib-components/messages/MessageReplyEditor.tsx:116-121`), so this should just flow through.

**If `MessageReplyEditor` already has an `autoFocus` that's always true** — in which case the `?focus=reply` plumbing is unnecessary and you can skip Step 5.5. Just make sure the restored draft banner renders.

- [ ] **Step 6: Run the test and verify it passes**

```bash
yarn vitest run src/citizen-frontend/messages/ThreadView.spec.tsx
```

Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git add frontend/src/citizen-frontend/webpush/useReplyDraftRestore.ts \
        frontend/src/citizen-frontend/messages/ThreadView.tsx \
        frontend/src/citizen-frontend/messages/ThreadView.spec.tsx
git commit -m "feat(citizen-frontend/messages): restore reply draft from push notification"
```

---

## Task 11: Add `restoredReplyDraft` i18n strings

**Files:**
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx`
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx`
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx`

**Note:** These strings are used ONLY by the in-app restored-draft banner. The notification action label, placeholder, and success/error ack strings are sent by the backend (Task 3) because the SW doesn't have access to the i18n machinery.

- [ ] **Step 1: Locate the messages namespace**

Grep each file for `messages: {` to find the citizen messages i18n namespace. The banner string is a natural fit there (adjacent to existing message-related strings), not under `personalDetails.webPushSection`.

- [ ] **Step 2: Add the string to each file**

In `fi.tsx`, inside the `messages: { ... }` object, add:

```tsx
restoredReplyDraft: 'Viimeksi kirjoittamasi vastaus palautettiin ilmoituksesta.',
```

In `sv.tsx`:

```tsx
restoredReplyDraft: 'Ditt senaste svar har återställts från notiset.',
```

In `en.tsx`:

```tsx
restoredReplyDraft: 'Your last reply was restored from the notification.',
```

- [ ] **Step 3: Run the frontend type check to verify the structural i18n shape**

From `frontend/`:

```bash
yarn tsc --noEmit
```

Expected: PASS — no "missing key" errors between the three language files.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx \
        frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx \
        frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx
git commit -m "feat(citizen-frontend/i18n): add restored reply draft banner string"
```

---

## Task 12: Manual end-to-end device test

**Purpose:** The SW + IDB + notification flow is hard to unit-test end-to-end. This task verifies the feature on a real Android Chrome device (the only platform currently supporting `type: 'text'` inline reply in web push).

**Files:** None — manual verification only.

- [ ] **Step 1: Start the stack and expose via tunnel**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
mise trust /Volumes/evaka/evaka/mise.toml
mise start
# in another terminal:
cloudflared tunnel --url http://localhost:9099
```

Note the generated `https://*.trycloudflare.com` URL. Restart pm2 with `TUNNEL_URL=<url>` set so SAML flows work via the tunnel.

- [ ] **Step 2: Subscribe to push on the device**

On your Android Chrome phone, visit the tunnel URL, log in via suomi.fi, navigate to personal details → web push, toggle push notifications on.

- [ ] **Step 3: Trigger a real message notification**

From a second browser (employee side), log in as a staff member with messaging access, send a message to the citizen you subscribed as. Verify the push arrives on the phone with a "Reply" action button.

- [ ] **Step 4: Test inline reply success**

Tap the "Reply" action. A text input appears (Android Chrome only). Type "inline test ok" and submit. Verify:
- The original notification is replaced by a "Reply sent" ack.
- On the employee side, the reply appears in the thread.

- [ ] **Step 5: Test offline error recovery**

Repeat Step 3 to get a new push. Enable airplane mode on the phone. Tap Reply, type "offline test", submit. Verify:
- An error notification "Reply not sent" appears.
- Tap the error notification. The app opens, navigates to the thread. If session is still valid, the reply textarea is prefilled with "offline test".
- Disable airplane mode, tap "Send" in the app UI. Verify the reply goes through.

- [ ] **Step 6: Test session-expiry recovery**

Repeat Step 3 to get a new push. On the phone, clear the `evakaSession` cookie via Chrome devtools (remote debugging) or wait for it to expire. Tap Reply, type "session expired test", submit. Verify:
- An error notification appears.
- Tapping the error notification opens the app → SAML login → after login, returns to `/messages/{threadId}` → reply textarea is prefilled with "session expired test".
- Sending via the app UI works.

- [ ] **Step 7: Test fallback (action button on non-inline-reply platform)**

Test on desktop Chrome or Firefox where `type: 'text'` may be rendered as a plain button. Tap the Reply button (no text input). Verify the app opens at `/messages/{threadId}?focus=reply` with the textarea focused.

- [ ] **Step 8: Test main notification click (no action)**

Tap the body of the notification (not the Reply action). Verify the app opens at `/messages/{threadId}` — this is the existing behavior, should still work.

- [ ] **Step 9: Document any findings**

If any step fails, file as a follow-up task. This manual test is the final gate before declaring the feature complete.

---

## Summary

**Tasks 1–6:** Backend payload extension and query refactoring (replyAction with threadId, recipient IDs, localized strings; LATERAL JOIN to compute reply-all participants; TDD with unit + integration tests).

**Tasks 7:** IndexedDB draft store utility with fake-indexeddb tests.

**Tasks 8–9:** Service worker extensions (declare action, handle click, POST reply, show ack, persist draft on failure).

**Tasks 10–11:** Thread view draft restoration and i18n strings.

**Task 12:** Manual end-to-end device verification.

**Total:** 12 tasks, backend (5) + frontend (6) + manual verification (1). Each task is a single commit with its own tests where feasible. The plan follows the existing POC's style: TDD with red-green-commit, JUnit 5 + mockito-kotlin on the backend, vitest + fake-indexeddb on the frontend, SW changes covered only by manual testing.
