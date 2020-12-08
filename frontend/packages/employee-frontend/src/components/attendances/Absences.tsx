// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
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

const Circle = styled.span<{ gray?: boolean }>`
  background-color: ${(p) =>
    p.gray ? Colors.greyscale.lighter : Colors.accents.orange};
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

  const preschoolAbsences =
    attendanceChild.placementType === 'PRESCHOOL'
      ? attendanceChild.absences.filter(
          (absence) => absence.careType === 'PRESCHOOL'
        )
      : undefined

  const preschoolDaycareAbsences =
    attendanceChild.placementType === 'PRESCHOOL_DAYCARE'
      ? attendanceChild.absences.filter(
          (absence) =>
            absence.careType === 'PRESCHOOL_DAYCARE' ||
            absence.careType === 'PRESCHOOL'
        )
      : undefined

  const preparatoryAbsences =
    attendanceChild.placementType === 'PREPARATORY'
      ? attendanceChild.absences.filter(
          (absence) => absence.careType === 'PRESCHOOL'
        )
      : undefined

  const preparatoryDaycareAbsences =
    attendanceChild.placementType === 'PREPARATORY_DAYCARE'
      ? attendanceChild.absences.filter(
          (absence) =>
            absence.careType === 'PRESCHOOL_DAYCARE' ||
            absence.careType === 'PRESCHOOL'
        )
      : undefined

  const daycareAbsences =
    attendanceChild.placementType === 'DAYCARE' ||
    attendanceChild.placementType === 'DAYCARE_PART_TIME'
      ? attendanceChild.absences.filter(
          (absence) => absence.careType === 'DAYCARE'
        )
      : undefined

  return (
    <FixedSpaceColumn>
      {attendanceChild.absences.length > 0 && (
        <CustomLabel>{i18n.absences.title}</CustomLabel>
      )}

      {preschoolAbsences && preschoolAbsences.length === 0 ? (
        <AttendanceItem>
          <Circle gray />

          <span>
            <Label>{i18n.common.types.PRESCHOOL}</Label>
            <AbsenceTypeLabel>{i18n.attendances.noAbsences}</AbsenceTypeLabel>
          </span>
        </AttendanceItem>
      ) : (
        preschoolAbsences &&
        preschoolAbsences.map((absence) => (
          <AttendanceItem key={absence.id}>
            <Circle />

            <span>
              <Label>{i18n.common.types[absence.careType]}</Label>
              <AbsenceTypeLabel>{i18n.absences.absence}</AbsenceTypeLabel>
            </span>
          </AttendanceItem>
        ))
      )}

      {preschoolDaycareAbsences && preschoolDaycareAbsences.length === 0 ? (
        <Fragment>
          <AttendanceItem>
            <Circle gray />

            <span>
              <Label>{i18n.common.types.PRESCHOOL}</Label>
              <AbsenceTypeLabel>{i18n.attendances.noAbsences}</AbsenceTypeLabel>
            </span>
          </AttendanceItem>
          <AttendanceItem>
            <Circle gray />

            <span>
              <Label>{i18n.common.types.PRESCHOOL_DAYCARE}</Label>
              <AbsenceTypeLabel>{i18n.attendances.noAbsences}</AbsenceTypeLabel>
            </span>
          </AttendanceItem>
        </Fragment>
      ) : (
        preschoolDaycareAbsences &&
        preschoolDaycareAbsences.map((absence) => (
          <AttendanceItem key={absence.id}>
            <Circle />
            <span>
              <Label>{i18n.common.types[absence.careType]}</Label>
              <AbsenceTypeLabel>{i18n.absences.absence}</AbsenceTypeLabel>
            </span>
          </AttendanceItem>
        ))
      )}

      {daycareAbsences && daycareAbsences.length === 0 ? (
        <AttendanceItem>
          <Circle gray />

          <span>
            <Label>{i18n.common.types.PRESCHOOL_DAYCARE}</Label>
            <AbsenceTypeLabel>{i18n.attendances.noAbsences}</AbsenceTypeLabel>
          </span>
        </AttendanceItem>
      ) : (
        daycareAbsences &&
        daycareAbsences.map((absence) => (
          <AttendanceItem key={absence.id}>
            <Circle />
            <span>
              <Label>{i18n.common.types[absence.careType]}</Label>
              <AbsenceTypeLabel>{i18n.absences.absence}</AbsenceTypeLabel>
            </span>
          </AttendanceItem>
        ))
      )}

      {preparatoryAbsences && preparatoryAbsences.length === 0 ? (
        <AttendanceItem>
          <Circle gray />

          <span>
            <Label>{i18n.common.types.PREPARATORY_EDUCATION}</Label>
            <AbsenceTypeLabel>{i18n.attendances.noAbsences}</AbsenceTypeLabel>
          </span>
        </AttendanceItem>
      ) : (
        preparatoryAbsences &&
        preparatoryAbsences.map((absence) => (
          <AttendanceItem key={absence.id}>
            <Circle />

            <span>
              <Label>{i18n.common.types[absence.careType]}</Label>
              <AbsenceTypeLabel>{i18n.absences.absence}</AbsenceTypeLabel>
            </span>
          </AttendanceItem>
        ))
      )}

      {preparatoryDaycareAbsences && preparatoryDaycareAbsences.length === 0 ? (
        <AttendanceItem>
          <Circle gray />

          <span>
            <Label>{i18n.common.types.PREPARATORY_DAYCARE}</Label>
            <AbsenceTypeLabel>{i18n.attendances.noAbsences}</AbsenceTypeLabel>
          </span>
        </AttendanceItem>
      ) : (
        preparatoryDaycareAbsences &&
        preparatoryDaycareAbsences.map((absence) => (
          <AttendanceItem key={absence.id}>
            <Circle />
            <span>
              <Label>{i18n.common.types[absence.careType]}</Label>
              <AbsenceTypeLabel>{i18n.absences.absence}</AbsenceTypeLabel>
            </span>
          </AttendanceItem>
        ))
      )}
    </FixedSpaceColumn>
  )
})
