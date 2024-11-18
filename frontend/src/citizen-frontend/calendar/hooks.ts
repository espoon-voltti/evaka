// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useState, useEffect } from 'react'

import { MonthlyTimeSummary } from './MonthlyHoursSummary'

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
  const [summaryInfoOpen, setSummaryInfoOpen] = useState(false)
  const [displayAlert, setDisplayAlert] = useState(false)

  useEffect(() => {
    setDisplayAlert(
      childSummaries.some(
        ({ reservedMinutes, usedServiceMinutes, serviceNeedMinutes }) =>
          reservedMinutes > serviceNeedMinutes ||
          usedServiceMinutes > serviceNeedMinutes
      )
    )
    setSummaryInfoOpen(
      childSummaries.some(
        ({ reservedMinutes, serviceNeedMinutes }) =>
          reservedMinutes > serviceNeedMinutes
      )
    )
  }, [childSummaries, selectedMonthData.month, selectedMonthData.year])

  const toggleSummaryInfo = useCallback(() => {
    setSummaryInfoOpen((prev) => !prev)
  }, [])

  return { summaryInfoOpen, displayAlert, toggleSummaryInfo }
}
