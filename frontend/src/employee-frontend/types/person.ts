// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

export const deserializePersonJSON = (
  data: JsonOf<PersonJSON>
): PersonJSON => ({
  ...data,
  dateOfBirth: LocalDate.parseIso(data.dateOfBirth),
  dateOfDeath: LocalDate.parseNullableIso(data.dateOfDeath)
})

export type SearchColumn =
  | 'last_name,first_name'
  | 'date_of_birth'
  | 'street_address'
  | 'social_security_number'
