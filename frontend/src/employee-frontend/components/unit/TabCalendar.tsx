// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import { UserContext } from 'employee-frontend/state/user'
import { combine, isLoading } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useQuery } from 'lib-common/utils/useQuery'
import { useApiState } from 'lib-common/utils/useRestApi'
import { useSyncQueryParams } from 'lib-common/utils/useSyncQueryParams'
import { ChoiceChip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3, H4, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faCalendarAlt, faChevronLeft, faChevronRight } from 'lib-icons'

import { getDaycareGroups, UnitData, UnitResponse } from '../../api/unit'
import { useTranslation } from '../../state/i18n'
import { UnitContext } from '../../state/unit'
import { DayOfWeek } from '../../types'
import { DaycareGroup } from '../../types/unit'
import { UnitFilters } from '../../utils/UnitFilters'
import { requireRole } from '../../utils/roles'
import Absences from '../absences/Absences'
import { renderResult } from '../async-rendering'
import { DataList } from '../common/DataList'

import UnitDataFilters from './UnitDataFilters'
import AttendanceGroupFilterSelect from './tab-calendar/AttendanceGroupFilterSelect'
import CalendarEventsSection from './tab-calendar/CalendarEventsSection'
import Occupancy from './tab-unit-information/Occupancy'
import UnitAttendanceReservationsView from './unit-reservations/UnitAttendanceReservationsView'

type CalendarMode = 'week' | 'month'

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

const getWeekDateRange = (date: LocalDate, operationalDays: DayOfWeek[]) => {
  const start = date.startOfWeek()
  return new FiniteDateRange(
    start,
    start.addDays(Math.max(...operationalDays) - 1)
  )
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
  const { unitInformation, unitData, filters, setFilters } =
    useContext(UnitContext)

  const [groups] = useApiState(() => getDaycareGroups(unitId), [unitId])

  const combinedResult = combine(unitInformation, unitData, groups)
  return renderResult(combinedResult, ([unitInformation, unitData, groups]) => (
    <TabContent
      unitInformation={unitInformation}
      unitData={unitData}
      isUnitLoading={isLoading(combinedResult)}
      filters={filters}
      setFilters={setFilters}
      groups={groups}
    />
  ))
})

interface TabContentProps {
  unitInformation: UnitResponse
  unitData: UnitData
  isUnitLoading: boolean
  filters: UnitFilters
  setFilters: React.Dispatch<React.SetStateAction<UnitFilters>>
  groups: DaycareGroup[]
}

const TabContent = React.memo(function TabContent({
  unitInformation,
  unitData,
  isUnitLoading,
  filters,
  setFilters,
  groups
}: TabContentProps) {
  const { i18n } = useTranslation()
  const unitId = unitInformation.daycare.id

  const { roles } = useContext(UserContext)

  const { enabledPilotFeatures } = unitInformation.daycare
  const reservationEnabled = enabledPilotFeatures.includes('RESERVATIONS')
  const realtimeStaffAttendanceEnabled = enabledPilotFeatures.includes(
    'REALTIME_STAFF_ATTENDANCE'
  )

  const query = useQuery()

  const selectedDateParam = query.get('date')
  const [selectedDate, setSelectedDate] = useState<LocalDate>(
    selectedDateParam
      ? LocalDate.parseIso(selectedDateParam)
      : LocalDate.todayInSystemTz()
  )

  const groupParam = query.get('group')
  const [selectedGroup, setSelectedGroup] = useState<AttendanceGroupFilter>(
    () => getDefaultGroup(groupParam, groups)
  )

  const modeParam = query.get('mode')
  const [requestedMode, setRequestedMode] = useState<CalendarMode>(
    modeParam && ['month', 'week'].includes(modeParam)
      ? (modeParam as CalendarMode)
      : realtimeStaffAttendanceEnabled
      ? 'week'
      : 'month'
  )

  const [mode, availableModes]: [CalendarMode, CalendarMode[]] =
    selectedGroup.type === 'staff' ||
    selectedGroup.type === 'no-group' ||
    selectedGroup.type === 'all-children'
      ? ['week', ['week']]
      : [requestedMode, ['month', 'week']]

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
    () => getWeekDateRange(selectedDate, operationalDays),
    [operationalDays, selectedDate]
  )

  const [calendarOpen, setCalendarOpen] = useState(true)
  const [attendancesOpen, setAttendancesOpen] = useState(true)
  const [eventsOpen, setEventsOpen] = useState(true)
  const [occupanciesOpen, setOccupanciesOpen] = useState(true)

  return (
    <>
      <CollapsibleContentArea
        title={<H3 noMargin>{i18n.unit.occupancies}</H3>}
        open={occupanciesOpen}
        toggleOpen={() => setOccupanciesOpen(!occupanciesOpen)}
        opaque
        data-qa="unit-attendances"
        data-isloading={isUnitLoading}
      >
        <FixedSpaceRow alignItems="center">
          <Label>{i18n.unit.filters.title}</Label>
          <UnitDataFilters
            canEdit={requireRole(
              roles,
              'ADMIN',
              'DIRECTOR',
              'SERVICE_WORKER',
              'UNIT_SUPERVISOR',
              'FINANCE_ADMIN'
            )}
            filters={filters}
            setFilters={setFilters}
          />
        </FixedSpaceRow>
        <Gap size="s" />
        <DataList>
          <div>
            <label>{i18n.unit.info.caretakers.titleLabel}</label>
            <span data-qa="unit-total-caretaker-count">
              <Caretakers unitData={unitData} />
            </span>
          </div>
        </DataList>
        <Gap />
        {unitData.unitOccupancies ? (
          <Occupancy
            filters={filters}
            occupancies={unitData.unitOccupancies}
            realtimeStaffAttendanceEnabled={realtimeStaffAttendanceEnabled}
            shiftCareUnit={unitInformation.daycare.roundTheClock}
          />
        ) : null}
      </CollapsibleContentArea>

      <Gap size="s" />

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
            <Absences
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
              isShiftCareUnit={unitInformation.daycare.roundTheClock}
              operationalDays={operationalDays}
              realtimeStaffAttendanceEnabled={realtimeStaffAttendanceEnabled}
              groups={groups}
              weekRange={weekRange}
            />
          ) : null}
        </CollapsibleContentArea>

        {selectedGroup.type === 'group' ||
        (selectedGroup.type === 'all-children' && mode === 'week') ? (
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
                weekDateRange={weekRange}
                unitId={unitId}
                groupId={
                  selectedGroup.type === 'group' ? selectedGroup.id : null
                }
              />
            </CollapsibleContentArea>
          </>
        ) : null}
      </CollapsibleContentArea>
    </>
  )
})

const Caretakers = React.memo(function Caretakers({
  unitData
}: {
  unitData: UnitData
}) {
  const { i18n } = useTranslation()

  const formatNumber = (num: number) =>
    parseFloat(num.toFixed(2)).toLocaleString()

  const min = formatNumber(unitData.caretakers.unitCaretakers.minimum)
  const max = formatNumber(unitData.caretakers.unitCaretakers.maximum)

  return min === max ? (
    <span>
      {min} {i18n.unit.info.caretakers.unitOfValue}
    </span>
  ) : (
    <span>{`${min} - ${max} ${i18n.unit.info.caretakers.unitOfValue}`}</span>
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
