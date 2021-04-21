package fi.espoo.evaka.serviceneednew

import fi.espoo.evaka.placement.PlacementType
import org.jdbi.v3.core.mapper.Nested
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class NewServiceNeed(
    val id: UUID,
    val placementId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    @Nested("option")
    val option: ServiceNeedOptionSummary,
    val shiftCare: Boolean
)

data class ServiceNeedOptionSummary(
    val id: UUID,
    val name: String
)

data class ServiceNeedOption(
    val id: UUID,
    val name: String,
    val validPlacementType: PlacementType,
    val defaultOption: Boolean,
    val feeCoefficient: BigDecimal,
    val voucherValueCoefficient: BigDecimal,
    val occupancyCoefficient: BigDecimal,
    val daycareHoursPerWeek: Int,
    val partDay: Boolean,
    val partWeek: Boolean
)
