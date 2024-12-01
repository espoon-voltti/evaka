// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/mobile-devices/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pairing.MobileDevicesController.getMobileDevices
*/
export async function getMobileDevices(
  request: {
    unitId: UUID
  },
  headers?: AxiosHeaders
): Promise<MobileDevice[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId]
  )
  const { data: json } = await client.request<JsonOf<MobileDevice[]>>({
    url: uri`/employee/mobile-devices`.toString(),
    method: 'GET',
    headers,
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pairing.MobileDevicesController.getPersonalMobileDevices
*/
export async function getPersonalMobileDevices(
  headers?: AxiosHeaders
): Promise<MobileDevice[]> {
  const { data: json } = await client.request<JsonOf<MobileDevice[]>>({
    url: uri`/employee/mobile-devices/personal`.toString(),
    method: 'GET',
    headers
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/mobile-devices/${request.id}/name`.toString(),
    method: 'PUT',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<PairingStatusRes> {
  const { data: json } = await client.request<JsonOf<PairingStatusRes>>({
    url: uri`/employee/public/pairings/${request.id}/status`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pairing.PairingsController.postPairing
*/
export async function postPairing(
  request: {
    body: PostPairingReq
  },
  headers?: AxiosHeaders
): Promise<Pairing> {
  const { data: json } = await client.request<JsonOf<Pairing>>({
    url: uri`/employee/pairings`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<Pairing> {
  const { data: json } = await client.request<JsonOf<Pairing>>({
    url: uri`/employee/pairings/${request.id}/response`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<PostPairingResponseReq>
  })
  return deserializeJsonPairing(json)
}
