// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPlus, faTrash } from 'Icons'
import orderBy from 'lodash/orderBy'
import partition from 'lodash/partition'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled, { css } from 'styled-components'
import { v4 as uuidv4 } from 'uuid'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UIContext } from 'employee-frontend/state/ui'
import FiniteDateRange from 'lib-common/finite-date-range'
import { BoundForm, useFormElems } from 'lib-common/form/hooks'
import {
  CalendarEvent,
  CalendarEventTime,
  CalendarEventTimeForm,
  DiscussionReservationDay
} from 'lib-common/generated/api-types/calendarevent'
import { ChildBasics } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { useMutation, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Tooltip from 'lib-components/atoms/Tooltip'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H3, fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import {
  addCalendarEventTimeMutation,
  groupDiscussionReservationDaysQuery
} from '../../queries'
import { ChildGroupInfo } from '../DiscussionSurveyView'
import { NewEventTimeForm } from '../survey-editor/DiscussionTimesForm'
import { eventTimeArray } from '../survey-editor/form'

import CalendarEventTimeInput from './CalendarEventTimeInput'
import CalendarEventTimeReservation, {
  DiscussionReservationModal
} from './CalendarEventTimeReservation'
import NewCalendarEventTimeEditor from './NewCalendarEventTimeEditor'

export interface CalendarMonth {
  month: number
  year: number
  weeks: CalendarWeek[]
}

export interface CalendarWeek {
  year: number
  weekNumber: number
  calendarDays: DiscussionReservationDay[]
}

type DateType = 'past' | 'today' | 'future' | 'otherMonth'
type CalendarMode = 'create' | 'reserve'

function dateType(year: number, month: number, date: LocalDate): DateType {
  if (date.year !== year || date.month !== month) return 'otherMonth'
  const today = LocalDate.todayInSystemTz()
  return date.isBefore(today) ? 'past' : date.isToday() ? 'today' : 'future'
}

function groupByWeek(days: DiscussionReservationDay[]): CalendarWeek[] {
  const weeks: CalendarWeek[] = []
  let currentWeek: CalendarWeek | undefined = undefined
  days.forEach((d) => {
    if (!currentWeek || currentWeek.weekNumber !== d.date.getIsoWeek()) {
      currentWeek = {
        year: d.date.year,
        weekNumber: d.date.getIsoWeek(),
        calendarDays: []
      }
      weeks.push(currentWeek)
    }
    currentWeek.calendarDays.push(d)
  })
  return weeks
}

export const groupByMonth = (
  dailyData: DiscussionReservationDay[]
): CalendarMonth[] => {
  const getWeekMonths = (weeklyData: CalendarWeek) => {
    const firstDay = weeklyData.calendarDays[0]
    const lastDay = weeklyData.calendarDays[weeklyData.calendarDays.length - 1]

    return firstDay.date.month === lastDay.date.month
      ? [[firstDay.date.month, firstDay.date.year]]
      : [
          [firstDay.date.month, firstDay.date.year],
          [lastDay.date.month, lastDay.date.year]
        ]
  }

  return groupByWeek(dailyData).reduce<CalendarMonth[]>(
    (monthlyData, weeklyData) => {
      const weekMonths = getWeekMonths(weeklyData).map(([month, year]) => ({
        month,
        year,
        weeks: [weeklyData]
      }))

      if (monthlyData.length === 0) {
        return weekMonths
      }

      const lastMonth = monthlyData[monthlyData.length - 1]
      const monthsBeforeLast = monthlyData.slice(0, monthlyData.length - 1)

      if (lastMonth.month === weekMonths[0].month) {
        return [
          ...monthsBeforeLast,
          {
            ...lastMonth,
            weeks: [...lastMonth.weeks, weeklyData]
          },
          ...(weekMonths[1] ? [weekMonths[1]] : [])
        ]
      }

      return [...monthsBeforeLast, lastMonth, ...weekMonths]
    },
    []
  )
}

export const OtherEventMarker = React.memo(
  function DiscussionReservationCalendar({
    events,
    surveys,
    date
  }: {
    events: CalendarEvent[]
    surveys: CalendarEvent[]
    date: LocalDate
  }) {
    const { i18n } = useTranslation()
    const t = i18n.unit.calendar.events.discussionReservation.calendar
    const eventCount = useMemo(
      () => events.length + surveys.length,
      [events, surveys]
    )
    const sortedSurveyTimesToday = useMemo(() => {
      const flatTimeInfos = surveys.flatMap((s) =>
        s.times
          .filter((time) => time.date.isEqual(date))
          .map((ft) => ({
            id: ft.id,
            startTime: ft.startTime,
            endTime: ft.endTime,
            title: s.title
          }))
      )
      return orderBy(flatTimeInfos, [(ti) => ti.startTime, (ti) => ti.endTime])
    }, [surveys, date])
    const tooltipContent = useMemo(
      () => (
        <>
          <span>{t.eventTooltipTitle}</span>
          <TooltipList>
            {events.map((e) => (
              <li key={`${date.format()}-${e.id}`}>{e.title}</li>
            ))}
            {sortedSurveyTimesToday.map((eti) => (
              <li
                key={`${date.format()}-${eti.id}-${eti.id}`}
              >{`${eti.startTime.format()} – ${eti.endTime.format()} ${eti.title}`}</li>
            ))}
          </TooltipList>
        </>
      ),
      [events, date, t.eventTooltipTitle, sortedSurveyTimesToday]
    )

    return (
      <Tooltip
        tooltip={tooltipContent}
        position={date.getIsoDayOfWeek() === 1 ? 'right' : 'bottom'}
        width="large"
      >
        <OtherEventRow data-qa="event-row">
          <span>{`${eventCount} ${eventCount > 1 ? t.otherEventPlural : t.otherEventSingular}`}</span>
        </OtherEventRow>
      </Tooltip>
    )
  }
)

export const DiscussionReservationCalendar = React.memo(
  function DiscussionReservationCalendar({
    unitId,
    groupId,
    eventData,
    invitees,
    calendarRange,
    times,
    addAction,
    removeAction
  }: {
    unitId: UUID
    groupId: UUID
    eventData: CalendarEvent | null
    invitees: ChildGroupInfo[]
    calendarRange: FiniteDateRange
    times: BoundForm<typeof eventTimeArray>
    addAction: (et: NewEventTimeForm) => void
    removeAction: (id: UUID) => void
  }) {
    const [reservationModalVisible, setReservationModalVisible] =
      useState(false)

    const [selectedEventTime, setSelectedEventTime] =
      useState<CalendarEventTime | null>(null)

    const calendarDays = useQueryResult(
      groupDiscussionReservationDaysQuery(
        unitId,
        groupId,
        calendarRange.start,
        calendarRange.end
      )
    )

    const dailyInvitees = useMemo(
      () =>
        invitees
          .filter((i) =>
            i.groupPlacements.some(
              (gp) => selectedEventTime && gp.includes(selectedEventTime.date)
            )
          )
          .map((i) => i.child),
      [selectedEventTime, invitees]
    )

    return (
      <>
        {reservationModalVisible && selectedEventTime && eventData && (
          <DiscussionReservationModal
            eventTime={selectedEventTime}
            onClose={() => {
              setReservationModalVisible(false)
              setSelectedEventTime(null)
            }}
            invitees={dailyInvitees}
            eventData={eventData}
          />
        )}

        {renderResult(calendarDays, (calendarDaysResult) => {
          const calendarMonths = groupByMonth(calendarDaysResult)
          return (
            <div>
              {calendarMonths.map((m) => (
                <TimesMonth
                  month={m.month}
                  weeks={m.weeks}
                  eventData={eventData}
                  year={m.year}
                  key={`${m.month}${m.year}`}
                  reserveAction={(et) => {
                    setSelectedEventTime(et)
                    setReservationModalVisible(true)
                  }}
                  removeAction={removeAction}
                  addAction={addAction}
                  times={times}
                  editMode="reserve"
                  reservationChildren={invitees.map((i) => i.child)}
                />
              ))}
            </div>
          )
        })}
      </>
    )
  }
)

export const DiscussionTimesCalendar = React.memo(
  function DiscussionReservationCalendar({
    unitId,
    groupId,
    times,
    addAction,
    removeAction,
    calendarRange
  }: {
    unitId: UUID
    groupId: UUID
    times: BoundForm<typeof eventTimeArray>
    addAction: (date: NewEventTimeForm) => void
    removeAction: (id: UUID) => void
    calendarRange: FiniteDateRange
  }) {
    const calendarDays = useQueryResult(
      groupDiscussionReservationDaysQuery(
        unitId,
        groupId,
        calendarRange.start,
        calendarRange.end
      )
    )

    return (
      <>
        {renderResult(calendarDays, (calendarDaysResult) => {
          const calendarMonths = groupByMonth(calendarDaysResult)
          return (
            <div>
              {calendarMonths.map((m) => (
                <TimesMonth
                  key={`${m.month}${m.year}`}
                  year={m.year}
                  month={m.month}
                  weeks={m.weeks}
                  times={times}
                  eventData={null}
                  addAction={addAction}
                  removeAction={removeAction}
                  editMode="create"
                  reservationChildren={[]}
                />
              ))}
            </div>
          )
        })}
      </>
    )
  }
)

export const TimesMonth = React.memo(function TimesMonth({
  eventData,
  year,
  month,
  weeks,
  times,
  addAction,
  removeAction,
  reserveAction,
  editMode,
  reservationChildren
}: {
  eventData: CalendarEvent | null
  year: number
  month: number
  weeks: CalendarWeek[]
  times: BoundForm<typeof eventTimeArray>
  addAction: (et: NewEventTimeForm) => void
  removeAction: (id: UUID) => void
  reserveAction?: (eventTime: CalendarEventTime) => void
  editMode: CalendarMode
  reservationChildren: ChildBasics[]
}) {
  const { i18n } = useTranslation()
  const monthHasCurrentOrFutureDays = useMemo(
    () =>
      weeks.some((w) =>
        w.calendarDays.some(
          (d) =>
            d.date.month === month &&
            d.date.isEqualOrAfter(LocalDate.todayInSystemTz())
        )
      ),
    [weeks, month]
  )

  return monthHasCurrentOrFutureDays ? (
    <ContentArea opaque={false} key={`${month}${year}`} paddingHorizontal="2px">
      <H3>{`${i18n.common.datetime.months[month - 1]} ${year}`}</H3>
      <Grid>
        {weeks.map((w) => (
          <TimesWeek
            eventData={eventData}
            key={`${w.weekNumber}${month}${year}`}
            year={year}
            month={month}
            week={w}
            times={times}
            addAction={addAction}
            removeAction={removeAction}
            reserveAction={reserveAction}
            editMode={editMode}
            reservationChildren={reservationChildren}
          />
        ))}
      </Grid>
    </ContentArea>
  ) : null
})

export const TimesWeek = React.memo(function TimesWeek({
  eventData,
  year,
  month,
  week,
  times,
  addAction,
  removeAction,
  reserveAction,
  reservationChildren
}: {
  eventData: CalendarEvent | null
  year: number
  month: number
  week: CalendarWeek
  times: BoundForm<typeof eventTimeArray>
  addAction: (et: NewEventTimeForm) => void
  removeAction: (id: UUID) => void
  reserveAction?: (eventTime: CalendarEventTime) => void
  editMode: CalendarMode
  reservationChildren: ChildBasics[]
}) {
  return (
    <>
      {week.calendarDays.map((d) =>
        eventData === null ? (
          times ? (
            <TimesDay
              key={`${d.date.formatIso()}${month}${year}`}
              day={d}
              times={times}
              dateType={dateType(year, month, d.date)}
              addAction={addAction}
              removeAction={removeAction}
            />
          ) : null
        ) : (
          reserveAction && (
            <TimesReservationDay
              eventData={eventData}
              removeAction={removeAction}
              addAction={addAction}
              times={times}
              key={`${d.date.formatIso()}${month}${year}`}
              day={d}
              dateType={dateType(year, month, d.date)}
              reserveAction={reserveAction}
              reservationChildren={reservationChildren}
            />
          )
        )
      )}
    </>
  )
})

export const TimesDay = React.memo(function TimesDay({
  times,
  day,
  dateType,
  addAction,
  removeAction
}: {
  times: BoundForm<typeof eventTimeArray>
  day: DiscussionReservationDay
  dateType: DateType
  addAction: (et: NewEventTimeForm) => void
  removeAction: (id: UUID) => void
}) {
  const { i18n } = useTranslation()

  const isWeekend = useMemo(
    () => [6, 7].includes(day.date.getIsoDayOfWeek()),
    [day.date]
  )

  const isOperationDay = useMemo(
    () => !day.isHoliday && day.isOperationalDay,
    [day.isHoliday, day.isOperationalDay]
  )

  const eventTimes = useFormElems(times)
  const eventTimesToday = useMemo(
    () => eventTimes.filter((t) => t.state.date.isEqual(day.date)),
    [eventTimes, day.date]
  )

  const [otherEvents, otherSurveys] = useMemo(
    () => partition(day.events, (ce) => ce.times.length === 0),
    [day.events]
  )

  return dateType === 'otherMonth' || dateType === 'past' ? (
    <InactiveCell />
  ) : (
    <DayCell
      $today={dateType === 'today'}
      $holiday={!isOperationDay}
      $weekend={isWeekend}
      data-qa={`times-calendar-day-${day.date.formatIso()}`}
    >
      <DayCellHeader>
        <DayCellDate $today={dateType === 'today'}>
          {`${i18n.common.datetime.weekdaysShort[day.date.getIsoDayOfWeek() - 1]} ${day.date.format('d.M.')}`}
        </DayCellDate>
      </DayCellHeader>

      {!isWeekend && isOperationDay && (
        <>
          {otherEvents.length > 0 ||
            (otherSurveys.length > 0 && (
              <EventContainer data-qa="events">
                <OtherEventMarker
                  events={otherEvents}
                  surveys={otherSurveys}
                  date={day.date}
                />
              </EventContainer>
            ))}
          {eventTimesToday && eventTimesToday.length > 0 && (
            <TimesContainer data-qa="times" className="edit">
              {eventTimesToday.map((t, i) => (
                <FixedSpaceRow
                  spacing="s"
                  alignItems="center"
                  key={`${day.date.format()}-time-input-${i}`}
                >
                  <CalendarEventTimeInput bind={t} />
                  <InlineButton
                    icon={faTrash}
                    onClick={() => removeAction(t.state.id ?? '')}
                    text=""
                  />
                </FixedSpaceRow>
              ))}
            </TimesContainer>
          )}
          <ButtonContainer>
            <InlineButton
              onClick={() =>
                addAction({
                  id: uuidv4(),
                  date: day.date,
                  childId: null,
                  timeRange: { startTime: '', endTime: '' }
                })
              }
              data-qa={`${day.date.formatIso()}-add-time-button`}
              icon={faPlus}
              text={i18n.common.add}
            />
          </ButtonContainer>
        </>
      )}
    </DayCell>
  )
})

export const TimesReservationDay = React.memo(function TimesReservationDay({
  reservationChildren,
  eventData,
  day,
  dateType,
  reserveAction,
  addAction,
  removeAction,
  times
}: {
  reservationChildren: ChildBasics[]
  eventData: CalendarEvent
  day: DiscussionReservationDay
  dateType: DateType
  reserveAction: (eventTime: CalendarEventTime) => void
  addAction: (eventTime: NewEventTimeForm) => void
  removeAction: (eventTimeId: UUID) => void
  times: BoundForm<typeof eventTimeArray>
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events.discussionReservation

  const isOperationDay = useMemo(
    () => !day.isHoliday && day.isOperationalDay,
    [day.isHoliday, day.isOperationalDay]
  )
  const isWeekend = useMemo(
    () => [6, 7].includes(day.date.getIsoDayOfWeek()),
    [day.date]
  )

  const { setErrorMessage } = useContext(UIContext)

  const { mutateAsync: addCalendarEventTime } = useMutation(
    addCalendarEventTimeMutation
  )

  const reservationEventTimes = useMemo(() => {
    const timesToday =
      eventData.times.filter((t) => t.date.isEqual(day.date)) ?? []
    const sortedTimesToday = orderBy(timesToday, [
      (e) => e.startTime,
      (e) => e.endTime,
      (e) => e.id
    ])
    return sortedTimesToday
  }, [eventData.times, day.date])

  const [otherEvents, otherSurveys] = useMemo(() => {
    const otherEvents = day.events.filter((e) => e.id !== eventData.id)
    return partition(otherEvents, (ce) => ce.times.length === 0)
  }, [day.events, eventData.id])

  const createNewTime = useCallback(
    () =>
      addAction({
        id: uuidv4(),
        date: day.date,
        childId: null,
        timeRange: { startTime: '', endTime: '' }
      }),
    [addAction, day.date]
  )

  const newTimeElems = useFormElems(times)

  const dailyTimes = useMemo(
    () => newTimeElems.filter((t) => t.state.date.isEqual(day.date)),
    [newTimeElems, day.date]
  )

  return dateType === 'otherMonth' || dateType === 'past' ? (
    <InactiveCell />
  ) : (
    <DayCell
      $today={dateType === 'today'}
      $holiday={!isOperationDay && !isWeekend}
      $weekend={isWeekend}
      data-qa={`times-calendar-day-${day.date.formatIso()}`}
    >
      <DayCellHeader>
        <DayCellDate $today={dateType === 'today'}>
          {`${i18n.common.datetime.weekdaysShort[day.date.getIsoDayOfWeek() - 1]} ${day.date.format('d.M.')}`}
        </DayCellDate>
      </DayCellHeader>
      {!isWeekend && isOperationDay && (
        <>
          {(otherEvents.length > 0 || otherSurveys.length > 0) && (
            <EventContainer data-qa="events">
              <OtherEventMarker
                events={otherEvents}
                surveys={otherSurveys}
                date={day.date}
              />
            </EventContainer>
          )}

          {reservationEventTimes && reservationEventTimes.length > 0 && (
            <TimesContainer className="reserve">
              {reservationEventTimes.map((e, i) => (
                <CalendarEventTimeReservation
                  key={`${day.date.format()}-reserve-input-${i}`}
                  eventTime={e}
                  reservationChild={reservationChildren.find(
                    (c) => c.id === e.childId
                  )}
                  reserveAction={reserveAction}
                />
              ))}
            </TimesContainer>
          )}

          {dailyTimes && dailyTimes.length > 0 && (
            <TimesContainer>
              {dailyTimes.map((time) => (
                <NewCalendarEventTimeEditor
                  key={`${day.date.format()}-new-time-input-${time.state.id}`}
                  bind={time}
                  addAction={(et: CalendarEventTimeForm) => {
                    addCalendarEventTime({
                      eventId: eventData.id,
                      form: et
                    })
                      .then(() => {
                        removeAction(time.state.id)
                      })
                      .catch(() =>
                        setErrorMessage({
                          title: t.eventTime.addError,
                          type: 'error',
                          resolveLabel: i18n.common.ok
                        })
                      )
                  }}
                  removeAction={removeAction}
                />
              ))}
            </TimesContainer>
          )}

          <ButtonContainer>
            <InlineButton
              onClick={createNewTime}
              data-qa={`${day.date.formatIso()}-add-time-button`}
              icon={faPlus}
              text={i18n.common.add}
            />
          </ButtonContainer>
        </>
      )}
    </DayCell>
  )
})

const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(5, 1fr) repeat(2, 70px);
  > * {
    margin-top: 0;
    margin-left: 0;
    margin-bottom: 1px;
    margin-right: 1px;
  }
`

const DayCell = styled.div<{
  $today: boolean
  $holiday: boolean
  $weekend: boolean
}>`
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  position: relative;
  min-height: 150px;
  padding: ${defaultMargins.s} 0px ${defaultMargins.s} 0px;
  background-color: ${(p) =>
    p.$holiday || p.$weekend
      ? p.theme.colors.grayscale.g4
      : p.theme.colors.grayscale.g0};
  border: none;
  outline: 1px solid ${colors.grayscale.g15};
  user-select: none;
  text-align: left;

  ${(p) =>
    p.$today
      ? css`
          border-top: 4px solid ${colors.status.success};
          padding-top: calc(${defaultMargins.s} - 3px);
        `
      : ''};
  font-size: 14px;
`

const DayCellHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin: 0px 0px ${defaultMargins.s} ${defaultMargins.xs};
  width: 100%;
`

const DayCellDate = styled.div<{ $today: boolean }>`
  font-family: Montserrat, sans-serif;
  font-style: normal;
  color: ${(p) => (p.$today ? colors.main.m1 : colors.grayscale.g100)};
  font-weight: ${fontWeights.normal};
`

const InactiveCell = styled.div`
  background-color: transparent;
`

const TimesContainer = styled.div`
  margin-bottom: 10px;

  &.edit {
    margin-left: 10px;
  }
`

const EventContainer = styled.div`
  margin-bottom: 10px;
`

const ButtonContainer = styled.div`
  margin-left: 10px;
`
const OtherEventRow = styled(FixedSpaceColumn)`
  color: ${(p) => p.theme.colors.main.m1};
  border-left: 6px solid transparent;
  padding: ${defaultMargins.xxs} calc(${defaultMargins.s} - 6px);
  border-color: ${(p) => p.theme.colors.accents.a9pink};
  margin-top: ${defaultMargins.xs};
`
const TooltipList = styled.ul`
  margin: 0px;
  padding: 0 0 0 20px;
`
