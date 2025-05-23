// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useMemo, useState } from 'react'
import { useLocation, useSearchParams } from 'wouter'

import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  CalendarEventId,
  ChildId
} from 'lib-common/generated/api-types/shared'
import { fromNullableUuid } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

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
      selectedEventId: CalendarEventId | undefined
      selectedChildId: ChildId | undefined
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
    selectedChildId: ChildId | undefined,
    selectedEventId: CalendarEventId | undefined
  ) => void
  closeModal: () => void
}

export function useCalendarModalState(): UseModalStateResult {
  const [searchParams] = useSearchParams()
  const [, navigate] = useLocation()

  const urlModalState = useMemo(
    () => parseQueryString(searchParams),
    [searchParams]
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
    (
      selectedChildId: ChildId | undefined,
      selectedEventId: CalendarEventId | undefined
    ) =>
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

function parseQueryString(
  searchParams: URLSearchParams
): URLModalState | undefined {
  const dateParam = searchParams.get('day')
  const modalParam = searchParams.get('modal')
  const startDateParam = searchParams.get('startDate')
  const endDateParam = searchParams.get('endDate')
  const selectedChildId =
    fromNullableUuid<ChildId>(searchParams.get('selectedChildId')) ?? undefined
  const selectedEventId =
    fromNullableUuid<CalendarEventId>(searchParams.get('selectedEventId')) ??
    undefined
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
