// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId

data class UnitData(
    val id: DaycareId,
    val name: String,
    val areaId: AreaId,
    val areaName: String,
    val language: String,
)
