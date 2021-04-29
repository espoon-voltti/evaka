// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.bulletin

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/citizen/bulletins")
class BulletinControllerCitizen {

    @GetMapping
    fun getReceivedBulletins(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam page: Int,
        @RequestParam pageSize: Int
    ): ResponseEntity<Paged<ReceivedBulletin>> {
        Audit.MessagingBulletinRead.log()
        user.requireOneOfRoles(UserRole.END_USER, UserRole.CITIZEN_WEAK)

        return db
            .read { it.getReceivedBulletinsByGuardian(user, page, pageSize) }
            .let { ResponseEntity.ok(it) }
    }

    @GetMapping("/unread")
    fun getUnreadCount(
        db: Database.Connection,
        user: AuthenticatedUser
    ): ResponseEntity<Int> {
        Audit.MessagingUnreadCountRead.log()
        user.requireOneOfRoles(UserRole.END_USER, UserRole.CITIZEN_WEAK)

        return db.read {
            it.getUnreadBulletinCountByGuardian(user)
        }.let { ResponseEntity.ok(it) }
    }

    @PutMapping("/{id}/read")
    fun markBulletinRead(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        Audit.MessagingBulletinMarkRead.log(id)
        user.requireOneOfRoles(UserRole.END_USER, UserRole.CITIZEN_WEAK)

        db.transaction {
            it.markBulletinRead(user, id)
        }

        return ResponseEntity.noContent().build()
    }
}
