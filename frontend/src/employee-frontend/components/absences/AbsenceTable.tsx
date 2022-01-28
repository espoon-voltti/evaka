// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React from 'react'
import { Link } from 'react-router-dom'
import styled, { css, useTheme } from 'styled-components'
import { AbsenceChild } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { fasExclamationTriangle } from 'lib-icons'
import Tooltip from '../../components/common/Tooltip'
import { Translations, useTranslation } from '../../state/i18n'
import { Cell, CellPart } from '../../types/absence'
import AgeIndicatorIcon from '../common/AgeIndicatorIcon'
import AbsenceCell, { DisabledCell } from './AbsenceCell'
import StaffAttendance from './StaffAttendance'
import { getMonthDays, getRange } from './utils'

interface AbsenceRowProps {
  absenceChild: AbsenceChild
  selectedCells: Cell[]
  toggleCellSelection: (id: UUID, parts: CellPart[]) => void
  dateCols: LocalDate[]
  emptyCols: number[]
  operationDays: LocalDate[]
  i18n: Translations
  selectedDate: LocalDate
  reservationEnabled: boolean
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
  return getRange(32 - dateColsLength)
}

const AbsenceTableRow = React.memo(function AbsenceTableRow({
  selectedCells,
  toggleCellSelection,
  absenceChild,
  dateCols,
  emptyCols,
  operationDays,
  i18n,
  selectedDate,
  reservationEnabled
}: AbsenceRowProps) {
  const theme = useTheme()
  const {
    child,
    placements,
    absences,
    backupCares,
    attendanceTotalHours,
    reservationTotalHours
  } = absenceChild

  const showAttendanceWarning =
    !!attendanceTotalHours &&
    !!reservationTotalHours &&
    attendanceTotalHours > reservationTotalHours

  return (
    <tr data-qa="absence-child-row">
      <td className="absence-child-name hover-highlight">
        <FixedSpaceRow spacing="xs">
          <AgeIndicatorIcon
            isUnder3={selectedDate.differenceInYears(child.dateOfBirth) < 3}
          />
          <Tooltip
            tooltipId={`tooltip_absence-child-name-${child.id}`}
            tooltipText={`${child.lastName}, ${child.firstName}`}
            place="top"
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
            key={`${child.id}${date.formatIso()}`}
            className={`${
              date.isToday() ? 'absence-cell-today' : ''
            } hover-highlight absence-cell-wrapper`}
            data-qa="absence-cell"
          >
            <AbsenceCell
              selectedCells={selectedCells}
              toggleCellSelection={toggleCellSelection}
              categories={placements[date.formatIso()]}
              absences={absences[date.formatIso()]}
              backupCare={backupCares[date.formatIso()] ?? false}
              date={date}
              childId={child.id}
            />
          </td>
        ) : (
          <td key={`${child.id}${date.formatIso()}`}>
            <DisabledCell />
          </td>
        )
      })}
      {emptyCols.map((item) => (
        <td key={item}>
          <DisabledCell />
        </td>
      ))}
      {reservationEnabled && (
        <>
          <td>
            <NumbersColumn>
              {reservationTotalHours !== null
                ? `${reservationTotalHours} h`
                : '-'}
              <Gap size="xs" horizontal />
              <WarningPlaceholder />
            </NumbersColumn>
          </td>
          <td>
            <NumbersColumn warning={showAttendanceWarning}>
              {attendanceTotalHours !== null
                ? `${attendanceTotalHours} h`
                : '-'}
              <Gap size="xs" horizontal />
              {showAttendanceWarning ? (
                <FontAwesomeIcon
                  icon={fasExclamationTriangle}
                  size="1x"
                  color={theme.colors.status.warning}
                />
              ) : (
                <WarningPlaceholder />
              )}
            </NumbersColumn>
          </td>
        </>
      )}
    </tr>
  )
})

const NumbersColumn = styled.div<{ warning?: boolean }>`
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  flex-wrap: nowrap;
  ${({ theme, warning }) =>
    warning &&
    css`
      color: ${theme.colors.accents.a2orangeDark};
      font-weight: ${fontWeights.semibold};
    `};
`

const WarningPlaceholder = styled.div`
  min-width: 1em;
  width: 1em;
`

interface AbsenceHeadProps {
  dateCols: LocalDate[]
  emptyCols: number[]
  operationDays: LocalDate[]
  reservationEnabled: boolean
}

const AbsenceTableHead = React.memo(function AbsenceTableHead({
  dateCols,
  emptyCols,
  operationDays,
  reservationEnabled
}: AbsenceHeadProps) {
  const { i18n, lang } = useTranslation()
  return (
    <thead>
      <tr>
        <th />
        {dateCols.map((item) =>
          operationDays.some((operationDay) => operationDay.isEqual(item)) ? (
            <th
              key={item.getDate()}
              className={classNames({
                'absence-header-today': item.isToday(),
                'absence-header-weekend': item.isWeekend()
              })}
            >
              <div>{item.format('EEEEEE', lang)}</div>
              <div>{item.getDate()}</div>
            </th>
          ) : (
            <th key={item.getDate()} />
          )
        )}
        {emptyCols.map((item) => (
          <th key={item} />
        ))}
        {reservationEnabled && (
          <>
            <th>{i18n.absences.table.reservationsTotal}</th>
            <th>{i18n.absences.table.attendancesTotal}</th>
          </>
        )}
      </tr>
    </thead>
  )
})

interface AbsenceTableProps {
  groupId: string
  selectedCells: Cell[]
  toggleCellSelection: (id: UUID, parts: CellPart[]) => void
  selectedDate: LocalDate
  childList: AbsenceChild[]
  operationDays: LocalDate[]
  reservationEnabled: boolean
}

export default React.memo(function AbsenceTable({
  groupId,
  selectedCells,
  toggleCellSelection,
  selectedDate,
  childList,
  operationDays,
  reservationEnabled
}: AbsenceTableProps) {
  const { i18n } = useTranslation()

  const dateCols = getMonthDays(selectedDate)
  const emptyCols = getEmptyCols(dateCols.length)

  const renderEmptyRow = () => (
    <tr>
      <td className="empty-row" />
    </tr>
  )

  return (
    <table className="table">
      <AbsenceTableHead
        dateCols={dateCols}
        emptyCols={emptyCols}
        operationDays={operationDays}
        reservationEnabled={reservationEnabled}
      />
      <tbody>
        {childList.map((item) => (
          <AbsenceTableRow
            key={item.child.id}
            selectedCells={selectedCells}
            toggleCellSelection={toggleCellSelection}
            absenceChild={item}
            dateCols={dateCols}
            emptyCols={emptyCols}
            operationDays={operationDays}
            i18n={i18n}
            selectedDate={selectedDate}
            reservationEnabled={reservationEnabled}
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
