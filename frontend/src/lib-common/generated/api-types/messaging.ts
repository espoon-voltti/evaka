// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import LocalDate from '../../local-date'
import { MessageAttachment } from './attachment'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.messaging.AccountType
*/
export type AccountType = 
  | 'PERSONAL'
  | 'GROUP'
  | 'CITIZEN'

/**
* Generated from fi.espoo.evaka.messaging.CitizenMessageBody
*/
export interface CitizenMessageBody {
  content: string
  recipients: MessageAccount[]
  title: string
}

/**
* Generated from fi.espoo.evaka.messaging.DraftContent
*/
export interface DraftContent {
  attachments: MessageAttachment[]
  content: string
  created: Date
  id: UUID
  recipientIds: UUID[]
  recipientNames: string[]
  title: string
  type: MessageType
}

/**
* Generated from fi.espoo.evaka.messaging.ChildRecipientsController.EditRecipientRequest
*/
export interface EditRecipientRequest {
  blocklisted: boolean
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
  readAt: Date | null
  recipientNames: string[] | null
  recipients: MessageAccount[]
  sender: MessageAccount
  sentAt: Date
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
* Generated from fi.espoo.evaka.messaging.MessageReceiver
*/
export interface MessageReceiver {
  childDateOfBirth: LocalDate
  childFirstName: string
  childId: UUID
  childLastName: string
  receiverPersons: MessageReceiverPerson[]
}

/**
* Generated from fi.espoo.evaka.messaging.MessageReceiverPerson
*/
export interface MessageReceiverPerson {
  accountId: UUID
  receiverFirstName: string
  receiverLastName: string
}

/**
* Generated from fi.espoo.evaka.messaging.MessageReceiversResponse
*/
export interface MessageReceiversResponse {
  groupId: UUID
  groupName: string
  receivers: MessageReceiver[]
}

/**
* Generated from fi.espoo.evaka.messaging.MessageThread
*/
export interface MessageThread {
  id: UUID
  messages: Message[]
  title: string
  type: MessageType
}

/**
* Generated from fi.espoo.evaka.messaging.MessageType
*/
export type MessageType = 
  | 'MESSAGE'
  | 'BULLETIN'

/**
* Generated from fi.espoo.evaka.messaging.NestedMessageAccount
*/
export interface NestedMessageAccount {
  account: MessageAccount
  daycareGroup: Group | null
}

/**
* Generated from fi.espoo.evaka.messaging.MessageController.PostMessageBody
*/
export interface PostMessageBody {
  attachmentIds: UUID[]
  content: string
  draftId: UUID | null
  recipientAccountIds: UUID[]
  recipientNames: string[]
  title: string
  type: MessageType
}

/**
* Generated from fi.espoo.evaka.messaging.Recipient
*/
export interface Recipient {
  blocklisted: boolean
  firstName: string
  lastName: string
  personId: string
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
  recipients: MessageAccount[]
  sentAt: Date
  threadTitle: string
  type: MessageType
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
  unreadCount: number
}

/**
* Generated from fi.espoo.evaka.messaging.UpsertableDraftContent
*/
export interface UpsertableDraftContent {
  content: string
  recipientIds: UUID[]
  recipientNames: string[]
  title: string
  type: MessageType
}
