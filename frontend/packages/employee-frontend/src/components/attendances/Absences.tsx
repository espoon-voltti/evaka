// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { AttendanceChild } from '~api/attendances'
import Colors from '~components/shared/Colors'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import { Label } from '~components/shared/Typography'
import { useTranslation } from '~state/i18n'

const AttendanceItem = styled.div`
  display: flex;
  align-items: center;
`

const Circle = styled.span`
  background-color: ${Colors.accents.orange};
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
  color: ${Colors.greyscale.dark};
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

  return (
    <FixedSpaceColumn>
      {attendanceChild.absences.length > 0 && (
        <CustomLabel>{i18n.absences.title}</CustomLabel>
      )}
      {attendanceChild.absences
        .filter((absence) => absence.careType === 'PRESCHOOL')
        .map((absence) => (
          <AttendanceItem key={absence.id}>
            <Circle />

            <span>
              <Label>{i18n.common.types.PRESCHOOL}</Label>
              <AbsenceTypeLabel>
                {i18n.absences.absenceTypes[absence.absenceType]}
              </AbsenceTypeLabel>
            </span>
          </AttendanceItem>
        ))}
      {attendanceChild.absences
        .filter((absence) => absence.careType === 'PRESCHOOL_DAYCARE')
        .map((absence) => (
          <AttendanceItem key={absence.id}>
            <Circle />
            <span>
              <Label>{i18n.common.types.PRESCHOOL_DAYCARE}</Label>
              <AbsenceTypeLabel>
                {i18n.absences.absenceTypes[absence.absenceType]}
              </AbsenceTypeLabel>
            </span>
          </AttendanceItem>
        ))}
    </FixedSpaceColumn>
  )
})
