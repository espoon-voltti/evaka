// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import { PlacementType } from 'lib-common/generated/enums'

export interface ServiceNeedOptionPublicInfo {
  id: UUID
  name: string
  validPlacementType: PlacementType
}

export interface ServiceNeedOptionSummary {
  id: UUID
  name: string
}
