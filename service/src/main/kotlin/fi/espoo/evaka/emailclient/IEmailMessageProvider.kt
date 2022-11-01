// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId

interface IEmailMessageProvider {

    val subjectForPendingDecisionEmail: String
    val subjectForClubApplicationReceivedEmail: String
    val subjectForDaycareApplicationReceivedEmail: String
    val subjectForPreschoolApplicationReceivedEmail: String
    val subjectForDecisionEmail: String

    fun getPendingDecisionEmailSubject(): String {
        return subjectForPendingDecisionEmail
    }

    fun getPendingDecisionEmailHtml(): String
    fun getPendingDecisionEmailText(): String

    fun getClubApplicationReceivedEmailSubject(): String {
        return subjectForClubApplicationReceivedEmail
    }

    fun getClubApplicationReceivedEmailHtml(): String
    fun getClubApplicationReceivedEmailText(): String

    fun getDaycareApplicationReceivedEmailSubject(): String {
        return subjectForDaycareApplicationReceivedEmail
    }

    fun getDaycareApplicationReceivedEmailHtml(): String
    fun getDaycareApplicationReceivedEmailText(): String

    fun getPreschoolApplicationReceivedEmailSubject(): String {
        return subjectForPreschoolApplicationReceivedEmail
    }

    fun getPreschoolApplicationReceivedEmailHtml(withinApplicationPeriod: Boolean): String
    fun getPreschoolApplicationReceivedEmailText(withinApplicationPeriod: Boolean): String

    fun getDecisionEmailSubject(): String {
        return subjectForDecisionEmail
    }

    fun getDecisionEmailHtml(childId: ChildId, decisionId: AssistanceNeedDecisionId): String
    fun getDecisionEmailText(childId: ChildId, decisionId: AssistanceNeedDecisionId): String
}
