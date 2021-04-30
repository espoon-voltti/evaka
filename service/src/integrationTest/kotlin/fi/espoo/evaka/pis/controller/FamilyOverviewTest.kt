// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.service.FamilyOverview
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class FamilyOverviewTest : FullApplicationTest() {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { tx ->
            tx.resetDatabase()
        }
    }

    @Test
    fun `handles 1 + 1`() {
        createTestFixture1plus1()
        val result = fetchAndParseFamilyDetails(testAdult_1.id)

        assertEquals(
            1, result.children.size
        )

        assertEquals(
            testAdult_1.id, result.headOfFamily.personId
        )

        assertEquals(
            setOf(testChild_1.id), result.children.map { it.personId }.toSet()
        )
    }

    @Test
    fun `handles 1 + 2`() {
        createTestFixture1plus2()
        val result = fetchAndParseFamilyDetails(testAdult_1.id)

        assertEquals(
            2, result.children.size
        )

        assertEquals(
            testAdult_1.id, result.headOfFamily.personId
        )

        assertEquals(
            setOf(testChild_1.id, testChild_2.id), result.children.map { it.personId }.toSet()
        )
    }

    @Test
    fun `handles 2 + 2`() {
        createTestFixture2plus2()
        val result = fetchAndParseFamilyDetails(testAdult_1.id)

        assertEquals(
            2, result.children.size
        )

        assertEquals(
            testAdult_1.id, result.headOfFamily.personId
        )

        assertEquals(
            testAdult_2.id, result.partner!!.personId
        )

        assertEquals(
            setOf(testChild_1.id, testChild_2.id), result.children.map { it.personId }.toSet()
        )
    }

    @Test
    fun `doesn't explode if children are missing`() {
        val result = fetchAndParseFamilyDetails(testAdult_1.id)

        assertEquals(
            0, result.children.size
        )

        assertEquals(
            testAdult_1.id, result.headOfFamily.personId
        )
    }

    private val testUser = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))

    private fun fetchAndParseFamilyDetails(personId: UUID): FamilyOverview {
        val (_, response, result) = http.get("/family/by-adult/$personId")
            .asUser(testUser)
            .responseString()

        assertEquals(200, response.statusCode)
        return objectMapper.readValue<FamilyOverview>(result.get())
    }

    private fun createTestFixture1plus1() {
        val (from, to) = LocalDate.now().let {
            listOf(it.minusYears(1), it.plusYears(1))
        }
        db.transaction { it.handle.createParentship(testChild_1.id, testAdult_1.id, from, to) }
    }

    private fun createTestFixture1plus2() {
        val (from, to) = LocalDate.now().let {
            listOf(it.minusYears(1), it.plusYears(1))
        }
        db.transaction {
            it.handle.createParentship(testChild_1.id, testAdult_1.id, from, to)
            it.handle.createParentship(testChild_2.id, testAdult_1.id, from, to)
        }
    }

    private fun createTestFixture2plus2() {
        val (from, to) = LocalDate.now().let {
            listOf(it.minusYears(1), it.plusYears(1))
        }
        db.transaction {
            it.handle.createParentship(testChild_1.id, testAdult_1.id, from, to)
            it.handle.createParentship(testChild_2.id, testAdult_1.id, from, to)
            it.handle.createPartnership(testAdult_1.id, testAdult_2.id, from, to)
        }
    }
}
