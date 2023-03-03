// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId

data class BiArea(val id: AreaId, val name: String)

data class BiUnit(val id: DaycareId, val area: AreaId, val name: String)
