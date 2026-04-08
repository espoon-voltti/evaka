// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.dto

import java.time.LocalDate

data class RestrictedDetails(val enabled: Boolean, val endDate: LocalDate? = null)
