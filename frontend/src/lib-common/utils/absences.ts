import { Absence } from 'lib-common/api-types/child/Absences'
import FiniteDateRange from 'lib-common/finite-date-range'
import { groupDatesToRanges } from 'lib-common/utils/local-date'

export function groupAbsencesByDateRange(
  absences: Absence[]
): FiniteDateRange[] {
  return groupDatesToRanges(absences.map((absence) => absence.date))
}
