// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import { combine, isLoading } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'
import { useQuery, useQueryResult } from 'lib-common/query'
import Main from 'lib-components/atoms/Main'
import { ContentArea } from 'lib-components/layout/Container'
import { Desktop, RenderOnlyOn } from 'lib-components/layout/responsive-layout'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'

import Footer from '../Footer'
import RequireAuth from '../RequireAuth'
import { renderResult } from '../async-rendering'
import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import useTitle from '../useTitle'
import { focusElementOnNextFrame } from '../utils/focus'

import AbsenceModal from './AbsenceModal'
import ActionPickerModal from './ActionPickerModal'
import CalendarListView from './CalendarListView'
import CalendarMonthView from './CalendarMonthView'
import CalendarNotifications from './CalendarNotifications'
import DailyServiceTimeNotifications from './DailyServiceTimeNotifications'
import DayView from './DayView'
import ReservationModal from './ReservationModal'
import DiscussionReservationModal from './discussion-reservation-modal/DiscussionReservationModal'
import DiscussionSurveyModal from './discussion-reservation-modal/DiscussionSurveyModal'
import { showModalEventTime } from './discussion-reservation-modal/discussion-survey'
import FixedPeriodSelectionModal from './holiday-modal/FixedPeriodSelectionModal'
import OpenRangesSelectionModal from './holiday-modal/OpenRangesSelectionModal'
import { useExtendedReservationsRange } from './hooks'
import { useCalendarModalState } from './modalState'
import {
  activeQuestionnaireQuery,
  calendarEventsQuery,
  holidayPeriodsQuery
} from './queries'

const defaultRange = new FiniteDateRange(
  LocalDate.todayInSystemTz().subMonths(1).startOfMonth().startOfWeek(),
  LocalDate.todayInSystemTz().addYears(1).lastDayOfMonth()
)

function useEventsRange(
  eventsRange: FiniteDateRange
): Result<CitizenCalendarEvent[]> {
  return useQueryResult(
    calendarEventsQuery({
      start: eventsRange.start,
      end: eventsRange.end
    })
  )
}

function getPreviousMonthRangeBeforeDate(
  beforeDate: LocalDate,
  rangeEnd: LocalDate
): FiniteDateRange {
  const start = beforeDate.subDays(1).startOfMonth().startOfWeek()
  return new FiniteDateRange(start, rangeEnd)
}

