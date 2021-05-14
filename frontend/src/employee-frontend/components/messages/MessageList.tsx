// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useTranslation } from 'employee-frontend/state/i18n'
import { Loading, Paged, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Pagination from 'lib-components/Pagination'
import { H1, H2 } from 'lib-components/typography'
import React, { useCallback, useEffect, useState } from 'react'
import styled from 'styled-components'
import { ContentArea } from '../../../lib-components/layout/Container'
import { getReceivedMessages } from './api'
import { ReceivedMessages } from './ReceivedMessages'
import { SingleThreadView } from './SingleThreadView'
import { MessageAccount, MessageThread } from './types'

const PAGE_SIZE = 20

const MessagesContainer = styled(ContentArea)`
  overflow: hidden;
`

interface Props {
  account: MessageAccount
  view: 'RECEIVED' | 'SENT'
}

export default React.memo(function MessagesList({ account, view }: Props) {
  const { i18n } = useTranslation()

  const [page, setPage] = useState<number>(1)
  const [pages, setPages] = useState<number>()
  const [receivedMessages, setReceivedMessages] = useState<
    Result<MessageThread[]>
  >(Loading.of())
  const [selectedThread, setSelectedThread] = useState<MessageThread>()

  const setMessagesResult = useCallback(
    (result: Result<Paged<MessageThread>>) => {
      setReceivedMessages(result.map((r) => r.data))
      if (result.isSuccess) {
        setPages(result.value.pages)
      }
    },
    []
  )

  const loadReceivedMessages = useRestApi(
    getReceivedMessages,
    setMessagesResult
  )

  useEffect(() => {
    setSelectedThread(undefined)
    switch (view) {
      case 'RECEIVED':
        loadReceivedMessages(account.id, page, PAGE_SIZE)
        break
      case 'SENT':
        setReceivedMessages(Loading.of())
    }
  }, [account.id, view, page, loadReceivedMessages])

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
      {view === 'RECEIVED' ? (
        <ReceivedMessages
          messages={receivedMessages}
          onViewThread={setSelectedThread}
        />
      ) : (
        <div>TODO sent messages</div>
      )}
      <Pagination
        pages={pages}
        currentPage={page}
        setPage={setPage}
        label={i18n.common.page}
      />
    </MessagesContainer>
  )
})
