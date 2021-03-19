// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useCallback, memo } from 'react'
import LocalDate from 'lib-common/local-date'
import {
  Absence,
  AbsenceType,
  CareType,
  CellPart,
  billableCareTypes,
  AbsenceBackupCare
} from '../../types/absence'
import { UUID } from '../../types'
import { AbsencesState, AbsencesContext } from '../../state/absence'
import Tooltip from '../../components/common/Tooltip'
import { useTranslation } from '../../state/i18n'

interface AbsenceCellPartProps {
  position: string
  absenceType?: AbsenceType
  isWeekend: boolean
}

function AbsenceCellPart({
  position,
  absenceType,
  isWeekend
}: AbsenceCellPartProps) {
  const specificClass =
    typeof absenceType !== 'undefined'
      ? `absence-cell-${position}-${absenceType}`
      : isWeekend
      ? `absence-cell-${position}-weekend`
      : `absence-cell-${position}-empty`
  return <div className={`absence-cell-${position} ${specificClass}`} />
}

interface AbsenceCellProps {
  childId: UUID
  date: LocalDate
  careTypes: CareType[]
  absences: Absence[]
  backupCare: AbsenceBackupCare | null
}

const getCellId = (childId: UUID, date: LocalDate) =>
  `${childId}-${date.formatIso()}`

function getCellParts(
  childId: UUID,
  date: LocalDate,
  careTypes: CareType[],
  absences: Absence[],
  backupCare: AbsenceBackupCare | null
): CellPart[] {
  function placementAbsence(placement: CareType): Absence | undefined {
    return absences.find(({ careType }) => careType === placement)
  }

  return careTypes.map((careType) => {
    const position = billableCareTypes.includes(careType) ? 'right' : 'left'
    if (backupCare) {
      return {
        id: `${childId}-${date.formatIso()}-${careType}-backup-care`,
        childId,
        date,
        careType,
        absenceType: 'TEMPORARY_RELOCATION',
        position
      }
    }
    const maybeAbsence = placementAbsence(careType)
    if (maybeAbsence) {
      const { id: absenceId, absenceType } = maybeAbsence
      return {
        id: absenceId,
        childId,
        date,
        careType,
        absenceType,
        position
      }
    } else {
      return {
        id: `${childId}-${date.formatIso()}-${careType}`,
        childId,
        date,
        careType,
        position
      }
    }
  })
}

export function DisabledCell() {
  return <div className="absence-cell absence-cell-disabled" />
}

const MemoizedCell = memo(function AbsenceCell({
  childId,
  date,
  careTypes,
  absences,
  backupCare,
  isSelected,
  toggle
}: AbsenceCellProps & {
  id: UUID
  isSelected: boolean
  toggle: (parts: CellPart[]) => void
}) {
  const parts = getCellParts(childId, date, careTypes, absences, backupCare)
  if (parts.length === 0) {
    return <DisabledCell />
  }

  const clickable = !backupCare
  return (
    <div
      className={`absence-cell ${isSelected ? 'absence-cell-selected' : ''}`}
      onClick={clickable ? () => toggle(parts) : undefined}
    >
      {parts.map(({ id: partId, absenceType, position }) => (
        <AbsenceCellPart
          position={position}
          absenceType={absenceType}
          key={partId}
          isWeekend={date.isWeekend()}
        />
      ))}
    </div>
  )
})

function AbsenceCellWrapper({
  childId,
  date,
  careTypes,
  absences,
  backupCare
}: AbsenceCellProps) {
  const { selectedCells, toggleCellSelection } = useContext<AbsencesState>(
    AbsencesContext
  )
  const { i18n } = useTranslation()

  const id = getCellId(childId, date)
  const isSelected = selectedCells.some(
    ({ id: selectedId }) => selectedId === id
  )

  const tooltipText = absences
    .map(
      ({ careType, absenceType }) =>
        `${i18n.absences.careTypes[careType]}: ${i18n.absences.absenceTypes[absenceType]}`
    )
    .join('<br/>')

  return (
    <Tooltip
      tooltipId={`tooltip_absence-${date.formatIso()}-${childId}`}
      tooltipText={tooltipText}
      place={'top'}
      className={'absence-tooltip'}
      delayShow={750}
    >
      <MemoizedCell
        id={id}
        childId={childId}
        date={date}
        careTypes={careTypes}
        absences={absences}
        backupCare={backupCare}
        isSelected={isSelected}
        toggle={useCallback(toggleCellSelection(id), [id])}
      />
    </Tooltip>
  )
}

export default AbsenceCellWrapper
