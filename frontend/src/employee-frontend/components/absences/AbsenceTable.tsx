// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import React from 'react'
import { Link } from 'react-router-dom'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import Tooltip from '../../components/common/Tooltip'
import { Translations, useTranslation } from '../../state/i18n'
import { Cell, CellPart, Child } from '../../types/absence'
import AgeIndicatorIcon from '../common/AgeIndicatorIcon'
import AbsenceCellWrapper, { DisabledCell } from './AbsenceCell'
import StaffAttendance from './StaffAttendance'
import { getMonthDays, getRange, getWeekDay } from './utils'

interface AbsenceRowProps {
  child: Child
  selectedCells: Cell[]
  toggleCellSelection: (id: UUID, parts: CellPart[]) => void
  dateCols: LocalDate[]
  emptyCols: number[]
  operationDays: LocalDate[]
  i18n: Translations
}

const shortChildName = (
  firstName: string,
  lastName: string,
  i18n: Translations
) => {
  const firstNames = firstName.split(/\s/)
  return lastName && firstName
    ? `${lastName}, ${firstNames[0]}`
    : i18n.common.noName
}

function getEmptyCols(dateColsLength: number): number[] {
  return getRange(31 - dateColsLength)
}

const AbsenceTableRow = React.memo(function AbsenceTableRow({
  selectedCells,
  toggleCellSelection,
  child,
  dateCols,
  emptyCols,
  operationDays,
  i18n
}: AbsenceRowProps) {
  const { id, placements, absences, backupCares } = child

  return (
    <tr data-qa="absence-child-row">
      <td className={'absence-child-name hover-highlight'}>
        <FixedSpaceRow spacing="xs">
          <AgeIndicatorIcon dateOfBirth={child.dob} />
          <Tooltip
            tooltipId={`tooltip_absence-child-name-${child.id}`}
            tooltipText={`${child.lastName}, ${child.firstName}`}
            place={'top'}
            delayShow={750}
          >
            <Link
              to={`/child-information/${child.id}`}
              data-qa="absence-child-link"
            >
              {shortChildName(child.firstName, child.lastName, i18n)}
            </Link>
          </Tooltip>
        </FixedSpaceRow>
      </td>
      {dateCols.map((date) => {
        return operationDays.some((operationDay) =>
          operationDay.isEqual(date)
        ) ? (
          <td
            key={`${id}${date.formatIso()}`}
            className={`${
              date.isToday() ? 'absence-cell-today' : ''
            } hover-highlight absence-cell-wrapper`}
            data-qa={'absence-cell'}
          >
            <AbsenceCellWrapper
              selectedCells={selectedCells}
              toggleCellSelection={toggleCellSelection}
              careTypes={placements[date.formatIso()]}
              absences={absences[date.formatIso()]}
              backupCare={backupCares[date.formatIso()]}
              date={date}
              childId={id}
            />
          </td>
        ) : (
          <td key={`${id}${date.formatIso()}`}>
            <DisabledCell />
          </td>
        )
      })}
      {emptyCols.map((item) => (
        <td key={item}>
          <DisabledCell />
        </td>
      ))}
    </tr>
  )
})

interface AbsenceHeadProps {
  dateCols: LocalDate[]
  emptyCols: number[]
  operationDays: LocalDate[]
}

const AbsenceTableHead = React.memo(function AbsenceTableHead({
  dateCols,
  emptyCols,
  operationDays
}: AbsenceHeadProps) {
  const { i18n } = useTranslation()
  return (
    <thead>
      <tr>
        <th>{i18n.absences.table.nameCol}</th>
        {dateCols.map((item) =>
          operationDays.some((operationDay) => operationDay.isEqual(item)) ? (
            <th
              key={item.getDate()}
              className={classNames({
                'absence-header': true,
                'absence-header-today': item.isToday(),
                'absence-header-weekend': item.isWeekend()
              })}
            >
              <div>{getWeekDay(item)}</div>
              <div>{item.getDate()}</div>
            </th>
          ) : (
            <th key={item.getDate()} />
          )
        )}
        {emptyCols.map((item) => (
          <th key={item} />
        ))}
      </tr>
    </thead>
  )
})

interface AbsenceTableProps {
  groupId: string
  selectedCells: Cell[]
  toggleCellSelection: (id: UUID, parts: CellPart[]) => void
  selectedDate: LocalDate
  childList: Child[]
  operationDays: LocalDate[]
}

export default React.memo(function AbsenceTable({
  groupId,
  selectedCells,
  toggleCellSelection,
  selectedDate,
  childList,
  operationDays
}: AbsenceTableProps) {
  const { i18n } = useTranslation()

  const dateColsBody =
    childList.length > 0
      ? Object.keys(childList[0].placements)
          .sort()
          .map((date) => LocalDate.parseIso(date))
      : []

  const dateColsHead =
    dateColsBody.length > 0 ? dateColsBody : getMonthDays(selectedDate)

  const emptyCols = getEmptyCols(dateColsHead.length)

  const renderEmptyRow = () => (
    <tr>
      <td className={'empty-row'} />
    </tr>
  )

  return (
    <table className="table">
      <AbsenceTableHead
        dateCols={dateColsHead}
        emptyCols={emptyCols}
        operationDays={operationDays}
      />
      <tbody>
        {childList.map((item) => (
          <AbsenceTableRow
            key={item.id}
            selectedCells={selectedCells}
            toggleCellSelection={toggleCellSelection}
            child={item}
            dateCols={dateColsBody}
            emptyCols={emptyCols}
            operationDays={operationDays}
            i18n={i18n}
          />
        ))}
        {renderEmptyRow()}
        <StaffAttendance
          groupId={groupId}
          selectedDate={selectedDate}
          emptyCols={emptyCols}
          operationDays={operationDays}
        />
      </tbody>
    </table>
  )
})
