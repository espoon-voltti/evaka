// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ApplicationId } from './shared'
import type { ApplicationStatus } from './application'
import type { AreaId } from './shared'
import type { Attachment } from './attachment'
import type { AttachmentId } from './shared'
import type { DaycareId } from './shared'
import FiniteDateRange from '../../finite-date-range'
import type { GroupId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { MessageAccountId } from './shared'
import type { MessageContentId } from './shared'
import type { MessageDraftId } from './shared'
import type { MessageId } from './shared'
import type { MessageThreadFolderId } from './shared'
import type { MessageThreadId } from './shared'
import type { MessagingCategory } from './placement'
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
  childId: PersonId | null
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
    applicationStatus: ApplicationStatus | null
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
  attachments: Attachment[]
  content: string
  createdAt: HelsinkiDateTime
  id: MessageDraftId
  recipientNames: string[]
  recipients: DraftRecipient[]
  sensitive: boolean
  title: string
  type: MessageType
  urgent: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.DraftRecipient
*/
export interface DraftRecipient {
  accountId: string
  starter: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.GetRecipientsResponse
*/
export interface GetRecipientsResponse {
  childrenToMessageAccounts: ChildMessageAccountAccess[]
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
  attachments: Attachment[]
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


export namespace MessageRecipient {
  /**
  * Generated from fi.espoo.evaka.messaging.MessageRecipient.Area
  */
  export interface Area {
    type: 'AREA'
    id: AreaId
  }

  /**
  * Generated from fi.espoo.evaka.messaging.MessageRecipient.Child
  */
  export interface Child {
    type: 'CHILD'
    id: PersonId
    starter: boolean
  }

  /**
  * Generated from fi.espoo.evaka.messaging.MessageRecipient.Citizen
  */
  export interface Citizen {
    type: 'CITIZEN'
    id: PersonId
  }

  /**
  * Generated from fi.espoo.evaka.messaging.MessageRecipient.Group
  */
  export interface Group {
    type: 'GROUP'
    id: GroupId
    starter: boolean
  }

  /**
  * Generated from fi.espoo.evaka.messaging.MessageRecipient.Unit
  */
  export interface Unit {
    type: 'UNIT'
    id: DaycareId
    starter: boolean
  }
}

/**
* Generated from fi.espoo.evaka.messaging.MessageRecipient
*/
export type MessageRecipient = MessageRecipient.Area | MessageRecipient.Child | MessageRecipient.Citizen | MessageRecipient.Group | MessageRecipient.Unit


/**
* Generated from fi.espoo.evaka.messaging.MessageRecipientType
*/
export type MessageRecipientType =
  | 'AREA'
  | 'UNIT'
  | 'UNIT_IN_AREA'
  | 'GROUP'
  | 'CHILD'
  | 'CITIZEN'

/**
* Generated from fi.espoo.evaka.messaging.MessageThread
*/
export interface MessageThread {
  applicationStatus: ApplicationStatus | null
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
* Generated from fi.espoo.evaka.messaging.MessageController.MessageThreadFolder
*/
export interface MessageThreadFolder {
  id: MessageThreadFolderId
  name: string
  ownerId: MessageAccountId
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
  placementTypes: MessagingCategory[]
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


export namespace SelectableRecipient {
  /**
  * Generated from fi.espoo.evaka.messaging.SelectableRecipient.Area
  */
  export interface Area {
    type: 'AREA'
    id: AreaId
    name: string
    receivers: SelectableRecipient.UnitInArea[]
  }

  /**
  * Generated from fi.espoo.evaka.messaging.SelectableRecipient.Child
  */
  export interface Child {
    type: 'CHILD'
    id: PersonId
    name: string
    startDate: LocalDate | null
  }

  /**
  * Generated from fi.espoo.evaka.messaging.SelectableRecipient.Citizen
  */
  export interface Citizen {
    type: 'CITIZEN'
    id: PersonId
    name: string
  }

  /**
  * Generated from fi.espoo.evaka.messaging.SelectableRecipient.Group
  */
  export interface Group {
    type: 'GROUP'
    hasStarters: boolean
    id: GroupId
    name: string
    receivers: SelectableRecipient.Child[]
  }

  /**
  * Generated from fi.espoo.evaka.messaging.SelectableRecipient.Unit
  */
  export interface Unit {
    type: 'UNIT'
    hasStarters: boolean
    id: DaycareId
    name: string
    receivers: SelectableRecipient.Group[]
  }

  /**
  * Generated from fi.espoo.evaka.messaging.SelectableRecipient.UnitInArea
  */
  export interface UnitInArea {
    type: 'UNIT_IN_AREA'
    id: DaycareId
    name: string
  }
}

/**
* Generated from fi.espoo.evaka.messaging.SelectableRecipient
*/
export type SelectableRecipient = SelectableRecipient.Area | SelectableRecipient.Child | SelectableRecipient.Citizen | SelectableRecipient.Group | SelectableRecipient.Unit | SelectableRecipient.UnitInArea


/**
* Generated from fi.espoo.evaka.messaging.SelectableRecipientsResponse
*/
export interface SelectableRecipientsResponse {
  accountId: MessageAccountId
  receivers: SelectableRecipient[]
}

/**
* Generated from fi.espoo.evaka.messaging.SentMessage
*/
export interface SentMessage {
  attachments: Attachment[]
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
  totalUnreadCount: number
  unreadCopyCount: number
  unreadCount: number
  unreadCountByFolder: Partial<Record<MessageThreadFolderId, number>>
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
  recipientNames: string[]
  recipients: DraftRecipient[]
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


export function deserializeJsonGetRecipientsResponse(json: JsonOf<GetRecipientsResponse>): GetRecipientsResponse {
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



export function deserializeJsonSelectableRecipientChild(json: JsonOf<SelectableRecipient.Child>): SelectableRecipient.Child {
  return {
    ...json,
    startDate: (json.startDate != null) ? LocalDate.parseIso(json.startDate) : null
  }
}

export function deserializeJsonSelectableRecipientGroup(json: JsonOf<SelectableRecipient.Group>): SelectableRecipient.Group {
  return {
    ...json,
    receivers: json.receivers.map(e => deserializeJsonSelectableRecipientChild(e))
  }
}

export function deserializeJsonSelectableRecipientUnit(json: JsonOf<SelectableRecipient.Unit>): SelectableRecipient.Unit {
  return {
    ...json,
    receivers: json.receivers.map(e => deserializeJsonSelectableRecipientGroup(e))
  }
}
export function deserializeJsonSelectableRecipient(json: JsonOf<SelectableRecipient>): SelectableRecipient {
  switch (json.type) {
    case 'CHILD': return deserializeJsonSelectableRecipientChild(json)
    case 'GROUP': return deserializeJsonSelectableRecipientGroup(json)
    case 'UNIT': return deserializeJsonSelectableRecipientUnit(json)
    default: return json
  }
}


export function deserializeJsonSelectableRecipientsResponse(json: JsonOf<SelectableRecipientsResponse>): SelectableRecipientsResponse {
  return {
    ...json,
    receivers: json.receivers.map(e => deserializeJsonSelectableRecipient(e))
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
