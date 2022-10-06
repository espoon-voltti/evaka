// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useMemo } from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import {
  MessageAccount,
  MessageThread
} from 'lib-common/generated/api-types/messaging'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { formatPreferredName } from 'lib-common/names'
import Pagination from 'lib-components/Pagination'
import EmptyMessageFolder from 'lib-components/employee/messages/EmptyMessageFolder'
import { ContentArea } from 'lib-components/layout/Container'
import { H1, H2 } from 'lib-components/typography'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'

import { MessageContext } from './MessageContext'
import { SingleThreadView } from './SingleThreadView'
import { ThreadList, ThreadListItem } from './ThreadList'
import { View } from './types-view'

const MessagesContainer = styled(ContentArea)`
  overflow-y: auto;
  flex: 1;
`

const getUniqueParticipants = (t: MessageThread): string[] => {
  const childStr =
    t.children.length > 0
      ? ` (${t.children
          .map((ch) => `${ch.lastName} ${formatPreferredName(ch)}`)
          .join(', ')})`
      : ''
  return Object.values(
    t.messages.reduce((acc, msg) => {
      acc[msg.sender.id] = msg.sender.name + childStr
      msg.recipients.forEach((rec) => (acc[rec.id] = rec.name))
      return acc
    }, {} as Record<string, string>)
  )
}

export interface Props {
  account: MessageAccount
  view: View
}

export default React.memo(function ThreadListContainer({
  account,
  view
}: Props) {
  const { i18n } = useTranslation()
  const {
    receivedMessages,
    sentMessages,
    messageDrafts,
    messageCopies,
    page,
    setPage,
    pages,
    selectedThread,
    selectThread,
    setSelectedDraft
  } = useContext(MessageContext)

  useEffect(
    function deselectThreadWhenViewChanges() {
      selectThread(undefined)
    },
    [account.id, selectThread, view]
  )

  const hasMessages = useMemo<boolean>(() => {
    if (view === 'RECEIVED' && receivedMessages.isSuccess) {
      return receivedMessages.value.length > 0
    } else if (view === 'SENT' && sentMessages.isSuccess) {
      return sentMessages.value.length > 0
    } else if (view === 'DRAFTS' && messageDrafts.isSuccess) {
      return messageDrafts.value.length > 0
    } else if (view === 'COPIES' && messageCopies.isSuccess) {
      return messageCopies.value.length > 0
    } else {
      return false
    }
  }, [view, receivedMessages, sentMessages, messageDrafts, messageCopies])

  if (selectedThread) {
    return (
      <SingleThreadView
        goBack={() => selectThread(undefined)}
        thread={selectedThread}
        accountId={account.id}
        view={view}
      />
    )
  }

  const threadToListItem = (
    thread: MessageThread,
    displayMessageCount: boolean,
    dataQa: string
  ): ThreadListItem => ({
    id: thread.id,
    title: thread.title,
    content: thread.messages[thread.messages.length - 1].content,
    urgent: thread.urgent,
    participants:
      view === 'SENT'
        ? thread.messages[0].recipientNames || getUniqueParticipants(thread)
        : getUniqueParticipants(thread),
    unread: thread.messages.some((m) => !m.readAt && m.sender.id != account.id),
    onClick: () => selectThread(thread),
    type: thread.type,
    timestamp: thread.messages[thread.messages.length - 1].sentAt,
    messageCount: displayMessageCount ? thread.messages.length : undefined,
    dataQa: dataQa
  })

  // TODO: Sent messages should probably be threads. Non trivial due to thread-splitting.
  const sentMessagesAsThreads: Result<MessageThread[]> = sentMessages.map(
    (value) =>
      value.map((message) => ({
        id: message.contentId,
        type: message.type,
        title: message.threadTitle,
        urgent: message.urgent,
        participants: message.recipientNames,
        children: [],
        messages: [
          {
            id: message.contentId,
            threadId: message.contentId,
            sender: { ...account },
            sentAt: message.sentAt,
            recipients: message.recipients,
            readAt: HelsinkiDateTime.now(),
            content: message.content,
            attachments: message.attachments,
            recipientNames: message.recipientNames
          }
        ]
      }))
  )

  const messageCopiesAsThreads: Result<MessageThread[]> = messageCopies.map(
    (value) =>
      value.map((message) => ({
        ...message,
        id: message.threadId,
        participants: [message.recipientName],
        children: [],
        messages: [
          {
            id: message.messageId,
            threadId: message.threadId,
            sender: {
              id: message.senderId,
              name: message.senderName,
              type: message.senderAccountType
            },
            sentAt: message.sentAt,
            recipients: [
              {
                id: message.recipientId,
                name: message.recipientName,
                type: message.recipientAccountType
              }
            ],
            readAt: message.readAt,
            content: message.content,
            attachments: message.attachments,
            recipientNames: message.recipientNames
          }
        ]
      }))
  )

  const receivedMessageItems: Result<ThreadListItem[]> = receivedMessages.map(
    (value) =>
      value.map((t) => threadToListItem(t, true, 'received-message-row'))
  )
  const sentMessageItems: Result<ThreadListItem[]> = sentMessagesAsThreads.map(
    (value) => value.map((t) => threadToListItem(t, false, 'sent-message-row'))
  )
  const draftMessageItems: Result<ThreadListItem[]> = messageDrafts.map(
    (value) =>
      value.map((draft) => ({
        id: draft.id,
        title: draft.title,
        content: draft.content,
        urgent: draft.urgent,
        participants: draft.recipientNames,
        unread: false,
        onClick: () => setSelectedDraft(draft),
        type: draft.type,
        timestamp: draft.created,
        messageCount: undefined,
        dataQa: 'draft-message-row'
      }))
  )
  const messageCopyItems = messageCopiesAsThreads.map((value) =>
    value.map((t) => threadToListItem(t, true, 'message-copy-row'))
  )

  const threadListItems: Result<ThreadListItem[]> = {
    RECEIVED: receivedMessageItems,
    SENT: sentMessageItems,
    DRAFTS: draftMessageItems,
    COPIES: messageCopyItems
  }[view]

  return hasMessages ? (
    <MessagesContainer opaque>
      <H1>{i18n.messages.messageList.titles[view]}</H1>
      {account.type !== 'PERSONAL' && <H2>{account.name}</H2>}
      <ThreadList items={threadListItems} />
      <Pagination
        pages={pages}
        currentPage={page}
        setPage={setPage}
        label={i18n.common.page}
      />
    </MessagesContainer>
  ) : (
    <EmptyMessageFolder
      loading={
        (view === 'RECEIVED' && receivedMessages.isLoading) ||
        (view === 'SENT' && sentMessages.isLoading) ||
        (view === 'DRAFTS' && messageDrafts.isLoading) ||
        (view === 'COPIES' && messageCopies.isLoading)
      }
      iconColor={colors.grayscale.g35}
      text={i18n.messages.emptyInbox}
    />
  )
})
