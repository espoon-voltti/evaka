import LocalDate from "../../lib-common/local-date";
import {Failure, Result, Success} from "../../lib-common/api";
import {client} from "../api-client";
import {JsonOf} from "../../lib-common/json";

export interface Reservation {
  startTime: string
  endTime: string
  childId: string
}

export interface DailyReservationData {
  date: LocalDate
  isHoliday: boolean
  reservations: Reservation[]
}

export async function getReservations(
  from: LocalDate,
  to: LocalDate
): Promise<Result<DailyReservationData[]>> {
  return client
    .get<JsonOf<DailyReservationData[]>>('/citizen/reservations', {
      params: { from: from.formatIso(), to: to.formatIso() }
    })
    .then((res) => Success.of(res.data.map(data => ({
      ...data,
      date: LocalDate.parseIso(data.date)
    }))))
    .catch((e) => Failure.fromError(e))
}
