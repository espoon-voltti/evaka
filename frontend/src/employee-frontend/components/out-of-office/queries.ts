// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  deleteOutOfOfficePeriod,
  getOutOfOfficePeriods,
  upsertOutOfOfficePeriod
} from '../../generated/api-clients/outofoffice'

const q = new Queries()

export const outOfOfficePeriodsQuery = q.query(getOutOfOfficePeriods)

export const deleteOutOfOfficePeriodMutation = q.mutation(
  deleteOutOfOfficePeriod,
  [outOfOfficePeriodsQuery]
)

export const upsertOutOfOfficePeriodMutation = q.mutation(
  upsertOutOfOfficePeriod,
  [outOfOfficePeriodsQuery]
)
