package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.data.getPricing
import fi.espoo.evaka.invoicing.domain.PricingWithValidity
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/finance-basics")
class FinanceBasicsController {
    @GetMapping("/pricing")
    fun getPricing(db: Database.Connection, user: AuthenticatedUser): List<PricingWithValidity> {
        Audit.FinanceBasicsPricingRead.log()
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        return db.read { it.getPricing().sortedByDescending { it.validDuring.start } }
    }
}
