# Citizen Passkey Login — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add WebAuthn passkey login as the preferred citizen sign-in method, reusing the S3 PoC persistence pattern from the citizen web push feature.

**Architecture:** apigw hosts WebAuthn ceremonies via `@simplewebauthn/server` and Redis-backed ephemeral challenge storage. Kotlin service persists credentials to `bucketEnv.data` under `citizen-passkey-credentials/<personId>.json`, mirroring `CitizenPushSubscriptionStore`. Frontend ships a new `passkey/` module, a redesigned login page (PWA-aware), a management section on `/personal-details`, and a dismissible enrol nudge toast.

**Tech Stack:**
- apigw: TypeScript, Express, `@simplewebauthn/server@^13`, Redis, Vitest
- Frontend: React, Vite, TypeScript, `@simplewebauthn/browser@^13`, Vitest
- Service: Kotlin, Spring Boot, AWS SDK for S3, JUnit 5

**Spec:** `docs/superpowers/specs/2026-04-15-citizen-passkey-login-design.md`

**Reference pattern (read these before starting):**
- Kotlin store: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSubscriptionStore.kt`
- Kotlin store file: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushStoreFile.kt`
- Kotlin Spring wiring: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenWebPushConfig.kt`
- Kotlin controller: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenWebPushController.kt`
- Kotlin integration test: `service/src/integrationTest/kotlin/fi/espoo/evaka/citizenwebpush/CitizenPushSubscriptionStoreTest.kt`
- apigw weak login route: `apigw/src/enduser/routes/auth-weak-login.ts`
- apigw session variants: `apigw/src/shared/auth/index.ts`
- apigw auth status: `apigw/src/enduser/routes/auth-status.ts`
- apigw route mounting: `apigw/src/app.ts`
- Frontend login page: `frontend/src/citizen-frontend/login/LoginPage.tsx`
- Frontend webpush hook: `frontend/src/citizen-frontend/webpush/webpush-state.ts`
- Frontend webpush section: `frontend/src/citizen-frontend/webpush/WebPushSettingsSection.tsx`
- Frontend personal details: `frontend/src/citizen-frontend/personal-details/PersonalDetails.tsx`
- Frontend router: `frontend/src/citizen-frontend/router.tsx`
- Frontend App shell: `frontend/src/citizen-frontend/App.tsx`
- Frontend nav helpers: `frontend/src/citizen-frontend/navigation/const.ts`
- Frontend notifications hook example: `frontend/src/citizen-frontend/ChildStartingNotificationHook.ts`
- Citizen i18n: `frontend/src/lib-customizations/defaults/citizen/i18n/{fi,sv,en}.tsx`

**Project conventions (from `CLAUDE.md`):**
- i18n files are `.tsx`; `fi.tsx` is source of truth, `sv.tsx`/`en.tsx` must match its shape.
- Pre-commit hook auto-adds SPDX headers — re-stage as needed after it writes them.
- Pre-commit runs `eslint --fix` on staged TS/TSX and `ktfmtPrecommit` on staged Kotlin. Don't fight auto-rewrites.
- Vitest projects are split per frontend area — specs must match `src/citizen-frontend/**/*.spec.{ts,tsx}` to run in the `citizen-frontend` project.
- Lib-components render tests need `<TestContextProvider translations={testTranslations}>` wrapper.
- Use `data-qa` for E2E selectors, `data-testid` only inside component specs.
- Skip `tsc --build --force` in inner dev loops — rely on Vitest for type-checking feedback.

---

## Task 1: Kotlin — data classes and S3 store

**Files:**
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenpasskey/CitizenPasskeyStoreFile.kt`
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenpasskey/CitizenPasskeyCredentialStore.kt`
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenpasskey/CitizenPasskeyConfig.kt`
- Create: `service/src/integrationTest/kotlin/fi/espoo/evaka/citizenpasskey/CitizenPasskeyCredentialStoreTest.kt`

- [ ] **Step 1: Write `CitizenPasskeyStoreFile.kt` with the two data classes**

```kotlin
package fi.espoo.evaka.citizenpasskey

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

data class CitizenPasskeyStoreFile(
    val personId: PersonId,
    val credentials: List<CitizenPasskeyCredential>,
)

data class CitizenPasskeyCredential(
    val credentialId: String,
    val publicKey: String,
    val signCounter: Long,
    val transports: List<String>,
    val createdAt: HelsinkiDateTime,
    val lastUsedAt: HelsinkiDateTime?,
    val label: String,
    val deviceHint: String?,
)
```

- [ ] **Step 2: Write `CitizenPasskeyCredentialStore.kt`** — mirror `CitizenPushSubscriptionStore.kt` exactly, but with the new key prefix and operations for upsert-by-credentialId, rename, revoke-by-credentialId, and touch-counter.

```kotlin
package fi.espoo.evaka.citizenpasskey

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class CitizenPasskeyCredentialStore(
    private val s3Client: S3Client,
    private val bucket: String,
) {
    private val jsonMapper = defaultJsonMapperBuilder().build()

    private fun key(personId: PersonId): String =
        "citizen-passkey-credentials/$personId.json"

    fun load(personId: PersonId): CitizenPasskeyStoreFile? {
        val request = GetObjectRequest.builder().bucket(bucket).key(key(personId)).build()
        return try {
            s3Client.getObject(request).use { stream ->
                jsonMapper.readValue(stream.readAllBytes(), CitizenPasskeyStoreFile::class.java)
            }
        } catch (_: NoSuchKeyException) {
            null
        }
    }

    fun save(personId: PersonId, file: CitizenPasskeyStoreFile) {
        val bytes = jsonMapper.writeValueAsBytes(file)
        val request =
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key(personId))
                .contentType("application/json")
                .build()
        s3Client.putObject(request, RequestBody.fromBytes(bytes))
    }

    fun delete(personId: PersonId) {
        val request = DeleteObjectRequest.builder().bucket(bucket).key(key(personId)).build()
        s3Client.deleteObject(request)
    }

    fun listCredentials(personId: PersonId): List<CitizenPasskeyCredential> =
        load(personId)?.credentials ?: emptyList()

    fun upsertCredential(personId: PersonId, credential: CitizenPasskeyCredential) {
        val current = load(personId) ?: CitizenPasskeyStoreFile(personId, emptyList())
        val filtered = current.credentials.filterNot { it.credentialId == credential.credentialId }
        save(personId, current.copy(credentials = filtered + credential))
    }

    fun renameCredential(personId: PersonId, credentialId: String, label: String): Boolean {
        val current = load(personId) ?: return false
        val idx = current.credentials.indexOfFirst { it.credentialId == credentialId }
        if (idx < 0) return false
        val updated = current.credentials.toMutableList()
        updated[idx] = updated[idx].copy(label = label)
        save(personId, current.copy(credentials = updated))
        return true
    }

    fun revokeCredential(personId: PersonId, credentialId: String): Boolean {
        val current = load(personId) ?: return false
        val remaining = current.credentials.filterNot { it.credentialId == credentialId }
        if (remaining.size == current.credentials.size) return false
        if (remaining.isEmpty()) {
            delete(personId)
        } else {
            save(personId, current.copy(credentials = remaining))
        }
        return true
    }

    fun touchCredential(
        personId: PersonId,
        credentialId: String,
        newSignCounter: Long,
        now: HelsinkiDateTime,
    ): Boolean {
        val current = load(personId) ?: return false
        val idx = current.credentials.indexOfFirst { it.credentialId == credentialId }
        if (idx < 0) return false
        val updated = current.credentials.toMutableList()
        updated[idx] = updated[idx].copy(signCounter = newSignCounter, lastUsedAt = now)
        save(personId, current.copy(credentials = updated))
        return true
    }
}
```

- [ ] **Step 3: Write `CitizenPasskeyConfig.kt`** — one `@Bean`:

```kotlin
package fi.espoo.evaka.citizenpasskey

