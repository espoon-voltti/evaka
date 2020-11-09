package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import org.jdbi.v3.core.Jdbi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class DummyVoucherServiceProviderReport(
    private val jdbi: Jdbi
) {
    @GetMapping("/reports/voucher-service-providers")
    fun getVoucherServiceProviders(
            user: AuthenticatedUser,
            @RequestParam areaId: UUID,
            @RequestParam year: Int,
            @RequestParam month: Int
    ): ResponseEntity<List<VoucherServiceProviderReportResultRow>> {
        Audit.OccupancyReportRead.log(targetId = areaId)
        user.requireOneOfRoles(Roles.SERVICE_WORKER, Roles.ADMIN, Roles.FINANCE_ADMIN, Roles.DIRECTOR)
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        val result = listOf(
            VoucherServiceProviderReportResultRow(
                "Tapiola",
                UUID.fromString("2dea517c-788e-11e9-be92-337b0505ffd8"),
                "Mankkaan päiväkoti",
                15,
                12345,
                "/todo",
                from, to
            )
        )

        return ResponseEntity.ok(result)
    }
}

data class VoucherServiceProviderReportResultRow(
        val areaName: String,
        val unitId: UUID,
        val unitName: String,
        val voucherChildCount: Int,
        val voucherSum: Int,
        val unitVoucherReportUri: String,
        val startDate: LocalDate,
        val endDate: LocalDate
)
