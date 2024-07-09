import {
  CalendarEventTime,
  CitizenCalendarEvent
} from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'

//main rule for allowing reservation or cancellation
function getStartOfDiscussionManipulationWindow(
  comparisonDay: LocalDate
): LocalDate {
  return comparisonDay.addBusinessDays(2)
}

export function showModalEventTime(
  et: CalendarEventTime,
  comparisonDay: LocalDate
): boolean {
  //reserved and not in the past, or within a 2 business day reservation window
  return (
    (et.childId && et.date.isEqualOrAfter(comparisonDay)) ||
    et.date.isEqualOrAfter(
      getStartOfDiscussionManipulationWindow(comparisonDay)
    )
  )
}

export function showSurveyReservationToast(
  event: CitizenCalendarEvent,
  comparisonDay: LocalDate
) {
  return Object.values(event.timesByChild).some(
    (childTimes) =>
      childTimes.every((t) => t.childId === null) &&
      childTimes.some((t) =>
        t.date.isEqualOrAfter(
          getStartOfDiscussionManipulationWindow(comparisonDay)
        )
      )
  )
}

export function isEventTimeCancellable(
  et: CalendarEventTime,
  comparisonDay: LocalDate
) {
  return getStartOfDiscussionManipulationWindow(comparisonDay).isEqualOrBefore(
    et.date
  )
}
