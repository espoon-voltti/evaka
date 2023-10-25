// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useState } from 'react'

import { Loading, Result, Success } from 'lib-common/api'
import { ChildDailyRecords } from 'lib-common/api-types/reservations'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { useMutationResult } from 'lib-common/query'
import {
  TimeRange,
  TimeRangeErrors,
  validateTimeRange
} from 'lib-common/reservations'
import { UUID } from 'lib-common/types'

import { deleteChildAbsences } from '../../../api/absences'
import {
  queryKeys as unitQueryKeys,
  postAttendancesMutation,
  postReservationsMutation
} from '../queries'

export interface EditState {
  childId: string
  date: LocalDate
  request: { id: number; result: Result<void> }
  reservations: Record<JsonOf<LocalDate>, TimeRangeWithErrors>[]
  attendances: Record<JsonOf<LocalDate>, TimeRangeWithErrors>[]
  absences: Record<JsonOf<LocalDate>, { type: AbsenceType } | null>[]
}

export interface TimeRangeWithErrors extends JsonOf<TimeRange> {
  errors: TimeRangeErrors
  lastSavedState: JsonOf<TimeRange>
}

let requestId = 0

export function useUnitReservationEditState(
  dailyData: ChildDailyRecords[],
  unitId: UUID
) {
  const [state, setState] = useState<EditState>()

  const startRequest = useCallback(() => {
    const id = requestId++
    setState((previousState) =>
      previousState
        ? {
            ...previousState,
            request: { id, result: Loading.of() }
          }
        : previousState
    )
    return id
  }, [])

  const updateRequestStatus = useCallback(
    (id: number) => (result: Result<void>) =>
      setState((previousState) =>
        previousState && previousState.request.id === id
          ? { ...previousState, request: { id, result } }
          : previousState
      ),
    []
  )

  const queryClient = useQueryClient()
  const stopEditing = useCallback(() => {
    setState(undefined)

    // TODO: It would be better if the data was saved on stopEditing instead of
    // on-the-fly. This would allow to invalidate the query normally after a
    // successful mutation.
    void queryClient.invalidateQueries({
      queryKey: unitQueryKeys.unitAttendanceReservations()
    })
  }, [queryClient])

  const startEditing = useCallback(
    (childId: string, date: LocalDate) => {
      const childData =
        dailyData.find((d) => d.child.id === childId)?.dailyData ?? []
      setState({
        childId,
        date,
        request: { id: requestId, result: Success.of() },
        reservations: childData.map((row) =>
          Object.fromEntries(
            Object.entries(row).map(([date, { reservation }]) => {
              const initial =
                reservation && reservation.type === 'TIMES'
                  ? {
                      startTime: reservation.startTime.format(),
                      endTime: reservation.endTime.format()
                    }
                  : { startTime: '', endTime: '' }
              return [
                date,
                {
                  ...initial,
                  errors: { startTime: undefined, endTime: undefined },
                  lastSavedState: initial
                }
              ]
            })
          )
        ),
        attendances: childData.map((row) =>
          Object.fromEntries(
            Object.entries(row).map(([date, { attendance }]) => {
              const initialRange = attendance
                ? { ...attendance, endTime: attendance.endTime ?? '' }
                : { startTime: '', endTime: '' }
              return [
                date,
                {
                  ...initialRange,
                  errors: { startTime: undefined, endTime: undefined },
                  lastSavedState: initialRange
                }
              ]
            })
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
      const reqId = startRequest()
      void deleteChildAbsences(state?.childId, date)
        .then(updateRequestStatus(reqId))
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
    (index: number, date: LocalDate, times: JsonOf<TimeRange>) => {
      setState((previousState) =>
        previousState
          ? {
              ...previousState,
              reservations: previousState.reservations.map((res, i) =>
                i === index
                  ? {
                      ...res,
                      [date.formatIso()]: {
                        ...res[date.formatIso()],
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

  const { mutateAsync: postReservations } = useMutationResult(
    postReservationsMutation
  )

  const saveReservation = useCallback(
    (date: LocalDate) => {
      if (!state?.childId) return

      const reservations = state.reservations.map(
        (dailyData) => dailyData[date.formatIso()]
      )
      if (reservations.every(lastSavedStateHasNotChanged)) return
      if (reservations.some(({ errors }) => errors.startTime || errors.endTime))
        return

      const filtered = reservations
        .filter(({ startTime, endTime }) => startTime && endTime)
        .map(({ startTime, endTime }) => ({
          start: LocalTime.parse(startTime),
          end: LocalTime.parse(endTime)
        }))

      const reservationRequest: DailyReservationRequest =
        filtered.length === 0
          ? {
              type: 'NOTHING',
              childId: state.childId,
              date
            }
          : {
              type: 'RESERVATIONS',
              childId: state.childId,
              date,
              reservation: filtered[0],
              secondReservation: filtered[1] ?? null
            }

      const reqId = startRequest()

      void postReservations([reservationRequest])
        .then(updateRequestStatus(reqId))
        .then(() =>
          setState((previousState) =>
            previousState
              ? {
                  ...previousState,
                  reservations: previousState.reservations.map((res, i) => ({
                    ...res,
                    [date.formatIso()]: {
                      ...res[date.formatIso()],
                      lastSavedState: {
                        startTime: reservations[i].startTime,
                        endTime: reservations[i].endTime
                      }
                    }
                  }))
                }
              : previousState
          )
        )
    },
    [state, startRequest, postReservations, updateRequestStatus]
  )

  const updateAttendance = useCallback(
    (index: number, date: LocalDate, times: JsonOf<TimeRange>) => {
      setState((previousState) =>
        previousState
          ? {
              ...previousState,
              attendances: previousState.attendances.map((res, i) =>
                i === index
                  ? {
                      ...res,
                      [date.formatIso()]: {
                        ...res[date.formatIso()],
                        ...times,
                        errors: validateTimeRange(times, true)
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

  const { mutateAsync: postAttendances } = useMutationResult(
    postAttendancesMutation
  )

  const saveAttendance = useCallback(
    (date: LocalDate) => {
      if (!state?.childId) return

      const attendances = state.attendances.map(
        (dailyData) => dailyData[date.formatIso()]
      )
      if (attendances.every(lastSavedStateHasNotChanged)) return
      if (attendances.some(({ errors }) => errors.startTime || errors.endTime))
        return

      const body = {
        date,
        attendances: attendances
          .filter(({ startTime }) => startTime)
          .map(({ startTime, endTime }) => ({
            startTime,
            endTime: endTime || null
          }))
      }

      const reqId = startRequest()
      void postAttendances({
        childId: state.childId,
        unitId,
        attendances: body
      })
        .then(updateRequestStatus(reqId))
        .then(() =>
          setState((previousState) =>
            previousState
              ? {
                  ...previousState,
                  attendances: previousState.attendances.map((res, i) => ({
                    ...res,
                    [date.formatIso()]: {
                      ...res[date.formatIso()],
                      lastSavedState: {
                        startTime: attendances[i].startTime,
                        endTime: attendances[i].endTime
                      }
                    }
                  }))
                }
              : previousState
          )
        )
    },
    [unitId, state, startRequest, postAttendances, updateRequestStatus]
  )

  return {
    editState: state,
    stopEditing,
    startEditing,
    deleteAbsence,
    updateReservation,
    saveReservation,
    updateAttendance,
    saveAttendance
  } as const
}

const lastSavedStateHasNotChanged = (timeRange: TimeRangeWithErrors) =>
  timeRange.startTime === timeRange.lastSavedState.startTime &&
  timeRange.endTime === timeRange.lastSavedState.endTime
