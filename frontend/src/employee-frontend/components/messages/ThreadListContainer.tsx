// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo } from 'react'
import styled from 'styled-components'

import { Loading, Result, Success } from 'lib-common/api'
import {
  MessageAccount,
  MessageThread
} from 'lib-common/generated/api-types/messaging'
import { formatPreferredName } from 'lib-common/names'
import Pagination from 'lib-components/Pagination'
import { ContentArea } from 'lib-components/layout/Container'
import EmptyMessageFolder from 'lib-components/messages/EmptyMessageFolder'
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
    t.messages.reduce(
      (acc, msg) => {
        acc[msg.sender.id] = msg.sender.name + childStr
        msg.recipients.forEach((rec) => (acc[rec.id] = rec.name))
        return acc
      },
      {} as Record<string, string>
    )
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
    archivedMessages,
    page,
    setPage,
    pages,
    refreshMessages,
    selectedThread,
    selectThread,
    setSelectedDraft,
    sentMessagesAsThreads,
    messageCopiesAsThreads
  } = useContext(MessageContext)

  const onArchive = useCallback(() => {
    selectThread(undefined)
    refreshMessages()
  }, [refreshMessages, selectThread])

  const hasMessages = useMemo<boolean>(() => {
    if (view === 'received' && receivedMessages.isSuccess) {
      return receivedMessages.value.length > 0
    } else if (view === 'sent' && sentMessages.isSuccess) {
      return sentMessages.value.length > 0
    } else if (view === 'drafts' && messageDrafts.isSuccess) {
      return messageDrafts.value.length > 0
    } else if (view === 'copies' && messageCopies.isSuccess) {
      return messageCopies.value.length > 0
    } else if (view === 'archive' && archivedMessages.isSuccess) {
      return archivedMessages.value.length > 0
    } else {
      return false
    }
  }, [
    view,
    receivedMessages,
    sentMessages,
    messageDrafts,
    messageCopies,
    archivedMessages
  ])

  const threadToListItem = useCallback(
    (
      thread: MessageThread,
      displayMessageCount: boolean,
      dataQa: string
    ): ThreadListItem => ({
      id: thread.id,
      title: thread.title,
      content: thread.messages[thread.messages.length - 1].content,
      urgent: thread.urgent,
      sensitive: thread.sensitive,
      participants:
        view === 'sent'
          ? thread.messages[0].recipientNames || getUniqueParticipants(thread)
          : getUniqueParticipants(thread),
      unread: thread.messages.some(
        (m) => !m.readAt && m.sender.id != account.id
      ),
      onClick: () => selectThread(thread),
      type: thread.type,
      timestamp: thread.messages[thread.messages.length - 1].sentAt,
      messageCount: displayMessageCount ? thread.messages.length : undefined,
      dataQa: dataQa
    }),
    [account.id, selectThread, view]
  )

  const receivedMessageItems: Result<ThreadListItem[]> = useMemo(
    () =>
      receivedMessages.map((value) =>
        value.map((t) => threadToListItem(t, true, 'received-message-row'))
      ),
    [receivedMessages, threadToListItem]
  )
  const sentMessageItems: Result<ThreadListItem[]> = useMemo(
    () =>
      sentMessagesAsThreads.map((value) =>
        value.map((t) => threadToListItem(t, false, 'sent-message-row'))
      ),
    [sentMessagesAsThreads, threadToListItem]
  )
  const draftMessageItems: Result<ThreadListItem[]> = useMemo(
    () =>
      messageDrafts.map((value) =>
        value.map((draft) => ({
          id: draft.id,
          title: draft.title,
          content: draft.content,
          urgent: draft.urgent,
          sensitive: draft.sensitive,
          participants: draft.recipientNames,
          unread: false,
          onClick: () => setSelectedDraft(draft),
          type: draft.type,
          timestamp: draft.created,
          messageCount: undefined,
          dataQa: 'draft-message-row'
        }))
      ),
    [messageDrafts, setSelectedDraft]
  )
  const messageCopyItems = useMemo(
    () =>
      messageCopiesAsThreads.map((value) =>
        value.map((t) => threadToListItem(t, true, 'message-copy-row'))
      ),
    [messageCopiesAsThreads, threadToListItem]
  )
  const messageArchivedItems = useMemo(
    () =>
      archivedMessages.map((value) =>
        value.map((t) => threadToListItem(t, true, 'archived-message-row'))
      ),
    [archivedMessages, threadToListItem]
  )

  const threadListItems: Result<ThreadListItem[]> = {
    received: receivedMessageItems,
    sent: sentMessageItems,
    drafts: draftMessageItems,
    copies: messageCopyItems,
    archive: messageArchivedItems,
    thread: selectedThread
      ? Success.of([
          threadToListItem(selectedThread, true, 'selected-thread-row')
        ])
      : Loading.of<ThreadListItem[]>()
  }[view]

  if (selectedThread) {
    return (
      <SingleThreadView
        goBack={() => selectThread(undefined)}
        thread={selectedThread}
        accountId={account.id}
        view={view}
        onArchived={view === 'received' ? onArchive : undefined}
      />
    )
  }

  return hasMessages ? (
    <MessagesContainer opaque>
      <H1>{i18n.messages.messageList.titles[view]}</H1>
      {account.type !== 'PERSONAL' && <H2>{account.name}</H2>}
      <ThreadList
        items={threadListItems}
        accountId={account.id}
        onArchive={view === 'received' ? onArchive : undefined}
      />
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
        (view === 'received' && receivedMessages.isLoading) ||
        (view === 'sent' && sentMessages.isLoading) ||
        (view === 'drafts' && messageDrafts.isLoading) ||
        (view === 'copies' && messageCopies.isLoading)
      }
      iconColor={colors.grayscale.g35}
      text={i18n.messages.emptyInbox}
    />
  )
})
