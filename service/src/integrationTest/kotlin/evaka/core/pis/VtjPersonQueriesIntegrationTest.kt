// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import evaka.core.PureJdbiTest
import evaka.core.daycare.getChild
import evaka.core.identity.Ssn
import evaka.core.pis.service.blockGuardian
import evaka.core.pis.service.getChildGuardians
import evaka.core.pis.service.getGuardianChildIds
import evaka.core.shared.PersonId
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.dto.Nationality
import evaka.core.vtjclient.dto.NativeLanguage
import evaka.core.vtjclient.dto.PersonAddress
import evaka.core.vtjclient.dto.RestrictedDetails
import evaka.core.vtjclient.dto.VtjPerson
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory

class VtjPersonQueriesIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 3, 2), LocalTime.of(12, 0))

    private val vtjPerson =
        VtjPerson(
            firstNames = "Matti Yrjö",
            lastName = "Meikäläinen",
            socialSecurityNumber = "010199-8137",
            residentialAddress =
                PersonAddress("Jokutie 66", "00100", "Helsinki", "Någonvägen 66", "Helsingfors"),
            nativeLanguage = NativeLanguage(languageName = "suomi", code = "fi"),
            nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "246")),
            restrictedDetails = RestrictedDetails(enabled = false),
            residenceCode = "abc123",
            municipalityOfResidence = "Helsinki",
            municipalityOfResidenceSe = "Helsingfors",
        )

    private val logAppender = ListAppender<ILoggingEvent>()
    private val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    @BeforeEach
    fun startLogCapture() {
        logAppender.start()
        rootLogger.addAppender(logAppender)
    }

    @AfterEach
    fun stopLogCapture() {
        rootLogger.detachAppender(logAppender)
        logAppender.stop()
    }

    @Test
    fun `applyPerson creates a row with a message account`() {
        val result = db.transaction { it.applyPerson(vtjPerson, now) }

        assertTrue(result.created)
        assertNull(result.newDateOfDeath)
        val person = db.read { it.getPersonById(result.id) }!!
        assertEquals("010199-8137", person.ssn?.value)
        assertEquals(LocalDate.of(1999, 1, 1), person.dateOfBirth)
        assertEquals("Matti Yrjö", person.firstName)
        assertEquals("Meikäläinen", person.lastName)
        assertEquals("Jokutie 66", person.streetAddress)
        assertEquals("00100", person.postalCode)
        assertEquals("Helsinki", person.postOffice)
        assertEquals("abc123", person.residenceCode)
        assertEquals("Helsinki", person.municipalityOfResidence)
        assertEquals("fi", person.language)
        assertEquals(listOf("246"), person.nationalities)
        assertEquals(now, person.updatedFromVtj)
        assertEquals(1, countMessageAccounts(result.id))
    }

    @Test
    fun `applyPerson uses the Swedish address of a Swedish speaker`() {
        val result = db.transaction {
            it.applyPerson(vtjPerson.copy(nativeLanguage = NativeLanguage(code = "sv")), now)
        }

        val person = db.read { it.getPersonById(result.id) }!!
        assertEquals("Någonvägen 66", person.streetAddress)
        assertEquals("Helsingfors", person.postOffice)
        assertEquals("Helsingfors", person.municipalityOfResidence)
    }

    @Test
    fun `applyPerson uses the Swedish address of a Swedish speaker when it updates an existing row`() {
        val existing = DevPerson(ssn = vtjPerson.socialSecurityNumber)
        db.transaction { it.insert(existing, DevPersonType.ADULT) }

        db.transaction {
            it.applyPerson(vtjPerson.copy(nativeLanguage = NativeLanguage(code = "sv")), now)
        }

        val person = db.read { it.getPersonById(existing.id) }!!
        assertEquals("Någonvägen 66", person.streetAddress)
        assertEquals("Helsingfors", person.postOffice)
        assertEquals("Helsingfors", person.municipalityOfResidence)
    }

    @Test
    fun `setSsnFromVtj does not replace the SSN of a person who already has one`() {
        // Another request may have added an SSN after the caller checked that there was none
        val person = DevPerson(ssn = "070644-937X")
        db.transaction { it.insert(person, DevPersonType.ADULT) }

        assertThrows<Conflict> { db.transaction { it.setSsnFromVtj(person.id, vtjPerson, now) } }

        assertEquals("070644-937X", db.read { it.getPersonById(person.id) }!!.ssn?.value)
    }

    @Test
    fun `setSsnFromVtj uses the Swedish address of a Swedish speaker`() {
        val person = DevPerson(ssn = null)
        db.transaction { it.insert(person, DevPersonType.ADULT) }

        db.transaction {
            it.setSsnFromVtj(
                person.id,
                vtjPerson.copy(nativeLanguage = NativeLanguage(code = "sv")),
                now,
            )
        }

        val updated = db.read { it.getPersonById(person.id) }!!
        assertEquals("Någonvägen 66", updated.streetAddress)
        assertEquals("Helsingfors", updated.postOffice)
        assertEquals("Helsingfors", updated.municipalityOfResidence)
    }

    @Test
    fun `applyPerson uses the Finnish address of a Swedish speaker who has no Swedish address`() {
        val result = db.transaction {
            it.applyPerson(
                vtjPerson.copy(
                    nativeLanguage = NativeLanguage(code = "sv"),
                    residentialAddress = PersonAddress("Jokutie 66", "00100", "Helsinki", "", ""),
                    municipalityOfResidenceSe = "",
                ),
                now,
            )
        }

        val person = db.read { it.getPersonById(result.id) }!!
        assertEquals("Jokutie 66", person.streetAddress)
        assertEquals("Helsinki", person.postOffice)
        assertEquals("Helsinki", person.municipalityOfResidence)
    }

    @Test
    fun `applyPerson updates the existing row by SSN and keeps the fields that VTJ does not own`() {
        val existing =
            DevPerson(
                ssn = vtjPerson.socialSecurityNumber,
                email = "matti@example.com",
                phone = "123",
            )
        db.transaction { it.insert(existing, DevPersonType.ADULT) }

        val result = db.transaction {
            it.applyPerson(vtjPerson.copy(lastName = "Virtanen"), now.plusHours(1))
        }

        assertFalse(result.created)
        assertEquals(existing.id, result.id)
        val person = db.read { it.getPersonById(existing.id) }!!
        assertEquals("Virtanen", person.lastName)
        assertEquals("matti@example.com", person.email)
        assertEquals("123", person.phone)
        assertEquals(now.plusHours(1), person.updatedFromVtj)
    }

    @Test
    fun `applyPerson logs the changed fields`() {
        db.transaction { it.applyPerson(vtjPerson, now) }
        logAppender.list.clear()

        val result = db.transaction {
            it.applyPerson(vtjPerson.copy(lastName = "Virtanen"), now.plusHours(1))
        }

        assertEquals(
            listOf("Updated person ${result.id} from VTJ, changed fields: [lastName]"),
            logAppender.list.map { it.formattedMessage }.filter { it.contains("changed fields") },
        )
    }

    @Test
    fun `applyPerson reports a new date of death only when the stored date of death was empty`() {
        val dateOfDeath = LocalDate.of(2026, 3, 1)
        val dead = vtjPerson.copy(dateOfDeath = dateOfDeath)

        val created = db.transaction { it.applyPerson(dead, now) }
        assertNull(created.newDateOfDeath)

        db.transaction { it.applyPerson(vtjPerson, now) }
        val died = db.transaction { it.applyPerson(dead, now) }
        assertEquals(dateOfDeath, died.newDateOfDeath)

        val stillDead = db.transaction { it.applyPerson(dead, now) }
        assertNull(stillDead.newDateOfDeath)
    }

    @Test
    fun `applyPerson resolves two concurrent creates of the same SSN into one row`() {
        val firstInserted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first =
                executor.submit<ApplyResult> {
                    dbInstance().connect { dbc ->
                        dbc.transaction { tx ->
                            tx.applyPerson(vtjPerson, now).also {
                                firstInserted.countDown()
                                assertTrue(releaseFirst.await(10, TimeUnit.SECONDS))
                            }
                        }
                    }
                }
            assertTrue(firstInserted.await(10, TimeUnit.SECONDS))
            val second =
                executor.submit<ApplyResult> {
                    dbInstance().connect { dbc ->
                        dbc.transaction {
                            it.applyPerson(vtjPerson.copy(lastName = "Virtanen"), now)
                        }
                    }
                }
            waitUntilATransactionWaitsForALock()
            releaseFirst.countDown()

            val firstResult = first.get(10, TimeUnit.SECONDS)
            val secondResult = second.get(10, TimeUnit.SECONDS)
            assertTrue(firstResult.created)
            assertFalse(secondResult.created)
            assertEquals(firstResult.id, secondResult.id)
            assertEquals("Virtanen", db.read { it.getPersonById(firstResult.id) }!!.lastName)
            assertEquals(1, countMessageAccounts(firstResult.id))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `setSsnFromVtj sets the SSN and the VTJ fields of a person who has no SSN`() {
        val person = DevPerson(ssn = null, firstName = "Tuntematon", email = "matti@example.com")
        db.transaction { it.insert(person, DevPersonType.ADULT) }

        val result = db.transaction { it.setSsnFromVtj(person.id, vtjPerson, now) }

        assertEquals(person.id, result.id)
        assertFalse(result.created)
        val updated = db.read { it.getPersonById(person.id) }!!
        assertEquals(Ssn.parse(vtjPerson.socialSecurityNumber), updated.ssn)
        assertEquals("Matti Yrjö", updated.firstName)
        assertEquals("Jokutie 66", updated.streetAddress)
        assertEquals("matti@example.com", updated.email)
        assertEquals(now, updated.updatedFromVtj)
    }

    @Test
    fun `applyGuardians replaces the guardians by difference, skips blocked guardians and sets the marker`() {
        val child = DevPerson(ssn = "050120A910J")
        val removed = DevPerson()
        val kept = DevPerson()
        val added = DevPerson()
        val blocked = DevPerson()
        db.transaction { tx ->
            // the child has a person row but no child row
            tx.insert(child, DevPersonType.RAW_ROW)
            listOf(removed, kept, added, blocked).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(DevGuardian(guardianId = removed.id, childId = child.id))
            tx.insert(DevGuardian(guardianId = kept.id, childId = child.id))
            tx.blockGuardian(childId = child.id, guardianId = blocked.id)
        }

        db.transaction { it.applyGuardians(child.id, setOf(kept.id, added.id, blocked.id), now) }

        assertEquals(setOf(kept.id, added.id), db.read { it.getChildGuardians(child.id) }.toSet())
        assertNotNull(db.read { it.getChild(child.id) })
        assertEquals(now, db.read { it.getPersonById(child.id) }!!.vtjGuardiansQueried)
    }

    @Test
    fun `applyDependants replaces the dependants by difference, skips blocked ones and sets the marker`() {
        val guardian = DevPerson(ssn = "150385-9013")
        val removed = DevPerson()
        val kept = DevPerson()
        val added = DevPerson()
        val blocked = DevPerson()
        db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(removed, DevPersonType.CHILD)
            tx.insert(kept, DevPersonType.CHILD)
            // the new dependant has a person row but no child row
            tx.insert(added, DevPersonType.RAW_ROW)
            tx.insert(blocked, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardian.id, childId = removed.id))
            tx.insert(DevGuardian(guardianId = guardian.id, childId = kept.id))
            tx.blockGuardian(childId = blocked.id, guardianId = guardian.id)
        }

        db.transaction {
            it.applyDependants(guardian.id, setOf(kept.id, added.id, blocked.id), now)
        }

        assertEquals(
            setOf(kept.id, added.id),
            db.read { it.getGuardianChildIds(guardian.id) }.toSet(),
        )
        assertNotNull(db.read { it.getChild(added.id) })
        assertEquals(now, db.read { it.getPersonById(guardian.id) }!!.vtjDependantsQueried)
    }

    @Test
    fun `a failure after applyDependants leaves neither the relation rows nor the marker`() {
        val guardian = DevPerson(ssn = "150385-9013")
        val child = DevPerson()
        db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
        }

        assertThrows<IllegalStateException> {
            db.transaction {
                it.applyDependants(guardian.id, setOf(child.id), now)
                error("failure before commit")
            }
        }

        assertEquals(emptySet(), db.read { it.getGuardianChildIds(guardian.id) }.toSet())
        assertNull(db.read { it.getPersonById(guardian.id) }!!.vtjDependantsQueried)
    }

    private fun countMessageAccounts(personId: PersonId): Int = db.read {
        it.createQuery {
                sql("SELECT count(*) FROM message_account WHERE person_id = ${bind(personId)}")
            }
            .exactlyOne()
    }

    private fun waitUntilATransactionWaitsForALock() {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            val waiting = db.read {
                it.createQuery {
                        sql(
                            """
SELECT EXISTS (
    SELECT FROM pg_stat_activity
    WHERE datname = current_database() AND wait_event_type = 'Lock'
)
"""
                        )
                    }
                    .exactlyOne<Boolean>()
            }
            if (waiting) return
            Thread.sleep(10)
        }
        error("No transaction started to wait for a lock")
    }
}
