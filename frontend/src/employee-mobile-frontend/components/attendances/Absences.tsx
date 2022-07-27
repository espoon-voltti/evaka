// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { Child } from 'lib-common/generated/api-types/attendance'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { formatCategory } from '../../types'

const AbsenceLabels = styled.div`
  text-align: center;
`

interface Props {
  child: Child
}

export default React.memo(function Absences({ child }: Props) {
  const { i18n } = useTranslation()

  if (child.absences.length === 0) return null

  const absenceCareTypes = child.absences
    .map(({ category }) => formatCategory(category, child.placementType, i18n))
    .join(', ')
  return (
    <>
      <HorizontalLine slim />

      <AbsenceLabels>
        <Label>{i18n.absences.title}</Label>{' '}
        <span data-qa="absence">{absenceCareTypes}</span>
      </AbsenceLabels>
    </>
  )
})
