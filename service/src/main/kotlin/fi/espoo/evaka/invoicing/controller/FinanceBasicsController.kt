package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.domain.FeeThresholdsWithValidity
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/finance-basics")
class FinanceBasicsController {
    @GetMapping("/fee-thresholds")
    fun getFeeThresholds(db: Database.Connection, user: AuthenticatedUser): List<FeeThresholdsWithValidity> {
        Audit.FinanceBasicsPricingRead.log()
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)

        return db.read { it.getFeeThresholds().sortedByDescending { it.validDuring.start } }
    }

    @PostMapping("/fee-thresholds")
    fun createFeeThresholds(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: CreateFeeThresholdsBody
    ) {
        Audit.FinanceBasicsPricingRead.log()
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)

        db.transaction {
            val latestThreshold = it.getFeeThresholds().maxByOrNull { it.validDuring.start }

            if (latestThreshold != null) {
                if (latestThreshold.validDuring.end != null && latestThreshold.validDuring.overlaps(body.validDuring))
                    throw BadRequest("New fee thresholds over lap with existing fee thresholds")

                if (latestThreshold.validDuring.end == null)
                    it.updateFeeThresholdsValidity(
                        latestThreshold.id,
                        latestThreshold.validDuring.copy(end = body.validDuring.start.minusDays(1))
                    )
            }

            it.insertNewFeeThresholds(body)
        }
    }
}

data class CreateFeeThresholdsBody(
    val validDuring: DateRange,
    val minIncomeThreshold2: Int,
    val minIncomeThreshold3: Int,
    val minIncomeThreshold4: Int,
    val minIncomeThreshold5: Int,
    val minIncomeThreshold6: Int,
    val incomeMultiplier2: BigDecimal,
    val incomeMultiplier3: BigDecimal,
    val incomeMultiplier4: BigDecimal,
    val incomeMultiplier5: BigDecimal,
    val incomeMultiplier6: BigDecimal,
    val maxIncomeThreshold2: Int,
    val maxIncomeThreshold3: Int,
    val maxIncomeThreshold4: Int,
    val maxIncomeThreshold5: Int,
    val maxIncomeThreshold6: Int,
    val incomeThresholdIncrease6Plus: Int,
    val siblingDiscount2: BigDecimal,
    val siblingDiscount2Plus: BigDecimal,
    val maxFee: Int,
    val minFee: Int
)

fun Database.Read.getFeeThresholds(): List<FeeThresholdsWithValidity> =
    createQuery("SELECT * FROM fee_thresholds")
        .mapTo<FeeThresholdsWithValidity>()
        .toList()

fun Database.Transaction.insertNewFeeThresholds(thresholds: CreateFeeThresholdsBody) =
    createUpdate(
        """
INSERT INTO fee_thresholds (
    id,
    valid_during,
    min_income_threshold_2,
    min_income_threshold_3,
    min_income_threshold_4,
    min_income_threshold_5,
    min_income_threshold_6,
    income_multiplier_2,
    income_multiplier_3,
    income_multiplier_4,
    income_multiplier_5,
    income_multiplier_6,
    max_income_threshold_2,
    max_income_threshold_3,
    max_income_threshold_4,
    max_income_threshold_5,
    max_income_threshold_6,
    income_threshold_increase_6_plus,
    sibling_discount_2,
    sibling_discount_2_plus,
    max_fee,
    min_fee
) VALUES (
    :id,
    :validDuring,
    :minIncomeThreshold2,
    :minIncomeThreshold3,
    :minIncomeThreshold4,
    :minIncomeThreshold5,
    :minIncomeThreshold6,
    :incomeMultiplier2,
    :incomeMultiplier3,
    :incomeMultiplier4,
    :incomeMultiplier5,
    :incomeMultiplier6,
    :maxIncomeThreshold2,
    :maxIncomeThreshold3,
    :maxIncomeThreshold4,
    :maxIncomeThreshold5,
    :maxIncomeThreshold6,
    :incomeThresholdIncrease6Plus,
    :siblingDiscount2,
    :siblingDiscount2Plus,
    :maxFee,
    :minFee
)
"""
    )
        .bindKotlin(thresholds)
        .bind("id", UUID.randomUUID())
        .execute()

fun Database.Transaction.updateFeeThresholdsValidity(id: UUID, newValidity: DateRange) =
    createUpdate("UPDATE fee_thresholds SET valid_during = :validDuring WHERE id = :id")
        .bind("id", id)
        .bind("validDuring", newValidity)
        .execute()
