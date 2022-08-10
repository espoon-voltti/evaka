// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { UnitInfo, UnitStats } from 'lib-common/generated/api-types/attendance'
import { Pairing } from 'lib-common/generated/api-types/pairing'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'

import { client } from './client'

type PairingStatus =
  | 'WAITING_CHALLENGE'
  | 'WAITING_RESPONSE'
  | 'READY'
  | 'PAIRED'

export type PairingResponse = Pairing

export interface PairingStatusResponse {
  status: PairingStatus
}

export function postPairingChallenge(
  challengeKey: string
): Promise<Result<PairingResponse>> {
  return client
    .post<JsonOf<PairingResponse>>(`/public/pairings/challenge`, {
      challengeKey
    })
    .then((res) => res.data)
    .then((pairingResponse) => {
      return {
        ...pairingResponse,
        expires: HelsinkiDateTime.parseIso(pairingResponse.expires)
      }
    })
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export function getPairingStatus(
  pairingId: string
): Promise<Result<PairingStatusResponse>> {
  return client
    .get<JsonOf<PairingStatusResponse>>(`/public/pairings/${pairingId}/status`)
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function authMobile(
  id: string,
  challengeKey: string,
  responseKey: string
): Promise<Result<void>> {
  return client
    .post<JsonOf<void>>(`/auth/mobile`, {
      id,
      challengeKey,
      responseKey
    })
    .then((res) => res.data)
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export function getMobileUnitInfo(unitId: string): Promise<Result<UnitInfo>> {
  return client
    .get<JsonOf<UnitInfo>>(`/mobile/units/${unitId}`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export function getMobileUnitStats(
  unitIds: string[]
): Promise<Result<UnitStats[]>> {
  return client
    .get<JsonOf<UnitStats[]>>('/mobile/units/stats', {
      params: {
        unitIds: unitIds.join(',')
      }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