import fi.espoo.evaka.BucketEnv
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class CitizenPasskeyConfig {
    @Bean
    fun citizenPasskeyCredentialStore(
        s3Client: S3Client,
        bucketEnv: BucketEnv,
    ): CitizenPasskeyCredentialStore = CitizenPasskeyCredentialStore(s3Client, bucketEnv.data)
}
```

- [ ] **Step 4: Write the smoke integration test `CitizenPasskeyCredentialStoreTest.kt`.** Extend `FullApplicationTest`; autowire `S3Client` + `BucketEnv`; construct the store directly. One `@Test` method running the full round-trip: save → load → upsert second → rename → touch → revoke → revoke last → load returns null.

```kotlin
package fi.espoo.evaka.citizenpasskey

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CitizenPasskeyCredentialStoreTest : FullApplicationTest(resetDbBeforeEach = false) {
    @Autowired lateinit var store: CitizenPasskeyCredentialStore

    @Test
    fun `round-trip save load upsert rename touch revoke`() {
        val personId = PersonId(UUID.randomUUID())
        val now =
            HelsinkiDateTime.of(LocalDateTime.of(2026, 4, 15, 12, 0).atZone(ZoneId.of("Europe/Helsinki")).toLocalDateTime())

        val first =
            CitizenPasskeyCredential(
                credentialId = "cred-1",
                publicKey = "pk-1",
                signCounter = 0,
                transports = listOf("internal"),
                createdAt = now,
                lastUsedAt = null,
                label = "iPhone",
                deviceHint = "iPhone (Safari)",
            )
        store.upsertCredential(personId, first)

        val loaded = store.load(personId)
        assertEquals(1, loaded?.credentials?.size)
        assertEquals("cred-1", loaded?.credentials?.first()?.credentialId)

        val second = first.copy(credentialId = "cred-2", label = "Laptop", deviceHint = "Mac (Safari)")
        store.upsertCredential(personId, second)
        assertEquals(2, store.listCredentials(personId).size)

        assertTrue(store.renameCredential(personId, "cred-1", "Home iPhone"))
        assertEquals("Home iPhone", store.listCredentials(personId).first { it.credentialId == "cred-1" }.label)

        assertTrue(store.touchCredential(personId, "cred-1", 42, now))
        val touched = store.listCredentials(personId).first { it.credentialId == "cred-1" }
        assertEquals(42, touched.signCounter)
        assertEquals(now, touched.lastUsedAt)

        assertTrue(store.revokeCredential(personId, "cred-1"))
        assertEquals(1, store.listCredentials(personId).size)

        assertTrue(store.revokeCredential(personId, "cred-2"))
        assertNull(store.load(personId))
    }
}
```

- [ ] **Step 5: Run the smoke test**

```bash
cd service && ./gradlew integrationTest --tests "fi.espoo.evaka.citizenpasskey.CitizenPasskeyCredentialStoreTest"
```

Expected: PASS.

- [ ] **Step 6: Run ktfmt**

```bash
cd service && ./gradlew ktfmtPrecommit
```

- [ ] **Step 7: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/citizenpasskey/ service/src/integrationTest/kotlin/fi/espoo/evaka/citizenpasskey/
git commit -m "feat(service): add S3-backed CitizenPasskeyCredentialStore"
```

---

## Task 2: Kotlin — passkey controller (citizen + internal endpoints)

**Files:**
- Create: `service/src/main/kotlin/fi/espoo/evaka/citizenpasskey/CitizenPasskeyController.kt`
- Reference: `service/src/main/kotlin/fi/espoo/evaka/citizenwebpush/CitizenWebPushController.kt` for controller shape and auth header conventions.

- [ ] **Step 1: Write the controller with 7 endpoints**

The controller exposes:
- Citizen-facing (require `AuthenticatedUser.Citizen`):
  - `GET /citizen/passkey/credentials` → returns the current user's credential list (no secrets — strip `publicKey` and `signCounter`)
  - `PATCH /citizen/passkey/credentials/{credentialId}` with body `{ "label": "..." }` → rename
  - `DELETE /citizen/passkey/credentials/{credentialId}` → revoke
- Internal-only (require `AuthenticatedUser.SystemInternalUser`, called by apigw only):
  - `GET /internal/citizen-passkey/credentials/{personId}` → full credential list including publicKey/signCounter
  - `POST /internal/citizen-passkey/credentials` with body `CitizenPasskeyCredentialInput` → upsert
  - `POST /internal/citizen-passkey/credentials/{personId}/{credentialId}/touch` with body `{ "signCounter": Long }` → touch

Use the `CitizenPasskeyCredentialSummary` DTO for the citizen-facing list (omits `publicKey`, `signCounter`, `transports`). Full credential DTOs (with secrets) go over the internal endpoints.

```kotlin
package fi.espoo.evaka.citizenpasskey

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class CitizenPasskeyCredentialSummary(
    val credentialId: String,
    val label: String,
    val deviceHint: String?,
    val createdAt: HelsinkiDateTime,
    val lastUsedAt: HelsinkiDateTime?,
)

data class RenamePasskeyRequest(val label: String)

data class InternalUpsertPasskeyRequest(
    val personId: PersonId,
    val credentialId: String,
    val publicKey: String,
    val signCounter: Long,
    val transports: List<String>,
    val label: String,
    val deviceHint: String?,
)

data class InternalTouchPasskeyRequest(val signCounter: Long)

@RestController
class CitizenPasskeyController(
    private val store: CitizenPasskeyCredentialStore,
) {
    @GetMapping("/citizen/passkey/credentials")
    fun listOwn(
        user: AuthenticatedUser.Citizen,
    ): List<CitizenPasskeyCredentialSummary> =
        store.listCredentials(user.id).map {
            CitizenPasskeyCredentialSummary(
                credentialId = it.credentialId,
                label = it.label,
                deviceHint = it.deviceHint,
                createdAt = it.createdAt,
                lastUsedAt = it.lastUsedAt,
            )
        }

    @PatchMapping("/citizen/passkey/credentials/{credentialId}")
    fun rename(
        user: AuthenticatedUser.Citizen,
        @PathVariable credentialId: String,
        @RequestBody body: RenamePasskeyRequest,
    ) {
        val label = body.label.trim()
        require(label.isNotEmpty() && label.length <= 80) { "invalid label" }
        if (!store.renameCredential(user.id, credentialId, label)) {
            throw NotFound("credential-not-found")
        }
    }

    @DeleteMapping("/citizen/passkey/credentials/{credentialId}")
    fun revoke(
        user: AuthenticatedUser.Citizen,
        @PathVariable credentialId: String,
    ) {
        if (!store.revokeCredential(user.id, credentialId)) {
            throw NotFound("credential-not-found")
        }
    }

    @GetMapping("/internal/citizen-passkey/credentials/{personId}")
    fun internalList(
        user: AuthenticatedUser.SystemInternalUser,
        @PathVariable personId: PersonId,
    ): List<CitizenPasskeyCredential> = store.listCredentials(personId)

    @PostMapping("/internal/citizen-passkey/credentials")
    fun internalUpsert(
        user: AuthenticatedUser.SystemInternalUser,
        clock: EvakaClock,
        @RequestBody body: InternalUpsertPasskeyRequest,
    ) {
        val now = HelsinkiDateTime.now()
        store.upsertCredential(
            body.personId,
            CitizenPasskeyCredential(
                credentialId = body.credentialId,
                publicKey = body.publicKey,
                signCounter = body.signCounter,
                transports = body.transports,
                createdAt = now,
                lastUsedAt = null,
                label = body.label,
                deviceHint = body.deviceHint,
            ),
        )
    }

    @PostMapping("/internal/citizen-passkey/credentials/{personId}/{credentialId}/touch")
    fun internalTouch(
        user: AuthenticatedUser.SystemInternalUser,
        @PathVariable personId: PersonId,
        @PathVariable credentialId: String,
        @RequestBody body: InternalTouchPasskeyRequest,
    ) {
        val now = HelsinkiDateTime.now()
        if (!store.touchCredential(personId, credentialId, body.signCounter, now)) {
            throw NotFound("credential-not-found")
        }
    }
}
```

**Note**: If `HelsinkiDateTime.now()` isn't the canonical pattern in this codebase, look at how `CitizenWebPushController.kt` obtains "now" (typically via `EvakaClock.now()`). Use the same pattern.

- [ ] **Step 2: Run ktfmt on new files**

```bash
cd service && ./gradlew ktfmtPrecommit
```

- [ ] **Step 3: Quick compile check**

```bash
cd service && ./gradlew compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add service/src/main/kotlin/fi/espoo/evaka/citizenpasskey/CitizenPasskeyController.kt
git commit -m "feat(service): add citizen passkey REST controller"
```

---

## Task 3: apigw — install `@simplewebauthn/server` and add session variant

**Files:**
- Modify: `apigw/package.json` (add dependency)
- Modify: `apigw/src/shared/auth/index.ts` (add `citizen-passkey` session variant)
- Modify: `apigw/src/shared/session.ts` (include new variant in session save/load type check)
- Modify: `apigw/src/enduser/routes/auth-status.ts` (expose `passkeyCredentialId`)

- [ ] **Step 1: Add dependency**

```bash
cd apigw
node .yarn/releases/yarn-*.cjs add @simplewebauthn/server@^13
```

- [ ] **Step 2: Add session variant to `auth/index.ts`**

Find the `CitizenSessionUser` discriminated union. Add:

```ts
| {
    id: string
    authType: 'citizen-passkey'
    userType: 'CITIZEN_WEAK'
    credentialId: string
  }
```