const CalendarPage = React.memo(function CalendarPage() {
  const user = useUser()
  const events = useEventsRange(defaultRange)
  const [reservationsDateRange, setReservationsDateRange] =
    useState<FiniteDateRange>(defaultRange)
  const { reservations: data, loading: reservationsLoading } =
    useExtendedReservationsRange(reservationsDateRange)

  const {
    modalState,
    openPickActionModal,
    openReservationModal,
    openHolidayModal,
    openAbsenceModal,
    openDiscussionSurveyModal,
    openDiscussionReservationModal,
    openDayModal,
    closeModal
  } = useCalendarModalState()

  const openReservationModalWithoutInitialRange = useCallback(() => {
    openReservationModal(undefined)
  }, [openReservationModal])

  const fetchPreviousData = useCallback(
    (beforeDate: LocalDate) =>
      setReservationsDateRange(
        getPreviousMonthRangeBeforeDate(beforeDate, reservationsDateRange.end)
      ),
    [reservationsDateRange.end]
  )

  // Based on the initial data fetch, index 1 represents the current month
  const [selectedMonthIndex, setSelectedMonthIndex] = useState(1)
  const prevMonth = useCallback(
    (beforeDate: LocalDate) => {
      setSelectedMonthIndex((prevIndex) => {
        if (prevIndex === 0) {
          fetchPreviousData(beforeDate)
        }
        return prevIndex > 0 ? prevIndex - 1 : prevIndex
      })
    },
    [fetchPreviousData]
  )
  const nextMonth = useCallback((monthDataLength: number) => {
    setSelectedMonthIndex((prevIndex) =>
      prevIndex < monthDataLength - 1 ? prevIndex + 1 : prevIndex
    )
  }, [])

  const dayIsReservable = useCallback(
    (date: LocalDate) =>
      data.map((data) => data.reservableRange.includes(date)).getOrElse(false),
    [data]
  )

  const holidayPeriods = useQueryResult(holidayPeriodsQuery())
  const dayIsHolidayPeriod = useCallback(
    (date: LocalDate) =>
      holidayPeriods
        .map((periods) => periods.some((p) => p.period.includes(date)))
        .getOrElse(false),
    [holidayPeriods]
  )

  const { data: questionnaire } = useQuery(activeQuestionnaireQuery())

  const firstReservableDate = useMemo(() => {
    if (data.isSuccess) {
      // First reservable day that has no reservations
      const firstReservableEmptyDate = data.value.days.find(
        (day) =>
          data.value.reservableRange.includes(day.date) &&
          day.children.length > 0 &&
          day.children.some(
            (child) =>
              child.scheduleType === 'RESERVATION_REQUIRED' &&
              child.reservations.length === 0 &&
              !child.absence
          )
      )
      return firstReservableEmptyDate?.date ?? null
    } else {
      return null
    }
  }, [data])

  if (!user || !user.accessibleFeatures.reservations) return null

  return (
    <>
      <DailyServiceTimeNotifications />
      {renderResult(
        combine(data, events, holidayPeriods),
        ([response, events, holidayPeriods]) => {
          //check whether user has any discussion surveys in open/editable state
          const showDiscussions = events.some(
            (e) =>
              e.eventType === 'DISCUSSION_SURVEY' &&
              Object.values(e.timesByChild).some((times) =>
                times!.some((t) =>
                  showModalEventTime(t, LocalDate.todayInHelsinkiTz())
                )
              )
          )
          return (
            <div data-qa="calendar-page" data-isloading={isLoading(data)}>
              <CalendarNotifications
                calendarDays={response.days}
                events={events}
              />
              <RenderOnlyOn mobile tablet>
                <ContentArea
                  opaque
                  paddingVertical="zero"
                  paddingHorizontal="zero"
                >
                  <CalendarListView
                    childData={response.children}
                    calendarDays={response.days}
                    onHoverButtonClick={openPickActionModal}
                    selectDate={openDayModal}
                    dayIsReservable={dayIsReservable}
                    dayIsHolidayPeriod={dayIsHolidayPeriod}
                    events={events}
                    showDiscussionAction={showDiscussions}
                    fetchPrevious={fetchPreviousData}
                    loading={reservationsLoading}
                  />
                </ContentArea>
              </RenderOnlyOn>
              <RenderOnlyOn desktop>
                <CalendarMonthView
                  childData={response.children}
                  calendarDays={response.days}
                  onCreateReservationClicked={
                    openReservationModalWithoutInitialRange
                  }
                  onCreateAbsencesClicked={(date) =>
                    openAbsenceModal(date, false)
                  }
                  onOpenDiscussionReservationsClicked={
                    openDiscussionSurveyModal
                  }
                  onReportHolidaysClicked={openHolidayModal}
                  selectedDate={
                    modalState?.type === 'day' ? modalState.date : undefined
                  }
                  selectDate={openDayModal}
                  includeWeekends={true}
                  dayIsReservable={dayIsReservable}
                  events={events}
                  isDiscussionActionVisible={showDiscussions}
                  loading={reservationsLoading}
                  prevMonth={prevMonth}
                  nextMonth={nextMonth}
                  selectedMonthIndex={selectedMonthIndex}
                  setSelectedMonthIndex={setSelectedMonthIndex}
                />
              </RenderOnlyOn>
              {modalState?.type === 'day' && (
                <DayView
                  date={modalState.date}
                  reservationsResponse={response}
                  selectDate={openDayModal}
                  onClose={() => {
                    closeModal()
                    focusElementOnNextFrame(
                      `calendar-day-${modalState.date.formatIso()}`
                    )
                  }}
                  openAbsenceModal={(date) => openAbsenceModal(date, true)}
                  events={events}
                  holidayPeriods={holidayPeriods}
                />
              )}
              {modalState?.type === 'pickAction' && (
                <ActionPickerModal
                  close={closeModal}
                  openReservations={openReservationModalWithoutInitialRange}
                  openDiscussionReservations={openDiscussionSurveyModal}
                  openAbsences={(date) => openAbsenceModal(date, false)}
                  openHolidays={openHolidayModal}
                  isDiscussionActionVisible={showDiscussions}
                />
              )}
              {modalState?.type === 'reservations' && (
                <ReservationModal
                  onClose={closeModal}
                  reservationsResponse={response}
                  onSuccess={closeModal}
                  initialStart={
                    modalState.initialRange?.start ?? firstReservableDate
                  }
                  initialEnd={modalState.initialRange?.end ?? null}
                  holidayPeriods={holidayPeriods}
                />
              )}
              {modalState?.type === 'absences' && (
                <AbsenceModal
                  close={closeModal}
                  onReturn={
                    modalState.returnToDayModal && modalState.initialDate
                      ? () =>
                          openDayModal(
                            modalState.initialDate ??
                              LocalDate.todayInHelsinkiTz()
                          )
                      : closeModal
                  }
                  initialDate={modalState.initialDate}
                  reservationsResponse={response}
                  holidayPeriods={holidayPeriods}
                />
              )}
              {featureFlags.discussionReservations &&
                modalState?.type === 'discussions' && (
                  <DiscussionSurveyModal
                    close={closeModal}
                    childData={response.children}
                    surveys={events.filter(
                      (e) =>
                        e.eventType === 'DISCUSSION_SURVEY' &&
                        e.period.end.isEqualOrAfter(LocalDate.todayInSystemTz())
                    )}
                    openDiscussionReservations={openDiscussionReservationModal}
                  />
                )}

              {featureFlags.discussionReservations &&
                modalState?.type === 'discussion-reservations' &&
                !!modalState.selectedChildId &&
                !!modalState.selectedEventId && (
                  <DiscussionReservationModal
                    close={closeModal}
                    childData={response.children.find(
                      (c) => c.id === modalState.selectedChildId
                    )}
                    eventData={events.find(
                      (e) => e.id === modalState.selectedEventId
                    )}
                  />
                )}
              {modalState?.type === 'holidays' && questionnaire && (
                <RequireAuth
                  strength={
                    questionnaire.questionnaire.requiresStrongAuth
                      ? 'STRONG'
                      : 'WEAK'
                  }
                >
                  {questionnaire.questionnaire.type === 'FIXED_PERIOD' ? (
                    <FixedPeriodSelectionModal
                      close={closeModal}
                      questionnaire={questionnaire.questionnaire}
                      availableChildren={response.children}
                      eligibleChildren={questionnaire.eligibleChildren}
                      previousAnswers={questionnaire.previousAnswers}
                    />
                  ) : questionnaire.questionnaire.type === 'OPEN_RANGES' ? (
                    <OpenRangesSelectionModal
                      close={closeModal}
                      questionnaire={questionnaire.questionnaire}
                      availableChildren={response.children}
                      eligibleChildren={questionnaire.eligibleChildren}
                      previousAnswers={questionnaire.previousAnswers}
                    />
                  ) : (
                    <div>Not Yet Implemented</div>
                  )}
                </RequireAuth>
              )}
            </div>
          )
        }
      )}
    </>
  )
})

const DesktopOnly = styled(Desktop)`
  position: relative;
`

export default React.memo(function CalendarPageWrapper() {
  const i18n = useTranslation()
  useTitle(i18n, i18n.calendar.title)

  return (
    <>
      <Main>
        <CalendarPage />
      </Main>
      <Footer />
      <DesktopOnly>
        <Gap size="X4L" />
      </DesktopOnly>
    </>
  )
})
