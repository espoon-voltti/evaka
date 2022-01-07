// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faThermometer } from 'lib-icons'
import React from 'react'
import styled from 'styled-components'
import { AbsenceType } from 'lib-common/generated/enums'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import colors from 'lib-customizations/common'
import { useTranslation } from '../../../state/i18n'

interface Props {
  type: AbsenceType
}

export default React.memo(function AbsenceDay({ type }: Props) {
  const { i18n } = useTranslation()
  if (type === 'SICKLEAVE')
    return (
      <AbsenceCell>
        <FixedSpaceRow spacing="xs" alignItems="center">
          <RoundIcon
            content={faThermometer}
            color={colors.accents.violet}
            size="m"
          />
          <div>{i18n.absences.absenceTypes.SICKLEAVE}</div>
        </FixedSpaceRow>
      </AbsenceCell>
    )

  return (
    <AbsenceCell>
      <FixedSpaceRow spacing="xs" alignItems="center">
        <RoundIcon content="–" color={colors.main.primary} size="m" />
        <div>
          {i18n.absences.absenceTypes[type] ??
            i18n.absences.absenceTypes.OTHER_ABSENCE}
        </div>
      </FixedSpaceRow>
    </AbsenceCell>
  )
})

const AbsenceCell = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  font-style: italic;
`
