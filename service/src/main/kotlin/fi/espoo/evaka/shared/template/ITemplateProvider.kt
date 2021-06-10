// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.template

interface ITemplateProvider {
    fun getFeeDecisionPath(): String
    fun getVoucherValueDecisionPath(): String
    fun getClubDecisionPath(): String
    fun getDaycareVoucherDecisionPath(): String
    fun getDaycareTransferDecisionPath(): String
    fun getDaycareDecisionPath(): String
    fun getPreschoolDecisionPath(): String
    fun getPreparatoryDecisionPath(): String
}
