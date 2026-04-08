// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.absence.application

import evaka.core.EmailEnv
import evaka.core.daycare.domain.Language
import evaka.core.daycare.getDaycare
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.pis.EmailMessageType
import evaka.core.pis.getPersonById
import evaka.core.placement.getPlacementsForChildDuring
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.NotFound
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
                            AbsenceApplicationStatus.ACCEPTED -> {
                                true
                            }

                            AbsenceApplicationStatus.REJECTED -> {
                                false
                            }

                            else -> {
                                throw IllegalArgumentException(
                                    "Invalid absence application status: ${application.status}"
                                )
                            }
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
