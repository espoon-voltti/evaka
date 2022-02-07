// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useState } from 'react'

import { Loading, Result, Success } from 'lib-common/api'
import { ChildDailyRecords } from 'lib-common/api-types/reservations'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import { TimeRange } from 'lib-common/generated/api-types/reservations'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { TimeRangeErrors, validateTimeRange } from 'lib-common/reservations'

import { deleteChildAbsences } from '../../../api/absences'
import { postReservations } from '../../../api/unit'

export interface EditState {
  childId: string
  date: LocalDate
  request: Result<void>
  reservations: Record<JsonOf<LocalDate>, TimeRangeWithErrors>[]
  absences: Record<JsonOf<LocalDate>, { type: AbsenceType } | null>[]
}

export interface TimeRangeWithErrors extends TimeRange {
  errors: TimeRangeErrors
}

export function useUnitReservationEditState(
  dailyData: ChildDailyRecords[],
  reloadReservations: () => void
) {
  const [state, setState] = useState<EditState>()

  const startRequest = useCallback(
    () =>
      setState((previousState) =>
        previousState
          ? { ...previousState, request: Loading.of() }
          : previousState
      ),
    []
  )

  const updateRequestStatus = useCallback(
    (result: Result<void>) =>
      setState((previousState) =>
        previousState ? { ...previousState, request: result } : previousState
      ),
    []
  )

  const stopEditing = useCallback(() => {
    setState(undefined)
    reloadReservations()
  }, [reloadReservations])

  const startEditing = useCallback(
    (childId: string, date: LocalDate) => {
      const childData =
        dailyData.find((d) => d.child.id === childId)?.dailyData ?? []
      setState({
        childId,
        date,
        request: Success.of(),
        reservations: childData.map((row) =>
          Object.fromEntries(
            Object.entries(row).map(([date, { reservation }]) => [
              date,
              {
                ...(reservation ?? { startTime: '', endTime: '' }),
                errors: { startTime: undefined, endTime: undefined }
              }
            ])
          )
        ),
        absences: childData.map((row) =>
          Object.fromEntries(
            Object.entries(row).map(([date, { absence }]) => [date, absence])
          )
        )
      })
    },
    [dailyData]
  )

  const deleteAbsence = useCallback(
    (index: number, date: LocalDate) => {
      if (!state?.childId) return
      startRequest()
      void deleteChildAbsences(state?.childId, date)
        .then(updateRequestStatus)
        .then(() =>
          setState((previousState) =>
            previousState
              ? {
                  ...previousState,
                  absences: previousState.absences.map((abs, i) =>
                    i === index ? { ...abs, [date.formatIso()]: null } : abs
                  )
                }
              : previousState
          )
        )
    },
    [state?.childId, startRequest, updateRequestStatus]
  )

  const updateReservation = useCallback(
    (index: number, date: LocalDate, times: TimeRange) => {
      setState((previousState) =>
        previousState
          ? {
              ...previousState,
              reservations: previousState.reservations.map((res, i) =>
                i === index
                  ? {
                      ...res,
                      [date.formatIso()]: {
                        ...times,
                        errors: validateTimeRange(times)
                      }
                    }
                  : res
              )
            }
          : previousState
      )
    },
    []
  )

  const saveReservation = useCallback(
    (date: LocalDate) => {
      if (!state?.childId) return

      const reservations = state.reservations.map(
        (dailyData) => dailyData[date.formatIso()]
      )
      if (reservations.some(({ errors }) => errors.startTime || errors.endTime))
        return

      const body = {
        childId: state.childId,
        date,
        reservations: reservations
          .filter(({ startTime, endTime }) => startTime && endTime)
          .map(({ startTime, endTime }) => ({
            startTime,
            endTime
          }))
      }

      startRequest()
      void postReservations([body]).then(updateRequestStatus)
    },
    [state, startRequest, updateRequestStatus]
  )

  return {
    editState: state,
    stopEditing,
    startEditing,
    deleteAbsence,
    updateReservation,
    saveReservation
  } as const
}
