// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ContentArea } from 'lib-components/layout/Container'
import Pagination from 'lib-components/Pagination'
import { H1, H2 } from 'lib-components/typography'
import React, { useContext, useEffect } from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { MessageContext } from './MessageContext'
import { MessageDrafts } from './MessageDrafts'
import { ReceivedMessages } from './ReceivedMessages'
import { SentMessages } from './SentMessages'
import { SingleThreadView } from './SingleThreadView'
import { AccountView } from './types-view'

const MessagesContainer = styled(ContentArea)`
  overflow-y: auto;
  flex: 1;
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
    selectThread
  } = useContext(MessageContext)

  useEffect(
    function deselectThreadWhenViewChanges() {
      selectThread(undefined)
    },
    [account.id, selectThread, view]
  )

  if (selectedThread) {
    return (
      <SingleThreadView
        goBack={() => selectThread(undefined)}
        thread={selectedThread}
        accountId={account.id}
      />
    )
  }

  return (
    <MessagesContainer opaque>
      <H1>{i18n.messages.messageList.titles[view]}</H1>
      {account.type !== 'PERSONAL' && <H2>{account.name}</H2>}
      {view === 'RECEIVED' && (
        <ReceivedMessages
          accountId={account.id}
          messages={receivedMessages}
          onSelectThread={selectThread}
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
