import LocalDate from 'lib-common/local-date'
import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api-client'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

export interface Reservation {
  startTime: Date
  endTime: Date
  childId: string
}

export interface DailyReservationData {
  date: LocalDate
  isHoliday: boolean
  reservations: Reservation[]
}

export interface ReservationChild {
  id: UUID
  firstName: string
}

export interface ReservationsResponse {
  dailyData: DailyReservationData[]
  children: ReservationChild[]
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
          reservations: data.reservations.map((r) => ({
            ...r,
            startTime: new Date(r.startTime),
            endTime: new Date(r.endTime)
          }))
        }))
      })
    )
    .catch((e) => Failure.fromError(e))
}

export interface DailyReservationRequest {
  date: LocalDate
  startTime: string
  endTime: string
}

interface ReservationRequest {
  children: UUID[]
  reservations: DailyReservationRequest[]
}

export async function postReservations(
  children: UUID[],
  reservations: DailyReservationRequest[]
): Promise<Result<null>> {
  const body: ReservationRequest = { children, reservations }
  return client
    .post('/citizen/reservations', body)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
