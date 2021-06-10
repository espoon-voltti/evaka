// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
export interface ServiceNeedOptionPublicInfo {
  id: UUID
  name: string
  validPlacementType: PlacementType
}

export interface ServiceNeedOptionSummary {
  id: UUID
  name: string
}

export type PlacementType =
  | 'CLUB'
  | 'DAYCARE'
  | 'DAYCARE_PART_TIME'
  | 'DAYCARE_FIVE_YEAR_OLDS'
  | 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS'
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'PREPARATORY'
  | 'PREPARATORY_DAYCARE'
  | 'TEMPORARY_DAYCARE'
  | 'TEMPORARY_DAYCARE_PART_DAY'
