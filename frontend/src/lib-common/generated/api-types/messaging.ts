// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import { ApplicationId } from './shared'
import { AttachmentId } from './shared'
import { DaycareId } from './shared'
import { GroupId } from './shared'
import { JsonOf } from '../../json'
import { MessageAccountId } from './shared'
import { MessageAttachment } from './attachment'
import { MessageContentId } from './shared'
import { MessageDraftId } from './shared'
import { MessageId } from './shared'
import { MessageReceiver } from '../../api-types/messaging'
import { MessageThreadId } from './shared'
import { PersonId } from './shared'

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
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.ChildMessageAccountAccess
*/
export interface ChildMessageAccountAccess {
  newMessage: MessageAccountId[]
  reply: MessageAccountId[]
}

/**
* Generated from fi.espoo.evaka.messaging.CitizenMessageBody
*/
export interface CitizenMessageBody {
  attachmentIds: AttachmentId[]
  children: PersonId[]
  content: string
  recipients: MessageAccountId[]
  title: string
}


export namespace CitizenMessageThread {
  /**
  * Generated from fi.espoo.evaka.messaging.CitizenMessageThread.Redacted
  */
  export interface Redacted {
    type: 'Redacted'
    hasUnreadMessages: boolean
    id: MessageThreadId
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
    id: MessageThreadId
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
* Generated from fi.espoo.evaka.messaging.MessageController.CreateMessageResponse
*/
export interface CreateMessageResponse {
  createdId: MessageContentId | null
}

/**
* Generated from fi.espoo.evaka.messaging.DraftContent
*/
export interface DraftContent {
  attachments: MessageAttachment[]
  content: string
  createdAt: HelsinkiDateTime
  id: MessageDraftId
  recipientIds: string[]
  recipientNames: string[]
  sensitive: boolean
  title: string
  type: MessageType
  urgent: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.GetReceiversResponse
*/
export interface GetReceiversResponse {
  childrenToMessageAccounts: Partial<Record<PersonId, ChildMessageAccountAccess>>
  messageAccounts: MessageAccountWithPresence[]
}

/**
* Generated from fi.espoo.evaka.messaging.Group
*/
export interface Group {
  id: GroupId
  name: string
  unitId: DaycareId
  unitName: string
}

/**
* Generated from fi.espoo.evaka.messaging.Message
*/
export interface Message {
  attachments: MessageAttachment[]
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
  type: AccountType
}

/**
* Generated from fi.espoo.evaka.messaging.MessageAccountWithPresence
*/
export interface MessageAccountWithPresence {
  account: MessageAccount
  outOfOffice: FiniteDateRange | null
}

/**
* Generated from fi.espoo.evaka.messaging.MessageChild
*/
export interface MessageChild {
  childId: PersonId
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
  messageId: MessageId
  readAt: HelsinkiDateTime | null
  recipientAccountType: AccountType
  recipientId: MessageAccountId
  recipientName: string
  recipientNames: string[]
  senderAccountType: AccountType
  senderId: MessageAccountId
  senderName: string
  sensitive: boolean
  sentAt: HelsinkiDateTime
  threadId: MessageThreadId
  title: string
  type: MessageType
  urgent: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.MessageReceiversResponse
*/
export interface MessageReceiversResponse {
  accountId: MessageAccountId
  receivers: MessageReceiver[]
}

/**
* Generated from fi.espoo.evaka.messaging.MessageRecipient
*/
export interface MessageRecipient {
  id: string
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
  id: MessageThreadId
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
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.MyAccountResponse
*/
export interface MyAccountResponse {
  accountId: MessageAccountId
  messageAttachmentsAllowed: boolean
}

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
  attachmentIds: AttachmentId[]
  content: string
  draftId: MessageDraftId | null
  filters: PostMessageFilters | null
  recipientNames: string[]
  recipients: MessageRecipient[]
  relatedApplicationId: ApplicationId | null
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
* Generated from fi.espoo.evaka.messaging.ReplyToMessageBody
*/
export interface ReplyToMessageBody {
  content: string
  recipientAccountIds: MessageAccountId[]
}

/**
* Generated from fi.espoo.evaka.messaging.SentMessage
*/
export interface SentMessage {
  attachments: MessageAttachment[]
  content: string
  contentId: MessageContentId
  recipientNames: string[]
  sensitive: boolean
  sentAt: HelsinkiDateTime
  threadTitle: string
  type: MessageType
  urgent: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.MessageController.ThreadByApplicationResponse
*/
export interface ThreadByApplicationResponse {
  thread: MessageThread | null
}

/**
* Generated from fi.espoo.evaka.messaging.MessageService.ThreadReply
*/
export interface ThreadReply {
  message: Message
  threadId: MessageThreadId
}

/**
* Generated from fi.espoo.evaka.messaging.UnreadCountByAccount
*/
export interface UnreadCountByAccount {
  accountId: MessageAccountId
  unreadCopyCount: number
  unreadCount: number
}

/**
* Generated from fi.espoo.evaka.messaging.UnreadCountByAccountAndGroup
*/
export interface UnreadCountByAccountAndGroup {
  accountId: MessageAccountId
  groupId: GroupId
  unreadCopyCount: number
  unreadCount: number
}

/**
* Generated from fi.espoo.evaka.messaging.UpdatableDraftContent
*/
export interface UpdatableDraftContent {
  content: string
  recipientIds: string[]
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
    createdAt: HelsinkiDateTime.parseIso(json.createdAt)
  }
}


export function deserializeJsonGetReceiversResponse(json: JsonOf<GetReceiversResponse>): GetReceiversResponse {
  return {
    ...json,
    messageAccounts: json.messageAccounts.map(e => deserializeJsonMessageAccountWithPresence(e))
  }
}


export function deserializeJsonMessage(json: JsonOf<Message>): Message {
  return {
    ...json,
    readAt: (json.readAt != null) ? HelsinkiDateTime.parseIso(json.readAt) : null,
    sentAt: HelsinkiDateTime.parseIso(json.sentAt)
  }
}


export function deserializeJsonMessageAccountWithPresence(json: JsonOf<MessageAccountWithPresence>): MessageAccountWithPresence {
  return {
    ...json,
    outOfOffice: (json.outOfOffice != null) ? FiniteDateRange.parseJson(json.outOfOffice) : null
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


export function deserializeJsonThreadByApplicationResponse(json: JsonOf<ThreadByApplicationResponse>): ThreadByApplicationResponse {
  return {
    ...json,
    thread: (json.thread != null) ? deserializeJsonMessageThread(json.thread) : null
  }
}


export function deserializeJsonThreadReply(json: JsonOf<ThreadReply>): ThreadReply {
  return {
    ...json,
    message: deserializeJsonMessage(json.message)
  }
}
