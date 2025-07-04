// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useState, useEffect } from 'react'

import type { Result } from 'lib-common/api'
import { Success } from 'lib-common/api'
import type FiniteDateRange from 'lib-common/finite-date-range'
import type {
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
  const [summaryInfoOpen, setSummaryInfoOpen] = useState(() =>
    childSummaries.some(
      ({ reservedMinutes, serviceNeedMinutes }) =>
        reservedMinutes > serviceNeedMinutes
    )
  )

  const toggleSummaryInfo = useCallback(() => {
    setSummaryInfoOpen((prev) => {
      const newState = !prev
      if (!newState) {
        setSummaryExplicitlyClosed(true)
      }
      return newState
    })
  }, [])

  useEffect(() => {
    if (
      !summaryExplicitlyClosed &&
      childSummaries.some(
        ({ reservedMinutes, serviceNeedMinutes }) =>
          reservedMinutes > serviceNeedMinutes
      )
    ) {
      setSummaryInfoOpen(true)
    }
  }, [childSummaries, summaryExplicitlyClosed])

  return { summaryInfoOpen, displayAlert, toggleSummaryInfo }
}

export function useMonthlySummaryInfo(
  childSummaries: MonthlyTimeSummary[],
  selectedMonthData: { month: number; year: number }
) {
  const [summaryInfoOpen, setSummaryInfoOpen] = useState(() =>
    childSummaries.some(
      ({ reservedMinutes, serviceNeedMinutes }) =>
        reservedMinutes > serviceNeedMinutes
    )
  )
  const [displayAlert, setDisplayAlert] = useState(false)

  useEffect(() => {
    setDisplayAlert(
      childSummaries.some(
        ({ reservedMinutes, usedServiceMinutes, serviceNeedMinutes }) =>
          reservedMinutes > serviceNeedMinutes ||
          usedServiceMinutes > serviceNeedMinutes
      )
    )
  }, [childSummaries, selectedMonthData.month, selectedMonthData.year])

  const toggleSummaryInfo = useCallback(() => {
    setSummaryInfoOpen((prev) => !prev)
  }, [])

  return { summaryInfoOpen, displayAlert, toggleSummaryInfo }
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
