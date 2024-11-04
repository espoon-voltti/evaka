// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { focusElementOnNextFrame } from 'citizen-frontend/utils/focus'
import { combine, isLoading, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { ReservationsResponse } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { useQuery, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { NotificationsContext } from 'lib-components/Notifications'
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

import AbsenceModal from './AbsenceModal'
import ActionPickerModal from './ActionPickerModal'
import CalendarGridView from './CalendarGridView'
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
import {
  activeQuestionnaireQuery,
  calendarEventsQuery,
  holidayPeriodsQuery,
  reservationsQuery
} from './queries'

function useReservationsDefaultRange(): Result<ReservationsResponse> {
  return useQueryResult(
    reservationsQuery({
      from: LocalDate.todayInSystemTz()
        .subMonths(1)
        .startOfMonth()
        .startOfWeek(),
      to: LocalDate.todayInSystemTz().addYears(1).lastDayOfMonth()
    })
  )
}

function useEventsDefaultRange(): Result<CitizenCalendarEvent[]> {
  return useQueryResult(
    calendarEventsQuery({
      start: LocalDate.todayInSystemTz()
        .subMonths(1)
        .startOfMonth()
        .startOfWeek(),
      end: LocalDate.todayInSystemTz().addYears(1).lastDayOfMonth()
    })
  )
}

const CalendarPage = React.memo(function CalendarPage() {
  const user = useUser()

  // TODO wip for featureFlags.calendarMonthView
  const [overrideToUseCalendarList, setOverrideToUseCalendarList] =
    useState(false)

  const data = useReservationsDefaultRange()
  const events = useEventsDefaultRange()

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

  const { addTimedNotification } = useContext(NotificationsContext)
  const i18n = useTranslation()
  const onSuccess = useCallback(() => {
    closeModal()
    addTimedNotification({
      children: i18n.common.saveSuccess
    })
  }, [addTimedNotification, closeModal, i18n.common.saveSuccess])

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
                times.some((t) =>
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
                  />
                </ContentArea>
              </RenderOnlyOn>
              <RenderOnlyOn desktop>
                {featureFlags.calendarMonthView && (
                  <CalendarToggle
                    value={overrideToUseCalendarList}
                    onClick={() =>
                      setOverrideToUseCalendarList(!overrideToUseCalendarList)
                    }
                  />
                )}
                {featureFlags.calendarMonthView &&
                !overrideToUseCalendarList ? (
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
                  />
                ) : (
                  <CalendarGridView
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
                  />
                )}
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
                  onSuccess={onSuccess}
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
                  onSuccess={onSuccess}
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
                  <FixedPeriodSelectionModal
                    close={closeModal}
                    questionnaire={questionnaire.questionnaire}
                    availableChildren={response.children}
                    eligibleChildren={questionnaire.eligibleChildren}
                    previousAnswers={questionnaire.previousAnswers}
                  />
                </RequireAuth>
              )}
            </div>
          )
        }
      )}
    </>
  )
})

// Modal states stored to the URL
type URLModalState =
  | { type: 'day'; date: LocalDate }
  | {
      type: 'absences'
      initialDate: LocalDate | undefined
      returnToDayModal: boolean
    }
  | { type: 'reservations'; initialRange: FiniteDateRange | undefined }
  | { type: 'holidays' }
  | { type: 'discussions' }
  | {
      type: 'discussion-reservations'
      selectedEventId: UUID | undefined
      selectedChildId: UUID | undefined
    }

// Modal state not stored to the URL
type NonURLModalState = { type: 'pickAction' }

// All possible modal states
type ModalState = URLModalState | NonURLModalState

interface UseModalStateResult {
  modalState: ModalState | undefined
  openDayModal: (date: LocalDate) => void
  openPickActionModal: () => void
  openReservationModal: (initialRange: FiniteDateRange | undefined) => void
  openAbsenceModal: (
    initialDate: LocalDate | undefined,
    returnToDayModal: boolean
  ) => void
  openHolidayModal: () => void
  openDiscussionSurveyModal: () => void
  openDiscussionReservationModal: (
    selectedChildId: UUID | undefined,
    selectedEventId: UUID | undefined
  ) => void
  closeModal: () => void
}

