// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled from 'styled-components'

import type { ChildAbsence } from 'lib-common/generated/api-types/attendance'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../common/i18n'
import { formatCategory } from '../types'

const AbsenceLabels = styled.div`
  text-align: center;
`

interface Props {
  absences: ChildAbsence[]
  placementType: PlacementType
}

export default React.memo(function Absences({
  absences,
  placementType
}: Props) {
  const { i18n } = useTranslation()

  const absenceCareTypes = useMemo(
    () =>
      absences
        .map(({ category }) => formatCategory(category, placementType, i18n))
        .join(', '),
    [absences, i18n, placementType]
  )
  if (absences.length === 0) return null

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
