// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useSyncQueryParams } from 'lib-common/utils/useSyncQueryParams'
import { ChoiceChip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faCalendarAlt, faChevronLeft, faChevronRight } from 'lib-icons'

import { UnitResponse } from '../../api/unit'
import { useTranslation } from '../../state/i18n'
import { UnitContext } from '../../state/unit'
import { DayOfWeek } from '../../types'
import { DaycareGroup } from '../../types/unit'
import GroupMonthCalendar from '../absences/GroupMonthCalendar'
import { renderResult } from '../async-rendering'

import { unitGroupsQuery } from './queries'
import AttendanceGroupFilterSelect from './tab-calendar/AttendanceGroupFilterSelect'
import CalendarEventsSection from './tab-calendar/CalendarEventsSection'
import Occupancy from './tab-unit-information/Occupancy'
import UnitAttendanceReservationsView from './unit-reservations/UnitAttendanceReservationsView'

export type CalendarMode = 'week' | 'month'

export type AttendanceGroupFilter =
  | { type: 'group'; id: UUID }
  | { type: 'all-children' | 'no-group' | 'staff' }

const GroupSelectorWrapper = styled.div`
  min-width: 320px;
`

const ColoredH3 = styled(H3)`
  color: ${(p) => p.theme.colors.main.m1};
`

export const stickyTopBarHeight = 75

const StickyTopBar = styled.div`
  position: sticky;
  top: 0;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  z-index: 20;
  height: ${stickyTopBarHeight}px;
  display: flex;
  align-items: center;
`

const TopHorizontalLine = styled(HorizontalLine)`
  margin-top: 0;
`

const getWeekDateRange = (date: LocalDate) => {
  const start = date.startOfWeek()
  const end = start.endOfWeek()
  return new FiniteDateRange(start, end)
}

const getMonthRange = (date: LocalDate) => {
  const start = date.startOfMonth().startOfWeek()
  const end = date.lastDayOfMonth().endOfWeek()
  return new FiniteDateRange(start, end)
}

function getDefaultGroup(
  groupParam: string | null,
  groups: DaycareGroup[]
): AttendanceGroupFilter {
  if (groupParam !== null) {
    if (
      groupParam === 'no-group' ||
      groupParam === 'staff' ||
      groupParam === 'all-children'
    ) {
      return { type: groupParam }
    }
    if (groups.some((g) => g.id === groupParam)) {
      return { type: 'group', id: groupParam }
    }
  }

  // Default to the first open group
  const group = sortBy(groups, [(g) => g.name.toLowerCase()]).find((group) =>
    new DateRange(group.startDate, group.endDate).includes(
      LocalDate.todayInSystemTz()
    )
  )
  if (group !== undefined) return { type: 'group', id: group.id }

  return { type: 'no-group' }
}

function attendanceGroupToString(group: AttendanceGroupFilter): string {
  if (group.type === 'group') return group.id
  return group.type
}

export default React.memo(function TabCalendar() {
  const { id: unitId } = useNonNullableParams<{ id: UUID }>()
  const { unitInformation, filters, setFilters } = useContext(UnitContext)

  return renderResult(unitInformation, (unitInformation) => (
    <>
      {unitInformation.permittedActions.has('READ_OCCUPANCIES') ? (
        <>
          <Occupancy
            unitId={unitId}
            filters={filters}
            setFilters={setFilters}
            realtimeStaffAttendanceEnabled={unitInformation.daycare.enabledPilotFeatures.includes(
              'REALTIME_STAFF_ATTENDANCE'
            )}
            shiftCareUnit={unitInformation.daycare.roundTheClock}
          />
          <Gap size="s" />
        </>
      ) : null}
      {unitInformation.permittedActions.has('READ_GROUP_DETAILS') ? (
        <Calendar unitId={unitId} unitInformation={unitInformation} />
      ) : null}
    </>
  ))
})

const Calendar = React.memo(function Calendar({
  unitId,
  unitInformation
}: {
  unitId: string
  unitInformation: UnitResponse
}) {
  const groups = useQueryResult(unitGroupsQuery(unitId))
  return renderResult(groups, (groups) => (
    <CalendarContent unitInformation={unitInformation} groups={groups} />
  ))
})

