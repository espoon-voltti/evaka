// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.FinanceBasicsController
import fi.espoo.evaka.invoicing.controller.ServiceNeedOptionVoucherValueRangeWithId
import fi.espoo.evaka.invoicing.controller.deleteVoucherValue
import fi.espoo.evaka.invoicing.service.generator.ServiceNeedOptionVoucherValueRange
import fi.espoo.evaka.invoicing.service.generator.getVoucherValuesByServiceNeedOption
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.ServiceNeedOptionVoucherValueId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class VoucherValueIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var financeBasicsController: FinanceBasicsController

    val mockClock = MockEvakaClock(2024, 4, 1, 10, 0)

    private val financeUser =
        AuthenticatedUser.Employee(
            id = testDecisionMaker_1.id,
            roles = setOf(UserRole.FINANCE_ADMIN)
        )

    val testVoucherValue =
        ServiceNeedOptionVoucherValueRange(
            snDefaultDaycare.id,
            DateRange(LocalDate.now(), LocalDate.now().plusMonths(2)),
            88000,
            BigDecimal.valueOf(1.0),
            88000,
            136400,
            BigDecimal.valueOf(1.0),
            136400
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `should throw when deleting nonexistent voucher value`() {
        assertThrows<NotFound> {
            deleteVoucherValue(
                ServiceNeedOptionVoucherValueId(
                    UUID.fromString("00000000-0000-0000-0000-000000000000")
                )
            )
        }
    }

    @Test
    fun `should throw when deleting voucher value which is not latest`() {
        val voucherValues = getVoucherValues()

        val defaultDaycareVoucherValues =
            (voucherValues[snDefaultDaycare.id])!!.sortedBy { it.voucherValues.range.start }
        val notLatest = defaultDaycareVoucherValues.first()

        assertThrows<BadRequest> { deleteVoucherValue(notLatest.id) }
    }

    @Test
    fun `should delete the voucher value`() {
        val voucherValuesBefore =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }
        val latest = voucherValuesBefore.last()

        deleteVoucherValue(latest.id)

        val voucherValuesAfter = getVoucherValues()[snDefaultDaycare.id]!!
        assertEquals(voucherValuesBefore.size - 1, voucherValuesAfter.size)
        assert(!voucherValuesAfter.map { it.id }.contains(latest.id))
    }

    @Test
    fun `should reopen the validity range for the now latest voucher value when deleting`() {
        val voucherValuesBefore =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }
        val latest = voucherValuesBefore.last()

        deleteVoucherValue(latest.id)

        val voucherValuesAfter =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }
        assertEquals(
            voucherValuesAfter
                .last()
                .voucherValues.range.end,
            null
        )
    }

    @Test
    fun `should work when deleting the last remaining voucher value`() {
        val voucherValues =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedByDescending {
                it.voucherValues.range.start
            }

        voucherValues.forEach { deleteVoucherValue(it.id) }
    }

    @Test
    fun `should insert new voucher value`() {
        val voucherValuesBefore =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }

        addVoucherValue(testVoucherValue)

        val voucherValuesAfter =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }

        assertEquals(voucherValuesAfter.size, voucherValuesBefore.size + 1)
    }

    @Test
    fun `should close the previously latest voucher value validity`() {
        addVoucherValue(testVoucherValue)

        val voucherValuesAfter =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }

        assertEquals(
            testVoucherValue.range.start.minusDays(1),
            voucherValuesAfter[1].voucherValues.range.end
        )
    }

    @Test
    fun `should throw when invalid service need option ID`() {
        assertThrows<BadRequest> {
            addVoucherValue(
                testVoucherValue.copy(
                    serviceNeedOptionId =
                        ServiceNeedOptionId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                )
            )
        }
    }

    @Test
    fun `should throw when adding a voucher value that starts before the existing latest one`() {
        assertThrows<BadRequest> {
            addVoucherValue(
                testVoucherValue.copy(range = DateRange(LocalDate.of(2024, 1, 1), null))
            )
        }
    }

    @Test
    fun `should work when service need option has no prior voucher values`() {
        db.transaction { tx -> tx.deleteTestVoucherValues() }
        addVoucherValue(testVoucherValue)
    }

    fun `should update an existing voucher value`() {
        val voucherValuesBefore =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }

        val newVoucherValue = voucherValuesBefore[1].voucherValues.copy(value = 89000)

        updateVoucherValue(voucherValuesBefore[1].id, newVoucherValue)

        val voucherValuesAfter =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }

        assertEquals(89000, voucherValuesAfter[1].voucherValues.value)
    }

    @Test
    fun `should update previous voucher value's validity when current validity start is moved later`() {
        val voucherValuesBefore =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }

        val newVoucherValue =
            voucherValuesBefore[1]
                .voucherValues
                .copy(
                    range =
                        DateRange(
                            voucherValuesBefore[1]
                                .voucherValues.range.start
                                .plusWeeks(2),
                            voucherValuesBefore[1].voucherValues.range.end
                        )
                )

        updateVoucherValue(voucherValuesBefore[1].id, newVoucherValue)

        val voucherValuesAfter =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }

        assertEquals(
            newVoucherValue.range.start.minusDays(1),
            voucherValuesAfter[0].voucherValues.range.end
        )
    }

    @Test
    fun `should update previous voucher value's validity when current validity start is moved earlier`() {
        val voucherValuesBefore =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }

        val newVoucherValue =
            voucherValuesBefore[1]
                .voucherValues
                .copy(
                    range =
                        DateRange(
                            voucherValuesBefore[1]
                                .voucherValues.range.start
                                .minusWeeks(2),
                            voucherValuesBefore[1].voucherValues.range.end
                        )
                )

        updateVoucherValue(voucherValuesBefore[1].id, newVoucherValue)

        val voucherValuesAfter =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }

        assertEquals(
            newVoucherValue.range.start.minusDays(1),
            voucherValuesAfter[0].voucherValues.range.end
        )
    }

    @Test
    fun `should work when there is only one voucher value`() {
        val voucherValuesBefore =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }
        db.transaction { tx -> tx.deleteVoucherValue(voucherValuesBefore[1].id) }
        updateVoucherValue(voucherValuesBefore[0].id, voucherValuesBefore[0].voucherValues)
    }

    fun deleteVoucherValue(id: ServiceNeedOptionVoucherValueId) {
        financeBasicsController.deleteVoucherValue(dbInstance(), financeUser, mockClock, id)
    }

    fun addVoucherValue(voucherValue: ServiceNeedOptionVoucherValueRange) {
        financeBasicsController.createVoucherValue(
            dbInstance(),
            financeUser,
            mockClock,
            voucherValue
        )
    }

    fun updateVoucherValue(
        id: ServiceNeedOptionVoucherValueId,
        voucherValue: ServiceNeedOptionVoucherValueRange
    ) {
        financeBasicsController.updateVoucherValue(
            dbInstance(),
            financeUser,
            mockClock,
            id,
            voucherValue
        )
    }

    fun getVoucherValues(): Map<ServiceNeedOptionId, List<ServiceNeedOptionVoucherValueRangeWithId>> =
        dbInstance().connect { dbc -> dbc.read { tx -> tx.getVoucherValuesByServiceNeedOption() } }

    fun Database.Transaction.deleteTestVoucherValues() {
        execute {
            sql(
                """
                DELETE FROM service_need_option_voucher_value
                WHERE service_need_option_id = ${bind(snDefaultDaycare.id)}
                """.trimIndent()
            )
        }
    }
}
