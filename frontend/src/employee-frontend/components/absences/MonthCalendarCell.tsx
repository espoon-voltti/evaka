// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'
import styled, { css } from 'styled-components'

import {
  AbsenceCategory,
  AbsenceWithModifierInfo,
  GroupMonthCalendarDayChild
} from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Tooltip from 'lib-components/atoms/Tooltip'
import { absenceColors } from 'lib-customizations/common'
import colors from 'lib-customizations/common'

import { CellPart } from '../../types/absence'

import { SelectedCell } from './GroupMonthCalendar'
import UnitCalendarDayCellTooltip from './UnitCalendarDayCellTooltip'

const cellSize = 20

const AbsenceCellPart = styled.div<{
  $position: 'left' | 'right'
  $absenceType: CellPart['absenceType']
  $isWeekend: boolean
  $isSelected: boolean
}>`
  position: absolute;
  height: ${cellSize}px;
  width: ${cellSize}px;
  border-style: solid;
  border-color: transparent;

  ${(p) =>
    !p.$isSelected &&
    css`
      border-width: ${partBorderWidth(p.$position)};
      ${partColor(p.$position, p.$absenceType, p.$isWeekend)};
    `};
`

function partBorderWidth(position: 'left' | 'right'): string {
  return position === 'left'
    ? `${cellSize}px ${cellSize}px 0 0`
    : `0 0 ${cellSize}px ${cellSize}px`
}

function partColor(
  position: 'left' | 'right',
  absenceType: CellPart['absenceType'],
  isWeekend: boolean
): string {
  const pos = position === 'left' ? 'top' : 'bottom'
  const color =
    absenceType !== undefined
      ? absenceColors[absenceType]
      : isWeekend
      ? colors.grayscale.g15
      : absenceColors.NO_ABSENCE
  return `border-${pos}-color: ${color}`
}

const getCellId = (childId: UUID, date: LocalDate) =>
  `${childId}-${date.formatIso()}`

function getCellParts(
  childId: UUID,
  date: LocalDate,
  categories: AbsenceCategory[],
  absences: AbsenceWithModifierInfo[] | undefined,
  backupCare: boolean
): CellPart[] {
  return categories.map((category) => {
    const position = category === 'BILLABLE' ? 'right' : 'left'
    if (backupCare) {
      return {
        id: `${childId}-${date.formatIso()}-${category}-backup-care`,
        childId,
        date,
        category,
        absenceType: 'TEMPORARY_RELOCATION',
        position
      }
    }
    const maybeAbsence = absences?.find(
      (absence) => absence.category === category
    )
    if (maybeAbsence) {
      return {
        childId,
        date,
        category,
        absenceType: maybeAbsence.absenceType,
        position
      }
    } else {
      return {
        id: `${childId}-${date.formatIso()}-${category}`,
        childId,
        date,
        category,
        absenceType: undefined,
        position
      }
    }
  })
}

interface AbsenceCellPartsProps {
  id: UUID
  childId: UUID
  date: LocalDate
  categories: AbsenceCategory[] | null
  absences: AbsenceWithModifierInfo[]
  backupCare: boolean
  isSelected: boolean
  isMissingHolidayReservation: boolean
  toggle: (parts: CellPart[]) => void
}

const AbsenceCellParts = React.memo(function AbsenceCellParts({
  childId,
  date,
  categories,
  absences,
  backupCare,
  isSelected,
  isMissingHolidayReservation,
  toggle
}: AbsenceCellPartsProps) {
  const parts = useMemo(
    () =>
      categories
        ? getCellParts(childId, date, categories, absences, backupCare)
        : [],
    [absences, backupCare, categories, childId, date]
  )
  if (parts.length === 0) {
    return <DisabledCell />
  }

  const clickable = !backupCare
  return (
    <AbsenceCellDiv
      $isSelected={isSelected}
      onClick={clickable ? () => toggle(parts) : undefined}
    >
      {parts.map(({ absenceType, position }) => (
        <AbsenceCellPart
          key={position}
          $isSelected={isSelected}
          $position={position}
          $absenceType={absenceType}
          $isWeekend={date.isWeekend()}
          data-position={position}
          data-absence-type={absenceType}
        />
      ))}
      {!isSelected && isMissingHolidayReservation ? (
        <MissingHolidayReservationMarker data-qa="missing-holiday-reservation" />
      ) : null}
    </AbsenceCellDiv>
  )
})

export const DisabledCell = styled.div`
  position: relative;
  height: ${cellSize}px;
  width: ${cellSize}px;
`

const AbsenceCellDiv = styled(DisabledCell)<{ $isSelected: boolean }>`
  ${(p) =>
    p.$isSelected &&
    css`
      border: 2px solid ${colors.main.m2};
      border-radius: 2px;
    `};
`

const MissingHolidayReservationMarker = styled.div`
  position: absolute;
  height: ${cellSize}px;
  width: ${cellSize}px;
  border: 2px solid ${colors.status.warning};
`

interface MonthCalendarCellProps {
  date: LocalDate
  childId: UUID
  day: GroupMonthCalendarDayChild
  selectedCells: SelectedCell[]
  toggleCellSelection: (cell: SelectedCell) => void
}

export default React.memo(function MonthCalendarCell({
  date,
  childId,
  day,
  selectedCells,
  toggleCellSelection
}: MonthCalendarCellProps) {
  const id = useMemo(() => getCellId(childId, date), [childId, date])
  const { absenceCategories, missingHolidayReservation } = day

  const isSelected = useMemo(
    () =>
      selectedCells.some(
        (cell) => cell.childId === childId && cell.date.isEqual(date)
      ),
    [childId, date, selectedCells]
  )

  const toggle = useCallback(
    () =>
      toggleCellSelection({
        childId,
        date,
        absenceCategories,
        missingHolidayReservation
      }),
    [
      absenceCategories,
      childId,
      date,
      missingHolidayReservation,
      toggleCellSelection
    ]
  )

  const hasTooltip =
    day.backupCare ||
    day.missingHolidayReservation ||
    day.absences.length > 0 ||
    day.reservations.length > 0 ||
    day.dailyServiceTimes !== null

  return (
    <Tooltip
      tooltip={
        hasTooltip ? (
          <UnitCalendarDayCellTooltip
            date={date}
            absences={day.absences}
            dailyServiceTimes={day.dailyServiceTimes}
            reservations={day.reservations}
            backupCare={day.backupCare}
            isMissingHolidayReservation={day.missingHolidayReservation}
          />
        ) : undefined
      }
      position="top"
      width="large"
      data-qa="absence-cell-tooltip"
    >
      <AbsenceCellParts
        id={id}
        childId={childId}
        date={date}
        categories={day.absenceCategories}
        absences={day.absences}
        backupCare={day.backupCare}
        isSelected={isSelected}
        isMissingHolidayReservation={day.missingHolidayReservation}
        toggle={toggle}
      />
    </Tooltip>
  )
})
