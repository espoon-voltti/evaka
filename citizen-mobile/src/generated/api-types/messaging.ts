// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Attachment } from './attachment'
import type { CitizenPushSubscriptionId } from './shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type { JsonOf } from 'lib-common/json'
import type { MessageAccountId } from './shared'
import type { MessageId } from './shared'
import type { MessageThreadId } from './shared'
import type { PersonId } from './shared'

/**
* Generated from fi.espoo.evaka.messaging.AccountType
*/
export type AccountType =
  | 'PERSONAL'
  | 'GROUP'
  | 'CITIZEN'
  | 'MUNICIPAL'
  | 'SERVICE_WORKER'
  | 'FINANCE'

/**
* Generated from fi.espoo.evaka.messaging.Message
*/
export interface Message {
  attachments: Attachment[]
  content: string
  id: MessageId
  readAt: HelsinkiDateTime | null
  recipientNames: string[] | null
  recipients: MessageAccount[]
  sender: MessageAccount
  sentAt: HelsinkiDateTime
  threadId: MessageThreadId
}

/**
* Generated from fi.espoo.evaka.messaging.MessageAccount
*/
export interface MessageAccount {
  id: MessageAccountId
  name: string
  personId: PersonId | null
  type: AccountType
}

/**
* Generated from fi.espoo.evaka.messaging.mobile.MobileMessage
*/
export interface MobileMessage {
  content: string
  id: MessageId
  readAt: HelsinkiDateTime | null
  senderAccountId: MessageAccountId
  senderName: string
  sentAt: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.messaging.mobile.MobileMyAccount
*/
export interface MobileMyAccount {
  accountId: MessageAccountId
  messageAttachmentsAllowed: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.mobile.MobileThread
*/
export interface MobileThread {
  id: MessageThreadId
  messages: MobileMessage[]
  title: string
}

/**
* Generated from fi.espoo.evaka.messaging.mobile.MobileThreadListItem
*/
export interface MobileThreadListItem {
  id: MessageThreadId
  lastMessageAt: HelsinkiDateTime
  lastMessagePreview: string
  senderName: string
  title: string
  unreadCount: number
}

/**
* Generated from fi.espoo.evaka.messaging.mobile.MobileThreadListResponse
*/
export interface MobileThreadListResponse {
  data: MobileThreadListItem[]
  hasMore: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.mobile.MessageControllerMobile.ReplyBody
*/
export interface ReplyBody {
  content: string
  recipientAccountIds: MessageAccountId[]
}

/**
* Generated from fi.espoo.evaka.messaging.MessageService.ThreadReply
*/
export interface ThreadReply {
  message: Message
  threadId: MessageThreadId
}

/**
* Generated from fi.espoo.evaka.messaging.mobile.CitizenPushSubscriptionController.UpsertBody
*/
export interface UpsertBody {
  deviceId: CitizenPushSubscriptionId
  expoPushToken: string
}


export function deserializeJsonMessage(json: JsonOf<Message>): Message {
  return {
    ...json,
    readAt: (json.readAt != null) ? HelsinkiDateTime.parseIso(json.readAt) : null,
    sentAt: HelsinkiDateTime.parseIso(json.sentAt)
  }
}


export function deserializeJsonMobileMessage(json: JsonOf<MobileMessage>): MobileMessage {
  return {
    ...json,
    readAt: (json.readAt != null) ? HelsinkiDateTime.parseIso(json.readAt) : null,
    sentAt: HelsinkiDateTime.parseIso(json.sentAt)
  }
}


export function deserializeJsonMobileThread(json: JsonOf<MobileThread>): MobileThread {
  return {
    ...json,
    messages: json.messages.map(e => deserializeJsonMobileMessage(e))
  }
}


export function deserializeJsonMobileThreadListItem(json: JsonOf<MobileThreadListItem>): MobileThreadListItem {
  return {
    ...json,
    lastMessageAt: HelsinkiDateTime.parseIso(json.lastMessageAt)
  }
}


export function deserializeJsonMobileThreadListResponse(json: JsonOf<MobileThreadListResponse>): MobileThreadListResponse {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonMobileThreadListItem(e))
  }
}


export function deserializeJsonThreadReply(json: JsonOf<ThreadReply>): ThreadReply {
  return {
    ...json,
    message: deserializeJsonMessage(json.message)
  }
}
