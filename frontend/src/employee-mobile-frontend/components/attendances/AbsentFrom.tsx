// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Child } from 'lib-common/generated/api-types/attendance'
import { AbsenceCategory } from 'lib-common/generated/api-types/daycare'
import Title from 'lib-components/atoms/Title'
import { fontWeights } from 'lib-components/typography'
import { useTranslation } from '../../state/i18n'
import { formatCategory } from '../../types'
import { CustomHorizontalLine } from './components'

const AbsentFromWrapper = styled.div`
  display: flex;
  flex-direction: column;
`

const AbsenceTitle = styled(Title)`
  font-size: 18px;
  font-style: normal;
  font-weight: ${fontWeights.medium};
  line-height: 27px;
  letter-spacing: 0;
  text-align: left;
  margin-top: 0;
  margin-bottom: 0;
`

const InfoText = styled.div``

interface AbsentFromProps {
  child: Child
  absentFrom: AbsenceCategory[]
}

export function AbsentFrom({ child, absentFrom }: AbsentFromProps) {
  const { i18n } = useTranslation()

  return (
    <AbsentFromWrapper>
      <CustomHorizontalLine />
      <AbsenceTitle size={2}>{i18n.attendances.absenceTitle}</AbsenceTitle>
      <InfoText>
        {absentFrom.length > 1
          ? i18n.attendances.missingFromPlural
          : i18n.attendances.missingFrom}
        :{' '}
        {absentFrom.map((category) => (
          <div key={category}>
            {formatCategory(category, child.placementType, i18n)}
          </div>
        ))}
      </InfoText>
    </AbsentFromWrapper>
  )
}
