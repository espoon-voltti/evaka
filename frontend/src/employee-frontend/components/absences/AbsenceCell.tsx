// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'
import LocalDate from 'lib-common/local-date'
import {
  AbsenceBackupCare,
  billableCareTypes,
  Cell,
  CellPart
} from '../../types/absence'
import Tooltip from '../../components/common/Tooltip'
import { useTranslation } from '../../state/i18n'
import { Absence } from 'lib-common/api-types/child/Absences'
import { AbsenceCareType, AbsenceType } from 'lib-common/generated/enums'
import { UUID } from 'lib-common/types'

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
  selectedCells: Cell[]
  toggleCellSelection: (id: UUID, parts: CellPart[]) => void
  date: LocalDate
  careTypes: AbsenceCareType[]
  absences: Absence[]
  backupCare: AbsenceBackupCare | null
}

const getCellId = (childId: UUID, date: LocalDate) =>
  `${childId}-${date.formatIso()}`

function getCellParts(
  childId: UUID,
  date: LocalDate,
  careTypes: AbsenceCareType[],
  absences: Absence[],
  backupCare: AbsenceBackupCare | null
): CellPart[] {
  function placementAbsence(placement: AbsenceCareType): Absence | undefined {
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

const Cell = React.memo(function Cell({
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
  const parts = useMemo(
    () => getCellParts(childId, date, careTypes, absences, backupCare),
    [absences, backupCare, careTypes, childId, date]
  )
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

export default React.memo(function AbsenceCell({
  childId,
  selectedCells,
  toggleCellSelection,
  date,
  careTypes,
  absences,
  backupCare
}: AbsenceCellProps) {
  const { i18n } = useTranslation()

  const id = useMemo(() => getCellId(childId, date), [childId, date])
  const isSelected = useMemo(
    () => selectedCells.some(({ id: selectedId }) => selectedId === id),
    [id, selectedCells]
  )

  const tooltipText = useMemo(
    () =>
      absences
        .map(
          ({ careType, absenceType }) =>
            `${i18n.absences.careTypes[careType]}: ${i18n.absences.absenceTypes[absenceType]}`
        )
        .join('<br/>'),
    [absences, i18n]
  )

  const toggle = useCallback(
    (cellParts: CellPart[]) => toggleCellSelection(id, cellParts),
    [id, toggleCellSelection]
  )

  return (
    <Tooltip
      tooltipId={`tooltip_absence-${date.formatIso()}-${childId}`}
      tooltipText={tooltipText}
      place={'top'}
      className={'absence-tooltip'}
      delayShow={750}
    >
      <Cell
        id={id}
        childId={childId}
        selectedCells={selectedCells}
        toggleCellSelection={toggleCellSelection}
        date={date}
        careTypes={careTypes}
        absences={absences}
        backupCare={backupCare}
        isSelected={isSelected}
        toggle={toggle}
      />
    </Tooltip>
  )
})
