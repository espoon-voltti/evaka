// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useState, useEffect } from 'react'

import type { Result } from 'lib-common/api'
import { Success } from 'lib-common/api'
import type FiniteDateRange from 'lib-common/finite-date-range'
import { useBoolean, type BoundForm } from 'lib-common/form/hooks'
import type { Form } from 'lib-common/form/types'
import type {
  ReservationChild,
  ReservationResponseDay,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'
import type LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'

import type { MonthlyTimeSummary } from './MonthlyHoursSummary'
import { reservationsQuery } from './queries'

export function useSummaryInfo(childSummaries: MonthlyTimeSummary[]) {
  const displayAlert = childSummaries.some(
    ({ reservedMinutes, usedServiceMinutes, serviceNeedMinutes }) =>
      reservedMinutes > serviceNeedMinutes ||
      usedServiceMinutes > serviceNeedMinutes
  )
  const [summaryExplicitlyClosed, setSummaryExplicitlyClosed] = useState(false)
  const [state, setState] = useState<{
    summaryInfoOpen: boolean
    prevChildSummaries: MonthlyTimeSummary[]
  }>(() => ({
    summaryInfoOpen: childSummaries.some(
      ({ reservedMinutes, serviceNeedMinutes }) =>
        reservedMinutes > serviceNeedMinutes
    ),
    prevChildSummaries: childSummaries
  }))

  if (
    !summaryExplicitlyClosed &&
    state.prevChildSummaries !== childSummaries &&
    childSummaries.some(
      ({ reservedMinutes, serviceNeedMinutes }) =>
        reservedMinutes > serviceNeedMinutes
    )
  ) {
    setState({
      summaryInfoOpen: true,
      prevChildSummaries: childSummaries
    })
  }

  const toggleSummaryInfo = useCallback(() => {
    setState((prev) => {
      const newState = !prev.summaryInfoOpen
      if (!newState) {
        setSummaryExplicitlyClosed(true)
      }
      return { ...prev, summaryInfoOpen: newState }
    })
  }, [])

  return {
    summaryInfoOpen: state.summaryInfoOpen,
    displayAlert,
    toggleSummaryInfo
  }
}

export function useMonthlySummaryInfo(
  childSummaries: MonthlyTimeSummary[],
  selectedMonthData: { month: number; year: number }
) {
  const [state, setState] = useState<{
    summaryInfoOpen: boolean
    displayAlert: boolean
    prevChildSummaries: MonthlyTimeSummary[]
    prevMonth: number
    prevYear: number
  }>(() => {
    const tooManyReservedMinutes = childSummaries.some(
      ({ reservedMinutes, serviceNeedMinutes }) =>
        reservedMinutes > serviceNeedMinutes
    )
    return {
      summaryInfoOpen: tooManyReservedMinutes,
      displayAlert: childSummaries.some(
        ({ reservedMinutes, usedServiceMinutes, serviceNeedMinutes }) =>
          reservedMinutes > serviceNeedMinutes ||
          usedServiceMinutes > serviceNeedMinutes
      ),
      prevChildSummaries: childSummaries,
      prevMonth: selectedMonthData.month,
      prevYear: selectedMonthData.year
    }
  })

  if (
    state.prevChildSummaries !== childSummaries ||
    state.prevMonth !== selectedMonthData.month ||
    state.prevYear !== selectedMonthData.year
  ) {
    const tooManyReservedMinutes = childSummaries.some(
      ({ reservedMinutes, serviceNeedMinutes }) =>
        reservedMinutes > serviceNeedMinutes
    )
    setState({
      summaryInfoOpen: tooManyReservedMinutes ? true : state.summaryInfoOpen,
      displayAlert: childSummaries.some(
        ({ reservedMinutes, usedServiceMinutes, serviceNeedMinutes }) =>
          reservedMinutes > serviceNeedMinutes ||
          usedServiceMinutes > serviceNeedMinutes
      ),
      prevChildSummaries: childSummaries,
      prevMonth: selectedMonthData.month,
      prevYear: selectedMonthData.year
    })
  }

  const toggleSummaryInfo = useCallback(() => {
    setState((prev) => ({ ...prev, summaryInfoOpen: !prev.summaryInfoOpen }))
  }, [])

  return {
    summaryInfoOpen: state.summaryInfoOpen,
    displayAlert: state.displayAlert,
    toggleSummaryInfo
  }
}

function useReservationsRange(
  from: LocalDate,
  to: LocalDate
): Result<ReservationsResponse> {
  return useQueryResult(
    reservationsQuery({
      from,
      to
    })
  )
}

export function useExtendedReservationsRange(dateRange: FiniteDateRange) {
  const fetchedReservations = useReservationsRange(
    dateRange.start,
    dateRange.end
  )
  const [reservations, setReservations] =
    useState<Result<ReservationsResponse>>(fetchedReservations)

  // Prepend fetched reservations to existing ones,
  // removing duplicates and sorting by date
  useEffect(() => {
    if (fetchedReservations.isSuccess) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- Legitimate: accumulating async query results from external API
      setReservations((prevReservations) => {
        if (prevReservations.isSuccess) {
          const extendedReservations = {
            value: {
              ...prevReservations.value,
              children: [...fetchedReservations.value.children],
              days: [
                ...fetchedReservations.value.days,
                ...prevReservations.value.days
              ]
                .reduce((uniqueDays, currentDay) => {
                  if (
                    !uniqueDays.some((day) => day.date.isEqual(currentDay.date))
                  ) {
                    uniqueDays.push(currentDay)
                  }
                  return uniqueDays
                }, [] as ReservationResponseDay[])
                .sort((a, b) => a.date.compareTo(b.date))
            }
          } as Success<ReservationsResponse>
          return Success.of(extendedReservations.value)
        } else {
          return fetchedReservations
        }
      })
    }
  }, [fetchedReservations])

  return { reservations, loading: fetchedReservations.isLoading }
}

