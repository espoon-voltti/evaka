// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine, isLoading, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { ReservationsResponse } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { useQuery, useQueryResult } from 'lib-common/query'
import { mockToday } from 'lib-common/utils/helpers'
import Main from 'lib-components/atoms/Main'
import { ContentArea } from 'lib-components/layout/Container'
import {
  Desktop,
  MobileAndTablet
} from 'lib-components/layout/responsive-layout'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import RequireAuth from '../RequireAuth'
import { renderResult } from '../async-rendering'
import { useUser } from '../auth/state'

import AbsenceModal from './AbsenceModal'
import ActionPickerModal from './ActionPickerModal'
import CalendarGridView from './CalendarGridView'
import CalendarListView from './CalendarListView'
import CalendarNotifications from './CalendarNotifications'
import DailyServiceTimeNotifications from './DailyServiceTimeNotifications'
import DayView from './DayView'
import ReservationModal from './ReservationModal'
import FixedPeriodSelectionModal from './holiday-modal/FixedPeriodSelectionModal'
import {
  activeQuestionnaireQuery,
  calendarEventsQuery,
  holidayPeriodsQuery,
  reservationsQuery
} from './queries'

function useReservationsDefaultRange(): Result<ReservationsResponse> {
  return useQueryResult(
    reservationsQuery(
      LocalDate.todayInSystemTz().subMonths(1).startOfMonth().startOfWeek(),
      LocalDate.todayInSystemTz().addYears(1).lastDayOfMonth()
    )
  )
}

function useEventsDefaultRange(): Result<CitizenCalendarEvent[]> {
  return useQueryResult(
    calendarEventsQuery(
      LocalDate.todayInSystemTz().subMonths(1).startOfMonth().startOfWeek(),
      LocalDate.todayInSystemTz().addYears(1).lastDayOfMonth()
    )
  )
}

const CalendarPage = React.memo(function CalendarPage() {
  const user = useUser()

  const data = useReservationsDefaultRange()
  const events = useEventsDefaultRange()

  const {
    modalState,
    openPickActionModal,
    openReservationModal,
    openHolidayModal,
    openAbsenceModal,
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

  const holidayPeriodInfo = useMemo(() => {
    const today = mockToday() ?? LocalDate.todayInSystemTz()
    return holidayPeriods.map((periods) =>
      periods
        .filter((p) => p.period.end.isEqualOrAfter(today))
        .map((p) => ({
          period: p.period,
          state: p.reservationDeadline.isEqualOrAfter(today)
            ? ('open' as const)
            : ('closed' as const)
        }))
    )
  }, [holidayPeriods])

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
        combine(data, events, holidayPeriodInfo),
        ([response, events, holidayPeriodInfo]) => (
          <div data-qa="calendar-page" data-isloading={isLoading(data)}>
            <CalendarNotifications calendarDays={response.days} />
            <MobileAndTablet>
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
                />
              </ContentArea>
            </MobileAndTablet>
            <DesktopOnly>
              <CalendarGridView
                childData={response.children}
                calendarDays={response.days}
                onCreateReservationClicked={
                  openReservationModalWithoutInitialRange
                }
                onCreateAbsencesClicked={openAbsenceModal}
                onReportHolidaysClicked={openHolidayModal}
                selectedDate={
                  modalState?.type === 'day' ? modalState.date : undefined
                }
                selectDate={openDayModal}
                includeWeekends={true}
                dayIsReservable={dayIsReservable}
                events={events}
              />
            </DesktopOnly>
            {modalState?.type === 'day' && (
              <DayView
                date={modalState.date}
                reservationsResponse={response}
                selectDate={openDayModal}
                onClose={closeModal}
                openAbsenceModal={openAbsenceModal}
                events={events}
                holidayPeriods={holidayPeriodInfo}
              />
            )}
            {modalState?.type === 'pickAction' && (
              <ActionPickerModal
                close={closeModal}
                openReservations={openReservationModalWithoutInitialRange}
                openAbsences={openAbsenceModal}
                openHolidays={openHolidayModal}
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
                holidayPeriods={holidayPeriodInfo}
              />
            )}
            {modalState?.type === 'absences' && (
              <AbsenceModal
                close={closeModal}
                initialDate={modalState.initialDate}
                reservationsResponse={response}
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
      )}
    </>
  )
})

// Modal states stored to the URL
type URLModalState =
  | { type: 'day'; date: LocalDate }
  | { type: 'absences'; initialDate: LocalDate | undefined }
  | { type: 'reservations'; initialRange: FiniteDateRange | undefined }
  | { type: 'holidays' }

// Modal state not stored to the URL
type NonURLModalState = { type: 'pickAction' }

// All possible modal states
type ModalState = URLModalState | NonURLModalState

interface UseModalStateResult {
  modalState: ModalState | undefined
  openDayModal: (date: LocalDate) => void
  openPickActionModal: () => void
  openReservationModal: (initialRange: FiniteDateRange | undefined) => void
  openAbsenceModal: (initialDate: LocalDate | undefined) => void
  openHolidayModal: () => void
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
    (initialDate: LocalDate | undefined) =>
      openModal({ type: 'absences', initialDate }),
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
    closeModal
  }
}

function parseQueryString(qs: string): URLModalState | undefined {
  const searchParams = new URLSearchParams(qs)
  const dateParam = searchParams.get('day')
  const modalParam = searchParams.get('modal')
  const startDateParam = searchParams.get('startDate')
  const endDateParam = searchParams.get('endDate')

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
    ? { type: 'absences', initialDate: date }
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
      }`
    case 'day':
      return `day=${modal.date.toString()}`
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
