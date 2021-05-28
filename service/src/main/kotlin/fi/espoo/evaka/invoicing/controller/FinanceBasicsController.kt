package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.domain.FeeThresholdsWithValidity
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/finance-basics")
class FinanceBasicsController {
    @GetMapping("/fee-thresholds")
    fun getFeeThresholds(db: Database.Connection, user: AuthenticatedUser): List<FeeThresholdsWithValidity> {
        Audit.FinanceBasicsPricingRead.log()
        user.requireOneOfRoles(UserRole.FINANCE_ADMIN)
        return db.read {
            it.createQuery("SELECT * FROM fee_thresholds ORDER BY valid_during DESC")
                .mapTo<FeeThresholdsWithValidity>()
                .toList()
        }
    }
}
