// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import { JsonOf } from '../../json'
import { MessageAttachment } from './attachment'
import { MessageReceiver } from '../../api-types/messaging'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.messaging.AccountType
*/
export type AccountType =
  | 'PERSONAL'
  | 'GROUP'
  | 'CITIZEN'
  | 'MUNICIPAL'
  | 'SERVICE_WORKER'

/**
* Generated from fi.espoo.evaka.messaging.AuthorizedMessageAccount
*/
export interface AuthorizedMessageAccount {
  account: MessageAccount
  daycareGroup: Group | null
}

/**
* Generated from fi.espoo.evaka.messaging.CitizenMessageBody
*/
export interface CitizenMessageBody {
  children: UUID[]
  content: string
  recipients: UUID[]
  title: string
}


export namespace CitizenMessageThread {
  /**
  * Generated from fi.espoo.evaka.messaging.CitizenMessageThread.Redacted
  */
  export interface Redacted {
    type: 'Redacted'
    hasUnreadMessages: boolean
    id: UUID
    lastMessageSentAt: HelsinkiDateTime | null
    sender: MessageAccount | null
    urgent: boolean
  }

  /**
  * Generated from fi.espoo.evaka.messaging.CitizenMessageThread.Regular
  */
  export interface Regular {
    type: 'Regular'
    children: MessageChild[]
    id: UUID
    isCopy: boolean
    messageType: MessageType
    messages: Message[]
    sensitive: boolean
    title: string
    urgent: boolean
  }
}

/**
* Generated from fi.espoo.evaka.messaging.CitizenMessageThread
*/
export type CitizenMessageThread = CitizenMessageThread.Redacted | CitizenMessageThread.Regular


