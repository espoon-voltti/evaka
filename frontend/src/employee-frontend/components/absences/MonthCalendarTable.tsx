// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import range from 'lodash/range'
import React, { useMemo } from 'react'
import { Link } from 'react-router-dom'
import styled, { css, useTheme } from 'styled-components'

import {
  GroupMonthCalendar,
  GroupMonthCalendarChild,
  GroupMonthCalendarDay,
  GroupMonthCalendarDayChild
} from 'lib-common/generated/api-types/absence'
import LocalDate from 'lib-common/local-date'
import TimeRange from 'lib-common/time-range'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Thead } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import { fasExclamationTriangle } from 'lib-icons'

import { Translations, useTranslation } from '../../state/i18n'
import { AgeIndicatorChip } from '../common/AgeIndicatorChip'
import { ContractDaysIndicatorChip } from '../common/ContractDaysIndicatorChip'

import { SelectedCell } from './GroupMonthCalendar'
import MonthCalendarCell, { DisabledCell } from './MonthCalendarCell'
import StaffAttendance from './StaffAttendance'

interface MonthCalendarRow {
  holidays: Record<string, boolean>
  operationTimes: (TimeRange | null)[]
  child: GroupMonthCalendarChild
  days: [LocalDate, GroupMonthCalendarDayChild | undefined][]
  emptyCols: number[]
  selectedCells: SelectedCell[]
  toggleCellSelection: (cell: SelectedCell) => void
  selectedDate: LocalDate
  reservationEnabled: boolean
}

