// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.getEmployee
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging

private val EMAIL_PATTERN = "^([\\w.%+-]+)@([\\w-]+\\.)+([\\w]{2,})\$".toRegex()

private val logger = KotlinLogging.logger {}

class Email
private constructor(
    val toAddress: String,
    val fromAddress: String,
    val content: EmailContent,
    val traceId: String,
) {
    companion object {
        fun create(
            dbc: Database.Connection,
            personId: PersonId,
            emailType: EmailMessageType,
            fromAddress: String,
            content: EmailContent,
            traceId: String,
        ): Email? {
            val (toAddress, disabledEmailTypes) =
                dbc.read { tx -> tx.getEmailAddressAndDisabledTypes(personId) }

            if (toAddress == null) {
                logger.warn("Will not send email due to missing email address: (traceId: $traceId)")
                return null
            }

            if (!toAddress.matches(EMAIL_PATTERN)) {
                logger.warn(
                    "Will not send email due to invalid toAddress \"$toAddress\": (traceId: $traceId)"
                )
                return null
            }

            if (emailType !in EmailMessageType.alwaysEnabled && emailType in disabledEmailTypes) {
                logger.info {
                    "Not sending email (traceId: $traceId): $emailType disabled for person $personId"
                }
                return null
            }

            return Email(toAddress, fromAddress, content, traceId)
        }

        fun createForEmployee(
            dbc: Database.Connection,
            employeeId: EmployeeId,
            content: EmailContent,
            traceId: String,
            fromAddress: String,
        ): Email? {
            val employee = dbc.read { it.getEmployee(employeeId) } ?: return null

            if (employee.email == null) {
                logger.warn("Will not send email due to missing email address: (traceId: $traceId)")
                return null
            }

            if (!employee.email.matches(EMAIL_PATTERN)) {
                logger.warn(
                    "Will not send email due to invalid toAddress \"${employee.email}\": (traceId: $traceId)"
                )
                return null
            }

            return Email(employee.email, fromAddress, content, traceId)
        }
    }
}

interface EmailClient {
    fun send(email: Email)
}

private data class EmailAndEnabledEmailTypes(
    val email: String?,
    val disabledEmailTypes: Set<EmailMessageType>,
)

private fun Database.Read.getEmailAddressAndDisabledTypes(
    personId: PersonId
): EmailAndEnabledEmailTypes {
    return createQuery {
            sql("""SELECT email, disabled_email_types FROM person WHERE id = ${bind(personId)}""")
        }
        .exactlyOne<EmailAndEnabledEmailTypes>()
        .let { it.copy(email = it.email?.trim()) }
}
