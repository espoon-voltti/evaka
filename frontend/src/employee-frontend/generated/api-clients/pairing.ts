// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { MobileDevice } from 'lib-common/generated/api-types/pairing'
import type { MobileDeviceId } from 'lib-common/generated/api-types/shared'
import type { Pairing } from 'lib-common/generated/api-types/pairing'
import type { PairingId } from 'lib-common/generated/api-types/shared'
import type { PairingStatusRes } from 'lib-common/generated/api-types/pairing'
import type { PostPairingReq } from 'lib-common/generated/api-types/pairing'
import type { PostPairingResponseReq } from 'lib-common/generated/api-types/pairing'
import type { RenameRequest } from 'lib-common/generated/api-types/pairing'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonPairing } from 'lib-common/generated/api-types/pairing'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.pairing.MobileDevicesController.deleteMobileDevice
*/
export async function deleteMobileDevice(
  request: {
    id: MobileDeviceId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/mobile-devices/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pairing.MobileDevicesController.getMobileDevices
*/
export async function getMobileDevices(
  request: {
    unitId: DaycareId
  }
): Promise<MobileDevice[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId]
  )
  const { data: json } = await client.request<JsonOf<MobileDevice[]>>({
    url: uri`/employee/mobile-devices`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pairing.MobileDevicesController.getPersonalMobileDevices
*/
export async function getPersonalMobileDevices(): Promise<MobileDevice[]> {
  const { data: json } = await client.request<JsonOf<MobileDevice[]>>({
    url: uri`/employee/mobile-devices/personal`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pairing.MobileDevicesController.putMobileDeviceName
*/
export async function putMobileDeviceName(
  request: {
    id: MobileDeviceId,
    body: RenameRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/mobile-devices/${request.id}/name`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<RenameRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pairing.PairingsController.getPairingStatus
*/
export async function getPairingStatus(
  request: {
    id: PairingId
  }
): Promise<PairingStatusRes> {
  const { data: json } = await client.request<JsonOf<PairingStatusRes>>({
    url: uri`/employee/public/pairings/${request.id}/status`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pairing.PairingsController.postPairing
*/
export async function postPairing(
  request: {
    body: PostPairingReq
  }
): Promise<Pairing> {
  const { data: json } = await client.request<JsonOf<Pairing>>({
    url: uri`/employee/pairings`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PostPairingReq>
  })
  return deserializeJsonPairing(json)
}


/**
* Generated from fi.espoo.evaka.pairing.PairingsController.postPairingResponse
*/
export async function postPairingResponse(
  request: {
    id: PairingId,
    body: PostPairingResponseReq
  }
): Promise<Pairing> {
  const { data: json } = await client.request<JsonOf<Pairing>>({
    url: uri`/employee/pairings/${request.id}/response`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PostPairingResponseReq>
  })
  return deserializeJsonPairing(json)
}
