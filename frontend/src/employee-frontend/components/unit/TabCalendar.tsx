// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useMemo, useRef, useState } from 'react'
import styled from 'styled-components'

import { UserContext } from 'employee-frontend/state/user'
import { combine, isLoading, Result } from 'lib-common/api'
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
import { faChevronLeft, faChevronRight, faCalendarAlt } from 'lib-icons'

import { getDaycareGroups, UnitData } from '../../api/unit'
import { useTranslation } from '../../state/i18n'
import { UnitContext } from '../../state/unit'
import { requireRole } from '../../utils/roles'
import { UUID_REGEX } from '../../utils/validation/validations'
import Absences from '../absences/Absences'
import { renderResult } from '../async-rendering'
import { DataList } from '../common/DataList'

import UnitDataFilters from './UnitDataFilters'
import CalendarEventsSection from './tab-calendar/CalendarEventsSection'
import GroupSelector from './tab-calendar/GroupSelector'
import Occupancy from './tab-unit-information/Occupancy'
import UnitAttendanceReservationsView from './unit-reservations/UnitAttendanceReservationsView'

type CalendarMode = 'week' | 'month'
type GroupId = UUID
export type AttendanceGroupFilter =
  | GroupId
  | 'no-group'
  | 'staff'
  | 'all'
  | null

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

const getWeekDateRange = (date: LocalDate, operationalDays: number[]) => {
  const start = date.startOfWeek()
  return new FiniteDateRange(
    start,
    start.addDays(Math.max(...operationalDays) - 1)
  )
}

const getDefaultGroup = (groupParam: string): AttendanceGroupFilter | null =>
  ['no-group', 'staff', 'all'].includes(groupParam) ||
  UUID_REGEX.test(groupParam)
    ? groupParam
    : null

export interface WeekData {
  dateRange: FiniteDateRange
  saved: boolean
  savingPromise: Promise<void>
}

export type WeekSavingFns = Map<string, () => Promise<void>>

