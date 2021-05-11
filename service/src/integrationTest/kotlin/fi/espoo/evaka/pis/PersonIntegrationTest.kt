package fi.espoo.evaka.pis

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.shared.dev.resetDatabase
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PersonIntegrationTest : PureJdbiTest() {
    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.resetDatabase()
        }
    }

    @Test
    fun `creating an empty person creates a message account`() {
        val person = db.transaction { createEmptyPerson(it) }
        Assertions.assertTrue(personHasMessageAccount(person.id))
    }

    @Test
    fun `creating a person creates a message account`() {
        val validSSN = "080512A918W"
        val req = PersonIdentityRequest(
            identity = ExternalIdentifier.SSN.getInstance(validSSN),
            firstName = "Matti",
            lastName = "Meikäläinen",
            email = "matti.meikalainen@example.com",
            language = "fi"
        )
        val person = db.transaction { createPerson(it, req) }

        Assertions.assertTrue(personHasMessageAccount(person.id))
    }

    @Test
    fun `creating a person from request body creates a message account`() {
        val body = CreatePersonBody(
            firstName = "Matti",
            lastName = "Meikäläinen",
            email = "matti.meikalainen@example.com",
            dateOfBirth = LocalDate.of(1920, 1, 1),
            phone = "123456",
            streetAddress = "Testikatu 1",
            postalCode = "12345",
            postOffice = "Espoo"
        )
        val personId = db.transaction { createPerson(it, body) }

        Assertions.assertTrue(personHasMessageAccount(personId))
    }

    private fun personHasMessageAccount(personId: UUID): Boolean {
        // language=SQL
        val sql = """
            SELECT EXISTS(
                SELECT * FROM message_account WHERE person_id = :personId
            )
        """.trimIndent()
        return db.read {
            it.createQuery(sql)
                .bind("personId", personId)
                .mapTo<Boolean>().first()
        }
    }
}
