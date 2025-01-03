// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import { DaycareId } from './shared'
import { EmployeeId } from './shared'
import { JsonOf } from '../../json'
import { MobileDeviceId } from './shared'
import { PairingId } from './shared'

/**
* Generated from fi.espoo.evaka.pairing.MobileDevice
*/
export interface MobileDevice {
  id: MobileDeviceId
  name: string
}

/**
* Generated from fi.espoo.evaka.pairing.MobileDeviceDetails
*/
export interface MobileDeviceDetails {
  employeeId: EmployeeId | null
  id: MobileDeviceId
  name: string
  personalDevice: boolean
  pushApplicationServerKey: string | null
  unitIds: DaycareId[]
}

/**
* Generated from fi.espoo.evaka.pairing.Pairing
*/
export interface Pairing {
  challengeKey: string
  employeeId: EmployeeId | null
  expires: HelsinkiDateTime
  id: PairingId
  mobileDeviceId: MobileDeviceId | null
  responseKey: string | null
  status: PairingStatus
  unitId: DaycareId | null
}

/**
* Generated from fi.espoo.evaka.pairing.PairingStatus
*/
export type PairingStatus =
  | 'WAITING_CHALLENGE'
  | 'WAITING_RESPONSE'
  | 'READY'
  | 'PAIRED'

/**
* Generated from fi.espoo.evaka.pairing.PairingsController.PairingStatusRes
*/
export interface PairingStatusRes {
  status: PairingStatus
}

/**
* Generated from fi.espoo.evaka.pairing.PairingsController.PostPairingChallengeReq
*/
export interface PostPairingChallengeReq {
  challengeKey: string
}


export namespace PostPairingReq {
  /**
  * Generated from fi.espoo.evaka.pairing.PairingsController.PostPairingReq.Employee
  */
  export interface Employee {
    employeeId: EmployeeId
  }

  /**
  * Generated from fi.espoo.evaka.pairing.PairingsController.PostPairingReq.Unit
  */
  export interface Unit {
    unitId: DaycareId
  }
}

/**
* Generated from fi.espoo.evaka.pairing.PairingsController.PostPairingReq
*/
export type PostPairingReq = PostPairingReq.Employee | PostPairingReq.Unit


/**
* Generated from fi.espoo.evaka.pairing.PairingsController.PostPairingResponseReq
*/
export interface PostPairingResponseReq {
  challengeKey: string
  responseKey: string
}

/**
* Generated from fi.espoo.evaka.pairing.MobileDevicesController.RenameRequest
*/
export interface RenameRequest {
  name: string
}


export function deserializeJsonPairing(json: JsonOf<Pairing>): Pairing {
  return {
    ...json,
    expires: HelsinkiDateTime.parseIso(json.expires)
  }
}