const MonthCalendarRow = React.memo(function MonthCalendarRow({
  holidays,
  operationTimes,
  child,
  days,
  emptyCols,
  selectedCells,
  toggleCellSelection,
  selectedDate,
  reservationEnabled
}: MonthCalendarRow) {
  const theme = useTheme()
  const { i18n } = useTranslation()
  const contractDayServiceNeeds = child.actualServiceNeeds.filter(
    (c) => c.hasContractDays
  )
  const hourInfo = useMemo(() => getHourInfo(child), [child])

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
            tooltip={
              <div>
                <p>
                  {child.lastName}, {child.firstName}
                </p>
                {child.actualServiceNeeds.map((need, i) => (
                  <p key={`service-need-option-${i}`}>{need.optionName}</p>
                ))}
              </div>
            }
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
      {days.map(([date, day]) =>
        day !== undefined && day.scheduleType !== 'TERM_BREAK' ? (
          <CalendarTd
            key={`${child.id}${date.formatIso()}`}
            $isToday={date.isToday()}
            data-qa={`absence-cell-${child.id}-${date.formatIso()}`}
          >
            <MonthCalendarCell
              date={date}
              holidays={holidays}
              operationTime={operationTimes[date.getIsoDayOfWeek() - 1]}
              childId={child.id}
              day={day}
              intermittent={child.actualServiceNeeds.some(
                (serviceNeed) =>
                  serviceNeed.childId === child.id &&
                  serviceNeed.validDuring.includes(date) &&
                  serviceNeed.shiftCare === 'INTERMITTENT'
              )}
              selectedCells={selectedCells}
              toggleCellSelection={toggleCellSelection}
            />
          </CalendarTd>
        ) : (
          <td key={`${child.id}${date.formatIso()}`}>
            <DisabledCell />
          </td>
        )
      )}
      {emptyCols.map((item) => (
        <td key={item}>
          <DisabledCell />
        </td>
      ))}
      {reservationEnabled && (
        <>
          <td>
            <NumbersColumn $warning={hourInfo.showReservedHoursWarning}>
              <span data-qa="reserved-hours">{hourInfo.reservedHours} h</span>
              <Gap size="xs" horizontal />
              {hourInfo.showReservedHoursWarning ? (
                <FontAwesomeIcon
                  icon={fasExclamationTriangle}
                  size="1x"
                  color={theme.colors.status.warning}
                  data-qa="reserved-hours-warning"
                />
              ) : (
                <WarningPlaceholder />
              )}
            </NumbersColumn>
          </td>
          <td>
            <NumbersColumn $warning={hourInfo.showUsedHoursWarning}>
              <span data-qa="used-hours">{hourInfo.usedHours} h</span>
              <Gap size="xs" horizontal />
              {hourInfo.showUsedHoursWarning ? (
                <FontAwesomeIcon
                  icon={fasExclamationTriangle}
                  size="1x"
                  color={theme.colors.status.warning}
                  data-qa="used-hours-warning"
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

function getHourInfo(child: GroupMonthCalendarChild): {
  reservedHours: number
  showReservedHoursWarning: boolean
  usedHours: number
  showUsedHoursWarning: boolean
} {
  const { usedService, reservationTotalHours, attendanceTotalHours } = child
  if (featureFlags.timeUsageInfo && usedService) {
    return {
      reservedHours: usedService.reservedHours,
      showReservedHoursWarning:
        usedService.reservedHours > usedService.serviceNeedHours,
      usedHours: usedService.usedServiceHours,
      showUsedHoursWarning:
        usedService.usedServiceHours > usedService.serviceNeedHours
    }
  } else {
    return {
      reservedHours: reservationTotalHours,
      showReservedHoursWarning: false,
      usedHours: attendanceTotalHours,
      showUsedHoursWarning: attendanceTotalHours > reservationTotalHours
    }
  }
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

const NumbersColumn = styled.div<{ $warning?: boolean }>`
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  flex-wrap: nowrap;
  ${({ theme, $warning }) =>
    $warning &&
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
  days: GroupMonthCalendarDay[]
  emptyCols: number[]
  reservationEnabled: boolean
}

const MonthCalendarTableHead = React.memo(function AbsenceTableHead({
  days,
  emptyCols,
  reservationEnabled
}: AbsenceHeadProps) {
  const { i18n, lang } = useTranslation()
  return (
    <Thead sticky>
      <AbsenceTr>
        <th />
        {days.map(({ date, children }) =>
          children !== null || featureFlags.intermittentShiftCare ? (
            // Operation day
            <AbsenceTh
              key={date.getDate()}
              $isToday={date.isToday()}
              $isWeekend={date.isWeekend()}
            >
              <div>{date.format('EEEEEE', lang)}</div>
              <div>{date.getDate()}</div>
            </AbsenceTh>
          ) : (
            <th key={date.getDate()} />
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
  groupMonthCalendar: GroupMonthCalendar
  selectedCells: SelectedCell[]
  toggleCellSelection: (cell: SelectedCell) => void
  selectedDate: LocalDate
  reservationEnabled: boolean
  staffAttendanceEnabled: boolean
}

export default React.memo(function MonthCalendarTable({
  groupId,
  groupMonthCalendar,
  selectedCells,
  toggleCellSelection,
  selectedDate,
  reservationEnabled,
  staffAttendanceEnabled
}: AbsenceTableProps) {
  const emptyCols = useMemo(
    () => range(32 - groupMonthCalendar.days.length),
    [groupMonthCalendar.days.length]
  )
  const tableRows = useMemo(
    () => getMonthCalendarRows(groupMonthCalendar),
    [groupMonthCalendar]
  )

  const holidays = groupMonthCalendar.days.reduce<Record<string, boolean>>(
    (prev, next) => {
      prev[next.date.formatIso()] = next.holiday
      return prev
    },
    {}
  )

  return (
    <MonthCalendarTableRoot>
      <MonthCalendarTableHead
        days={groupMonthCalendar.days}
        emptyCols={emptyCols}
        reservationEnabled={reservationEnabled}
      />
      <tbody>
        {tableRows.map(({ child, days }) => (
          <MonthCalendarRow
            key={child.id}
            holidays={holidays}
            operationTimes={groupMonthCalendar.daycareOperationTimes}
            child={child}
            days={days}
            emptyCols={emptyCols}
            selectedCells={selectedCells}
            toggleCellSelection={toggleCellSelection}
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
            days={groupMonthCalendar.days}
            emptyCols={emptyCols}
          />
        )}
      </tbody>
    </MonthCalendarTableRoot>
  )
})

interface MonthCalendarRowData {
  child: GroupMonthCalendarChild
  days: [LocalDate, GroupMonthCalendarDayChild | undefined][]
}

function getMonthCalendarRows(
  data: GroupMonthCalendar
): MonthCalendarRowData[] {
  return data.children.map((child) => ({
    child,
    days: data.days.map((day) => [
      day.date,
      day.children
        ? day.children.find((c) => c.childId === child.id)
        : undefined
    ])
  }))
}

const MonthCalendarTableRoot = styled.table`
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

const CalendarTd = styled.td<{ $isToday: boolean }>`
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
  &:hover ${CalendarTd}, &:hover ${ChildNameTd} {
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
