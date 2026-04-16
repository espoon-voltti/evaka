// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PostSignalRequest } from 'lib-common/generated/api-types/videocall'
import type { SignalsResponse } from 'lib-common/generated/api-types/videocall'
import type { StartVideoCallRequest } from 'lib-common/generated/api-types/videocall'
import type { StartVideoCallResponse } from 'lib-common/generated/api-types/videocall'
import type { UUID } from 'lib-common/types'
import type { VideoCallRoomInfo } from 'lib-common/generated/api-types/videocall'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.videocall.VideoCallController.employeeGetSignals
*/
export async function employeeGetSignals(
  request: {
    roomId: UUID,
    since: number
  }
): Promise<SignalsResponse> {
  const params = createUrlSearchParams(
    ['since', request.since.toString()]
  )
  const { data: json } = await client.request<JsonOf<SignalsResponse>>({
    url: uri`/employee/video-call/${request.roomId}/signals`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.videocall.VideoCallController.employeePostSignal
*/
export async function employeePostSignal(
  request: {
    roomId: UUID,
    body: PostSignalRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/video-call/${request.roomId}/signal`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PostSignalRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.videocall.VideoCallController.endEmployee
*/
export async function endEmployee(
  request: {
    roomId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/video-call/${request.roomId}/end`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.videocall.VideoCallController.getRoomEmployee
*/
export async function getRoomEmployee(
  request: {
    roomId: UUID
  }
): Promise<VideoCallRoomInfo> {
  const { data: json } = await client.request<JsonOf<VideoCallRoomInfo>>({
    url: uri`/employee/video-call/${request.roomId}`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.videocall.VideoCallController.startCall
*/
export async function startCall(
  request: {
    body: StartVideoCallRequest
  }
): Promise<StartVideoCallResponse> {
  const { data: json } = await client.request<JsonOf<StartVideoCallResponse>>({
    url: uri`/employee/video-call`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<StartVideoCallRequest>
  })
  return json
}
