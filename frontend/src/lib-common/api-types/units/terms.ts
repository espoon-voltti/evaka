// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { JsonOf } from '../../json'
import LocalDate from '../../local-date'

export interface ClubTerm {
  term: {
    start: LocalDate
    end: LocalDate
  }
}

export const deserializeClubTerm = (clubTerm: JsonOf<ClubTerm>): ClubTerm => ({
  term: {
    start: LocalDate.parseIso(clubTerm.term.start),
    end: LocalDate.parseIso(clubTerm.term.end)
  }
})

export interface PreschoolTerm {
  finnishPreschool: FiniteDateRange
  swedishPreschool: FiniteDateRange
  extendedTerm: FiniteDateRange
  applicationPeriod: FiniteDateRange
}

export const deserializePreschoolTerm = (
  preschoolTerm: JsonOf<PreschoolTerm>
): PreschoolTerm => ({
  finnishPreschool: FiniteDateRange.parseJson(preschoolTerm.finnishPreschool),
  swedishPreschool: FiniteDateRange.parseJson(preschoolTerm.swedishPreschool),
  extendedTerm: FiniteDateRange.parseJson(preschoolTerm.extendedTerm),
  applicationPeriod: FiniteDateRange.parseJson(preschoolTerm.applicationPeriod)
})
