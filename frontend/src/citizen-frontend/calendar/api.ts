import LocalDate from 'lib-common/local-date'
import FiniteDateRange from 'lib-common/finite-date-range'
import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api-client'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { AbsenceType } from 'lib-common/generated/enums'
import { DailyReservationRequest } from 'lib-common/api-types/reservations'

export interface ChildDailyData {
  childId: string
  absence: AbsenceType | null
  reservation: {
    startTime: Date
    endTime: Date
  } | null
}

export interface DailyReservationData {
  date: LocalDate
  isHoliday: boolean
  children: ChildDailyData[]
}

export interface ReservationChild {
  id: UUID
  firstName: string
  preferredName?: string
}

export interface ReservationsResponse {
  dailyData: DailyReservationData[]
  children: ReservationChild[]
  reservableDays: FiniteDateRange
}

export async function getReservations(
  from: LocalDate,
  to: LocalDate
): Promise<Result<ReservationsResponse>> {
  return client
    .get<JsonOf<ReservationsResponse>>('/citizen/reservations', {
      params: { from: from.formatIso(), to: to.formatIso() }
    })
    .then((res) =>
      Success.of({
        ...res.data,
        dailyData: res.data.dailyData.map((data) => ({
          ...data,
          date: LocalDate.parseIso(data.date),
          children: data.children.map((child) => ({
            ...child,
            reservation: child.reservation
              ? {
                  startTime: new Date(child.reservation.startTime),
                  endTime: new Date(child.reservation.endTime)
                }
              : null
          }))
        })),
        reservableDays: FiniteDateRange.parseJson(res.data.reservableDays)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function postReservations(
  reservations: DailyReservationRequest[]
): Promise<Result<null>> {
  return client
    .post('/citizen/reservations', reservations)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export interface AbsencesRequest {
  childIds: string[]
  dateRange: FiniteDateRange
  absenceType: AbsenceType
}

export async function postAbsences(
  request: AbsencesRequest
): Promise<Result<void>> {
  return client
    .post('/citizen/absences', request)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
