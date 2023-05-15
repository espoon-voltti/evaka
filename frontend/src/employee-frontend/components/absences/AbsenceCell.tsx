// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useCallback, useMemo } from 'react'
import styled, { css } from 'styled-components'

import {
  AbsenceCategory,
  AbsenceWithModifierInfo,
  ChildReservation
} from 'lib-common/generated/api-types/daycare'
import { HelsinkiDateTimeRange } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Tooltip from 'lib-components/atoms/Tooltip'
import { absenceColors } from 'lib-customizations/common'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import { Cell, CellPart } from '../../types/absence'

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
      const { id: absenceId, absenceType } = maybeAbsence
      return {
        id: absenceId,
        childId,
        date,
        category,
        absenceType,
        position
      }
    } else {
      return {
        id: `${childId}-${date.formatIso()}-${category}`,
        childId,
        date,
        category,
        position
      }
    }
  })
}

interface AbsenceCellPartsProps {
  id: UUID
  childId: UUID
  date: LocalDate
  categories: AbsenceCategory[] | undefined
  absences: AbsenceWithModifierInfo[] | undefined
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
      {parts.map(({ id: partId, absenceType, position }) => (
        <AbsenceCellPart
          key={partId}
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

interface AbsenceCellProps {
  childId: UUID
  selectedCells: Cell[]
  missingHolidayReservations: LocalDate[]
  toggleCellSelection: (id: UUID, parts: CellPart[]) => void
  date: LocalDate
  categories: AbsenceCategory[] | undefined
  absences: AbsenceWithModifierInfo[] | undefined
  backupCare: boolean
  reservations: ChildReservation[]
  dailyServiceTimes: HelsinkiDateTimeRange[]
}

export default React.memo(function AbsenceCell({
  childId,
  selectedCells,
  missingHolidayReservations,
  toggleCellSelection,
  date,
  categories,
  absences = [],
  backupCare,
  reservations,
  dailyServiceTimes
}: AbsenceCellProps) {
  const { i18n } = useTranslation()

  const id = useMemo(() => getCellId(childId, date), [childId, date])
  const isMissingHolidayReservation = useMemo(
    () => missingHolidayReservations.some((d) => d.isEqual(date)),
    [date, missingHolidayReservations]
  )
  const isSelected = useMemo(
    () => selectedCells.some(({ id: selectedId }) => selectedId === id),
    [id, selectedCells]
  )

  const attendanceDayTooltipElements = useMemo(() => {
    const reservationElements = reservations.map((res, index) => {
      const reservationText =
        res.reservation.type === 'TIMES'
          ? `${
              i18n.absences.reservation
            } ${res.reservation.startTime.format()} - ${res.reservation.endTime.format()}`
          : i18n.absences.present
      const userTypeText =
        res.createdByEvakaUserType === 'CITIZEN'
          ? i18n.absences.guardian
          : i18n.absences.staff
      return (
        <Fragment key={index}>
          {reservationText}
          <br />
          {res.created.toLocalDate().format()} {userTypeText}
        </Fragment>
      )
    })
    const dailyServiceTimeElements = dailyServiceTimes.map((dst, index) => (
      <Fragment key={index}>
        {`${i18n.absences.dailyServiceTime} ${dst.start
          .toLocalTime()
          .format()} - ${dst.end.toLocalTime().format()}`}
      </Fragment>
    ))
    return reservationElements.length > 0 ||
      dailyServiceTimeElements.length > 0 ? (
      <div data-qa={`attendance-tooltip-${date.toString()}`}>
        {reservationElements}
        {reservationElements.length > 0 && <br />}
        {dailyServiceTimeElements}
      </div>
    ) : undefined
  }, [i18n, reservations, dailyServiceTimes, date])

  const tooltipText = useMemo(
    () =>
      backupCare
        ? i18n.absences.absenceTypes['TEMPORARY_RELOCATION']
        : absences.length > 0
        ? absences.map(
            ({ category, absenceType, modifiedAt, modifiedByType }, index) => (
              <Fragment key={index}>
                {index !== 0 && <br />}
                {`${i18n.absences.absenceCategories[category]}: ${i18n.absences.absenceTypes[absenceType]}`}
                <br />
                {`${modifiedAt.toLocalDate().format()} ${
                  i18n.absences.modifiedByType[modifiedByType]
                }`}
              </Fragment>
            )
          )
        : isMissingHolidayReservation
        ? i18n.absences.missingHolidayReservation
        : attendanceDayTooltipElements,
    [
      absences,
      backupCare,
      i18n,
      isMissingHolidayReservation,
      attendanceDayTooltipElements
    ]
  )

  const toggle = useCallback(
    (cellParts: CellPart[]) => toggleCellSelection(id, cellParts),
    [id, toggleCellSelection]
  )

  return (
    <Tooltip
      tooltip={tooltipText}
      position="top"
      width="large"
      data-qa="absence-cell-tooltip"
    >
      <AbsenceCellParts
        id={id}
        childId={childId}
        date={date}
        categories={categories}
        absences={absences}
        backupCare={backupCare}
        isSelected={isSelected}
        isMissingHolidayReservation={isMissingHolidayReservation}
        toggle={toggle}
      />
    </Tooltip>
  )
})