const CalendarContent = React.memo(function CalendarContent({
  unitInformation,
  groups
}: {
  unitInformation: UnitResponse
  groups: DaycareGroup[]
}) {
  const { i18n } = useTranslation()
  const unitId = unitInformation.daycare.id

  const { enabledPilotFeatures } = unitInformation.daycare
  const reservationEnabled = enabledPilotFeatures.includes('RESERVATIONS')
  const realtimeStaffAttendanceEnabled = enabledPilotFeatures.includes(
    'REALTIME_STAFF_ATTENDANCE'
  )

  // Using READ_CHILD_ATTENDANCES to check if the user can see the week view is not exactly accurate,
  // but it's hard to do a finer grained check with the current UX.
  const hasPermissionToReadAttendances = unitInformation.permittedActions.has(
    'READ_CHILD_ATTENDANCES'
  )

  const [searchParams] = useSearchParams()

  const selectedDateParam = searchParams.get('date')
  const [selectedDate, setSelectedDate] = useState<LocalDate>(
    selectedDateParam
      ? LocalDate.parseIso(selectedDateParam)
      : LocalDate.todayInSystemTz()
  )

  const groupParam = searchParams.get('group')
  const [selectedGroup, setSelectedGroup] = useState<AttendanceGroupFilter>(
    () => getDefaultGroup(groupParam, groups)
  )

  const modeParam = searchParams.get('mode')
  const [requestedMode, setRequestedMode] = useState<CalendarMode>(
    modeParam && ['month', 'week'].includes(modeParam)
      ? (modeParam as CalendarMode)
      : realtimeStaffAttendanceEnabled && hasPermissionToReadAttendances
        ? 'week'
        : 'month'
  )

  const [mode, availableModes]: [CalendarMode, CalendarMode[]] =
    hasPermissionToReadAttendances
      ? selectedGroup.type === 'staff' ||
        selectedGroup.type === 'no-group' ||
        selectedGroup.type === 'all-children'
        ? ['week', ['week']]
        : [requestedMode, ['month', 'week']]
      : ['month', ['month']]

  useSyncQueryParams({
    group: attendanceGroupToString(selectedGroup),
    date: selectedDate.toString(),
    mode
  })
  const operationalDays = useMemo((): DayOfWeek[] => {
    const days = unitInformation.daycare.operationDays
    return days.length === 0 ? [1, 2, 3, 4, 5] : days
  }, [unitInformation.daycare.operationDays])

  const weekRange = useMemo(
    () => getWeekDateRange(selectedDate),
    [selectedDate]
  )

  const monthRange = useMemo(() => getMonthRange(selectedDate), [selectedDate])

  const [calendarOpen, setCalendarOpen] = useState(true)
  const [attendancesOpen, setAttendancesOpen] = useState(true)
  const [eventsOpen, setEventsOpen] = useState(true)

  return (
    <CollapsibleContentArea
      open={calendarOpen}
      toggleOpen={() => setCalendarOpen(!calendarOpen)}
      title={<H3 noMargin>{i18n.unit.calendar.title}</H3>}
      opaque
    >
      {(reservationEnabled || realtimeStaffAttendanceEnabled) &&
      availableModes.length >= 2 ? (
        <FixedSpaceRow spacing="xs" justifyContent="flex-end">
          {availableModes.map((m) => (
            <ChoiceChip
              key={m}
              data-qa={`choose-calendar-mode-${m}`}
              text={i18n.unit.calendar.modes[m]}
              selected={mode === m}
              onChange={() => setRequestedMode(m)}
            />
          ))}
        </FixedSpaceRow>
      ) : null}

      <StickyTopBar>
        <FixedSpaceRow spacing="s" alignItems="center">
          <GroupSelectorWrapper>
            <AttendanceGroupFilterSelect
              groups={groups}
              value={selectedGroup}
              onChange={setSelectedGroup}
              data-qa="attendances-group-select"
              realtimeStaffAttendanceEnabled={realtimeStaffAttendanceEnabled}
            />
          </GroupSelectorWrapper>

          <ActiveDateRangeSelector
            selectedDate={selectedDate}
            setSelectedDate={setSelectedDate}
            mode={mode}
            weekRange={weekRange}
          />
        </FixedSpaceRow>
      </StickyTopBar>

      <TopHorizontalLine dashed slim />

      <CollapsibleContentArea
        open={attendancesOpen}
        toggleOpen={() => setAttendancesOpen(!attendancesOpen)}
        title={<H4 noMargin>{i18n.unit.calendar.attendances.title}</H4>}
        opaque
        paddingHorizontal="zero"
        paddingVertical="zero"
      >
        {mode === 'month' && selectedGroup.type === 'group' ? (
          <GroupMonthCalendar
            groupId={selectedGroup.id}
            selectedDate={selectedDate}
            reservationEnabled={reservationEnabled}
            staffAttendanceEnabled={!realtimeStaffAttendanceEnabled}
          />
        ) : null}

        {mode === 'week' ? (
          <UnitAttendanceReservationsView
            unitId={unitId}
            selectedGroup={selectedGroup}
            selectedDate={selectedDate}
            setSelectedDate={setSelectedDate}
            operationalDays={operationalDays}
            realtimeStaffAttendanceEnabled={realtimeStaffAttendanceEnabled}
            groups={groups}
            weekRange={weekRange}
          />
        ) : null}
      </CollapsibleContentArea>

      {unitInformation.permittedActions.has('READ_CALENDAR_EVENTS') &&
      (selectedGroup.type === 'group' ||
        (selectedGroup.type === 'all-children' && mode === 'week')) ? (
        <>
          <HorizontalLine dashed slim />

          <CollapsibleContentArea
            open={eventsOpen}
            toggleOpen={() => setEventsOpen(!eventsOpen)}
            title={<H4 noMargin>{i18n.unit.calendar.events.title}</H4>}
            opaque
            paddingHorizontal="zero"
            paddingVertical="zero"
          >
            <CalendarEventsSection
              selectedDate={selectedDate}
              dateRange={mode === 'week' ? weekRange : monthRange}
              operationalDays={operationalDays}
              unitId={unitId}
              groupId={selectedGroup.type === 'group' ? selectedGroup.id : null}
            />
          </CollapsibleContentArea>
        </>
      ) : null}
    </CollapsibleContentArea>
  )
})

