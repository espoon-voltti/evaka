// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { DaycareId } from './shared'
import type { EmployeeId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import type { MobileDeviceId } from './shared'
import type { PairingId } from './shared'

/**
* Generated from evaka.core.pairing.MobileDevice
*/
export interface MobileDevice {
  id: MobileDeviceId
  name: string
}

/**
* Generated from evaka.core.pairing.MobileDeviceDetails
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
* Generated from evaka.core.pairing.Pairing
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
* Generated from evaka.core.pairing.PairingStatus
*/
export type PairingStatus =
  | 'WAITING_CHALLENGE'
  | 'WAITING_RESPONSE'
  | 'READY'
  | 'PAIRED'

/**
* Generated from evaka.core.pairing.PairingsController.PairingStatusRes
*/
export interface PairingStatusRes {
  status: PairingStatus
}

/**
* Generated from evaka.core.pairing.PairingsController.PostPairingChallengeReq
*/
export interface PostPairingChallengeReq {
  challengeKey: string
}


export namespace PostPairingReq {
  /**
  * Generated from evaka.core.pairing.PairingsController.PostPairingReq.Employee
  */
  export interface Employee {
    employeeId: EmployeeId
  }

  /**
  * Generated from evaka.core.pairing.PairingsController.PostPairingReq.Unit
  */
  export interface Unit {
    unitId: DaycareId
  }
}

/**
* Generated from evaka.core.pairing.PairingsController.PostPairingReq
*/
export type PostPairingReq = PostPairingReq.Employee | PostPairingReq.Unit


/**
* Generated from evaka.core.pairing.PairingsController.PostPairingResponseReq
*/
export interface PostPairingResponseReq {
  challengeKey: string
  responseKey: string
}

/**
* Generated from evaka.core.pairing.MobileDevicesController.RenameRequest
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
