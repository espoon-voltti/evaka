// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backupcare

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.placement.childPlacementsHasConsecutiveRange
import fi.espoo.evaka.placement.clearCalendarEventAttendees
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.JdbiException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BackupCareController(private val accessControl: AccessControl) {
    @GetMapping("/employee/children/{childId}/backup-cares")
    fun getChildBackupCares(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): ChildBackupCaresResponse {
        return ChildBackupCaresResponse(
            db.connect { dbc ->
                    dbc.read { tx ->
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Child.READ_BACKUP_CARE,
                            childId,
                        )
                        val backupCares = tx.getBackupCaresForChild(childId)
                        val backupCareIds = backupCares.map { bc -> bc.id }
                        val permittedActions =
                            accessControl.getPermittedActions<BackupCareId, Action.BackupCare>(
                                tx,
                                user,
                                clock,
                                backupCareIds,
                            )
                        backupCares.map { bc ->
                            ChildBackupCareResponse(bc, permittedActions[bc.id] ?: emptySet())
                        }
                    }
                }
                .also {
                    Audit.ChildBackupCareRead.log(
                        targetId = AuditId(childId),
                        meta = mapOf("count" to it.size),
                    )
                }
        )
    }

    @PostMapping("/employee/children/{childId}/backup-cares")
    fun createBackupCare(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: NewBackupCare,
    ): BackupCareCreateResponse {
        try {
            val id =
                db.connect { dbc ->
                    dbc.transaction { tx ->
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Child.CREATE_BACKUP_CARE,
                            childId,
                        )
                        if (!tx.childPlacementsHasConsecutiveRange(childId, body.period)) {
                            throw BadRequest(
                                "The new backup care period is not contained within a placement"
                            )
                        }
                        tx.getPlacementsForChildDuring(childId, body.period.start, body.period.end)
                            .forEach { placement ->
                                tx.clearCalendarEventAttendees(
                                    childId,
                                    placement.unitId,
                                    body.period,
                                )
                            }
                        tx.createBackupCare(childId, body)
                    }
                }
            Audit.ChildBackupCareCreate.log(
                targetId = AuditId(childId),
                objectId = AuditId(body.unitId),
            )
            return BackupCareCreateResponse(id)
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @PostMapping("/employee/backup-cares/{id}")
    fun updateBackupCare(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: BackupCareId,
        @RequestBody body: BackupCareUpdateRequest,
    ) {
        try {
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.BackupCare.UPDATE,
                        id,
                    )
                    if (
                        !tx.childPlacementsHasConsecutiveRange(
                            tx.getBackupCareChildId(id),
                            body.period,
                        )
                    ) {
                        throw BadRequest(
                            "The new backup care period is not contained within a placement"
                        )
                    }

                    val existing = tx.getBackupCare(id)
                    if (existing != null) {
                        if (!existing.period.start.isEqual(body.period.start)) {
                            if (existing.period.start.isBefore(body.period.start)) {
                                // start was shrunk
                                tx.clearCalendarEventAttendees(
                                    existing.childId,
                                    existing.unitId,
                                    FiniteDateRange(
                                        existing.period.start,
                                        body.period.start.minusDays(1),
                                    ),
                                )
                            } else {
                                // the backup care was extended; clear calendar event attendees for
                                // the main placement
                                // for the extended period
                                tx.getPlacementsForChildDuring(
                                        existing.childId,
                                        body.period.start,
                                        existing.period.start,
                                    )
                                    .forEach { placement ->
                                        tx.clearCalendarEventAttendees(
                                            existing.childId,
                                            placement.unitId,
                                            FiniteDateRange(
                                                body.period.start,
                                                existing.period.start,
                                            ),
                                        )
                                    }
                            }
                        }

                        if (!existing.period.end.isEqual(body.period.end)) {
                            if (existing.period.end.isAfter(body.period.end)) {
                                // end was shrunk
                                tx.clearCalendarEventAttendees(
                                    existing.childId,
                                    existing.unitId,
                                    FiniteDateRange(
                                        body.period.end.plusDays(1),
                                        existing.period.end,
                                    ),
                                )
                            } else {
                                // the backup care was extended; clear calendar event attendees for
                                // the main placement
                                // for the extended period
                                tx.getPlacementsForChildDuring(
                                        existing.childId,
                                        existing.period.end,
                                        body.period.end,
                                    )
                                    .forEach { placement ->
                                        tx.clearCalendarEventAttendees(
                                            existing.childId,
                                            placement.unitId,
                                            FiniteDateRange(existing.period.end, body.period.end),
                                        )
                                    }
                            }
                        }
                    }
                    tx.updateBackupCare(id, body.period, body.groupId)
                }
            }
            Audit.BackupCareUpdate.log(
                targetId = AuditId(id),
                objectId = body.groupId?.let(AuditId::invoke),
            )
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @DeleteMapping("/employee/backup-cares/{id}")
    fun deleteBackupCare(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: BackupCareId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.BackupCare.DELETE, id)
                val backupCare = tx.getBackupCare(id)
                if (backupCare != null) {
                    tx.clearCalendarEventAttendees(
                        backupCare.childId,
                        backupCare.unitId,
                        backupCare.period,
                    )
                }
                tx.deleteBackupCare(id)
            }
        }
        Audit.BackupCareDelete.log(targetId = AuditId(id))
    }
}

data class ChildBackupCaresResponse(val backupCares: List<ChildBackupCareResponse>)

data class BackupCareUpdateRequest(val period: FiniteDateRange, val groupId: GroupId?)

data class BackupCareCreateResponse(val id: BackupCareId)