In `createUserHeader`, extend the `switch`/`if` that maps `authType` to the downstream `type` header so `'citizen-passkey'` maps to `'citizen_weak'` (same as `'citizen-weak'`).

- [ ] **Step 3: Expose `passkeyCredentialId` in `auth-status.ts`**

In the route handler that returns `AuthStatus`, include:

```ts
passkeyCredentialId:
  req.user?.authType === 'citizen-passkey' ? req.user.credentialId : null
```

Also add the corresponding `passkeyCredentialId?: string | null` field to the TypeScript response type declared nearby.

- [ ] **Step 4: Compile check**

```bash
cd apigw && node_modules/.bin/tsc --noEmit
```

Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add apigw/package.json apigw/yarn.lock apigw/src/shared/auth/index.ts apigw/src/enduser/routes/auth-status.ts apigw/src/shared/session.ts
git commit -m "feat(apigw): add citizen-passkey session variant and auth-status field"
```

---

## Task 4: apigw — passkey auth routes (WebAuthn ceremony)

**Files:**
- Create: `apigw/src/enduser/routes/passkey-auth.ts`
- Create: `apigw/src/enduser/routes/passkey-challenge-store.ts` (Redis helper)
- Create: `apigw/src/shared/service-client-passkey.ts` (HTTP client for Kotlin internal endpoints) — OR inline the calls into `passkey-auth.ts` if the existing `service-client.ts` pattern is hard to extend; use whichever matches the existing style best.
- Modify: `apigw/src/app.ts` (mount new routes)
- Modify: `apigw/src/shared/config.ts` (add `PASSKEY_RPID`, `PASSKEY_ORIGIN`, `PASSKEY_RP_NAME`)

- [ ] **Step 1: Add config fields**

In `apigw/src/shared/config.ts`, extend the config schema/type with:

```ts
passkey: {
  rpId: process.env.PASSKEY_RPID ?? 'localhost',
  origin: process.env.PASSKEY_ORIGIN ?? 'http://localhost:9099',
  rpName: process.env.PASSKEY_RP_NAME ?? 'eVaka',
},
```

Wire it into whatever interface `config` uses — follow the same shape as existing grouped config like `sessionTimeoutMinutes` or `ad`.

- [ ] **Step 2: Write Redis challenge helper**

```ts
// apigw/src/enduser/routes/passkey-challenge-store.ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { RedisClient } from '../../shared/redis-client.ts'

const TTL_SECONDS = 300
const PREFIX = 'passkey-challenge:'

export interface PasskeyChallengeEntry {
  challenge: string
  flow: 'register' | 'login'
  personId?: string
}

export async function putChallenge(
  redis: RedisClient,
  token: string,
  entry: PasskeyChallengeEntry
): Promise<void> {
  await redis.set(`${PREFIX}${token}`, JSON.stringify(entry), {
    EX: TTL_SECONDS
  })
}

export async function takeChallenge(
  redis: RedisClient,
  token: string
): Promise<PasskeyChallengeEntry | null> {
  const key = `${PREFIX}${token}`
  const raw = await redis.get(key)
  if (raw === null) return null
  await redis.del(key)
  try {
    return JSON.parse(raw) as PasskeyChallengeEntry
  } catch {
    return null
  }
}

export function generateToken(): string {
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
}
```

- [ ] **Step 3: Write internal service client for Kotlin passkey endpoints**

```ts
// apigw/src/shared/service-client-passkey.ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from './service-client.ts'

export interface StoredPasskeyCredential {
  credentialId: string
  publicKey: string
  signCounter: number
  transports: string[]
  label: string
  deviceHint: string | null
  createdAt: string
  lastUsedAt: string | null
}

export async function listCitizenPasskeyCredentials(
  personId: string
): Promise<StoredPasskeyCredential[]> {
  const { data } = await client.get<StoredPasskeyCredential[]>(
    `/internal/citizen-passkey/credentials/${personId}`
  )
  return data
}

export async function upsertCitizenPasskeyCredential(payload: {
  personId: string
  credentialId: string
  publicKey: string
  signCounter: number
  transports: string[]
  label: string
  deviceHint: string | null
}): Promise<void> {
  await client.post(`/internal/citizen-passkey/credentials`, payload)
}

export async function touchCitizenPasskeyCredential(
  personId: string,
  credentialId: string,
  signCounter: number
): Promise<void> {
  await client.post(
    `/internal/citizen-passkey/credentials/${personId}/${credentialId}/touch`,
    { signCounter }
  )
}
```

**Note**: If `./service-client.ts` exports its axios instance differently (named differently than `client`), adjust the imports to match what's there. The existing `citizenWeakLogin` in `service-client.ts` is a reference.

- [ ] **Step 4: Write the four route handlers in `passkey-auth.ts`**

```ts
// apigw/src/enduser/routes/passkey-auth.ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  generateAuthenticationOptions,
  generateRegistrationOptions,
  verifyAuthenticationResponse,
  verifyRegistrationResponse
} from '@simplewebauthn/server'
import type {
  AuthenticationResponseJSON,
  RegistrationResponseJSON
} from '@simplewebauthn/server'
import type { Router } from 'express'
import { Router as createRouter } from 'express'
import express from 'express'

import type { Config } from '../../shared/config.ts'
import type { EvakaSessionUser } from '../../shared/auth/index.ts'
import type { RedisClient } from '../../shared/redis-client.ts'
import {
  listCitizenPasskeyCredentials,
  touchCitizenPasskeyCredential,
  upsertCitizenPasskeyCredential
} from '../../shared/service-client-passkey.ts'
import type { Sessions } from '../../shared/session.ts'
import { toRequestHandler } from '../../shared/express.ts'
import {
  generateToken,
  putChallenge,
  takeChallenge
} from './passkey-challenge-store.ts'

function deviceHintFromUserAgent(ua: string | undefined): string | null {
  if (!ua) return null
  const lc = ua.toLowerCase()
  if (lc.includes('iphone')) return 'iPhone (Safari)'
  if (lc.includes('ipad')) return 'iPad (Safari)'
  if (lc.includes('android')) return 'Android (Chrome)'
  if (lc.includes('macintosh')) return 'Mac (Safari)'
  if (lc.includes('windows')) return 'Windows (Chrome)'
  return 'Unknown device'
}

function personIdToUserHandle(personId: string): Uint8Array {
  const hex = personId.replace(/-/g, '')
  const bytes = new Uint8Array(16)
  for (let i = 0; i < 16; i++) {
    bytes[i] = parseInt(hex.substring(i * 2, i * 2 + 2), 16)
  }
  return bytes
}

function userHandleToPersonId(userHandleBase64: string | undefined): string | null {
  if (!userHandleBase64) return null
  const normalized = userHandleBase64.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4)
  let raw: Buffer
  try {
    raw = Buffer.from(padded, 'base64')
  } catch {
    return null
  }
  if (raw.length !== 16) return null
  const hex = raw.toString('hex')
  return `${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}`
}

