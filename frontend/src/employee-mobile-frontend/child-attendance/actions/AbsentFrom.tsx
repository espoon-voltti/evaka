// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { AbsenceCategory } from 'lib-common/generated/api-types/daycare'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import Title from 'lib-components/atoms/Title'
import { fontWeights } from 'lib-components/typography'

import { CustomHorizontalLine } from '../../common/components'
import { useTranslation } from '../../common/i18n'
import { formatCategory } from '../../types'

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
  placementType: PlacementType
  absentFrom: AbsenceCategory[]
}

export function AbsentFrom({ placementType, absentFrom }: AbsentFromProps) {
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
            {formatCategory(category, placementType, i18n)}
          </div>
        ))}
      </InfoText>
    </AbsentFromWrapper>
  )
}
