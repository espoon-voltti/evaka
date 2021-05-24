import { orderBy } from 'lodash'

import { Absence } from 'lib-common/api-types/child/Absences'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

export function groupAbsencesByDateRange(
  absences: Absence[]
): FiniteDateRange[] {
  if (absences.length > 0) {
    const sorted = orderBy(absences, ['date'])
    const absencesGroupedByDate: Absence[][] = [[sorted[0]]]
    let previousDate: LocalDate = absencesGroupedByDate[0][0].date
    for (const absence of sorted) {
      if (absence.date.differenceInDays(previousDate) > 1) {
        previousDate = absence.date
        absencesGroupedByDate.push([absence])
      } else {
        previousDate = absence.date
        absencesGroupedByDate[absencesGroupedByDate.length - 1].push(absence)
      }
    }

    const absenceDateRanges: FiniteDateRange[] = []
    for (const absence of absencesGroupedByDate) {
      absence.length > 1
        ? absenceDateRanges.push(
            new FiniteDateRange(
              absence[0].date,
              absence[absence.length - 1].date
            )
          )
        : absenceDateRanges.push(
            new FiniteDateRange(absence[0].date, absence[0].date)
          )
    }
    return absenceDateRanges
  } else {
    return []
  }
}