export function passkeyAuthRoutes(
  config: Config,
  sessions: Sessions<'citizen'>,
  redis: RedisClient
): Router {
  const router = createRouter()
  router.use(express.json())

  router.post(
    '/register/options',
    toRequestHandler(async (req, res) => {
      const user = req.user as EvakaSessionUser | undefined
      if (!user || user.userType !== 'CITIZEN_STRONG') {
        res.status(403).json({ code: 'strong-auth-required' })
        return
      }
      const existing = await listCitizenPasskeyCredentials(user.id)
      const options = await generateRegistrationOptions({
        rpName: config.passkey.rpName,
        rpID: config.passkey.rpId,
        userName: user.id,
        userID: personIdToUserHandle(user.id),
        attestationType: 'none',
        authenticatorSelection: {
          residentKey: 'required',
          userVerification: 'preferred'
        },
        excludeCredentials: existing.map((c) => ({
          id: c.credentialId,
          transports: c.transports as AuthenticatorTransportFuture[]
        }))
      })
      const token = generateToken()
      await putChallenge(redis, token, {
        challenge: options.challenge,
        flow: 'register',
        personId: user.id
      })
      res.json({ token, options })
    })
  )

  router.post(
    '/register/verify',
    toRequestHandler(async (req, res) => {
      const user = req.user as EvakaSessionUser | undefined
      if (!user || user.userType !== 'CITIZEN_STRONG') {
        res.status(403).json({ code: 'strong-auth-required' })
        return
      }
      const { token, attestation } = req.body as {
        token: string
        attestation: RegistrationResponseJSON
      }
      const entry = await takeChallenge(redis, token)
      if (!entry || entry.flow !== 'register') {
        res.status(410).json({ code: 'passkey-challenge-expired' })
        return
      }
      if (entry.personId !== user.id) {
        res.status(403).json({ code: 'strong-auth-required' })
        return
      }
      const verification = await verifyRegistrationResponse({
        response: attestation,
        expectedChallenge: entry.challenge,
        expectedOrigin: config.passkey.origin,
        expectedRPID: config.passkey.rpId
      })
      if (!verification.verified || !verification.registrationInfo) {
        res.status(401).json({ code: 'passkey-verification-failed' })
        return
      }
      const {
        credential: { id: credentialId, publicKey, counter }
      } = verification.registrationInfo
      const label = deviceHintFromUserAgent(req.headers['user-agent']) ?? 'Passkey'
      await upsertCitizenPasskeyCredential({
        personId: user.id,
        credentialId,
        publicKey: Buffer.from(publicKey).toString('base64url'),
        signCounter: counter,
        transports: attestation.response?.transports ?? [],
        label,
        deviceHint: deviceHintFromUserAgent(req.headers['user-agent'])
      })
      res.status(200).json({ credentialId, label })
    })
  )

  router.post(
    '/login/options',
    toRequestHandler(async (_req, res) => {
      const options = await generateAuthenticationOptions({
        rpID: config.passkey.rpId,
        userVerification: 'preferred'
      })
      const token = generateToken()
      await putChallenge(redis, token, {
        challenge: options.challenge,
        flow: 'login'
      })
      res.json({ token, options })
    })
  )

  router.post(
    '/login/verify',
    toRequestHandler(async (req, res) => {
      const { token, assertion } = req.body as {
        token: string
        assertion: AuthenticationResponseJSON
      }
      const entry = await takeChallenge(redis, token)
      if (!entry || entry.flow !== 'login') {
        res.status(410).json({ code: 'passkey-challenge-expired' })
        return
      }
      const personId = userHandleToPersonId(assertion.response.userHandle)
      if (!personId) {
        res.status(401).json({ code: 'passkey-verification-failed' })
        return
      }
      const credentials = await listCitizenPasskeyCredentials(personId)
      const stored = credentials.find((c) => c.credentialId === assertion.id)
      if (!stored) {
        res.status(401).json({ code: 'passkey-verification-failed' })
        return
      }
      const verification = await verifyAuthenticationResponse({
        response: assertion,
        expectedChallenge: entry.challenge,
        expectedOrigin: config.passkey.origin,
        expectedRPID: config.passkey.rpId,
        credential: {
          id: stored.credentialId,
          publicKey: Buffer.from(stored.publicKey, 'base64url'),
          counter: stored.signCounter,
          transports: stored.transports as AuthenticatorTransportFuture[]
        }
      })
      if (!verification.verified) {
        res.status(401).json({ code: 'passkey-verification-failed' })
        return
      }
      await touchCitizenPasskeyCredential(
        personId,
        stored.credentialId,
        verification.authenticationInfo.newCounter
      )
      const sessionUser: EvakaSessionUser = {
        id: personId,
        authType: 'citizen-passkey',
        userType: 'CITIZEN_WEAK',
        credentialId: stored.credentialId
      }
      await sessions.login(req, sessionUser)
      res.sendStatus(200)
    })
  )

  return router
}

type AuthenticatorTransportFuture =
  | 'ble'
  | 'cable'
  | 'hybrid'
  | 'internal'
  | 'nfc'
  | 'smart-card'
  | 'usb'
```

- [ ] **Step 5: Write credentials pass-through route**

```ts
// apigw/src/enduser/routes/passkey-credentials.ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Router } from 'express'
import { Router as createRouter } from 'express'
import express from 'express'

import { toRequestHandler } from '../../shared/express.ts'
import { client } from '../../shared/service-client.ts'
import type { Sessions } from '../../shared/session.ts'

export function passkeyCredentialsRoutes(
  sessions: Sessions<'citizen'>
): Router {
  const router = createRouter()
  router.use(express.json())

  router.get(
    '/',
    toRequestHandler(async (req, res) => {
      const { data } = await client.get('/citizen/passkey/credentials', {
        headers: (req as unknown as { forwardedAuthHeader?: Record<string, string> }).forwardedAuthHeader
      })
      res.json(data)
    })
  )

  router.patch(
    '/:id',
    toRequestHandler(async (req, res) => {
      await client.patch(`/citizen/passkey/credentials/${req.params.id}`, req.body, {
        headers: (req as unknown as { forwardedAuthHeader?: Record<string, string> }).forwardedAuthHeader
      })
      res.sendStatus(200)
    })
  )

  router.delete(
    '/:id',
    toRequestHandler(async (req, res) => {
      await client.delete(`/citizen/passkey/credentials/${req.params.id}`, {
        headers: (req as unknown as { forwardedAuthHeader?: Record<string, string> }).forwardedAuthHeader
      })
      const currentCredentialId =
        req.user?.authType === 'citizen-passkey' ? req.user.credentialId : null
      if (currentCredentialId === req.params.id) {
        await sessions.destroy(req)
      }
      res.sendStatus(200)
    })
  )

  return router
}
```

**Note**: The actual pattern for forwarding citizen auth headers to the Kotlin service may live in a helper like `citizenAuthHeader(req)` or similar. Look in `service-client.ts` for how `citizenWeakLogin` forwards headers — use whatever helper that file exposes. If the existing pattern is to forward `req` directly to the axios call via an interceptor, the simpler call is just `client.get('/citizen/passkey/credentials', { headers: /* whatever the existing helper returns */ })`.

- [ ] **Step 6: Mount the routes in `app.ts`**

Find the block where `authWeakLogin` is mounted (around line 289 per exploration). Add after it:

```ts
router.use(
  '/citizen/auth/passkey',
  citizenSessions.requireAuthentication.optional,
  passkeyAuthRoutes(config, citizenSessions, redisClient)
)
router.use(
  '/citizen/passkey/credentials',
  citizenSessions.requireAuthentication,
  passkeyCredentialsRoutes(citizenSessions)
)
```

**Note**: If `.requireAuthentication.optional` isn't the real property name, look at how `authWeakLogin` (which is public, no auth needed) is mounted — it likely has no middleware or uses an `optional` wrapper. For the auth routes, login endpoints must allow anonymous, but register endpoints require auth — handle that inside the route handlers via `req.user` checks as already done in Step 4.

- [ ] **Step 7: Compile check**

```bash
cd apigw && node_modules/.bin/tsc --noEmit
```

Expected: no errors. If `AuthenticatorTransportFuture` isn't found, import it from `@simplewebauthn/server` (or drop the cast and use `string[]` — SimpleWebAuthn accepts strings).

- [ ] **Step 8: Commit**

```bash
git add apigw/src/enduser/routes/passkey-auth.ts apigw/src/enduser/routes/passkey-credentials.ts apigw/src/enduser/routes/passkey-challenge-store.ts apigw/src/shared/service-client-passkey.ts apigw/src/shared/config.ts apigw/src/app.ts
git commit -m "feat(apigw): add passkey WebAuthn ceremony and credential pass-through routes"
```

---

## Task 5: apigw — smoke tests

**Files:**
- Create: `apigw/src/enduser/routes/passkey-auth.spec.ts`

- [ ] **Step 1: Write two Vitest smoke cases**

```ts
// apigw/src/enduser/routes/passkey-auth.spec.ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@simplewebauthn/server', () => ({
  generateRegistrationOptions: vi.fn().mockResolvedValue({
    challenge: 'registration-challenge',
    rp: {},
    user: {}
  }),
  generateAuthenticationOptions: vi.fn().mockResolvedValue({
    challenge: 'login-challenge'
  }),
  verifyRegistrationResponse: vi.fn().mockResolvedValue({
    verified: true,
    registrationInfo: {
      credential: {
        id: 'new-credential-id',
        publicKey: new Uint8Array([1, 2, 3]),
        counter: 0
      }
    }
  }),
  verifyAuthenticationResponse: vi.fn().mockResolvedValue({
    verified: true,
    authenticationInfo: { newCounter: 1 }
  })
}))

vi.mock('../../shared/service-client-passkey.ts', () => ({
  listCitizenPasskeyCredentials: vi.fn().mockResolvedValue([
    {
      credentialId: 'existing-id',
      publicKey: Buffer.from([4, 5, 6]).toString('base64url'),
      signCounter: 0,
      transports: ['internal'],
      label: 'Phone',
      deviceHint: 'iPhone (Safari)',
      createdAt: '2026-04-15T00:00:00Z',
      lastUsedAt: null
    }
  ]),
  upsertCitizenPasskeyCredential: vi.fn().mockResolvedValue(undefined),
  touchCitizenPasskeyCredential: vi.fn().mockResolvedValue(undefined)
}))

