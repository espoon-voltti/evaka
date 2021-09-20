// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EmptyMessageFolder from './EmptyMessageFolder'
import { ContentArea } from 'lib-components/layout/Container'
import Pagination from 'lib-components/Pagination'
import { H1, H2 } from 'lib-components/typography'
import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { MessageContext } from './MessageContext'
import { ThreadList, ThreadListItem } from './ThreadList'
import { SingleThreadView } from './SingleThreadView'
import { AccountView } from './types-view'
import { Result } from 'lib-common/api'
import { MessageThread } from 'lib-common/api-types/messaging/message'

const MessagesContainer = styled(ContentArea)`
  overflow-y: auto;
  flex: 1;
`

const getUniqueParticipants: (t: MessageThread) => string[] = (
  t: MessageThread
) =>
  Object.values(
    t.messages.reduce((acc, msg) => {
      acc[msg.sender.id] = msg.sender.name
      msg.recipients.forEach((rec) => (acc[rec.id] = rec.name))
      return acc
    }, {})
  )

export default React.memo(function ThreadListContainer({
  account,
  view
}: AccountView) {
  const { i18n } = useTranslation()
  const {
    receivedMessages,
    sentMessages,
    messageDrafts,
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

  const [messageCount, setMessageCount] = useState<number>(0)

  useEffect(() => {
    if (view === 'RECEIVED' && receivedMessages.isSuccess) {
      setMessageCount(receivedMessages.value.length)
    } else if (view === 'SENT' && sentMessages.isSuccess) {
      setMessageCount(sentMessages.value.length)
    } else if (view === 'DRAFTS' && messageDrafts.isSuccess) {
      setMessageCount(messageDrafts.value.length)
    } else {
      setMessageCount(0)
    }
  }, [
    view,
    messageCount,
    setMessageCount,
    receivedMessages,
    sentMessages,
    messageDrafts
  ])

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
  ) => ({
    id: thread.id,
    title: thread.title,
    content: thread.messages[thread.messages.length - 1].content,
    participants: getUniqueParticipants(thread),
    unread: thread.messages.some((m) => !m.readAt && m.sender.id != account.id),
    onClick: () => selectThread(thread),
    type: thread.type,
    timestamp: thread.messages[thread.messages.length - 1].sentAt,
    messageCount: displayMessageCount ? thread.messages.length : undefined,
    dataQa: dataQa
  })

  // TODO: Sent messages should probably be threads. Non trivial due to thread-splitting.
  const sentMessagesAsThreads = sentMessages.map((value) =>
    value.map((message) => ({
      id: message.contentId,
      type: message.type,
      title: message.threadTitle,
      messages: [
        {
          id: message.contentId,
          sender: { ...account },
          sentAt: message.sentAt,
          recipients: message.recipients,
          readAt: new Date(),
          content: message.content
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
        participants: draft.recipientNames,
        unread: false,
        onClick: () => setSelectedDraft(draft),
        type: draft.type,
        timestamp: draft.created,
        messageCount: undefined,
        dataQa: 'draft-message-row'
      }))
  )

  const threadListItems: Result<ThreadListItem[]> = {
    RECEIVED: receivedMessageItems,
    RECEIVERS: receivedMessageItems,
    SENT: sentMessageItems,
    DRAFTS: draftMessageItems
  }[view]

  return messageCount > 0 ? (
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
        (view === 'DRAFTS' && messageDrafts.isLoading)
      }
    />
  )
})
