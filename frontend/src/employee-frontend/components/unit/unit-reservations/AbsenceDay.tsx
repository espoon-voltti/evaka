// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { absenceColors, absenceIcons } from 'lib-customizations/common'

import { useTranslation } from '../../../state/i18n'

interface Props {
  type: AbsenceType
}

export default React.memo(function AbsenceDay({ type }: Props) {
  const { i18n } = useTranslation()
  return (
    <FixedSpaceRow spacing="xs" alignItems="center" data-qa="absence">
      <RoundIcon
        content={absenceIcons[type]}
        color={absenceColors[type]}
        size="m"
      />
      <span>{i18n.absences.absenceTypesShort[type]}</span>
    </FixedSpaceRow>
  )
})