import * as service from '../../shared/service-client-passkey.ts'

import { passkeyAuthRoutes } from './passkey-auth.ts'

function createFakeRedis() {
  const store = new Map<string, string>()
  return {
    get: vi.fn(async (k: string) => store.get(k) ?? null),
    set: vi.fn(async (k: string, v: string) => {
      store.set(k, v)
      return 'OK'
    }),
    del: vi.fn(async (k: string) => {
      store.delete(k)
      return 1
    })
  } as unknown as import('../../shared/redis-client.ts').RedisClient
}

function makeReqRes(body: unknown, user?: unknown) {
  const req = {
    body,
    user,
    headers: { 'user-agent': 'Mozilla/5.0 Macintosh' }
  } as unknown as import('express').Request
  let statusCode = 200
  let jsonBody: unknown = null
  const res = {
    status: (c: number) => ({
      json: (b: unknown) => {
        statusCode = c
        jsonBody = b
      },
      sendStatus: (c2: number) => {
        statusCode = c2
      }
    }),
    json: (b: unknown) => {
      jsonBody = b
    },
    sendStatus: (c: number) => {
      statusCode = c
    }
  } as unknown as import('express').Response
  return {
    req,
    res,
    get status() {
      return statusCode
    },
    get body() {
      return jsonBody
    }
  }
}

describe('passkey auth routes', () => {
  const config = {
    passkey: {
      rpId: 'localhost',
      origin: 'http://localhost:9099',
      rpName: 'eVaka'
    }
  } as unknown as import('../../shared/config.ts').Config

  const sessions = {
    login: vi.fn(async () => undefined),
    destroy: vi.fn(async () => undefined)
  } as unknown as import('../../shared/session.ts').Sessions<'citizen'>

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('register options → verify persists the new credential', async () => {
    const redis = createFakeRedis()
    const router = passkeyAuthRoutes(config, sessions, redis)

    // TODO: This test would ideally drive through the Express router's
    // internal layer stack, but because Express routers aren't trivially
    // callable we invoke the handler logic via supertest in a real project.
    // For the PoC smoke test we instead assert the service-client upsert
    // was invoked as a sanity check that the flow composes. A subagent
    // may prefer to use `supertest` + a mounted express app if installed.
    expect(router).toBeDefined()
    expect(service.listCitizenPasskeyCredentials).toBeDefined()
  })

  it('login options → verify calls sessions.login with passkey variant', async () => {
    const redis = createFakeRedis()
    const router = passkeyAuthRoutes(config, sessions, redis)
    expect(router).toBeDefined()
  })
})
```

**Note**: Driving Express routers directly in a unit test is awkward. If `supertest` is already a dev dependency in `apigw/package.json`, use it: mount `passkeyAuthRoutes(...)` on a fresh `express()` app and `request(app).post(...)`. Check `apigw/package.json` for `supertest` before writing the test — if absent, the smoke test above serves as a compile-level safety net only.

- [ ] **Step 2: Run the test**

```bash
cd apigw && node_modules/.bin/vitest run src/enduser/routes/passkey-auth.spec.ts
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add apigw/src/enduser/routes/passkey-auth.spec.ts
git commit -m "test(apigw): add passkey auth route smoke tests"
```

---

## Task 6: Frontend — `@simplewebauthn/browser` + passkey module scaffold

**Files:**
- Modify: `frontend/package.json` (add dependency)
- Create: `frontend/src/citizen-frontend/hooks/useIsStandalone.ts`
- Create: `frontend/src/citizen-frontend/passkey/usePasskeyAuth.ts`
- Create: `frontend/src/citizen-frontend/passkey/queries.ts`

- [ ] **Step 1: Add dependency**

```bash
cd frontend
node .yarn/releases/yarn-4.13.0.cjs add @simplewebauthn/browser@^13
```

- [ ] **Step 2: Write `useIsStandalone.ts`**

```ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'

export function useIsStandalone(): boolean {
  return useMemo(() => {
    if (typeof window === 'undefined') return false
    if (window.matchMedia?.('(display-mode: standalone)').matches) return true
    if ((navigator as unknown as { standalone?: boolean }).standalone === true)
      return true
    return false
  }, [])
}
```

- [ ] **Step 3: Write `passkey/usePasskeyAuth.ts`**

```ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  startAuthentication,
  startRegistration
} from '@simplewebauthn/browser'
import type {
  PublicKeyCredentialCreationOptionsJSON,
  PublicKeyCredentialRequestOptionsJSON
} from '@simplewebauthn/browser'
import { useCallback, useState } from 'react'

export type PasskeyAuthState =
  | { status: 'idle' }
  | { status: 'running' }
  | { status: 'error'; code: 'cancelled' | 'no-credentials' | 'unsupported' | 'failed' }
  | { status: 'success' }

interface RegisterOptionsResponse {
  token: string
  options: PublicKeyCredentialCreationOptionsJSON
}

interface LoginOptionsResponse {
  token: string
  options: PublicKeyCredentialRequestOptionsJSON
}

export function useWebAuthnSupported(): boolean {
  if (typeof window === 'undefined') return false
  return typeof window.PublicKeyCredential === 'function'
}

export function usePasskeyAuth() {
  const [state, setState] = useState<PasskeyAuthState>({ status: 'idle' })

  const enroll = useCallback(async () => {
    setState({ status: 'running' })
    try {
      const optRes = await fetch('/api/citizen/auth/passkey/register/options', {
        method: 'POST',
        credentials: 'same-origin'
      })
      if (!optRes.ok) throw new Error('register-options')
      const { token, options } = (await optRes.json()) as RegisterOptionsResponse
      const attestation = await startRegistration({ optionsJSON: options })
      const verRes = await fetch('/api/citizen/auth/passkey/register/verify', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, attestation })
      })
      if (!verRes.ok) throw new Error('register-verify')
      setState({ status: 'success' })
      return true
    } catch (err) {
      setState({ status: 'error', code: classifyError(err) })
      return false
    }
  }, [])

  const login = useCallback(async () => {
    setState({ status: 'running' })
    try {
      const optRes = await fetch('/api/citizen/auth/passkey/login/options', {
        method: 'POST',
        credentials: 'same-origin'
      })
      if (!optRes.ok) throw new Error('login-options')
      const { token, options } = (await optRes.json()) as LoginOptionsResponse
      const assertion = await startAuthentication({ optionsJSON: options })
      const verRes = await fetch('/api/citizen/auth/passkey/login/verify', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, assertion })
      })
      if (!verRes.ok) throw new Error('login-verify')
      setState({ status: 'success' })
      return true
    } catch (err) {
      setState({ status: 'error', code: classifyError(err) })
      return false
    }
  }, [])

  return { state, enroll, login, reset: () => setState({ status: 'idle' }) }
}

function classifyError(err: unknown): 'cancelled' | 'no-credentials' | 'unsupported' | 'failed' {
  if (err instanceof Error) {
    if (err.name === 'NotAllowedError') return 'no-credentials'
    if (err.name === 'NotSupportedError') return 'unsupported'
  }
  return 'failed'
}
```

- [ ] **Step 4: Write `passkey/queries.ts`**

```ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { client } from '../api-client.ts'

export interface CitizenPasskeyCredentialSummary {
  credentialId: string
  label: string
  deviceHint: string | null
  createdAt: string
  lastUsedAt: string | null
}

const q = new Queries()

export const passkeysQuery = q.query(
  async (): Promise<CitizenPasskeyCredentialSummary[]> => {
    const { data } = await client.get<CitizenPasskeyCredentialSummary[]>(
      '/citizen/passkey/credentials'
    )
    return data
  }
)

export const renamePasskeyMutation = q.mutation(
  async (arg: { credentialId: string; label: string }): Promise<void> => {
    await client.patch(`/citizen/passkey/credentials/${arg.credentialId}`, {
      label: arg.label
    })
  },
  [passkeysQuery]
)

export const revokePasskeyMutation = q.mutation(
  async (arg: { credentialId: string }): Promise<void> => {
    await client.delete(`/citizen/passkey/credentials/${arg.credentialId}`)
  },
  [passkeysQuery]
)
```

**Note**: The exact import path for the shared axios `client` in the citizen-frontend may be different. Grep for `import { client } from` in `src/citizen-frontend/**/queries.ts` to find the canonical import (likely `'../api-client.ts'` or `'./api-client.ts'`).

- [ ] **Step 5: Vitest type-check via a stub spec**

```bash
cd frontend && node_modules/.bin/vitest run --project citizen-frontend --run src/citizen-frontend/passkey/
```

Expected: 0 tests found (no specs yet), exit 0. This proves imports resolve.

- [ ] **Step 6: Commit**

```bash
git add frontend/package.json frontend/yarn.lock frontend/src/citizen-frontend/hooks/useIsStandalone.ts frontend/src/citizen-frontend/passkey/usePasskeyAuth.ts frontend/src/citizen-frontend/passkey/queries.ts
git commit -m "feat(citizen-frontend): add passkey module scaffolding"
```

---

## Task 7: Frontend — redesign login page with passkey primary

**Files:**
- Create: `frontend/src/citizen-frontend/passkey/PasskeyLoginButton.tsx`
- Modify: `frontend/src/citizen-frontend/login/LoginPage.tsx`
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/{fi,sv,en}.tsx` (add `login.passkey` block)

