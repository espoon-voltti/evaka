// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import range from 'lodash/range'
import React, { useMemo } from 'react'
import { Link } from 'react-router'
import styled, { css, useTheme } from 'styled-components'

import type {
  GroupMonthCalendar,
  GroupMonthCalendarChild,
  GroupMonthCalendarDay,
  GroupMonthCalendarDayChild
} from 'lib-common/generated/api-types/absence'
import type { GroupId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import type TimeRange from 'lib-common/time-range'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Td, Thead } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors, { absenceColors } from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import { fasExclamationTriangle } from 'lib-icons'

import type { Translations } from '../../state/i18n'
import { useTranslation } from '../../state/i18n'
import { AgeIndicatorChip } from '../common/AgeIndicatorChip'
import { ContractDaysIndicatorChip } from '../common/ContractDaysIndicatorChip'

import type { SelectedCell } from './GroupMonthCalendar'
import MonthCalendarCell, {
  AbsenceCellDiv,
  DisabledCell
} from './MonthCalendarCell'
import StaffAttendance from './StaffAttendance'

interface MonthCalendarRow {
  holidays: Record<string, boolean>
  operationTimes: (TimeRange | null)[]
  shiftCareOperationTimes: (TimeRange | null)[] | null
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
  shiftCareOperationTimes,
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

  const selectedCellsOfChild = useMemo(
    () => selectedCells.filter((c) => c.childId === child.id),
    [selectedCells, child.id]
  )

  const selectedDates = useMemo(
    () => selectedCellsOfChild.map((c) => c.date),
    [selectedCellsOfChild]
  )

  const daysForChild = useMemo(
    () =>
      days.filter(([_, day]) => day !== undefined && day.childId === child.id),
    [days, child.id]
  )

  const fullySelected = useMemo(
    () => daysForChild.every(([date]) => selectedDates.includes(date)),
    [daysForChild, selectedDates]
  )

  return (
    <AbsenceTr data-qa={`absence-child-row-${child.id}`}>
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
      <Td
        align="center"
        verticalAlign="middle"
        onClick={() => {
          if (fullySelected) {
            selectedCellsOfChild.forEach(toggleCellSelection)
          } else {
            daysForChild
              .filter(([date]) => !selectedDates.includes(date))
              .forEach(([date, day]) => {
                if (day !== undefined) {
                  toggleCellSelection({
                    childId: child.id,
                    date,
                    absenceCategories: day.absenceCategories
                  })
                }
              })
          }
        }}
      >
        <SelectAll $isSelected={fullySelected} />
      </Td>
      {days.map(([date, day]) =>
        day !== undefined && day.scheduleType !== 'TERM_BREAK' ? (
          <CalendarTd
            key={`${child.id}${date.formatIso()}`}
            $isToday={date.isToday()}
            data-qa={`absence-cell-${date.formatIso()}`}
          >
            <MonthCalendarCell
              date={date}
              holidays={holidays}
              operationTime={
                (shiftCareOperationTimes ?? operationTimes)[
                  date.getIsoDayOfWeek() - 1
                ]
              }
              childId={child.id}
              day={day}
              intermittentShiftCare={day.shiftCare === 'INTERMITTENT'}
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
  if (usedService) {
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
        <th>{i18n.absences.table.selectAll}</th>
        {days.map(({ date, isOperationDay }) =>
          isOperationDay || featureFlags.intermittentShiftCare ? (
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
  groupId: GroupId
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
      prev[next.date.formatIso()] = !next.isOperationDay
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
            shiftCareOperationTimes={groupMonthCalendar.shiftCareOperationTimes}
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
      day.children.find((c) => c.childId === child.id)
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

const SelectAll = styled(AbsenceCellDiv)<{ $isSelected: boolean }>`
  display: inline-block;
  ${(p) =>
    p.$isSelected ? '' : `background-color: ${absenceColors.NO_ABSENCE};`}
`
