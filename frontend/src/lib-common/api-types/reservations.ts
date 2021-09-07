import LocalDate from '../local-date'

export interface DailyReservationRequest {
  childId: string
  date: LocalDate
  reservation: TimeRange | null
}

interface TimeRange {
  startTime: string
  endTime: string
}