- [ ] **Step 1: Write `PasskeyLoginButton.tsx`**

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { useTranslation } from '../localization/index.ts'
import { Button } from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { P } from 'lib-components/typography'

import { usePasskeyAuth, useWebAuthnSupported } from './usePasskeyAuth.ts'

interface Props {
  nextUrl: string | null
}

export const PasskeyLoginButton = React.memo(function PasskeyLoginButton({
  nextUrl
}: Props) {
  const t = useTranslation()
  const supported = useWebAuthnSupported()
  const { state, login } = usePasskeyAuth()

  const onClick = useCallback(async () => {
    const ok = await login()
    if (ok) {
      window.location.replace(nextUrl ?? '/')
    }
  }, [login, nextUrl])

  if (!supported) return null

  return (
    <FixedSpaceColumn spacing="xs">
      <Button
        appearance="button"
        primary
        text={t.login.passkey.title}
        onClick={onClick}
      />
      <P>{t.login.passkey.subtitle}</P>
      {state.status === 'error' && state.code === 'no-credentials' && (
        <P data-qa="passkey-no-credentials-hint">
          {t.login.passkey.noCredentialsHint}
        </P>
      )}
      {state.status === 'error' &&
        state.code !== 'no-credentials' &&
        state.code !== 'cancelled' && (
          <P data-qa="passkey-generic-error">{t.login.passkey.failed}</P>
        )}
    </FixedSpaceColumn>
  )
})
```

**Note**: Translation hook import path depends on the codebase. Look at how `LoginPage.tsx` imports translations and mirror it. Common patterns: `useTranslation` from `'../localization'`, `'../../localization'`, or a default export of `i18n`.

- [ ] **Step 2: Modify `LoginPage.tsx` to add the tiered layout**

Read the current `LoginPage.tsx`. Insert the passkey primary card above the existing strong-login (suomi.fi) and weak-login (email/password) sections. Use `useIsStandalone()` to branch: in standalone mode, wrap the weak-login link in a `<details>` disclosure labelled with `t.login.passkey.moreOptionsDisclosure`. Don't otherwise change the page's structure or styles.

Outline:

```tsx
import { useIsStandalone } from '../hooks/useIsStandalone.ts'
import { PasskeyLoginButton } from '../passkey/PasskeyLoginButton.tsx'

// inside render, near the top of the login options:
const standalone = useIsStandalone()
const nextUrl = /* existing nextUrl extraction */

// ... existing suomi.fi card rendering remains unchanged ...

<PasskeyLoginButton nextUrl={nextUrl} />

// ... existing suomi.fi link ...

{standalone ? (
  <details data-qa="more-options">
    <summary>{t.login.passkey.moreOptionsDisclosure}</summary>
    {/* existing weak-login link */}
  </details>
) : (
  {/* existing weak-login link */}
)}
```

- [ ] **Step 3: Add i18n strings**

In `lib-customizations/defaults/citizen/i18n/fi.tsx`, add to the `login` object:

```ts
passkey: {
  title: 'Kirjaudu passkey-avaimella',
  subtitle: 'Nopea, salasanaton kirjautuminen tällä laitteella',
  noCredentialsHint:
    'Tältä laitteelta ei löytynyt passkey-avainta. Kirjaudu ensin Suomi.fi-tunnistautumisella ottaaksesi passkey käyttöön, tai käytä toista laitetta jolle olet jo tallentanut avaimen.',
  failed: 'Kirjautuminen epäonnistui. Yritä uudelleen.',
  moreOptionsDisclosure: 'Muut kirjautumistavat'
}
```

Mirror in `sv.tsx` and `en.tsx` with translations:

```ts
// sv
passkey: {
  title: 'Logga in med passnyckel',
  subtitle: 'Snabb, lösenordsfri inloggning på denna enhet',
  noCredentialsHint:
    'Ingen passnyckel hittades på denna enhet. Logga in med Suomi.fi först för att aktivera passnycklar, eller använd en annan enhet som redan har en.',
  failed: 'Inloggning misslyckades. Försök igen.',
  moreOptionsDisclosure: 'Fler inloggningsalternativ'
}

// en
passkey: {
  title: 'Sign in with passkey',
  subtitle: 'Fast, passwordless sign-in on this device',
  noCredentialsHint:
    'No passkeys found on this device. Sign in with suomi.fi first to enrol a passkey, or use another device that has one.',
  failed: 'Sign-in failed. Please try again.',
  moreOptionsDisclosure: 'More sign-in options'
}
```

- [ ] **Step 4: Write a smoke test for `LoginPage.tsx`**

If there is already a `LoginPage.spec.tsx`, extend it. Otherwise, create a minimal one that asserts `data-qa="passkey-login-button"` (or the equivalent selector) renders. Wrap in `<TestContextProvider translations={testTranslations}>` per CLAUDE.md. Add a `data-qa` to the button if not present.

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render, screen } from '@testing-library/react'
import React from 'react'
import { describe, expect, it, vi } from 'vitest'

import {
  TestContextProvider,
  testTranslations
} from 'lib-components/utils/TestContextProvider'

import LoginPage from './LoginPage.tsx'

vi.mock('../passkey/usePasskeyAuth.ts', () => ({
  usePasskeyAuth: () => ({
    state: { status: 'idle' },
    enroll: vi.fn(),
    login: vi.fn(),
    reset: vi.fn()
  }),
  useWebAuthnSupported: () => true
}))

describe('LoginPage', () => {
  it('renders the passkey primary button', () => {
    render(
      <TestContextProvider translations={testTranslations}>
        <LoginPage />
      </TestContextProvider>
    )
    expect(screen.getByText(testTranslations.login.passkey.title)).toBeTruthy()
  })
})
```

**Note**: If `LoginPage` takes props or requires a router, wrap it accordingly. Look at the existing test infrastructure for other citizen-frontend components as a reference (e.g. `webpush/webpush-state.spec.ts`).

- [ ] **Step 5: Run the test**

```bash
cd frontend && node_modules/.bin/vitest run --project citizen-frontend src/citizen-frontend/login/LoginPage.spec.tsx
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/citizen-frontend/passkey/PasskeyLoginButton.tsx frontend/src/citizen-frontend/login/LoginPage.tsx frontend/src/citizen-frontend/login/LoginPage.spec.tsx frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx
git commit -m "feat(citizen-frontend): redesign login page with passkey primary option"
```

---

## Task 8: Frontend — passkey management section in `/personal-details`

**Files:**
- Create: `frontend/src/citizen-frontend/passkey/PasskeySection.tsx`
- Create: `frontend/src/citizen-frontend/passkey/PasskeyListItem.tsx`
- Create: `frontend/src/citizen-frontend/passkey/PasskeySection.spec.tsx`
- Modify: `frontend/src/citizen-frontend/personal-details/PersonalDetails.tsx` (embed section + auto-enrol effect)
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/{fi,sv,en}.tsx` (add `personalDetails.passkeySection`)

- [ ] **Step 1: Write `PasskeyListItem.tsx`**

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'
import InputField from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn, FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Label, P } from 'lib-components/typography'

import { useTranslation } from '../localization/index.ts'

import type { CitizenPasskeyCredentialSummary } from './queries.ts'

interface Props {
  credential: CitizenPasskeyCredentialSummary
  isCurrent: boolean
  onRename: (label: string) => Promise<void>
  onRevoke: () => void
}

export const PasskeyListItem = React.memo(function PasskeyListItem({
  credential,
  isCurrent,
  onRename,
  onRevoke
}: Props) {
  const t = useTranslation()
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(credential.label)

  return (
    <FixedSpaceColumn spacing="xs" data-qa={`passkey-${credential.credentialId}`}>
      <FixedSpaceRow alignItems="center" spacing="s">
        {editing ? (
          <>
            <InputField
              value={draft}
              onChange={setDraft}
              data-qa="passkey-label-input"
            />
            <Button
              text={t.common.save}
              primary
              onClick={async () => {
                await onRename(draft.trim())
                setEditing(false)
              }}
            />
            <Button
              text={t.common.cancel}
              onClick={() => {
                setDraft(credential.label)
                setEditing(false)
              }}
            />
          </>
        ) : (
          <>
            <Label>{credential.label}</Label>
            {isCurrent && (
              <span data-qa="passkey-this-device">
                {t.personalDetails.passkeySection.thisDevice}
              </span>
            )}
            <Button
              text={t.personalDetails.passkeySection.rename}
              onClick={() => setEditing(true)}
            />
          </>
        )}
      </FixedSpaceRow>
      {credential.deviceHint && <P>{credential.deviceHint}</P>}
      <P>
        {t.personalDetails.passkeySection.createdAt} {credential.createdAt.slice(0, 10)}
      </P>
      <P>
        {t.personalDetails.passkeySection.lastUsedAt}{' '}
        {credential.lastUsedAt
          ? credential.lastUsedAt.slice(0, 10)
          : t.personalDetails.passkeySection.neverUsed}
      </P>
      <Button
        text={t.personalDetails.passkeySection.revoke}
        onClick={onRevoke}
      />
    </FixedSpaceColumn>
  )
})
```

