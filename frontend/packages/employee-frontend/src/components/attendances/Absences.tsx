// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { AttendanceChild } from '~api/attendances'
import colors from '@evaka/lib-components/src/colors'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import { Label } from '@evaka/lib-components/src/typography'
import { useTranslation } from '~state/i18n'
import { formatCareType } from '~types/absence'

const AttendanceItem = styled.div`
  display: flex;
  align-items: center;
`

const Circle = styled.span<{ gray?: boolean }>`
  background-color: ${(p) =>
    p.gray ? colors.greyscale.lighter : colors.accents.orange};
  width: 20px;
  height: 20px;
  border-radius: 50%;
  margin-right: 20px;
`

const AbsenceTypeLabel = styled.div`
  font-style: italic;
  font-weight: normal;
  font-size: 15px;
  line-height: 22px;
  color: ${colors.greyscale.dark};
`

const CustomLabel = styled(Label)`
  text-transform: uppercase;
`

interface ChildListItemProps {
  attendanceChild: AttendanceChild
}

export default React.memo(function Absences({
  attendanceChild
}: ChildListItemProps) {
  const { i18n } = useTranslation()

  if (attendanceChild.absences.length === 0) return null

  return (
    <FixedSpaceColumn>
      <CustomLabel>{i18n.absences.title}</CustomLabel>

      {attendanceChild.absences.map((absence) => (
        <AttendanceItem key={absence.id}>
          <Circle />

          <span>
            <Label>
              {formatCareType(
                absence.careType,
                attendanceChild.placementType,
                attendanceChild.entitledToFreeFiveYearsOldDaycare,
                i18n
              )}
            </Label>
            <AbsenceTypeLabel>{i18n.absences.absence}</AbsenceTypeLabel>
          </span>
        </AttendanceItem>
      ))}
    </FixedSpaceColumn>
  )
})
