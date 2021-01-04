package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.getDecisionsByGuardian
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen")
class ApplicationControllerCitizen() {
    @GetMapping("/decisions")
    fun getDecisions(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<List<Decision>> {
        Audit.DecisionRead.log(targetId = user.id)
        user.requireOneOfRoles(UserRole.END_USER)
        return ResponseEntity.ok(db.read { getDecisionsByGuardian(it.handle, user.id, AclAuthorization.All) })
    }
}