// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import YearMonth from 'lib-common/year-month'
import { AttendanceSummary } from 'lib-common/generated/api-types/children'
import { ChildAndPermittedActions } from 'lib-common/generated/api-types/children'
import { DailyServiceTimes } from 'lib-common/generated/api-types/dailyservicetimes'
import { JsonOf } from 'lib-common/json'
import { ServiceNeedSummary } from 'lib-common/generated/api-types/serviceneed'
import { UUID } from 'lib-common/types'
import { client } from '../../api-client'
import { deserializeJsonDailyServiceTimes } from 'lib-common/generated/api-types/dailyservicetimes'
import { deserializeJsonServiceNeedSummary } from 'lib-common/generated/api-types/serviceneed'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.children.ChildControllerCitizen.getChildAttendanceSummary
*/
export async function getChildAttendanceSummary(
  request: {
    childId: UUID,
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
    childId: UUID
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
    childId: UUID
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
