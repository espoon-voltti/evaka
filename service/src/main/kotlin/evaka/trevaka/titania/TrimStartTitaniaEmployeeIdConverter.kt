// SPDX-FileCopyrightText: 2021-2023 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.titania

import evaka.core.titania.TitaniaEmployeeIdConverter

class TrimStartTitaniaEmployeeIdConverter : TitaniaEmployeeIdConverter {
    override fun fromTitania(employeeId: String): String = employeeId.trimStart('0')
}
