// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { PlacementType } from 'lib-common/generated/api-types/placement'
import { StaticChip } from 'lib-components/atoms/Chip'
import { careTypeColors } from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import type { CareTypeLabel } from '../../types'

const placementTypeToCareTypeLabel = (
  type: PlacementType | 'backup-care'
): CareTypeLabel => {
  switch (type) {
    case 'backup-care':
      return 'backup-care'
    case 'CLUB':
      return 'club'
    case 'DAYCARE':
    case 'DAYCARE_PART_TIME':
      return 'daycare'
    case 'DAYCARE_FIVE_YEAR_OLDS':
    case 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS':
      return 'daycare5yo'
    case 'PREPARATORY':
    case 'PREPARATORY_DAYCARE':
      return 'preparatory'
    case 'PRESCHOOL':
    case 'PRESCHOOL_DAYCARE':
      return 'preschool'
    case 'SCHOOL_SHIFT_CARE':
      return 'school-shift-care'
    case 'TEMPORARY_DAYCARE':
    case 'TEMPORARY_DAYCARE_PART_DAY':
      return 'temporary'
  }
}

interface Props {
  type: PlacementType | 'backup-care'
}

export function CareTypeChip({ type }: Props) {
  const { i18n } = useTranslation()

  const careType = placementTypeToCareTypeLabel(type)
  const text = i18n.common.careTypeLabels[careType]
  const color = careTypeColors[careType]
  return <StaticChip color={color}>{text}</StaticChip>
}
