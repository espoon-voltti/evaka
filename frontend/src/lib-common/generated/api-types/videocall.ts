// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import type { PersonId } from './shared'
import type { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.videocall.PendingVideoCall
*/
export interface PendingVideoCall {
  childName: string
  createdAt: HelsinkiDateTime
  employeeName: string
  roomId: UUID
}

/**
* Generated from fi.espoo.evaka.videocall.PostSignalRequest
*/
export interface PostSignalRequest {
  data: string
  kind: string
}

/**
* Generated from fi.espoo.evaka.videocall.SignalsResponse
*/
export interface SignalsResponse {
  nextSince: number
  signals: VideoCallSignal[]
}

/**
* Generated from fi.espoo.evaka.videocall.StartVideoCallRequest
*/
export interface StartVideoCallRequest {
  childId: PersonId
}

/**
* Generated from fi.espoo.evaka.videocall.StartVideoCallResponse
*/
export interface StartVideoCallResponse {
  childName: string
  guardianIds: PersonId[]
  roomId: UUID
}

/**
* Generated from fi.espoo.evaka.videocall.VideoCallRole
*/
export type VideoCallRole =
  | 'EMPLOYEE'
  | 'CITIZEN'

/**
* Generated from fi.espoo.evaka.videocall.VideoCallRoomInfo
*/
export interface VideoCallRoomInfo {
  childName: string
  employeeName: string
  roomId: UUID
  status: VideoCallRoomStatus
  yourRole: VideoCallRole
}

/**
* Generated from fi.espoo.evaka.videocall.VideoCallRoomStatus
*/
export type VideoCallRoomStatus =
  | 'RINGING'
  | 'ACTIVE'
  | 'ENDED'

/**
* Generated from fi.espoo.evaka.videocall.VideoCallSignal
*/
export interface VideoCallSignal {
  data: string
  from: VideoCallRole
  kind: string
  seq: number
}


export function deserializeJsonPendingVideoCall(json: JsonOf<PendingVideoCall>): PendingVideoCall {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt)
  }
}
