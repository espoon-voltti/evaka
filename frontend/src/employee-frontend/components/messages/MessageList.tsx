// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ContentArea } from 'lib-components/layout/Container'
import Pagination from 'lib-components/Pagination'
import { H1, H2 } from 'lib-components/typography'
import React, { useCallback, useContext, useEffect } from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { markThreadRead } from './api'
import { MessageDrafts } from './MessageDrafts'
import { MessagesPageContext } from './MessagesPageContext'
import { ReceivedMessages } from './ReceivedMessages'
import { SentMessages } from './SentMessages'
import { SingleThreadView } from './SingleThreadView'
import { MessageThread } from './types'
import { AccountView } from './types-view'

const MessagesContainer = styled(ContentArea)`
  overflow: hidden;
  flex-grow: 1;
`

export default React.memo(function MessagesList({
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
    setSelectedThread,
    refreshMessages
  } = useContext(MessagesPageContext)

  useEffect(() => {
    setSelectedThread(undefined)
  }, [account.id, setSelectedThread, view])

  const onSelectThread = useCallback(
    (thread: MessageThread) => {
      setSelectedThread(thread)

      const hasUnreadMessages = thread.messages.some((m) => !m.readAt)
      if (hasUnreadMessages) {
        void markThreadRead(account.id, thread.id).then(() =>
          refreshMessages(account.id)
        )
      }
    },
    [account.id, refreshMessages, setSelectedThread]
  )

  if (selectedThread) {
    return (
      <SingleThreadView
        goBack={() => setSelectedThread(undefined)}
        thread={selectedThread}
      />
    )
  }

  return (
    <MessagesContainer opaque>
      <H1>{i18n.messages.messageList.titles[view]}</H1>
      {!account.personal && <H2>{account.name}</H2>}
      {view === 'RECEIVED' && (
        <ReceivedMessages
          messages={receivedMessages}
          onSelectThread={onSelectThread}
        />
      )}
      {view === 'SENT' && <SentMessages messages={sentMessages} />}
      {view === 'DRAFTS' && <MessageDrafts drafts={messageDrafts} />}
      <Pagination
        pages={pages}
        currentPage={page}
        setPage={setPage}
        label={i18n.common.page}
      />
    </MessagesContainer>
  )
})
