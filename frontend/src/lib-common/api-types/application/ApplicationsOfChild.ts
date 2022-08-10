// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { ApplicationsOfChild } from '../../generated/api-types/application'
import { JsonOf } from '../../json'
import LocalDate from '../../local-date'

export const deserializeApplicationsOfChild = (
  json: JsonOf<ApplicationsOfChild>
): ApplicationsOfChild => ({
  ...json,
  applicationSummaries: json.applicationSummaries.map((json2) => ({
    ...json2,
    sentDate: LocalDate.parseNullableIso(json2.sentDate),
    startDate: LocalDate.parseNullableIso(json2.startDate),
    createdDate: HelsinkiDateTime.parseIso(json2.createdDate),
    modifiedDate: HelsinkiDateTime.parseIso(json2.modifiedDate)
  }))
})
