// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import Title from 'lib-components/atoms/Title'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { useTranslation } from '../../state/i18n'
import { formatCareType } from '../../types'
import { Child } from 'lib-common/generated/api-types/attendance'

const CustomLabel = styled(Label)`
  min-width: 150px;
  display: inline-block;
`

const AbsencesTitle = styled(Title)`
  font-size: 18px;
`

interface ChildListItemProps {
  child: Child
}

export default React.memo(function Absences({ child }: ChildListItemProps) {
  const { i18n } = useTranslation()

  if (child.absences.length === 0) return null

  return (
    <FixedSpaceColumn>
      <AbsencesTitle size={2}>{i18n.absences.title}</AbsencesTitle>

      {child.absences.map((absence) => (
        <div key={absence.id} data-qa="absence">
          <CustomLabel>
            {formatCareType(absence.careType, child.placementType, i18n)}
          </CustomLabel>
          <span>{i18n.absences.absence}</span>
        </div>
      ))}
    </FixedSpaceColumn>
  )
})