type ActiveDateRangeSelectorProps = {
  selectedDate: LocalDate
  setSelectedDate: React.Dispatch<React.SetStateAction<LocalDate>>
  mode: CalendarMode
  weekRange: FiniteDateRange
}

const ActiveDateRangeSelector = React.memo(function ActiveDateRangeSelector({
  selectedDate,
  setSelectedDate,
  mode,
  weekRange
}: ActiveDateRangeSelectorProps) {
  const { i18n } = useTranslation()

  const startDate =
    mode === 'week' ? weekRange.start : selectedDate.startOfMonth()
  const endDate = mode === 'week' ? weekRange.end : startDate.lastDayOfMonth()

  const addUnitOfTime = useCallback(
    (date: LocalDate) =>
      mode === 'week' ? date.addDays(7) : date.addMonths(1),
    [mode]
  )
  const subUnitOfTime = useCallback(
    (date: LocalDate) =>
      mode === 'week' ? date.subDays(7) : date.subMonths(1),
    [mode]
  )

  return (
    <>
      <div data-qa-date-range={new FiniteDateRange(startDate, endDate)} />
      <ColoredH3 noMargin>
        {startDate.format('dd.MM.')}–{endDate.format('dd.MM.yyyy')}
      </ColoredH3>
      <FixedSpaceRow spacing="xxs">
        <IconButton
          icon={faChevronLeft}
          onClick={() => setSelectedDate(subUnitOfTime(selectedDate))}
          data-qa="previous-week"
          aria-label={i18n.unit.calendar.previousWeek}
        />
        <IconButton
          icon={faChevronRight}
          onClick={() => setSelectedDate(addUnitOfTime(selectedDate))}
          data-qa="next-week"
          aria-label={i18n.unit.calendar.nextWeek}
        />
      </FixedSpaceRow>
      <InlineButton
        icon={faCalendarAlt}
        text={i18n.common.today}
        onClick={() => setSelectedDate(LocalDate.todayInSystemTz())}
      />
    </>
  )
})
