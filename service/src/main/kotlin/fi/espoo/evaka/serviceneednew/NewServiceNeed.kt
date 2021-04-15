package fi.espoo.evaka.serviceneednew

import org.jdbi.v3.core.mapper.Nested
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
