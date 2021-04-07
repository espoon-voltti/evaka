// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.daycarydailynote

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController()
@RequestMapping("/daycare-daily-note")
class DaycareDailyNoteController(
    private val acl: AccessControlList
) {

    @GetMapping("/daycare/group/{groupId}")
    fun getDaycareDailyNotesForGroup(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable groupId: UUID
    ): ResponseEntity<List<DaycareDailyNote>> {
        Audit.DaycareDailyNoteRead.log(user.id)

        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.MOBILE)

        return db.read { it.getGroupDaycareDailyNotes(groupId) + it.getDaycareDailyNotesForChildrenInGroup(groupId) }.let { ResponseEntity.ok(it) }
    }

    @GetMapping("/child/{childId}")
    fun getDaycareDailyNotesForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<List<DaycareDailyNote>> {
        Audit.DaycareDailyNoteRead.log(user.id)

        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.MOBILE)

        return db.read { it.getChildDaycareDailyNotes(childId) }.let { ResponseEntity.ok(it) }
    }

    @PostMapping("/child/{childId}")
    fun createDailyNoteForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody body: DaycareDailyNote
    ): ResponseEntity<UUID> {
        Audit.DaycareDailyNoteCreate.log(user.id)

        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.MOBILE)

        return db.transaction { ResponseEntity.ok(it.createDaycareDailyNote(body.copy(childId = childId))) }
    }

    @PutMapping("/child/{childId}")
    fun updateDailyNoteForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody body: DaycareDailyNote
    ): ResponseEntity<DaycareDailyNote> {
        Audit.DaycareDailyNoteUpdate.log(user.id)

        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.MOBILE)

        return db.transaction { ResponseEntity.ok(it.updateDaycareDailyNote(body.copy(childId = childId))) }
    }

    @PostMapping("/group/{groupId}")
    fun createDailyNoteForGroup(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable groupId: UUID,
        @RequestBody body: DaycareDailyNote
    ): ResponseEntity<UUID> {
        Audit.DaycareDailyNoteCreate.log(user.id)

        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.MOBILE)

        return db.transaction { ResponseEntity.ok(it.createDaycareDailyNote(body.copy(groupId = groupId))) }
    }

    @PutMapping("/group/{groupId}")
    fun updateDailyNoteForGroup(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable groupId: UUID,
        @RequestBody body: DaycareDailyNote
    ): ResponseEntity<DaycareDailyNote> {
        Audit.DaycareDailyNoteUpdate.log(user.id)

        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.MOBILE)

        return db.transaction { ResponseEntity.ok(it.updateDaycareDailyNote(body.copy(groupId = groupId))) }
    }

    @DeleteMapping("/{noteId}")
    fun deleteDailyNote(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable noteId: UUID
    ) {
        Audit.DaycareDailyNoteDelete.log(user.id)

        acl.getRolesForDailyNote(user, noteId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.MOBILE)

        return db.transaction { it.deleteDaycareDailyNote(noteId) }.let { ResponseEntity.ok() }
    }

    @DeleteMapping("/child/{childId}")
    fun deleteDailyNoteForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ) {
        Audit.DaycareDailyNoteDelete.log(user.id)

        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.MOBILE)

        return db.transaction { it.deleteChildDaycareDailyNotes(childId) }.let { ResponseEntity.ok() }
    }
}
