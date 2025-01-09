// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PasswordBlacklistIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var passwordBlacklist: PasswordBlacklist

    private val directory =
        Path.of(
            this.javaClass.classLoader.getResource("password-blacklist")?.toURI()
                ?: error("Failed to locate password blacklist directory")
        )
    private val blacklistedPassword = Sensitive("Password123!")

    @BeforeEach
    fun beforeEach() {
        val importCount = passwordBlacklist.importBlacklists(db, directory)
        assertEquals(2, importCount)
        assertTrue(db.read { it.isPasswordBlacklisted(blacklistedPassword) })
    }

    @Test
    fun `unchanged files are skipped`() {
        val importCount = passwordBlacklist.importBlacklists(db, directory)
        assertEquals(0, importCount)
    }

    @Test
    fun `the same password can be imported multiple times from different sources`() {
        db.transaction { tx ->
            val source =
                PasswordBlacklistSource(name = "someotherfile", updatedAt = HelsinkiDateTime.now())
            val importCount =
                passwordBlacklist.importPasswords(tx, source, sequenceOf(blacklistedPassword.value))
            assertEquals(1, importCount)
        }
    }
}
