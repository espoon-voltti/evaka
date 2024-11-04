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
    setSummaryInfoOpen(false)

    const shouldDisplayAlert = childSummaries.some(
      ({ reservedMinutes, usedServiceMinutes, serviceNeedMinutes }) =>
        reservedMinutes > serviceNeedMinutes ||
        usedServiceMinutes > serviceNeedMinutes
    )

    setDisplayAlert(shouldDisplayAlert)
    if (shouldDisplayAlert) {
      setSummaryInfoOpen(true)
    }
  }, [childSummaries, selectedMonthData.month, selectedMonthData.year])

  const toggleSummaryInfo = useCallback(() => {
    setSummaryInfoOpen((prev) => !prev)
  }, [])

  return { summaryInfoOpen, displayAlert, toggleSummaryInfo }
}
