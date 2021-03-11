// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.daycarydailynote

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController()
@RequestMapping("/daycare-daily-note")
class DaycareDailyNoteController {

    @GetMapping("/daycare/group/{groupId}")
    fun getDaycareDailyNotesForGroup(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable groupId: UUID
    ): ResponseEntity<List<DaycareDailyNote>> {
        Audit.DaycareDailyNoteRead.log(user.id)
        return db.read { it.getGroupDaycareDailyNotes(groupId) }.let { ResponseEntity.ok(it) }
    }

    @GetMapping("/child/{childId}")
    fun getDaycareDailyNotesForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<List<DaycareDailyNote>> {
        Audit.DaycareDailyNoteRead.log(user.id)
        return db.read { it.getChildDaycareDailyNotes(childId) }.let { ResponseEntity.ok(it) }
    }

    @PostMapping("/child/{childId")
    fun createOrUpdateDaycareDailyNoteForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: DaycareDailyNote
    ) {
        Audit.DaycareDailyNoteCreate.log(user.id)
        return db.transaction { it.upsertDaycareDailyNote(body) }.let { ResponseEntity.ok() }
    }
}
