// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { PlacementType } from 'lib-common/generated/api-types/placement'
import { StaticChip } from 'lib-components/atoms/Chip'
import { careTypeColors } from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'

export type CareTypeLabel =
  | 'club'
  | 'daycare'
  | 'daycare5yo'
  | 'preschool'
  | 'preparatory'
  | 'backup-care'
  | 'temporary'
  | 'school-shift-care'
  | 'connected-daycare'

const placementTypeToCareTypeLabel = (
  type: PlacementType | 'backup-care' | 'connected-daycare'
): CareTypeLabel => {
  switch (type) {
    case 'backup-care':
      return 'backup-care'
    case 'connected-daycare':
      return 'connected-daycare'
    case 'CLUB':
      return 'club'
    case 'DAYCARE':
    case 'DAYCARE_PART_TIME':
    case 'PRESCHOOL_DAYCARE_ONLY':
    case 'PREPARATORY_DAYCARE_ONLY':
      return 'daycare'
    case 'DAYCARE_FIVE_YEAR_OLDS':
    case 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS':
      return 'daycare5yo'
    case 'PREPARATORY':
    case 'PREPARATORY_DAYCARE':
      return 'preparatory'
    case 'PRESCHOOL':
    case 'PRESCHOOL_DAYCARE':
    case 'PRESCHOOL_CLUB':
      return 'preschool'
    case 'SCHOOL_SHIFT_CARE':
      return 'school-shift-care'
    case 'TEMPORARY_DAYCARE':
    case 'TEMPORARY_DAYCARE_PART_DAY':
      return 'temporary'
  }
}

interface Props {
  type: PlacementType | 'backup-care' | 'connected-daycare'
}

export function CareTypeChip({ type }: Props) {
  const { i18n } = useTranslation()

  const careType = placementTypeToCareTypeLabel(type)
  const text = i18n.common.careTypeLabels[careType]
  const color = careTypeColors[careType]
  return <StaticChip color={color}>{text}</StaticChip>
}
