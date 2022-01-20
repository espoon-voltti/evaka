// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { AbsenceType } from 'lib-common/generated/enums'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import { absenceColors } from 'lib-customizations/common'
import { faThermometer } from 'lib-icons'
import { useTranslation } from '../../../state/i18n'

const absenceIcons = {
  UNKNOWN_ABSENCE: '?',
  OTHER_ABSENCE: '-',
  SICKLEAVE: faThermometer,
  PLANNED_ABSENCE: 'P',
  PARENTLEAVE: '-',
  FORCE_MAJEURE: '-',
  TEMPORARY_RELOCATION: '-',
  TEMPORARY_VISITOR: '-',
  NO_ABSENCE: '-'
} as const

interface Props {
  type: AbsenceType
}

export default React.memo(function AbsenceDay({ type }: Props) {
  const { i18n } = useTranslation()
  return (
    <FixedSpaceRow spacing={defaultMargins.xs} alignItems="center">
      <RoundIcon
        content={absenceIcons[type]}
        color={absenceColors[type]}
        size="m"
      />
      <span>{i18n.absences.absenceTypesShort[type]}</span>
    </FixedSpaceRow>
  )
})