export function useCalendarModalState(): UseModalStateResult {
  const location = useLocation()
  const navigate = useNavigate()

  const urlModalState = useMemo(
    () => parseQueryString(location.search),
    [location.search]
  )
  const [nonUrlModalState, setNonUrlModalState] = useState<NonURLModalState>()

  const openModal = useCallback(
    (modal: URLModalState) => {
      setNonUrlModalState(undefined)
      navigate(`/calendar?${buildQueryString(modal)}`)
    },
    [navigate]
  )
  const closeModal = useCallback(() => {
    setNonUrlModalState(undefined)
    navigate('/calendar')
  }, [navigate])

  const openDayModal = useCallback(
    (date: LocalDate) => openModal({ type: 'day', date }),
    [openModal]
  )
  const openReservationModal = useCallback(
    (initialRange: FiniteDateRange | undefined) =>
      openModal({ type: 'reservations', initialRange }),
    [openModal]
  )
  const openAbsenceModal = useCallback(
    (initialDate: LocalDate | undefined, returnToDayModal: boolean) =>
      openModal({ type: 'absences', initialDate, returnToDayModal }),
    [openModal]
  )

  const openDiscussionSurveyModal = useCallback(
    () => openModal({ type: 'discussions' }),
    [openModal]
  )
  const openDiscussionReservationModal = useCallback(
    (selectedChildId: UUID | undefined, selectedEventId: UUID | undefined) =>
      openModal({
        type: 'discussion-reservations',
        selectedChildId,
        selectedEventId
      }),
    [openModal]
  )
  const openHolidayModal = useCallback(
    () => openModal({ type: 'holidays' }),
    [openModal]
  )

  const openPickActionModal = useCallback(
    () => setNonUrlModalState({ type: 'pickAction' }),
    []
  )

  return {
    modalState: nonUrlModalState ?? urlModalState,
    openDayModal,
    openPickActionModal,
    openReservationModal,
    openAbsenceModal,
    openHolidayModal,
    openDiscussionSurveyModal,
    openDiscussionReservationModal,
    closeModal
  }
}

function parseQueryString(qs: string): URLModalState | undefined {
  const searchParams = new URLSearchParams(qs)
  const dateParam = searchParams.get('day')
  const modalParam = searchParams.get('modal')
  const startDateParam = searchParams.get('startDate')
  const endDateParam = searchParams.get('endDate')
  const selectedChildId = searchParams.get('selectedChildId') ?? undefined
  const selectedEventId = searchParams.get('selectedEventId') ?? undefined
  const returnToDayModal = searchParams.has('returnToDayModal')

  const date = dateParam ? LocalDate.tryParseIso(dateParam) : undefined
  const startDate = startDateParam
    ? LocalDate.tryParseIso(startDateParam)
    : undefined
  const endDate = endDateParam ? LocalDate.tryParseIso(endDateParam) : undefined
  const range =
    startDate && endDate
      ? FiniteDateRange.tryCreate(startDate, endDate)
      : undefined
  return modalParam === 'reservations'
    ? { type: 'reservations', initialRange: range }
    : modalParam === 'holidays'
      ? { type: 'holidays' }
      : modalParam === 'absences'
        ? { type: 'absences', initialDate: date, returnToDayModal }
        : modalParam === 'discussions'
          ? { type: 'discussions' }
          : modalParam === 'discussion-reservations'
            ? {
                type: 'discussion-reservations',
                selectedChildId,
                selectedEventId
              }
            : date
              ? { type: 'day', date }
              : undefined
}

function buildQueryString(modal: URLModalState): string {
  switch (modal.type) {
    case 'holidays':
      return 'modal=holidays'
    case 'reservations':
      return `modal=reservations${
        modal.initialRange
          ? `&startDate=${modal.initialRange.start.formatIso()}&endDate=${modal.initialRange.end.formatIso()}`
          : ''
      }`
    case 'absences':
      return `modal=absences${
        modal.initialDate ? '&day=' + modal.initialDate.toString() : ''
      }${modal.returnToDayModal ? '&returnToDayModal=true' : ''}`
    case 'day':
      return `day=${modal.date.toString()}`
    case 'discussions':
      return `modal=discussions`
    case 'discussion-reservations':
      return `modal=discussion-reservations${modal.selectedChildId ? '&selectedChildId=' + modal.selectedChildId : ''}${modal.selectedEventId ? '&selectedEventId=' + modal.selectedEventId : ''}`
  }
}

const DesktopOnly = styled(Desktop)`
  position: relative;
`

export default React.memo(function CalendarPageWrapper() {
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

const CalendarToggle: React.FC<{
  value: boolean
  onClick: () => void
}> = ({ value, onClick }) => {
  return (
    <button
      id="wip-calendar-toggle"
      style={{
        display: 'block',
        position: 'fixed',
        bottom: 0,
        left: 0,
        zIndex: 100,
        padding: '4px 8px',
        minWidth: '120px'
      }}
      tabIndex={-1}
      onClick={onClick}
    >
      {value ? 'Month view' : 'List view'}
    </button>
  )
}
