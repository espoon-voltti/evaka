// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.bulletin

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/bulletins")
class BulletinControllerEmployee(
    private val acl: AccessControlList,
    private val bulletinNotificationEmailService: BulletinNotificationEmailService
) {

    data class CreateBulletinRequest(
        val unitId: UUID
    )
    @PostMapping
    fun createBulletin(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: CreateBulletinRequest
    ): ResponseEntity<Bulletin> {
        Audit.MessagingBulletinDraftCreate.log()
        authorizeAdminSupervisorOrStaff(user)

        return db.transaction { tx ->
            tx.initBulletin(
                user = user,
                unitId = body.unitId
            ).let { tx.getBulletin(it)!! }
        }.let {
            ResponseEntity.ok(it)
        }
    }

    @GetMapping("/sent")
    fun getSentBulletins(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam unitId: UUID,
        @RequestParam page: Int,
        @RequestParam pageSize: Int
    ): ResponseEntity<Paged<Bulletin>> {
        Audit.MessagingBulletinRead.log(unitId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)

        return db.transaction { tx ->
            tx.getSentBulletinsByUnit(unitId, page, pageSize)
        }.let { ResponseEntity.ok(it) }
    }

    @GetMapping("/draft")
    fun getDraftBulletins(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam unitId: UUID,
        @RequestParam page: Int,
        @RequestParam pageSize: Int
    ): ResponseEntity<Paged<Bulletin>> {
        Audit.MessagingBulletinDraftRead.log()
        authorizeAdminSupervisorOrStaff(user)

        return db.transaction { tx ->
            tx.getOwnBulletinDrafts(user, unitId, page, pageSize)
        }.let { ResponseEntity.ok(it) }
    }

    data class BulletinReceiverPerson(
        val receiverId: UUID,
        val receiverFirstName: String,
        val receiverLastName: String
    )
    data class BulletinReceiver(
        val childId: UUID,
        val childFirstName: String,
        val childLastName: String,
        val childDateOfBirth: LocalDate,
        val receiverPersons: List<BulletinReceiverPerson>
    )
    data class BulletinReceiversResponse(
        val groupId: UUID,
        val groupName: String,
        val receivers: List<BulletinReceiver>
    )
    @GetMapping("/receivers")
    fun getReceiversForNewBulletin(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam unitId: UUID
    ): ResponseEntity<List<BulletinReceiversResponse>> {
        Audit.MessagingBulletinReceiversRead.log(unitId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)

        return db.transaction { tx ->
            tx.getReceiversForNewBulletin(user, unitId)
        }.let { ResponseEntity.ok(it) }
    }

    data class BulletinUpdate(
        val groupId: UUID?,
        val title: String,
        val content: String
    )
    @PutMapping("/{id}")
    fun updateBulletin(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: BulletinUpdate
    ): ResponseEntity<Unit> {
        Audit.MessagingBulletinDraftUpdate.log(id)
        authorizeAdminSupervisorOrStaff(user)

        db.transaction { tx ->
            tx.updateDraftBulletin(
                user = user,
                id = id,
                groupId = body.groupId,
                title = body.title,
                content = body.content
            )
        }

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    fun deleteDraftBulletin(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        Audit.MessagingBulletinDraftDelete.log(id)
        authorizeAdminSupervisorOrStaff(user)

        db.transaction { tx ->
            tx.deleteDraftBulletin(user, id)
        }

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/send")
    fun sendBulletin(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        Audit.MessagingBulletinSend.log(id)
        authorizeAdminSupervisorOrStaff(user)

        db.transaction { tx ->
            tx.getBulletin(id)?.let {
                if (it.groupId != null) {
                    acl.getRolesForUnitGroup(user, it.groupId)
                        .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)
                } else throw BadRequest("Must select group before sending")
            }
            tx.sendBulletin(user, id)
            bulletinNotificationEmailService.scheduleSendingBulletinNotifications(tx, id)
        }

        return ResponseEntity.noContent().build()
    }

    private fun authorizeAdminSupervisorOrStaff(user: AuthenticatedUser) {
        if (!user.hasOneOfRoles(UserRole.ADMIN)) {
            if (acl.getAuthorizedUnits(user).ids?.isEmpty() != false) {
                throw Forbidden("Permission denied")
            }
        }
    }
}