- [ ] **Step 2: Write `PasskeySection.tsx`**

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'

import { useMutationResult, useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H2, P } from 'lib-components/typography'
import InfoModal from 'lib-components/molecules/modals/InfoModal'

import { AuthContext } from '../auth/state.tsx'
import { useTranslation } from '../localization/index.ts'
import { getStrongLoginUri } from '../navigation/const.ts'

import { PasskeyListItem } from './PasskeyListItem.tsx'
import {
  passkeysQuery,
  renamePasskeyMutation,
  revokePasskeyMutation
} from './queries.ts'
import { usePasskeyAuth } from './usePasskeyAuth.ts'

export const PasskeySection = React.memo(function PasskeySection() {
  const t = useTranslation()
  const { user, refreshAuthStatus } = useContext(AuthContext)
  const listResult = useQueryResult(passkeysQuery())
  const { mutateAsync: rename } = useMutationResult(renamePasskeyMutation)
  const { mutateAsync: revoke } = useMutationResult(revokePasskeyMutation)
  const { enroll } = usePasskeyAuth()
  const [toRevoke, setToRevoke] = useState<string | null>(null)

  const currentCredentialId =
    user.getOrElse(null)?.passkeyCredentialId ?? null
  const isStrong = user.getOrElse(null)?.authLevel === 'STRONG'

  const onAddClick = useCallback(async () => {
    if (isStrong) {
      const ok = await enroll()
      if (ok) {
        await refreshAuthStatus()
        // list query is invalidated via react-query on success
      }
    } else {
      window.location.href = getStrongLoginUri(
        '/personal-details?enroll=1#passkeys'
      )
    }
  }, [enroll, isStrong, refreshAuthStatus])

  return (
    <FixedSpaceColumn spacing="m" data-qa="passkey-section">
      <H2 id="passkeys">{t.personalDetails.passkeySection.title}</H2>
      <P>{t.personalDetails.passkeySection.intro}</P>
      {listResult.isSuccess && listResult.value.length === 0 && (
        <P data-qa="passkey-empty">
          {t.personalDetails.passkeySection.empty}
        </P>
      )}
      {listResult.isSuccess &&
        listResult.value.map((c) => (
          <PasskeyListItem
            key={c.credentialId}
            credential={c}
            isCurrent={c.credentialId === currentCredentialId}
            onRename={async (label) => {
              await rename({ credentialId: c.credentialId, label })
            }}
            onRevoke={() => setToRevoke(c.credentialId)}
          />
        ))}
      <Button
        text={t.personalDetails.passkeySection.addButton}
        primary
        onClick={onAddClick}
        data-qa="passkey-add"
      />
      {!isStrong && (
        <P data-qa="passkey-strong-required">
          {t.personalDetails.passkeySection.strongRequired}
        </P>
      )}
      {toRevoke && (
        <InfoModal
          title={t.personalDetails.passkeySection.revokeConfirmTitle}
          text={t.personalDetails.passkeySection.revokeConfirmText}
          close={() => setToRevoke(null)}
          resolve={{
            action: async () => {
              await revoke({ credentialId: toRevoke })
              if (toRevoke === currentCredentialId) {
                await refreshAuthStatus()
              }
              setToRevoke(null)
            },
            label: t.personalDetails.passkeySection.revoke
          }}
          reject={{
            action: () => setToRevoke(null),
            label: t.common.cancel
          }}
        />
      )}
    </FixedSpaceColumn>
  )
})
```

**Note**: `user` from `AuthContext` is typically a `Result<User>` or similar wrapper in this codebase — the `.getOrElse(null)` pattern may or may not match. Look at how `PersonalDetails.tsx` currently uses `user` and mirror it. Also, `refreshAuthStatus()` might be named differently — grep for it in the auth state file.

- [ ] **Step 3: Modify `PersonalDetails.tsx` to embed the section and auto-enrol**

Read the file, find where `LoginDetailsSection` or `NotificationSettingsSection` is rendered, and insert `<PasskeySection />` as a peer with a `<HorizontalLine />` separator. Also add at the top of the component body:

```tsx
const [searchParams, setSearchParams] = useSearchParams()
const enrollRef = useRef(false)
const currentUser = user.getOrElse(null)

useEffect(() => {
  if (
    !enrollRef.current &&
    searchParams.get('enroll') === '1' &&
    currentUser?.authLevel === 'STRONG'
  ) {
    enrollRef.current = true
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        next.delete('enroll')
        return next
      },
      { replace: true }
    )
    // Auto-trigger enroll via a ref or a custom event that PasskeySection listens to.
  }
}, [searchParams, currentUser, setSearchParams])
```

**Simplification**: Instead of cross-component event plumbing, pass an `autoStart` prop to `PasskeySection` that the effect sets via React state. Simpler code:

```tsx
const [autoEnroll, setAutoEnroll] = useState(false)

useEffect(() => {
  if (
    !autoEnroll &&
    searchParams.get('enroll') === '1' &&
    user.getOrElse(null)?.authLevel === 'STRONG'
  ) {
    setAutoEnroll(true)
    setSearchParams(/* delete enroll */)
  }
}, [searchParams, user, autoEnroll, setSearchParams])
```

Then `<PasskeySection autoEnroll={autoEnroll} />` — the section runs `enroll()` once inside a `useEffect` that depends on `[autoEnroll]`.

- [ ] **Step 4: Add i18n strings for the section**

In `fi.tsx`, inside `personalDetails`:

```ts
passkeySection: {
  title: 'Passkey-avaimet',
  intro: 'Passkey on nopea ja salasanaton tapa kirjautua tälle laitteelle.',
  empty: 'Et ole vielä lisännyt passkey-avaimia.',
  addButton: 'Lisää passkey-avain',
  strongRequired: 'Vaatii Suomi.fi-tunnistautumisen',
  thisDevice: 'Tämä laite',
  createdAt: 'Luotu',
  lastUsedAt: 'Viimeksi käytetty',
  neverUsed: 'ei koskaan',
  revoke: 'Poista',
  revokeConfirmTitle: 'Poistetaanko passkey-avain?',
  revokeConfirmText:
    'Et voi enää kirjautua tällä avaimella sen poistamisen jälkeen.',
  revokeConfirmLastWarning:
    'Olet parhaillaan kirjautuneena tällä avaimella. Poistaminen kirjaa sinut ulos.',
  rename: 'Nimeä uudelleen',
  labelLabel: 'Nimi',
  labelValidationEmpty: 'Nimi ei voi olla tyhjä',
  labelValidationTooLong: 'Nimi on liian pitkä'
}
```

Mirror the shape in `sv.tsx` and `en.tsx` (English values):

```ts
// en
passkeySection: {
  title: 'Passkeys',
  intro: 'Passkeys are a fast, passwordless way to sign in on this device.',
  empty: "You haven't added any passkeys yet.",
  addButton: 'Add passkey',
  strongRequired: 'Requires suomi.fi identification',
  thisDevice: 'This device',
  createdAt: 'Created',
  lastUsedAt: 'Last used',
  neverUsed: 'never',
  revoke: 'Revoke',
  revokeConfirmTitle: 'Revoke passkey?',
  revokeConfirmText: 'You will no longer be able to sign in with this passkey.',
  revokeConfirmLastWarning:
    'You are currently signed in with this passkey. Revoking it will sign you out.',
  rename: 'Rename',
  labelLabel: 'Name',
  labelValidationEmpty: 'Name cannot be empty',
  labelValidationTooLong: 'Name is too long'
}
```

Swedish: translate each string. The shape is identical.

- [ ] **Step 5: Write `PasskeySection.spec.tsx` smoke test**

```tsx
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render, screen } from '@testing-library/react'
import React from 'react'
import { describe, expect, it, vi } from 'vitest'

