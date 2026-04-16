// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenpasskey

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CitizenPasskeyCredentialStoreTest : FullApplicationTest(resetDbBeforeEach = false) {
    @Autowired private lateinit var store: CitizenPasskeyCredentialStore

    private val personId = PersonId(UUID.randomUUID())
    private val now =
        HelsinkiDateTime.from(ZonedDateTime.parse("2026-04-14T10:00:00+03:00[Europe/Helsinki]"))

    private fun credential(id: String, label: String = "My Key") =
        CitizenPasskeyCredential(
            credentialId = id,
            publicKey = "base64-pubkey-$id",
            signCounter = 0L,
            transports = listOf("internal"),
            createdAt = now,
            lastUsedAt = null,
            label = label,
            deviceHint = null,
        )

    @Test
    fun `full round-trip`() {
        val cred1 = credential("cred-1", "Key One")
        val cred2 = credential("cred-2", "Key Two")

        // 1. Upsert first credential — list size 1
        store.upsertCredential(personId, cred1)
        assertEquals(1, store.listCredentials(personId).size)

        // 2. Upsert second credential — list size 2
        store.upsertCredential(personId, cred2)
        assertEquals(2, store.listCredentials(personId).size)

        // 3. Rename first credential — label updated
        val renamed = store.renameCredential(personId, "cred-1", "Renamed Key")
        assertTrue(renamed)
        val afterRename = store.listCredentials(personId).single { it.credentialId == "cred-1" }
        assertEquals("Renamed Key", afterRename.label)

        // 4. Touch first credential with counter 42 — signCounter and lastUsedAt updated
        val laterNow =
            HelsinkiDateTime.from(ZonedDateTime.parse("2026-04-14T11:00:00+03:00[Europe/Helsinki]"))
        val touched = store.touchCredential(personId, "cred-1", 42L, laterNow)
        assertTrue(touched)
        val afterTouch = store.listCredentials(personId).single { it.credentialId == "cred-1" }
        assertEquals(42L, afterTouch.signCounter)
        assertEquals(laterNow, afterTouch.lastUsedAt)

        // 5. Revoke first credential — list size 1
        val revoked1 = store.revokeCredential(personId, "cred-1")
        assertTrue(revoked1)
        assertEquals(1, store.listCredentials(personId).size)

        // 6. Revoke second credential — credentials list empty.
        //    The decisions bucket denies s3:DeleteObject for the service, so
        //    the store logical-deletes by writing an empty-list file instead
        //    of a real DeleteObject (see CitizenPasskeyCredentialStore.delete).
        val revoked2 = store.revokeCredential(personId, "cred-2")
        assertTrue(revoked2)
        val afterFinalRevoke = store.load(personId)
        assertNotNull(afterFinalRevoke)
        assertTrue(afterFinalRevoke.credentials.isEmpty())

        // 7. Revoke on missing credential returns false
        val revokedMissing = store.revokeCredential(personId, "cred-1")
        assertFalse(revokedMissing)
    }
}
