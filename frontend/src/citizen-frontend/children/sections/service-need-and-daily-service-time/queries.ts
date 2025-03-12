// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getChildAttendanceSummary,
  getChildDailyServiceTimes,
  getChildServiceNeeds
} from '../../../generated/api-clients/children'

const q = new Queries()

export const childServiceNeedsQuery = q.query(getChildServiceNeeds)
export const attendanceSummaryQuery = q.query(getChildAttendanceSummary)
export const childDailyServiceTimesQuery = q.query(getChildDailyServiceTimes)
