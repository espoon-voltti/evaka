// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AttendanceSummary } from 'lib-common/generated/api-types/children'
import type { ChildAndPermittedActions } from 'lib-common/generated/api-types/children'
import type { DailyServiceTimes } from 'lib-common/generated/api-types/dailyservicetimes'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { ServiceNeedSummary } from 'lib-common/generated/api-types/serviceneed'
import YearMonth from 'lib-common/year-month'
import { client } from '../../api-client'
import { deserializeJsonDailyServiceTimes } from 'lib-common/generated/api-types/dailyservicetimes'
import { deserializeJsonServiceNeedSummary } from 'lib-common/generated/api-types/serviceneed'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.children.ChildControllerCitizen.getChildAttendanceSummary
*/
export async function getChildAttendanceSummary(
  request: {
    childId: PersonId,
    yearMonth: YearMonth
  }
): Promise<AttendanceSummary> {
  const { data: json } = await client.request<JsonOf<AttendanceSummary>>({
    url: uri`/citizen/children/${request.childId}/attendance-summary/${request.yearMonth.formatIso()}`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.children.ChildControllerCitizen.getChildDailyServiceTimes
*/
export async function getChildDailyServiceTimes(
  request: {
    childId: PersonId
  }
): Promise<DailyServiceTimes[]> {
  const { data: json } = await client.request<JsonOf<DailyServiceTimes[]>>({
    url: uri`/citizen/children/${request.childId}/daily-service-times`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonDailyServiceTimes(e))
}


/**
* Generated from fi.espoo.evaka.children.ChildControllerCitizen.getChildServiceNeeds
*/
export async function getChildServiceNeeds(
  request: {
    childId: PersonId
  }
): Promise<ServiceNeedSummary[]> {
  const { data: json } = await client.request<JsonOf<ServiceNeedSummary[]>>({
    url: uri`/citizen/children/${request.childId}/service-needs`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonServiceNeedSummary(e))
}


/**
* Generated from fi.espoo.evaka.children.ChildControllerCitizen.getChildren
*/
export async function getChildren(): Promise<ChildAndPermittedActions[]> {
  const { data: json } = await client.request<JsonOf<ChildAndPermittedActions[]>>({
    url: uri`/citizen/children`.toString(),
    method: 'GET'
  })
  return json
}