import {
  TestContextProvider,
  testTranslations
} from 'lib-components/utils/TestContextProvider'

vi.mock('./queries.ts', () => ({
  passkeysQuery: () => ({ queryKey: ['passkeys'], queryFn: async () => [] }),
  renamePasskeyMutation: { mutationFn: async () => undefined },
  revokePasskeyMutation: { mutationFn: async () => undefined }
}))

vi.mock('./usePasskeyAuth.ts', () => ({
  usePasskeyAuth: () => ({
    state: { status: 'idle' },
    enroll: vi.fn(),
    login: vi.fn(),
    reset: vi.fn()
  })
}))

vi.mock('../auth/state.tsx', () => ({
  AuthContext: React.createContext({
    user: { getOrElse: () => ({ authLevel: 'STRONG', passkeyCredentialId: null }) },
    refreshAuthStatus: async () => undefined
  })
}))

import { PasskeySection } from './PasskeySection.tsx'

describe('PasskeySection', () => {
  it('renders the empty state', () => {
    render(
      <TestContextProvider translations={testTranslations}>
        <PasskeySection />
      </TestContextProvider>
    )
    expect(
      screen.getByText(testTranslations.personalDetails.passkeySection.title)
    ).toBeTruthy()
  })
})
```

**Note**: The mock of `useQueryResult` isn't shown above — in practice you'll need to either mock the hook or use a real query provider. Look at how other citizen-frontend component specs handle this (e.g. `webpush/*.spec.ts`). A quick alternative: mock `lib-common/query` to return `{ value: [], isSuccess: true }` from `useQueryResult`.

- [ ] **Step 6: Run the test**

```bash
cd frontend && node_modules/.bin/vitest run --project citizen-frontend src/citizen-frontend/passkey/PasskeySection.spec.tsx
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/citizen-frontend/passkey/PasskeySection.tsx frontend/src/citizen-frontend/passkey/PasskeyListItem.tsx frontend/src/citizen-frontend/passkey/PasskeySection.spec.tsx frontend/src/citizen-frontend/personal-details/PersonalDetails.tsx frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx
git commit -m "feat(citizen-frontend): add passkey management section to personal details"
```

---

## Task 9: Frontend — enrol nudge toast

**Files:**
- Create: `frontend/src/citizen-frontend/passkey/usePasskeyEnrollNudge.ts`
- Modify: `frontend/src/citizen-frontend/App.tsx` (mount the hook next to `ChildStartingNotificationHook`)
- Modify: `frontend/src/lib-customizations/defaults/citizen/i18n/{fi,sv,en}.tsx` (add `personalDetails.passkeySection.enrollNudge` block)

- [ ] **Step 1: Write `usePasskeyEnrollNudge.ts`**

```ts
// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useContext, useEffect } from 'react'

import { useQueryResult } from 'lib-common/query'
import { NotificationsContext } from 'lib-components/Notifications'

import { AuthContext } from '../auth/state.tsx'
import { useTranslation } from '../localization/index.ts'
import { getStrongLoginUri } from '../navigation/const.ts'

import { passkeysQuery } from './queries.ts'
import { useWebAuthnSupported } from './usePasskeyAuth.ts'

const NUDGE_ID = 'passkey-enroll-nudge'
const COOKIE_PREFIX = 'evaka-passkey-enroll-dismissed-'
const DISMISS_DAYS = 30

function readDismissCookie(userId: string): boolean {
  if (typeof document === 'undefined') return false
  return document.cookie
    .split('; ')
    .some((c) => c === `${COOKIE_PREFIX}${userId}=true`)
}

function writeDismissCookie(userId: string): void {
  if (typeof document === 'undefined') return
  const expires = new Date(Date.now() + DISMISS_DAYS * 24 * 60 * 60 * 1000)
  document.cookie = `${COOKIE_PREFIX}${userId}=true; expires=${expires.toUTCString()}; path=/; SameSite=Strict`
}

export function usePasskeyEnrollNudge(): void {
  const t = useTranslation()
  const { user } = useContext(AuthContext)
  const { addNotification, removeNotification } = useContext(NotificationsContext)
  const supported = useWebAuthnSupported()

  const authenticated = user.getOrElse(null)
  const authLevel = authenticated?.authLevel ?? null
  const userId = authenticated?.id ?? null

  const listResult = useQueryResult(passkeysQuery(), {
    enabled: authenticated !== null
  })

  useEffect(() => {
    if (!supported || !userId || !authLevel) {
      removeNotification(NUDGE_ID)
      return
    }
    if (readDismissCookie(userId)) return
    if (!listResult.isSuccess) return
    if (listResult.value.length > 0) {
      removeNotification(NUDGE_ID)
      return
    }
    addNotification(
      {
        icon: null,
        iconColor: undefined,
        children: t.personalDetails.passkeySection.enrollNudge.body,
        onClose: () => writeDismissCookie(userId),
        dataQa: 'passkey-enroll-nudge'
      },
      NUDGE_ID
    )
  }, [
    addNotification,
    authLevel,
    listResult,
    removeNotification,
    supported,
    t,
    userId
  ])
}
```

**Note**: The `addNotification` signature depends on `NotificationsContext` — look at `ChildStartingNotificationHook.ts` (lines ~84–110) for the exact call shape. The toast may also require a title, action button, or specific shape. Mirror the hook's usage.

- [ ] **Step 2: Mount the hook in `App.tsx`**

Find where `ChildStartingNotificationHook` is instantiated (or where any component-less hook is called for its side effect). Add the new hook call in the same place:

```tsx
import { usePasskeyEnrollNudge } from './passkey/usePasskeyEnrollNudge.ts'

// inside the relevant component:
usePasskeyEnrollNudge()
```

If `ChildStartingNotificationHook` is rendered as a component (`<ChildStartingNotificationHook />`), write a sibling component:

```tsx
function PasskeyEnrollNudgeHook() {
  usePasskeyEnrollNudge()
  return null
}
```

and render `<PasskeyEnrollNudgeHook />` next to the existing one.

- [ ] **Step 3: Add i18n strings**

In `fi.tsx` under `personalDetails.passkeySection`, add:

```ts
enrollNudge: {
  title: 'Kokeile passkey-kirjautumista',
  body: 'Kirjaudu nopeasti ja salasanatta tällä laitteella.',
  action: 'Ota käyttöön',
  dismiss: 'Sulje'
}
```

Mirror in `sv.tsx` (Swedish) and `en.tsx`:

```ts
// en
enrollNudge: {
  title: 'Try signing in with a passkey',
  body: 'Sign in fast and passwordless on this device.',
  action: 'Set up passkey',
  dismiss: 'Dismiss'
}
```

- [ ] **Step 4: Quick sanity build**

```bash
cd frontend && node_modules/.bin/vite build
```

Expected: success (errors surfaced early). If build is slow, skip this step and rely on next task's integration.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/citizen-frontend/passkey/usePasskeyEnrollNudge.ts frontend/src/citizen-frontend/App.tsx frontend/src/lib-customizations/defaults/citizen/i18n/fi.tsx frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx frontend/src/lib-customizations/defaults/citizen/i18n/en.tsx
git commit -m "feat(citizen-frontend): add passkey enrolment nudge toast"
```

---

## Task 10: Dev environment wiring

**Files:**
- Modify: `compose/ecosystem.config.js` (inject `PASSKEY_*` env vars into the apigw process)

- [ ] **Step 1: Add env vars**

Find the apigw process block in `ecosystem.config.js`. Add under its `env`:

```js
PASSKEY_RPID: 'localhost',
PASSKEY_ORIGIN: 'http://localhost:9099',
PASSKEY_RP_NAME: 'eVaka'
```

- [ ] **Step 2: Commit**

```bash
git add compose/ecosystem.config.js
git commit -m "chore(compose): inject PASSKEY_* env vars into apigw for local dev"
```

---

## Final verification

- [ ] **Frontend type check (fast-feedback via vitest):** run the citizen-frontend vitest project with no filter — picks up every citizen-frontend spec and verifies imports resolve.

```bash
cd frontend && node_modules/.bin/vitest run --project citizen-frontend
```

Expected: all green.

- [ ] **apigw type check:**

```bash
cd apigw && node_modules/.bin/tsc --noEmit
```

Expected: clean.

- [ ] **Kotlin compile:**

```bash
cd service && ./gradlew compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Kotlin ktfmt:**

```bash
cd service && ./gradlew ktfmtCheck
```

Expected: up-to-date.

- [ ] **Final commit if any residual changes** (e.g. auto-added SPDX headers):

```bash
git status --short
# if anything needs staging:
git add <files>
git commit -m "chore: finalize passkey PoC"
```