// Hook for announcing messages to screen reader users
// Needs the ariaLiveBustingValue trick to make sure repeated messages are read out,
// e.g. Mac VoiceOver seems to cache the content otherwise.
// Needs the message to be cleared after timeout to prevent screen readers from reading it out again.
// Works best with aria-live set to "polite"
export const useScreenReaderMessage = (): [
  string | null,
  (message: string) => void
] => {
  const [screenReaderMessage, setScreenReaderMessage] = useState<string | null>(
    null
  )
  const [isMessageTimerOn, setIsMessageTimerOn] = useState(false)
  const [ariaLiveBustingValue, { toggle }] = useBoolean(false)
  const showScreenReaderMessage = useCallback(
    (message: string) => {
      if (isMessageTimerOn) return
      toggle()
      setScreenReaderMessage(message + (ariaLiveBustingValue ? '\u200B' : ''))
      setIsMessageTimerOn(true)
      setTimeout(() => {
        setScreenReaderMessage(null)
        setIsMessageTimerOn(false)
      }, 1000)
    },
    [ariaLiveBustingValue, isMessageTimerOn, toggle]
  )
  return [screenReaderMessage, showScreenReaderMessage]
}

export const useRemovePlacementPendingChildSelections = (
  selectedChildren: BoundForm<
    Form<
      ReservationChild[],
      'required',
      ReservationChild[],
      Form<ReservationChild, never, ReservationChild, unknown>
    >
  >,
  rangeEnd: LocalDate | undefined
) => {
  useEffect(() => {
    if (!selectedChildren.isValid() || !rangeEnd) return

    const currentSelectedChildren = selectedChildren.value()

    const placementStartedChildren = currentSelectedChildren.filter((child) => {
      return (
        child.upcomingPlacementStartDate === null ||
        !child.upcomingPlacementStartDate.isAfter(rangeEnd)
      )
    })

    if (placementStartedChildren.length !== currentSelectedChildren.length) {
      selectedChildren.set(placementStartedChildren)
    }
  }, [rangeEnd, selectedChildren])
}
