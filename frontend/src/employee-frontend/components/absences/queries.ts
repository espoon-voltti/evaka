// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  addPresences,
  deleteHolidayReservations,
  groupMonthCalendar,
  upsertAbsences
} from '../../generated/api-clients/absence'

const q = new Queries()

export const groupMonthCalendarQuery = q.query(groupMonthCalendar)

export const upsertAbsencesMutation = q.mutation(upsertAbsences, [
  groupMonthCalendarQuery.prefix
])

export const addPresencesMutation = q.mutation(addPresences, [
  groupMonthCalendarQuery.prefix
])

export const deleteHolidayReservationsMutation = q.mutation(
  deleteHolidayReservations,
  [groupMonthCalendarQuery.prefix]
)
