// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { MobileDevice } from 'lib-common/generated/api-types/pairing'
import { Pairing } from 'lib-common/generated/api-types/pairing'
import { PairingStatusRes } from 'lib-common/generated/api-types/pairing'
import { PostPairingReq } from 'lib-common/generated/api-types/pairing'
import { PostPairingResponseReq } from 'lib-common/generated/api-types/pairing'
import { RenameRequest } from 'lib-common/generated/api-types/pairing'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonPairing } from 'lib-common/generated/api-types/pairing'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.pairing.MobileDevicesController.deleteMobileDevice
*/
export async function deleteMobileDevice(
  request: {
    id: UUID
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
    unitId: UUID
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
    id: UUID,
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
    id: UUID
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
    id: UUID,
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