export default React.memo(function TabCalendar() {
  const { i18n } = useTranslation()
  const { id: unitId } = useNonNullableParams<{ id: UUID }>()
  const { unitInformation, unitData, filters, setFilters } =
    useContext(UnitContext)
  const { roles } = useContext(UserContext)

  const query = useQuery()

  const modeParam = query.get('mode')
  const [mode, setMode] = useState<CalendarMode>(
    modeParam && ['month', 'week'].includes(modeParam)
      ? (modeParam as CalendarMode)
      : 'month'
  )

  const selectedDateParam = query.get('date')
  const [selectedDate, setSelectedDate] = useState<LocalDate>(
    selectedDateParam
      ? LocalDate.parseIso(selectedDateParam)
      : LocalDate.todayInSystemTz()
  )

  const groupParam = query.get('group')
  const [groupId, setGroupId] = useState<AttendanceGroupFilter>(() =>
    groupParam ? getDefaultGroup(groupParam) : null
  )

  useSyncQueryParams({
    ...(groupId ? { group: groupId } : {}),
    date: selectedDate.toString(),
    mode
  })

  const operationalDays = useMemo(
    () =>
      unitInformation
        .map(({ daycare }) => daycare.operationDays)
        .getOrElse(null) ?? [1, 2, 3, 4, 5],
    [unitInformation]
  )

  // Before changing the week, the current week's data should be saved
  // because it is possible the user has started adding an overnight
  // entry over the week boundary, so the partial data should be saved
  // before navigating to the next week. The callbacks to save the data
  // are stored here, and added in the row components.
  const weekSavingFns = useRef<WeekSavingFns>(new Map())

  const [week, setWeek] = useState({
    dateRange: getWeekDateRange(selectedDate, operationalDays),
    saved: true,
    savingPromise: Promise.resolve()
  })

  useEffect(() => {
    let cancelled = false

    const dateRange = getWeekDateRange(selectedDate, operationalDays)
    setWeek({
      dateRange,
      saved: false,
      savingPromise: Promise.all(
        Array.from(weekSavingFns.current.values()).map((fn) => fn())
      ).then(() => {
        if (!cancelled) {
          setWeek((week) =>
            // strict equality check: ensure the current week is the
            // same one as when originally started, even when switching
            // going x* -> y -> x the save at * should be ignored at the end
            week.dateRange === dateRange
              ? {
                  ...week,
                  saved: true
                }
              : week
          )
        }
      })
    })

    return () => {
      cancelled = true
    }
  }, [selectedDate, operationalDays])

  const [groups] = useApiState(() => getDaycareGroups(unitId), [unitId])

  const reservationEnabled = unitInformation
    .map((u) => u.daycare.enabledPilotFeatures.includes('RESERVATIONS'))
    .getOrElse(false)

  const realtimeStaffAttendanceEnabled = unitInformation
    .map((u) =>
      u.daycare.enabledPilotFeatures.includes('REALTIME_STAFF_ATTENDANCE')
    )
    .getOrElse(false)

  useEffect(() => {
    if (realtimeStaffAttendanceEnabled) {
      setMode('week')
    }
  }, [realtimeStaffAttendanceEnabled])

  const onlyShowWeeklyView =
    groupId === 'staff' || groupId === 'no-group' || groupId === 'all'

  const [calendarOpen, setCalendarOpen] = useState(true)
  const [attendancesOpen, setAttendancesOpen] = useState(true)
  const [eventsOpen, setEventsOpen] = useState(true)
  const [occupanciesOpen, setOccupanciesOpen] = useState(true)

  return (
    <>
      <CollapsibleContentArea
        open={calendarOpen}
        toggleOpen={() => setCalendarOpen(!calendarOpen)}
        title={<H3 noMargin>{i18n.unit.calendar.title}</H3>}
        opaque
      >
        {(reservationEnabled || realtimeStaffAttendanceEnabled) &&
          !onlyShowWeeklyView && (
            <FixedSpaceRow spacing="xs" justifyContent="flex-end">
              {(['week', 'month'] as const).map((m) => (
                <ChoiceChip
                  key={m}
                  data-qa={`choose-calendar-mode-${m}`}
                  text={i18n.unit.calendar.modes[m]}
                  selected={mode === m}
                  onChange={() => setMode(m)}
                />
              ))}
            </FixedSpaceRow>
          )}

        <StickyTopBar>
          <div data-qa-week-range={week.dateRange.toString()} />
          <FixedSpaceRow spacing="s" alignItems="center">
            <GroupSelectorWrapper>
              <GroupSelector
                groups={groups}
                selected={groupId}
                onSelect={setGroupId}
                data-qa="attendances-group-select"
                realtimeStaffAttendanceEnabled={realtimeStaffAttendanceEnabled}
              />
            </GroupSelectorWrapper>

            <ColoredH3 noMargin>
              {week.dateRange.start.format('dd.MM.')}–
              {week.dateRange.end.format('dd.MM.yyyy')}
            </ColoredH3>
            <FixedSpaceRow spacing="xxs">
              <IconButton
                icon={faChevronLeft}
                onClick={() => setSelectedDate(selectedDate.subDays(7))}
                data-qa="previous-week"
                aria-label={i18n.unit.calendar.previousWeek}
              />
              <IconButton
                icon={faChevronRight}
                onClick={() => setSelectedDate(selectedDate.addDays(7))}
                data-qa="next-week"
                aria-label={i18n.unit.calendar.nextWeek}
              />
            </FixedSpaceRow>
            <InlineButton
              icon={faCalendarAlt}
              text={i18n.common.today}
              onClick={() => setSelectedDate(LocalDate.todayInSystemTz())}
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
          {mode === 'month' && groupId !== null && !onlyShowWeeklyView && (
            <Absences
              groupId={groupId}
              selectedDate={selectedDate}
              reservationEnabled={reservationEnabled}
              staffAttendanceEnabled={!realtimeStaffAttendanceEnabled}
            />
          )}

          {((mode === 'week' && groupId !== null) || onlyShowWeeklyView) && (
            <UnitAttendanceReservationsView
              unitId={unitId}
              groupId={groupId}
              selectedDate={selectedDate}
              setSelectedDate={setSelectedDate}
              isShiftCareUnit={unitInformation
                .map(({ daycare }) => daycare.roundTheClock)
                .getOrElse(false)}
              operationalDays={operationalDays}
              realtimeStaffAttendanceEnabled={realtimeStaffAttendanceEnabled}
              groups={groups}
              week={week}
              weekSavingFns={weekSavingFns}
            />
          )}
        </CollapsibleContentArea>

        {groupId !== 'no-group' &&
          groupId !== 'staff' &&
          (mode === 'week' || onlyShowWeeklyView) && (
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
                  weekDateRange={week.dateRange}
                  unitId={unitId}
                  selectedGroupId={groupId}
                />
              </CollapsibleContentArea>
            </>
          )}
      </CollapsibleContentArea>

      <Gap size="s" />

      <CollapsibleContentArea
        title={<H3 noMargin>{i18n.unit.occupancies}</H3>}
        open={occupanciesOpen}
        toggleOpen={() => setOccupanciesOpen(!occupanciesOpen)}
        opaque
        data-qa="unit-attendances"
        data-isloading={isLoading(unitData)}
      >
        <FixedSpaceRow alignItems="center">
          <Label>{i18n.unit.filters.title}</Label>
          <UnitDataFilters
            canEdit={requireRole(
              roles,
              'ADMIN',
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
        {renderResult(
          combine(unitData, unitInformation),
          ([unitData, unitInformation]) =>
            unitData.unitOccupancies ? (
              <Occupancy
                filters={filters}
                occupancies={unitData.unitOccupancies}
                realtimeStaffAttendanceEnabled={realtimeStaffAttendanceEnabled}
                shiftCareUnit={unitInformation.daycare.roundTheClock}
              />
            ) : null
        )}
      </CollapsibleContentArea>
    </>
  )
})

const Caretakers = React.memo(function Caretakers({
  unitData
}: {
  unitData: Result<UnitData>
}) {
  const { i18n } = useTranslation()

  const formatNumber = (num: number) =>
    parseFloat(num.toFixed(2)).toLocaleString()

  return unitData
    .map((unitData) => {
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
    .getOrElse(null)
})
