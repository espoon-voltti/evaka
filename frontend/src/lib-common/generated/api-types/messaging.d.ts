// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import LocalDate from '../../local-date'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.messaging.message.AccountType
*/
export type AccountType = 
  | 'PERSONAL'
  | 'GROUP'
  | 'CITIZEN'

/**
* Generated from fi.espoo.evaka.messaging.message.CitizenMessageBody
*/
export interface CitizenMessageBody {
  content: string
  recipients: MessageAccount[]
  title: string
}

/**
* Generated from fi.espoo.evaka.messaging.daycarydailynote.DaycareDailyNote
*/
export interface DaycareDailyNote {
  childId: UUID | null
  date: LocalDate
  feedingNote: DaycareDailyNoteLevelInfo | null
  groupId: UUID | null
  id: UUID | null
  modifiedAt: Date | null
  modifiedBy: string | null
  note: string | null
  reminderNote: string | null
  reminders: DaycareDailyNoteReminder[]
  sleepingMinutes: number | null
  sleepingNote: DaycareDailyNoteLevelInfo | null
}

/**
* Generated from fi.espoo.evaka.messaging.daycarydailynote.DaycareDailyNoteLevelInfo
*/
export type DaycareDailyNoteLevelInfo = 
  | 'GOOD'
  | 'MEDIUM'
  | 'NONE'

/**
* Generated from fi.espoo.evaka.messaging.daycarydailynote.DaycareDailyNoteReminder
*/
export type DaycareDailyNoteReminder = 
  | 'DIAPERS'
  | 'CLOTHES'
  | 'LAUNDRY'

/**
* Generated from fi.espoo.evaka.messaging.message.ChildRecipientsController.EditRecipientRequest
*/
export interface EditRecipientRequest {
  blocklisted: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.message.Group
*/
export interface Group {
  id: UUID
  name: string
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.messaging.message.Message
*/
export interface Message {
  content: string
  id: UUID
  readAt: Date | null
  recipients: MessageAccount[]
  sender: MessageAccount
  sentAt: Date
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageAccount
*/
export interface MessageAccount {
  id: UUID
  name: string
  type: AccountType
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageReceiver
*/
export interface MessageReceiver {
  childDateOfBirth: LocalDate
  childFirstName: string
  childId: UUID
  childLastName: string
  receiverPersons: MessageReceiverPerson[]
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageReceiverPerson
*/
export interface MessageReceiverPerson {
  accountId: UUID
  receiverFirstName: string
  receiverLastName: string
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageReceiversResponse
*/
export interface MessageReceiversResponse {
  groupId: UUID
  groupName: string
  receivers: MessageReceiver[]
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageThread
*/
export interface MessageThread {
  id: UUID
  messages: Message[]
  title: string
  type: MessageType
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageType
*/
export type MessageType = 
  | 'MESSAGE'
  | 'BULLETIN'

/**
* Generated from fi.espoo.evaka.messaging.message.NestedMessageAccount
*/
export interface NestedMessageAccount {
  account: MessageAccount
  daycareGroup: Group | null
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageController.PostMessageBody
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
* Generated from fi.espoo.evaka.messaging.message.Recipient
*/
export interface Recipient {
  blocklisted: boolean
  firstName: string
  guardian: boolean
  headOfChild: boolean
  lastName: string
  personId: string
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageController.ReplyToMessageBody
*/
export interface ReplyToMessageBody {
  content: string
  recipientAccountIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageControllerCitizen.ReplyToMessageBody
*/
export interface ReplyToMessageBody {
  content: string
  recipientAccountIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.messaging.message.SentMessage
*/
export interface SentMessage {
  content: string
  contentId: UUID
  recipientNames: string[]
  recipients: MessageAccount[]
  sentAt: Date
  threadTitle: string
  type: MessageType
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageService.ThreadReply
*/
export interface ThreadReply {
  message: Message
  threadId: UUID
}

/**
* Generated from fi.espoo.evaka.messaging.message.UnreadCountByAccount
*/
export interface UnreadCountByAccount {
  accountId: UUID
  unreadCount: number
}

/**
* Generated from fi.espoo.evaka.messaging.message.UpsertableDraftContent
*/
export interface UpsertableDraftContent {
  content: string
  recipientIds: UUID[]
  recipientNames: string[]
  title: string
  type: MessageType
}
