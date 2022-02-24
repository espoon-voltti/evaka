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
import { UUID } from 'lib-common/types'

import { deleteChildAbsences } from '../../../api/absences'
import { postAttendances, postReservations } from '../../../api/unit'

export interface EditState {
  childId: string
  date: LocalDate
  request: { id: number; result: Result<void> }
  reservations: Record<JsonOf<LocalDate>, TimeRangeWithErrors>[]
  attendances: Record<JsonOf<LocalDate>, TimeRangeWithErrors>[]
  absences: Record<JsonOf<LocalDate>, { type: AbsenceType } | null>[]
}

export interface TimeRangeWithErrors extends TimeRange {
  errors: TimeRangeErrors
  lastSavedState: TimeRange
}

let requestId = 0

export function useUnitReservationEditState(
  dailyData: ChildDailyRecords[],
  reloadReservations: () => void,
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
        request: { id: requestId, result: Success.of() },
        reservations: childData.map((row) =>
          Object.fromEntries(
            Object.entries(row).map(([date, { reservation }]) => {
              const initial = reservation ?? { startTime: '', endTime: '' }
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

  const saveReservation = useCallback(
    (date: LocalDate) => {
      if (!state?.childId) return

      const reservations = state.reservations.map(
        (dailyData) => dailyData[date.formatIso()]
      )
      if (reservations.every(lastSavedStateHasNotChanged)) return
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

      const reqId = startRequest()
      void postReservations([body])
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
    [state, startRequest, updateRequestStatus]
  )

  const updateAttendance = useCallback(
    (index: number, date: LocalDate, times: TimeRange) => {
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
      void postAttendances(state.childId, unitId, body)
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
    [unitId, state, startRequest, updateRequestStatus]
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
