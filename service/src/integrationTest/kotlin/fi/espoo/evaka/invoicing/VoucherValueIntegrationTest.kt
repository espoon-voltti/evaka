package fi.espoo.evaka.invoicing

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.FinanceBasicsController
import fi.espoo.evaka.invoicing.controller.ServiceNeedOptionVoucherValueRangeWithId
import fi.espoo.evaka.invoicing.service.generator.getVoucherValuesByServiceNeedOption
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.ServiceNeedOptionVoucherValueId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.util.UUID
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

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `should throw when deleting nonexistent voucher value`() {
        assertThrows<NotFound>({
            deleteVoucherValue(
                ServiceNeedOptionVoucherValueId(
                    UUID.fromString("00000000-0000-0000-0000-000000000000")
                )
            )
        })
    }

    @Test
    fun `should throw when deleting voucher value which is not latest`() {
        val voucherValues = getVoucherValues()

        val defaultDaycareVoucherValues =
            (voucherValues[snDefaultDaycare.id])!!.sortedBy { it.voucherValues.range.start }
        val notLatest = defaultDaycareVoucherValues.first()

        assertThrows<BadRequest>({ deleteVoucherValue(notLatest.id) })
    }

    @Test
    fun `should delete the voucher value`() {
        val voucherValuesBefore =
            getVoucherValues()[snDefaultDaycare.id]!!.sortedBy { it.voucherValues.range.start }
        val latest = voucherValuesBefore.last()

        deleteVoucherValue(latest.id)

        val voucherValuesAfter = getVoucherValues()[snDefaultDaycare.id]!!
        assert(voucherValuesAfter.size == voucherValuesBefore.size - 1)
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
        assert(voucherValuesAfter.last().voucherValues.range.end == null)
    }

    fun deleteVoucherValue(id: ServiceNeedOptionVoucherValueId) {
        financeBasicsController.deleteVoucherValue(dbInstance(), financeUser, mockClock, id)
    }

    fun getVoucherValues(): Map<ServiceNeedOptionId, List<ServiceNeedOptionVoucherValueRangeWithId>> =
        dbInstance().connect { dbc -> dbc.read { tx -> tx.getVoucherValuesByServiceNeedOption() } }
}
