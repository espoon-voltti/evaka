// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/integration/titania")
class TitaniaController(private val titaniaService: TitaniaService) {

    @PutMapping("/working-time-events")
    fun updateWorkingTimeEvents(
        @RequestBody request: UpdateWorkingTimeEventsRequest,
        user: AuthenticatedUser.Integration,
        db: Database,
    ): UpdateWorkingTimeEventsResponse {
        return db.connect { dbc ->
            lateinit var result: UpdateWorkingTimeEventsServiceResponse
            dbc.transaction { tx -> result = titaniaService.updateWorkingTimeEvents(tx, request) }
            result.createdEmployees.forEach { Audit.EmployeeCreate.log(targetId = AuditId(it)) }
            if (result.overlappingShifts.isNotEmpty()) {
                throw TitaniaException(
                    TitaniaErrorDetail(
                        errorcode = TitaniaError.CONFLICTING_SHIFTS,
                        message = "Conflicting working time events found",
                    )
                )
            }
            result.updateWorkingTimeEventsResponse
        }
    }

    @PostMapping("/stamped-working-time-events")
    fun getStampedWorkingTimeEvents(
        @RequestBody request: GetStampedWorkingTimeEventsRequest,
        user: AuthenticatedUser.Integration,
        db: Database,
    ): GetStampedWorkingTimeEventsResponse {
        return db.connect { dbc ->
            dbc.read { tx -> titaniaService.getStampedWorkingTimeEvents(tx, request) }
        }
    }

    @ExceptionHandler(value = [TitaniaException::class])
    fun titaniaExceptionHandler(
        ex: TitaniaException,
        req: HttpServletRequest,
    ): ResponseEntity<TitaniaErrorResponse> {
        val message = "${ex.status.name} (${ex.message})"
        when (ex.status.series()) {
            HttpStatus.Series.CLIENT_ERROR -> logger.warn { message }
            HttpStatus.Series.SERVER_ERROR -> logger.error { message }
            else -> logger.info { message }
        }
        return ResponseEntity.status(ex.status)
            .body(TitaniaErrorResponse(faultactor = req.requestURI, detail = ex.detail))
    }
}
