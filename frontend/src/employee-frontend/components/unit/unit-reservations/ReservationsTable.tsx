// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import { sortBy } from 'lodash'
import React, { useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import styled, { css } from 'styled-components'

import { Result } from 'lib-common/api'
import {
  Child,
  ChildDailyRecords,
  OperationalDay
} from 'lib-common/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { fontWeights, H4 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCheck } from 'lib-icons'

import EllipsisMenu from '../../../components/common/EllipsisMenu'
import { Translations, useTranslation } from '../../../state/i18n'
import { formatName } from '../../../utils'
import { AgeIndicatorIcon } from '../../common/AgeIndicatorIcon'

import ChildDay from './ChildDay'
import { useUnitReservationEditState } from './reservation-table-edit-state'

interface Props {
  operationalDays: OperationalDay[]
  allDayRows: ChildDailyRecords[]
  onMakeReservationForChild: (child: Child) => void
  selectedDate: LocalDate
  reloadReservations: () => void
}

export default React.memo(function ReservationsTable(props: Props) {
  const { operationalDays, onMakeReservationForChild, selectedDate } = props
  const { i18n, lang } = useTranslation()
  const {
    editState,
    stopEditing,
    startEditing,
    deleteAbsence,
    updateReservation,
    saveReservation
  } = useUnitReservationEditState(props.allDayRows, props.reloadReservations)

  useEffect(stopEditing, [stopEditing, selectedDate])

  const allDayRows = useMemo(
    () =>
      sortBy(
        props.allDayRows,
        ({ child }) => child.lastName,
        ({ child }) => child.firstName
      ),
    [props.allDayRows]
  )

  return (
    <Table>
      <Thead>
        <Tr>
          <CustomTh>{i18n.unit.attendanceReservations.childName}</CustomTh>
          {operationalDays.map(({ date, isHoliday }) => (
            <DateTh shrink key={date.formatIso()} faded={isHoliday}>
              <Date centered highlight={date.isToday()}>
                {date.format('EEEEEE dd.MM.', lang)}
              </Date>
              <DayHeader>
                <span>{i18n.unit.attendanceReservations.startTime}</span>
                <span>{i18n.unit.attendanceReservations.endTime}</span>
              </DayHeader>
            </DateTh>
          ))}
          <CustomTh shrink />
        </Tr>
      </Thead>
      <Tbody>
        {allDayRows.flatMap(({ child, dailyData }) => {
          const multipleRows = dailyData.length > 1
          return dailyData.map((childDailyRecordRow, index) => {
            const childEditState =
              editState?.childId === child.id &&
              editState.date.isEqual(selectedDate)
                ? editState
                : undefined

            return (
              <DayTr
                key={`${child.id}-${index}`}
                data-qa={`reservation-row-child-${child.id}`}
              >
                <NameTd partialRow={multipleRows} rowIndex={index}>
                  {index == 0 && (
                    <ChildName>
                      <AgeIndicatorIcon
                        isUnder3={
                          selectedDate.differenceInYears(child.dateOfBirth) < 3
                        }
                      />
                      <Link to={`/child-information/${child.id}`}>
                        {formatName(
                          child.firstName.split(/\s/)[0],
                          child.lastName,
                          i18n,
                          true
                        )}
                      </Link>
                    </ChildName>
                  )}
                </NameTd>
                {operationalDays.map((day) => (
                  <DayTd
                    key={day.date.formatIso()}
                    className={classNames({ 'is-today': day.date.isToday() })}
                    partialRow={multipleRows}
                    rowIndex={index}
                  >
                    <ChildDay
                      day={day}
                      dailyServiceTimes={child.dailyServiceTimes}
                      dataForAllDays={childDailyRecordRow}
                      rowIndex={index}
                      editState={childEditState}
                      deleteAbsence={deleteAbsence}
                      updateReservation={updateReservation}
                      saveReservation={saveReservation}
                    />
                  </DayTd>
                ))}
                {index === 0 && (
                  <StyledTd
                    partialRow={multipleRows}
                    rowIndex={index}
                    rowSpan={multipleRows ? 2 : 1}
                  >
                    {childEditState ? (
                      <EditStateIndicator
                        status={childEditState.request}
                        stopEditing={stopEditing}
                      />
                    ) : (
                      <RowMenu
                        i18n={i18n}
                        child={child}
                        selectedDate={selectedDate}
                        startEditing={startEditing}
                        openReservationModal={onMakeReservationForChild}
                      />
                    )}
                  </StyledTd>
                )}
              </DayTr>
            )
          })
        })}
      </Tbody>
    </Table>
  )
})

const EditStateIndicator = React.memo(function EditStateIndicator({
  status,
  stopEditing
}: {
  status: Result<void>
  stopEditing: () => void
}) {
  return (
    <IconButton
      icon={faCheck}
      onClick={stopEditing}
      disabled={status.isLoading}
    />
  )
})

const RowMenu = React.memo(function RowMenu({
  i18n,
  child,
  selectedDate,
  startEditing,
  openReservationModal
}: {
  i18n: Translations
  child: Child
  selectedDate: LocalDate
  startEditing: (c: string, d: LocalDate) => void
  openReservationModal: (c: Child) => void
}) {
  return (
    <EllipsisMenu
      items={[
        {
          id: 'edit-row',
          label: i18n.unit.attendanceReservations.editRow,
          onClick: () => startEditing(child.id, selectedDate)
        },
        {
          id: 'reservation-modal',
          label: i18n.unit.attendanceReservations.openReservationModal,
          onClick: () => openReservationModal(child)
        }
      ]}
      data-qa={`ellipsis-menu-${child.id}`}
    />
  )
})

const CustomTh = styled(Th)<{ shrink?: boolean }>`
  vertical-align: bottom;
  ${(p) =>
    p.shrink &&
    css`
      width: 0; // causes the column to take as little space as possible
    `}
`

const DateTh = styled(CustomTh)<{ faded: boolean }>`
  ${(p) => p.faded && `color: ${colors.grayscale.g35};`}
`

const DayHeader = styled.div`
  display: flex;
  justify-content: space-evenly;
  gap: ${defaultMargins.xs};
`

const StyledTd = styled(Td)<{ partialRow: boolean; rowIndex: number }>`
  border-right: 1px solid ${colors.grayscale.g15};
  vertical-align: middle;
  ${(p) => p.partialRow && p.rowIndex === 0 && `border-bottom-style: dashed;`}
  ${(p) => p.partialRow && p.rowIndex > 0 && `border-top-style: dashed;`}
`

const DayTd = styled(StyledTd)`
  padding: 0;
`

const DayTr = styled(Tr)`
  > ${DayTd}.is-today {
    border-left: 2px solid ${colors.status.success};
    border-right: 2px solid ${colors.status.success};
  }

  &:first-child {
    > ${DayTd}.is-today {
      border-top: 2px solid ${colors.status.success};
    }
  }
  &:last-child {
    > ${DayTd}.is-today {
      border-bottom: 2px solid ${colors.status.success};
    }
  }
`

const NameTd = styled(StyledTd)`
  width: 350px;
  ${(p) => p.partialRow && p.rowIndex === 0 && `border-bottom-style: none;`}
  ${(p) => p.partialRow && p.rowIndex > 0 && `border-top-style: none;`}
`

const ChildName = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: flex-start;
  align-items: center;

  a {
    margin-left: ${defaultMargins.xs};
  }
`

const Date = styled(H4)<{ highlight: boolean }>`
  margin: 0 0 ${defaultMargins.s};
  ${(p) =>
    p.highlight &&
    css`
      color: ${colors.main.m2};
      font-weight: ${fontWeights.semibold};
    `}
`
