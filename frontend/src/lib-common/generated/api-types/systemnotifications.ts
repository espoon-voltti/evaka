// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import { JsonOf } from '../../json'

/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.CurrentNotificationResponseCitizen
*/
export interface CurrentNotificationResponseCitizen {
  notification: SystemNotificationCitizens | null
}

/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.CurrentNotificationResponseEmployee
*/
export interface CurrentNotificationResponseEmployee {
  notification: SystemNotificationEmployees | null
}

/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationCitizens
*/
export interface SystemNotificationCitizens {
  text: string
  textEn: string
  textSv: string
  validTo: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationEmployees
*/
export interface SystemNotificationEmployees {
  text: string
  validTo: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationTargetGroup
*/
export type SystemNotificationTargetGroup =
  | 'CITIZENS'
  | 'EMPLOYEES'

/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.SystemNotificationsResponse
*/
export interface SystemNotificationsResponse {
  citizens: SystemNotificationCitizens | null
  employees: SystemNotificationEmployees | null
}


export function deserializeJsonCurrentNotificationResponseCitizen(json: JsonOf<CurrentNotificationResponseCitizen>): CurrentNotificationResponseCitizen {
  return {
    ...json,
    notification: (json.notification != null) ? deserializeJsonSystemNotificationCitizens(json.notification) : null
  }
}


export function deserializeJsonCurrentNotificationResponseEmployee(json: JsonOf<CurrentNotificationResponseEmployee>): CurrentNotificationResponseEmployee {
  return {
    ...json,
    notification: (json.notification != null) ? deserializeJsonSystemNotificationEmployees(json.notification) : null
  }
}


export function deserializeJsonSystemNotificationCitizens(json: JsonOf<SystemNotificationCitizens>): SystemNotificationCitizens {
  return {
    ...json,
    validTo: HelsinkiDateTime.parseIso(json.validTo)
  }
}


export function deserializeJsonSystemNotificationEmployees(json: JsonOf<SystemNotificationEmployees>): SystemNotificationEmployees {
  return {
    ...json,
    validTo: HelsinkiDateTime.parseIso(json.validTo)
  }
}


export function deserializeJsonSystemNotificationsResponse(json: JsonOf<SystemNotificationsResponse>): SystemNotificationsResponse {
  return {
    ...json,
    citizens: (json.citizens != null) ? deserializeJsonSystemNotificationCitizens(json.citizens) : null,
    employees: (json.employees != null) ? deserializeJsonSystemNotificationEmployees(json.employees) : null
  }
}
