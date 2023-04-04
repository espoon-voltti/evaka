// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import { Link } from 'react-router-dom'
import styled, { css, useTheme } from 'styled-components'

import { AbsenceChild } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Thead } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { fasExclamationTriangle } from 'lib-icons'

import { Translations, useTranslation } from '../../state/i18n'
import { Cell, CellPart } from '../../types/absence'
import { AgeIndicatorChip } from '../common/AgeIndicatorChip'
import { ContractDaysIndicatorChip } from '../common/ContractDaysIndicatorChip'

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
    missingHolidayReservations,
    attendanceTotalHours,
    reservationTotalHours
  } = absenceChild

  const contractDayServiceNeeds = absenceChild.actualServiceNeeds.filter(
    (c) => c.hasContractDays
  )

  const showAttendanceWarning =
    !!attendanceTotalHours &&
    !!reservationTotalHours &&
    attendanceTotalHours > reservationTotalHours

  return (
    <AbsenceTr data-qa="absence-child-row">
      <ChildNameTd>
        <FixedSpaceRow spacing="xs" alignItems="center">
          <AgeIndicatorChip
            age={selectedDate.differenceInYears(child.dateOfBirth)}
          />
          {contractDayServiceNeeds.length > 0 && (
            <ContractDaysIndicatorChip
              contractDayServiceNeeds={contractDayServiceNeeds}
            />
          )}
          <Tooltip
            tooltip={`${child.lastName}, ${child.firstName}`}
            position="top"
          >
            <FixedSpaceRow alignItems="center">
              <Link
                to={`/child-information/${child.id}`}
                data-qa="absence-child-link"
              >
                {shortChildName(child.firstName, child.lastName, i18n)}
              </Link>
            </FixedSpaceRow>
          </Tooltip>
        </FixedSpaceRow>
      </ChildNameTd>
      {dateCols.map((date) => {
        return operationDays.some((operationDay) =>
          operationDay.isEqual(date)
        ) ? (
          <AbsenceTd
            key={`${child.id}${date.formatIso()}`}
            $isToday={date.isToday()}
            data-qa={`absence-cell-${child.id}-${date.formatIso()}`}
          >
            <AbsenceCell
              selectedCells={selectedCells}
              missingHolidayReservations={missingHolidayReservations}
              toggleCellSelection={toggleCellSelection}
              categories={placements[date.formatIso()]}
              absences={absences[date.formatIso()]}
              backupCare={backupCares[date.formatIso()] ?? false}
              date={date}
              childId={child.id}
            />
          </AbsenceTd>
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
    </AbsenceTr>
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
    <Thead sticky>
      <AbsenceTr>
        <th />
        {dateCols.map((item) =>
          operationDays.some((operationDay) => operationDay.isEqual(item)) ? (
            <AbsenceTh
              key={item.getDate()}
              $isToday={item.isToday()}
              $isWeekend={item.isWeekend()}
            >
              <div>{item.format('EEEEEE', lang)}</div>
              <div>{item.getDate()}</div>
            </AbsenceTh>
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
      </AbsenceTr>
    </Thead>
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
  staffAttendanceEnabled: boolean
}

export default React.memo(function AbsenceTable({
  groupId,
  selectedCells,
  toggleCellSelection,
  selectedDate,
  childList,
  operationDays,
  reservationEnabled,
  staffAttendanceEnabled
}: AbsenceTableProps) {
  const { i18n } = useTranslation()

  const dateCols = getMonthDays(selectedDate)
  const emptyCols = getEmptyCols(dateCols.length)

  return (
    <AbsenceTableRoot>
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
        <EmptyRow>
          <td />
        </EmptyRow>
        {staffAttendanceEnabled && (
          <StaffAttendance
            groupId={groupId}
            selectedDate={selectedDate}
            emptyCols={emptyCols}
            operationDays={operationDays}
          />
        )}
      </tbody>
    </AbsenceTableRoot>
  )
})

const AbsenceTableRoot = styled.table`
  border-collapse: collapse;
  width: 100%;

  td,
  th {
    padding: 5px 3px;
    border-bottom: none;
  }

  th {
    text-align: center;
    text-transform: uppercase;
    color: ${colors.grayscale.g70};
    vertical-align: center;
    font-weight: ${fontWeights.semibold};
    font-size: 0.8rem;
  }

  td {
    cursor: pointer;
  }
`

const AbsenceTd = styled.td<{ $isToday: boolean }>`
  ${(p: { $isToday: boolean }) =>
    p.$isToday && `background-color: ${colors.grayscale.g4}`};
`

const ChildNameTd = styled.td`
  white-space: nowrap;

  a {
    display: inline-block;
    overflow: hidden;
    width: 100px;
    max-width: 100px;

    @media screen and (min-width: 1216px) {
      width: 176px;
      max-width: 176px;
    }
  }
`

const AbsenceTr = styled.tr`
  &:hover ${AbsenceTd}, &:hover ${ChildNameTd} {
    background-color: ${colors.grayscale.g4};

    &:first-child {
      box-shadow: -8px 0 0 ${colors.grayscale.g4};
    }

    &:last-child {
      box-shadow: 8px 0 0 ${colors.grayscale.g4};
    }
  }
`

const AbsenceTh = styled.th<{ $isToday: boolean; $isWeekend: boolean }>`
  ${(p) =>
    p.$isToday &&
    css`
      background-color: ${colors.grayscale.g4};

      @media print {
        background: none;
        color: inherit;
        font-weight: bolder;
      }
    `};
  ${(p) =>
    p.$isWeekend &&
    css`
      color: ${colors.grayscale.g100};
    `};
`

const EmptyRow = styled.tr`
  color: transparent;

  td {
    cursor: default;
    height: 20px;
  }
`
