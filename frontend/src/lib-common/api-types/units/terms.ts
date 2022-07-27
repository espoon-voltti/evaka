// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  ClubTerm,
  PreschoolTerm
} from 'lib-common/generated/api-types/daycare'

import type { JsonOf } from '../../json'

export const deserializeClubTerm = (clubTerm: JsonOf<ClubTerm>): ClubTerm => ({
  applicationPeriod: FiniteDateRange.parseJson(clubTerm.applicationPeriod),
  term: FiniteDateRange.parseJson(clubTerm.term)
})

export const deserializePreschoolTerm = (
  preschoolTerm: JsonOf<PreschoolTerm>
): PreschoolTerm => ({
  finnishPreschool: FiniteDateRange.parseJson(preschoolTerm.finnishPreschool),
  swedishPreschool: FiniteDateRange.parseJson(preschoolTerm.swedishPreschool),
  extendedTerm: FiniteDateRange.parseJson(preschoolTerm.extendedTerm),
  applicationPeriod: FiniteDateRange.parseJson(preschoolTerm.applicationPeriod)
})
