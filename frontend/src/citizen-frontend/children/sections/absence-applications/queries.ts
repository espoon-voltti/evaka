// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  deleteAbsenceApplication,
  getAbsenceApplications,
  postAbsenceApplication
} from '../../../generated/api-clients/absence'

const q = new Queries()

export const getAbsenceApplicationsQuery = q.query(getAbsenceApplications)
export const postAbsenceApplicationMutation = q.mutation(
  postAbsenceApplication,
  [({ body: { childId } }) => getAbsenceApplicationsQuery({ childId })]
)
export const deleteAbsenceApplicationMutation = q.mutation(
  deleteAbsenceApplication,
  [getAbsenceApplicationsQuery.prefix]
)