/**
* Generated from fi.espoo.evaka.messaging.DraftContent
*/
export interface DraftContent {
  attachments: MessageAttachment[]
  content: string
  created: HelsinkiDateTime
  id: UUID
  recipientIds: UUID[]
  recipientNames: string[]
  sensitive: boolean
  title: string
  type: MessageType
  urgent: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.ChildRecipientsController.EditRecipientRequest
*/
export interface EditRecipientRequest {
  blocklisted: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.GetReceiversResponse
*/
export interface GetReceiversResponse {
  childrenToMessageAccounts: Record<UUID, UUID[]>
  messageAccounts: MessageAccount[]
}

/**
* Generated from fi.espoo.evaka.messaging.Group
*/
export interface Group {
  id: UUID
  name: string
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.messaging.Message
*/
export interface Message {
  attachments: MessageAttachment[]
  content: string
  id: UUID
  readAt: HelsinkiDateTime | null
  recipientNames: string[] | null
  recipients: MessageAccount[]
  sender: MessageAccount
  sentAt: HelsinkiDateTime
  threadId: UUID
}

/**
* Generated from fi.espoo.evaka.messaging.MessageAccount
*/
export interface MessageAccount {
  id: UUID
  name: string
  type: AccountType
}

/**
* Generated from fi.espoo.evaka.messaging.MessageChild
*/
export interface MessageChild {
  childId: UUID
  firstName: string
  lastName: string
  preferredName: string
}

/**
* Generated from fi.espoo.evaka.messaging.MessageCopy
*/
export interface MessageCopy {
  attachments: MessageAttachment[]
  content: string
  messageId: UUID
  readAt: HelsinkiDateTime | null
  recipientAccountType: AccountType
  recipientId: UUID
  recipientName: string
  recipientNames: string[]
  senderAccountType: AccountType
  senderId: UUID
  senderName: string
  sensitive: boolean
  sentAt: HelsinkiDateTime
  threadId: UUID
  title: string
  type: MessageType
  urgent: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.MessageReceiversResponse
*/
export interface MessageReceiversResponse {
  accountId: UUID
  receivers: MessageReceiver[]
}

/**
* Generated from fi.espoo.evaka.messaging.MessageRecipient
*/
export interface MessageRecipient {
  id: UUID
  type: MessageRecipientType
}

/**
* Generated from fi.espoo.evaka.messaging.MessageRecipientType
*/
export type MessageRecipientType =
  | 'AREA'
  | 'UNIT'
  | 'GROUP'
  | 'CHILD'
  | 'CITIZEN'

/**
* Generated from fi.espoo.evaka.messaging.MessageThread
*/
export interface MessageThread {
  children: MessageChild[]
  id: UUID
  isCopy: boolean
  messages: Message[]
  sensitive: boolean
  title: string
  type: MessageType
  urgent: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.MessageType
*/
export type MessageType =
  | 'MESSAGE'
  | 'BULLETIN'

/**
* Generated from fi.espoo.evaka.messaging.PagedCitizenMessageThreads
*/
export interface PagedCitizenMessageThreads {
  data: CitizenMessageThread[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.messaging.PagedMessageCopies
*/
export interface PagedMessageCopies {
  data: MessageCopy[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.messaging.PagedMessageThreads
*/
export interface PagedMessageThreads {
  data: MessageThread[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.messaging.PagedSentMessages
*/
export interface PagedSentMessages {
  data: SentMessage[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.messaging.MessageController.PostMessageBody
*/
export interface PostMessageBody {
  attachmentIds: UUID[]
  content: string
  draftId: UUID | null
  filters: PostMessageFilters | null
  recipientNames: string[]
  recipients: MessageRecipient[]
  relatedApplicationId: UUID | null
  sensitive: boolean
  title: string
  type: MessageType
  urgent: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.MessageController.PostMessageFilters
*/
export interface PostMessageFilters {
  familyDaycare: boolean
  intermittentShiftCare: boolean
  serviceNeedOptionIds: UUID[]
  shiftCare: boolean
  yearsOfBirth: number[]
}

/**
* Generated from fi.espoo.evaka.messaging.MessageController.PostMessagePreflightBody
*/
export interface PostMessagePreflightBody {
  filters: PostMessageFilters | null
  recipients: MessageRecipient[]
}

/**
* Generated from fi.espoo.evaka.messaging.MessageController.PostMessagePreflightResponse
*/
export interface PostMessagePreflightResponse {
  numberOfRecipientAccounts: number
}

/**
* Generated from fi.espoo.evaka.messaging.Recipient
*/
export interface Recipient {
  blocklisted: boolean
  firstName: string
  lastName: string
  personId: UUID
}

/**
* Generated from fi.espoo.evaka.messaging.ReplyToMessageBody
*/
export interface ReplyToMessageBody {
  content: string
  recipientAccountIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.messaging.SentMessage
*/
export interface SentMessage {
  attachments: MessageAttachment[]
  content: string
  contentId: UUID
  recipientNames: string[]
  sensitive: boolean
  sentAt: HelsinkiDateTime
  threadTitle: string
  type: MessageType
  urgent: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.MessageService.ThreadReply
*/
export interface ThreadReply {
  message: Message
  threadId: UUID
}

/**
* Generated from fi.espoo.evaka.messaging.UnreadCountByAccount
*/
export interface UnreadCountByAccount {
  accountId: UUID
  unreadCopyCount: number
  unreadCount: number
}

/**
* Generated from fi.espoo.evaka.messaging.UnreadCountByAccountAndGroup
*/
export interface UnreadCountByAccountAndGroup {
  accountId: UUID
  groupId: UUID
  unreadCopyCount: number
  unreadCount: number
}

/**
* Generated from fi.espoo.evaka.messaging.UpdatableDraftContent
*/
export interface UpdatableDraftContent {
  content: string
  recipientIds: UUID[]
  recipientNames: string[]
  sensitive: boolean
  title: string
  type: MessageType
  urgent: boolean
}



export function deserializeJsonCitizenMessageThreadRedacted(json: JsonOf<CitizenMessageThread.Redacted>): CitizenMessageThread.Redacted {
  return {
    ...json,
    lastMessageSentAt: (json.lastMessageSentAt != null) ? HelsinkiDateTime.parseIso(json.lastMessageSentAt) : null
  }
}

export function deserializeJsonCitizenMessageThreadRegular(json: JsonOf<CitizenMessageThread.Regular>): CitizenMessageThread.Regular {
  return {
    ...json,
    messages: json.messages.map(e => deserializeJsonMessage(e))
  }
}
export function deserializeJsonCitizenMessageThread(json: JsonOf<CitizenMessageThread>): CitizenMessageThread {
  switch (json.type) {
    case 'Redacted': return deserializeJsonCitizenMessageThreadRedacted(json)
    case 'Regular': return deserializeJsonCitizenMessageThreadRegular(json)
    default: return json
  }
}


export function deserializeJsonDraftContent(json: JsonOf<DraftContent>): DraftContent {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created)
  }
}


export function deserializeJsonMessage(json: JsonOf<Message>): Message {
  return {
    ...json,
    readAt: (json.readAt != null) ? HelsinkiDateTime.parseIso(json.readAt) : null,
    sentAt: HelsinkiDateTime.parseIso(json.sentAt)
  }
}


export function deserializeJsonMessageCopy(json: JsonOf<MessageCopy>): MessageCopy {
  return {
    ...json,
    readAt: (json.readAt != null) ? HelsinkiDateTime.parseIso(json.readAt) : null,
    sentAt: HelsinkiDateTime.parseIso(json.sentAt)
  }
}


export function deserializeJsonMessageThread(json: JsonOf<MessageThread>): MessageThread {
  return {
    ...json,
    messages: json.messages.map(e => deserializeJsonMessage(e))
  }
}


export function deserializeJsonPagedCitizenMessageThreads(json: JsonOf<PagedCitizenMessageThreads>): PagedCitizenMessageThreads {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonCitizenMessageThread(e))
  }
}


export function deserializeJsonPagedMessageCopies(json: JsonOf<PagedMessageCopies>): PagedMessageCopies {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonMessageCopy(e))
  }
}


export function deserializeJsonPagedMessageThreads(json: JsonOf<PagedMessageThreads>): PagedMessageThreads {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonMessageThread(e))
  }
}


export function deserializeJsonPagedSentMessages(json: JsonOf<PagedSentMessages>): PagedSentMessages {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonSentMessage(e))
  }
}


export function deserializeJsonSentMessage(json: JsonOf<SentMessage>): SentMessage {
  return {
    ...json,
    sentAt: HelsinkiDateTime.parseIso(json.sentAt)
  }
}


export function deserializeJsonThreadReply(json: JsonOf<ThreadReply>): ThreadReply {
  return {
    ...json,
    message: deserializeJsonMessage(json.message)
  }
}
