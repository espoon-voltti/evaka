// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.domain

import evaka.core.shared.AreaId
import evaka.core.shared.DaycareId

data class UnitData(
    val id: DaycareId,
    val name: String,
    val areaId: AreaId,
    val areaName: String,
    val language: String,
)
