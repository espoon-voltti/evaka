// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.daycarydailynote

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareDailyNoteId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/daycare-daily-note")
class DaycareDailyNoteController(
    private val acl: AccessControlList,
    private val accessControl: AccessControl
) {

    @GetMapping("/daycare/group/{groupId}")
    fun getDaycareDailyNotesForGroup(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable groupId: GroupId
    ): List<DaycareDailyNote> {
        Audit.DaycareDailyNoteRead.log(groupId)
        accessControl.requirePermissionFor(user, Action.Group.READ_DAYCARE_DAILY_NOTES, groupId)

        return db.read { it.getGroupDaycareDailyNotes(groupId) + it.getDaycareDailyNotesForChildrenInGroup(groupId) }
    }

    @GetMapping("/child/{childId}")
    fun getDaycareDailyNotesForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): List<DaycareDailyNote> {
        Audit.DaycareDailyNoteRead.log(childId)

        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.MOBILE)

        return db.read { it.getChildDaycareDailyNotes(childId) }
    }

    @PostMapping("/child/{childId}")
    fun createDailyNoteForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody body: DaycareDailyNoteBody
    ): DaycareDailyNoteId {
        Audit.DaycareDailyNoteCreate.log(childId)

        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.MOBILE)

        return db.transaction { it.createDaycareDailyNote(body.copy(childId = childId), user.id) }
    }

    @PutMapping("/child/{childId}/{noteId}")
    fun updateDailyNoteForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable noteId: DaycareDailyNoteId,
        @PathVariable childId: UUID,
        @RequestBody body: DaycareDailyNoteBody
    ): DaycareDailyNote {
        Audit.DaycareDailyNoteUpdate.log(noteId, childId)

        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.MOBILE)

        return db.transaction { it.updateDaycareDailyNote(noteId, body.copy(childId = childId), user.id) }
    }

    @PostMapping("/group/{groupId}")
    fun createDailyNoteForGroup(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable groupId: GroupId,
        @RequestBody body: DaycareDailyNoteBody
    ): DaycareDailyNoteId {
        Audit.DaycareDailyNoteCreate.log(groupId)

        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.MOBILE)

        return db.transaction { it.createDaycareDailyNote(body.copy(groupId = groupId), user.id) }
    }

    @PutMapping("/group/{groupId}/{noteId}")
    fun updateDailyNoteForGroup(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable noteId: DaycareDailyNoteId,
        @PathVariable groupId: GroupId,
        @RequestBody body: DaycareDailyNoteBody
    ): DaycareDailyNote {
        Audit.DaycareDailyNoteUpdate.log(noteId, groupId)

        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.MOBILE)

        return db.transaction { it.updateDaycareDailyNote(noteId, body.copy(groupId = groupId), user.id) }
    }

    @DeleteMapping("/{noteId}")
    fun deleteDailyNote(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable noteId: DaycareDailyNoteId
    ) {
        Audit.DaycareDailyNoteDelete.log(noteId)

        acl.getRolesForDailyNote(user, noteId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.MOBILE)

        return db.transaction { it.deleteDaycareDailyNote(noteId) }
    }

    @DeleteMapping("/child/{childId}")
    fun deleteDailyNoteForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ) {
        Audit.DaycareDailyNoteDelete.log(childId)

        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.MOBILE)

        return db.transaction { it.deleteChildDaycareDailyNotes(childId) }
    }
}
