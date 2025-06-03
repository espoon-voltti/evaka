// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import { Failure, Loading, Success, wrapResult } from 'lib-common/api'
import type {
  MessageAccount,
  MessageThread,
  MessageThreadFolder
} from 'lib-common/generated/api-types/messaging'
import type {
  MessageAccountId,
  MessageThreadId
} from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'
import { formatPersonName } from 'lib-common/names'
import Pagination from 'lib-components/Pagination'
import Select from 'lib-components/atoms/dropdowns/Select'
import { ContentArea } from 'lib-components/layout/Container'
import EmptyMessageFolder from 'lib-components/messages/EmptyMessageFolder'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H1, H2 } from 'lib-components/typography'
import colors from 'lib-customizations/common'

import { moveThreadToFolder } from '../../generated/api-clients/messaging'
import { useTranslation } from '../../state/i18n'

import { MessageContext } from './MessageContext'
import { SingleThreadView } from './SingleThreadView'
import type { ThreadListItem } from './ThreadList'
import { ThreadList } from './ThreadList'
import type { View } from './types-view'
import { isFolderView, isStandardView } from './types-view'

const moveThreadToFolderResult = wrapResult(moveThreadToFolder)

const MessagesContainer = styled(ContentArea)`
  overflow-y: auto;
  flex: 1;
`

const getUniqueParticipants = (t: MessageThread): string[] => {
  const childStr =
    t.children.length > 0
      ? ` (${t.children
          .map((ch) => formatPersonName(ch, 'Last Preferred'))
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
    folders,
    receivedMessages,
    sentMessages,
    messageDrafts,
    messageCopies,
    archivedMessages,
    messagesInFolder,
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

  const [folderChangeTarget, setFolderChangeTarget] =
    useState<MessageThreadId | null>(null)

  const onMessageMoved = useCallback(() => {
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
    } else if (isFolderView(view) && messagesInFolder.isSuccess) {
      return messagesInFolder.value.length > 0
    } else {
      return false
    }
  }, [
    view,
    receivedMessages,
    sentMessages,
    messageDrafts,
    messageCopies,
    archivedMessages,
    messagesInFolder
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
        view === 'sent' || view === 'copies'
          ? thread.messages[0].recipientNames || getUniqueParticipants(thread)
          : getUniqueParticipants(thread),
      unread: thread.messages.some(
        (m) => !m.readAt && m.sender.id !== account.id
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
          id: fromUuid<MessageThreadId>(draft.id), // Hack
          title: draft.title,
          content: draft.content,
          urgent: draft.urgent,
          sensitive: draft.sensitive,
          participants: draft.recipientNames,
          unread: false,
          onClick: () => setSelectedDraft(draft),
          type: draft.type,
          timestamp: draft.createdAt,
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
  const messageFolderItems = useMemo(
    () =>
      messagesInFolder.map((value) =>
        value.map((t) => threadToListItem(t, true, 'folder-message-row'))
      ),
    [messagesInFolder, threadToListItem]
  )

  const threadListItems: Result<ThreadListItem[]> = {
    received: receivedMessageItems,
    sent: sentMessageItems,
    drafts: draftMessageItems,
    copies: messageCopyItems,
    archive: messageArchivedItems,
    folder: messageFolderItems,
    thread: selectedThread
      ? Success.of([
          threadToListItem(selectedThread, true, 'selected-thread-row')
        ])
      : Loading.of<ThreadListItem[]>()
  }[isStandardView(view) ? view : 'folder']

  const hasFolders = folders.isSuccess && folders.value.length > 0

  return (
    <>
      {folderChangeTarget && folders.isSuccess && (
        <FolderChangeModal
          accountId={account.id}
          threadId={folderChangeTarget}
          folders={folders.value}
          onSuccess={onMessageMoved}
          onClose={() => setFolderChangeTarget(null)}
        />
      )}

      {selectedThread ? (
        <SingleThreadView
          goBack={() => selectThread(undefined)}
          thread={selectedThread}
          accountId={account.id}
          view={view}
          onArchived={
            view === 'received' || isFolderView(view)
              ? onMessageMoved
              : undefined
          }
        />
      ) : hasMessages ? (
        <MessagesContainer opaque>
          <H1>
            {isStandardView(view)
              ? i18n.messages.messageList.titles[view]
              : view.name}
          </H1>
          {account.type !== 'PERSONAL' && <H2>{account.name}</H2>}
          <ThreadList
            items={threadListItems}
            accountId={account.id}
            onArchived={
              view === 'received' || isFolderView(view)
                ? onMessageMoved
                : undefined
            }
            onChangeFolder={
              hasFolders && (view === 'received' || isFolderView(view))
                ? (id) => setFolderChangeTarget(id)
                : undefined
            }
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
      )}
    </>
  )
})

const FolderChangeModal = React.memo(function FolderChangeModal({
  accountId,
  threadId,
  folders,
  onSuccess,
  onClose
}: {
  accountId: MessageAccountId
  threadId: MessageThreadId
  folders: MessageThreadFolder[]
  onSuccess: () => void
  onClose: () => void
}) {
  const { i18n } = useTranslation()
  const [folder, setFolder] = useState<MessageThreadFolder | null>(null)
  return (
    <AsyncFormModal
      data-qa="change-folder-modal"
      title={i18n.messages.changeFolder.modalTitle}
      resolveAction={() =>
        folder
          ? moveThreadToFolderResult({
              accountId: accountId,
              threadId: threadId,
              folderId: folder.id
            })
          : Promise.resolve(Failure.of({ message: 'No folder selected' }))
      }
      resolveLabel={i18n.messages.changeFolder.modalOk}
      onSuccess={() => {
        onSuccess()
        onClose()
      }}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!folder}
    >
      <Select
        items={folders}
        selectedItem={folder}
        onChange={(value) => setFolder(value)}
        getItemLabel={(item) => item.name}
        getItemValue={(item) => item.id}
        placeholder={i18n.common.select}
        data-qa="folder-select"
      />
    </AsyncFormModal>
  )
})
