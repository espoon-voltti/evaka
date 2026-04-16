// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PendingVideoCall } from 'lib-common/generated/api-types/videocall'
import type { PostSignalRequest } from 'lib-common/generated/api-types/videocall'
import type { SignalsResponse } from 'lib-common/generated/api-types/videocall'
import type { UUID } from 'lib-common/types'
import type { VideoCallRoomInfo } from 'lib-common/generated/api-types/videocall'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonPendingVideoCall } from 'lib-common/generated/api-types/videocall'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.videocall.VideoCallController.acceptCall
*/
export async function acceptCall(
  request: {
    roomId: UUID
  }
): Promise<VideoCallRoomInfo> {
  const { data: json } = await client.request<JsonOf<VideoCallRoomInfo>>({
    url: uri`/citizen/video-call/${request.roomId}/accept`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.videocall.VideoCallController.citizenGetSignals
*/
export async function citizenGetSignals(
  request: {
    roomId: UUID,
    since: number
  }
): Promise<SignalsResponse> {
  const params = createUrlSearchParams(
    ['since', request.since.toString()]
  )
  const { data: json } = await client.request<JsonOf<SignalsResponse>>({
    url: uri`/citizen/video-call/${request.roomId}/signals`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.videocall.VideoCallController.citizenPostSignal
*/
export async function citizenPostSignal(
  request: {
    roomId: UUID,
    body: PostSignalRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/video-call/${request.roomId}/signal`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PostSignalRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.videocall.VideoCallController.endCitizen
*/
export async function endCitizen(
  request: {
    roomId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/video-call/${request.roomId}/end`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.videocall.VideoCallController.getRoomCitizen
*/
export async function getRoomCitizen(
  request: {
    roomId: UUID
  }
): Promise<VideoCallRoomInfo> {
  const { data: json } = await client.request<JsonOf<VideoCallRoomInfo>>({
    url: uri`/citizen/video-call/${request.roomId}`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.videocall.VideoCallController.listPending
*/
export async function listPending(): Promise<PendingVideoCall[]> {
  const { data: json } = await client.request<JsonOf<PendingVideoCall[]>>({
    url: uri`/citizen/video-call/pending`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPendingVideoCall(e))
}
