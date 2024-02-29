// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { renderHook, act } from '@testing-library/react'

import { MonthlyTimeSummary } from './MonthlyHoursSummary'
import { useSummaryInfo } from './hooks'

describe('useSummaryInfo', () => {
  it('initializes summary info open state based on child summaries', () => {
    const { result } = renderHook(() =>
      useSummaryInfo([
        { reservedMinutes: 100, serviceNeedMinutes: 80 }
      ] as MonthlyTimeSummary[])
    )
    expect(result.current.summaryInfoOpen).toBe(true)
  })
  it('initializes summary info closed state based on child summaries', () => {
    const { result } = renderHook(() =>
      useSummaryInfo([
        { reservedMinutes: 80, serviceNeedMinutes: 180 }
      ] as MonthlyTimeSummary[])
    )
    expect(result.current.summaryInfoOpen).toBe(false)
  })

  it("Opens summary info when child's new reservations exceed service time", () => {
    const { result, rerender } = renderHook(
      ({ childSummaries }) => useSummaryInfo(childSummaries),
      {
        initialProps: {
          childSummaries: [
            { reservedMinutes: 80, serviceNeedMinutes: 180 }
          ] as MonthlyTimeSummary[]
        }
      }
    )

    // Initial state with summaries not initially open
    expect(result.current.summaryInfoOpen).toBe(false)

    // Rerender with new summary that should open the summary info
    rerender({
      childSummaries: [
        { reservedMinutes: 120, serviceNeedMinutes: 100 }
      ] as MonthlyTimeSummary[]
    })

    expect(result.current.summaryInfoOpen).toBe(true)
  })

  it("Keeps summary info closed when it's explicitly closed", () => {
    const { result, rerender } = renderHook(
      ({ childSummaries }) => useSummaryInfo(childSummaries),
      {
        initialProps: {
          childSummaries: [
            { reservedMinutes: 120, serviceNeedMinutes: 80 }
          ] as MonthlyTimeSummary[]
        }
      }
    )

    expect(result.current.summaryInfoOpen).toBe(true)

    act(() => {
      result.current.toggleSummaryInfo()
    })
    expect(result.current.summaryInfoOpen).toBe(false)

    rerender({
      childSummaries: [
        { reservedMinutes: 180, serviceNeedMinutes: 80 }
      ] as MonthlyTimeSummary[]
    })

    expect(result.current.summaryInfoOpen).toBe(false)
  })
})
