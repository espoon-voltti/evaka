// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.FeeAlterationController
import fi.espoo.evaka.invoicing.data.upsertFeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeeAlterationIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private fun assertEqualEnough(expected: List<FeeAlteration>, actual: List<FeeAlteration>) {
        val nullId = FeeAlterationId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        assertEquals(
            expected.map { it.copy(id = nullId, updatedAt = null) }.toSet(),
            actual.map { it.copy(id = nullId, updatedAt = null) }.toSet()
        )
    }

    private fun deserializeResult(json: String) =
        jsonMapper
            .readValue<List<FeeAlterationController.FeeAlterationWithPermittedActions>>(json)
            .map { it.data }

    @BeforeEach
    fun setup() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    private val user =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))
    private val personId = testChild_1.id
    private val clock = RealEvakaClock()

    private val testFeeAlteration =
        FeeAlteration(
            id = FeeAlterationId(UUID.randomUUID()),
            personId = personId,
            type = FeeAlteration.Type.DISCOUNT,
            amount = 50,
            isAbsolute = false,
            validFrom = LocalDate.of(2019, 1, 1),
            validTo = LocalDate.of(2019, 1, 31),
            notes = "",
            updatedBy = EvakaUserId(testDecisionMaker_1.id.raw)
        )

    @Test
    fun `getFeeAlterations works with no data in DB`() {
        val (_, response, result) =
            http.get("/fee-alterations?personId=$personId").asUser(user).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(listOf(), deserializeResult(result.get()))
    }

    @Test
    fun `getFeeAlterations works with single fee alteration in DB`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, testFeeAlteration) }

        val (_, response, result) =
            http.get("/fee-alterations?personId=$personId").asUser(user).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(listOf(testFeeAlteration), deserializeResult(result.get()))
    }

    @Test
    fun `getFeeAlterations works with multiple fee alterations in DB`() {
        val feeAlterations =
            listOf(
                testFeeAlteration.copy(
                    id = FeeAlterationId(UUID.randomUUID()),
                    validFrom = testFeeAlteration.validFrom.plusYears(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1)
                ),
                testFeeAlteration
            )
        db.transaction { tx -> feeAlterations.forEach { tx.upsertFeeAlteration(clock, it) } }

        val (_, response, result) =
            http.get("/fee-alterations?personId=$personId").asUser(user).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(feeAlterations, deserializeResult(result.get()))
    }

    @Test
    fun `createFeeAlterations works with valid fee alteration`() {
        http
            .post("/fee-alterations?personId=$personId")
            .asUser(user)
            .jsonBody(jsonMapper.writeValueAsString(testFeeAlteration))
            .response()

        val (_, response, result) =
            http.get("/fee-alterations?personId=$personId").asUser(user).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(
            listOf(testFeeAlteration.copy(updatedBy = EvakaUserId(testDecisionMaker_1.id.raw))),
            deserializeResult(result.get())
        )
    }

    @Test
    fun `createFeeAlterations throws with invalid date range`() {
        val feeAlteration =
            testFeeAlteration.copy(validTo = testFeeAlteration.validFrom.minusDays(1))
        val (_, response, _) =
            http
                .post("/fee-alterations?personId=$personId")
                .asUser(user)
                .jsonBody(jsonMapper.writeValueAsString(feeAlteration))
                .response()
        assertEquals(400, response.statusCode)
    }

    @Test
    fun `updateFeeAlterations works with valid fee alteration`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, testFeeAlteration) }

        val updated = testFeeAlteration.copy(amount = 100)
        http
            .put("/fee-alterations/${testFeeAlteration.id}?personId=$personId")
            .asUser(user)
            .jsonBody(jsonMapper.writeValueAsString(updated))
            .response()

        val (_, response, result) =
            http.get("/fee-alterations?personId=$personId").asUser(user).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(
            listOf(updated.copy(updatedBy = EvakaUserId(testDecisionMaker_1.id.raw))),
            deserializeResult(result.get())
        )
    }

    @Test
    fun `updateFeeAlterations throws with invalid date rage`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, testFeeAlteration) }

        val updated = testFeeAlteration.copy(validTo = testFeeAlteration.validFrom.minusDays(1))
        val (_, response, _) =
            http
                .put("/fee-alterations/${testFeeAlteration.id}?personId=$personId")
                .asUser(user)
                .jsonBody(jsonMapper.writeValueAsString(updated))
                .response()
        assertEquals(400, response.statusCode)
    }

    @Test
    fun `delete works with existing fee alteration`() {
        val deletedId = FeeAlterationId(UUID.randomUUID())
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)
            tx.upsertFeeAlteration(clock, testFeeAlteration.copy(id = deletedId))
        }

        http.delete("/fee-alterations/$deletedId").asUser(user).response()

        val (_, response, result) =
            http.get("/fee-alterations?personId=$personId").asUser(user).responseString()
        assertEquals(200, response.statusCode)

        assertEquals(1, deserializeResult(result.get()).size)
        assertFalse(deserializeResult(result.get()).any { it.id == deletedId })
    }

    @Test
    fun `delete does nothing with non-existent id`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, testFeeAlteration) }

        http.delete("/fee-alterations/${UUID.randomUUID()}").asUser(user).response()

        val (_, response, result) =
            http.get("/fee-alterations?personId=$personId").asUser(user).responseString()
        assertEquals(200, response.statusCode)

        assertEqualEnough(listOf(testFeeAlteration), deserializeResult(result.get()))
    }
}
