// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence.application

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.stereotype.Service

@Service
class AbsenceApplicationService(
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailEnv: EmailEnv,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailClient: EmailClient,
) {
    init {
        asyncJobRunner.registerHandler(::sendDecidedEmail)
    }

    private fun sendDecidedEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendAbsenceApplicationDecidedEmail,
    ) {
        val (application, guardian, language) =
            db.read { tx ->
                val application =
                    tx.selectAbsenceApplication(msg.absenceApplicationId) ?: throw NotFound()
                val guardian = tx.getPersonById(PersonId(application.createdBy.raw))
                val language =
                    tx.getPlacementsForChildDuring(
                            childId = application.childId,
                            start = application.startDate,
                            end = application.startDate,
                        )
                        .firstOrNull()
                        ?.let { placement -> tx.getDaycare(placement.unitId)?.language }
                        ?: Language.fi
                Triple(application, guardian, language)
            }
        if (guardian != null) {
            Email.create(
                    db,
                    guardian.id,
                    EmailMessageType.DECISION_NOTIFICATION,
                    emailEnv.sender(language),
                    emailMessageProvider.absenceApplicationDecidedNotification(
                        when (application.status) {
                            AbsenceApplicationStatus.ACCEPTED -> true
                            AbsenceApplicationStatus.REJECTED -> false
                            else ->
                                throw IllegalArgumentException(
                                    "Invalid absence application status: ${application.status}"
                                )
                        },
                        application.startDate,
                        application.endDate,
                    ),
                    "${msg.absenceApplicationId}",
                )
                ?.also { emailClient.send(it) }
        }
    }
}
