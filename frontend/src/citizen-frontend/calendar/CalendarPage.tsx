// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine, isLoading, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import {
  DailyReservationData,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'
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
import NonReservableDaysWarningModal from './NonReservableDaysWarningModal'
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
    openNonReservableDaysWarningModal,
    closeModal
  } = useCalendarModalState()

  const openReservationModalWithoutInitialRange = useCallback(() => {
    openReservationModal(undefined)
  }, [openReservationModal])

  const dayIsReservable = useCallback(
    ({ date, isHoliday }: DailyReservationData) =>
      data
        .map(({ children }) =>
          children.some(
            ({ maxOperationalDays, inShiftCareUnit }) =>
              maxOperationalDays.includes(date.getIsoDayOfWeek()) &&
              (inShiftCareUnit || !isHoliday)
          )
        )
        .getOrElse(false),
    [data]
  )

  const holidayPeriods = useQueryResult(holidayPeriodsQuery)
  const dayIsHolidayPeriod = useCallback(
    (date: LocalDate) =>
      holidayPeriods
        .map((periods) => periods.some((p) => p.period.includes(date)))
        .getOrElse(false),
    [holidayPeriods]
  )

  const upcomingHolidayPeriods = useMemo(() => {
    const today = mockToday() ?? LocalDate.todayInSystemTz()
    return holidayPeriods.map((periods) =>
      periods
        .filter((p) => p.period.end.isEqualOrAfter(today))
        .map((p) => ({
          period: p.period,
          isOpen: p.reservationDeadline.isEqualOrAfter(today)
        }))
    )
  }, [holidayPeriods])

  const { data: questionnaire } = useQuery(activeQuestionnaireQuery)

  const firstReservableDate = useMemo(() => {
    if (data.isSuccess) {
      const allReservableDateRanges = Object.keys(
        data.value.reservableDays
      ).flatMap((childId) => data.value.reservableDays[childId])

      // First reservable day that has no reservations
      const firstReservableEmptyDate = data.value.dailyData.find(
        (day) =>
          allReservableDateRanges.find((range) => range.includes(day.date)) &&
          day.children.length == 0
      )
      return firstReservableEmptyDate?.date ?? null
    } else {
      return LocalDate.todayInSystemTz()
    }
  }, [data])

  const firstPlannedAbsenceDate = data
    .map((response) =>
      Object.values(response.reservableDays)
        .flatMap((reservableDays) => reservableDays)
        .reduce<LocalDate | null>(
          (prev, { start: cur }) => (prev !== null && prev < cur ? prev : cur),
          null
        )
    )
    .getOrElse(null)

  if (!user || !user.accessibleFeatures.reservations) return null

  return (
    <>
      <DailyServiceTimeNotifications />

      {renderResult(
        combine(data, events, upcomingHolidayPeriods),
        ([response, events, upcomingHolidayPeriods]) => (
          <div data-qa="calendar-page" data-isloading={isLoading(data)}>
            <CalendarNotifications />
            <MobileAndTablet>
              <ContentArea
                opaque
                paddingVertical="zero"
                paddingHorizontal="zero"
              >
                <CalendarListView
                  childData={response.children}
                  dailyData={response.dailyData}
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
                dailyData={response.dailyData}
                onCreateReservationClicked={
                  openReservationModalWithoutInitialRange
                }
                onCreateAbsencesClicked={openAbsenceModal}
                onReportHolidaysClicked={openHolidayModal}
                selectedDate={
                  modalState?.type === 'day' ? modalState.date : undefined
                }
                selectDate={openDayModal}
                includeWeekends={response.includesWeekends}
                dayIsReservable={dayIsReservable}
                events={events}
              />
            </DesktopOnly>
            {modalState?.type === 'day' && (
              <DayView
                date={modalState.date}
                reservationsResponse={response}
                selectDate={openDayModal}
                close={closeModal}
                openAbsenceModal={openAbsenceModal}
                events={events}
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
                availableChildren={response.children}
                onSuccess={(containsNonReservableDays: boolean) => {
                  if (containsNonReservableDays) {
                    openNonReservableDaysWarningModal()
                  } else {
                    closeModal()
                  }
                }}
                reservableDays={response.reservableDays}
                initialStart={
                  modalState.initialRange?.start ?? firstReservableDate
                }
                initialEnd={modalState.initialRange?.end ?? null}
                existingReservations={response.dailyData}
                upcomingHolidayPeriods={upcomingHolidayPeriods}
              />
            )}
            {modalState?.type === 'absences' && (
              <AbsenceModal
                close={closeModal}
                initialDate={modalState.initialDate}
                availableChildren={response.children}
                firstPlannedAbsenceDate={firstPlannedAbsenceDate}
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
            {modalState?.type === 'nonReservableDaysWarning' && (
              <NonReservableDaysWarningModal onClose={closeModal} />
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

// Modal statse not stored to the URL
type NonURLModalState =
  | { type: 'pickAction' }
  | { type: 'nonReservableDaysWarning' }

// All possible modal states
type ModalState = URLModalState | NonURLModalState

interface UseModalStateResult {
  modalState: ModalState | undefined
  openDayModal: (date: LocalDate) => void
  openPickActionModal: () => void
  openReservationModal: (initialRange: FiniteDateRange | undefined) => void
  openAbsenceModal: (initialDate: LocalDate | undefined) => void
  openHolidayModal: () => void
  openNonReservableDaysWarningModal: () => void
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
  const openNonReservableDaysWarningModal = useCallback(
    () => setNonUrlModalState({ type: 'nonReservableDaysWarning' }),
    []
  )

  return {
    modalState: nonUrlModalState ?? urlModalState,
    openDayModal,
    openPickActionModal,
    openReservationModal,
    openAbsenceModal,
    openHolidayModal,
    openNonReservableDaysWarningModal,
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
