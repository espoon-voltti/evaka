package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.data.searchValueDecisions
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.utils.parseEnum
import org.jdbi.v3.core.Jdbi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/value-decisions")
class VoucherValueDecisionController(private val jdbi: Jdbi) {
    @GetMapping("/search")
    fun search(
        user: AuthenticatedUser,
        @RequestParam(required = true) page: Int,
        @RequestParam(required = true) pageSize: Int,
        @RequestParam(required = false) sortBy: VoucherValueDecisionSortParam?,
        @RequestParam(required = false) sortDirection: SortDirection?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) area: String?,
        @RequestParam(required = false) unit: String?,
        @RequestParam(required = false) searchTerms: String?
    ): ResponseEntity<VoucherValueDecisionSearchResult> {
        Audit.VoucherValueDecisionSearch.log()
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val maxPageSize = 5000
        if (pageSize > maxPageSize) throw BadRequest("Maximum page size is $maxPageSize")
        val (total, valueDecisions) = jdbi.transaction { h ->
            h.searchValueDecisions(
                page,
                pageSize,
                sortBy ?: VoucherValueDecisionSortParam.STATUS,
                sortDirection ?: SortDirection.DESC,
                status?.let { parseEnum<VoucherValueDecisionStatus>(it) } ?: throw BadRequest("Status is a mandatory parameter"),
                area?.split(",") ?: listOf(),
                unit?.let { parseUUID(it) },
                searchTerms ?: ""
            )
        }
        val pages =
            if (total % pageSize == 0) total / pageSize
            else (total / pageSize) + 1
        return ResponseEntity.ok(VoucherValueDecisionSearchResult(valueDecisions, total, pages))
    }
}

data class VoucherValueDecisionSearchResult(
    val data: List<VoucherValueDecisionSummary>,
    val total: Int,
    val pages: Int
)

enum class VoucherValueDecisionSortParam {
    HEAD_OF_FAMILY,
    STATUS
}
