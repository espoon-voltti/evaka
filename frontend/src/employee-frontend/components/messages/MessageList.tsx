// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useTranslation } from 'employee-frontend/state/i18n'
import { Loading, Paged, Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { tabletMin } from 'lib-components/breakpoints'
import colors from 'lib-components/colors'
import Pagination from 'lib-components/Pagination'
import { H1 } from 'lib-components/typography'
import React, { useCallback, useEffect, useState } from 'react'
import styled from 'styled-components'
import { getReceivedMessages } from './api'
import { ReceivedMessages } from './ReceivedMessages'
import { MessageThread } from './types'

const PAGE_SIZE = 20

interface Props {
  accountId: UUID
  view: 'RECEIVED' | 'SENT'
}

export default React.memo(function MessagesList({ accountId, view }: Props) {
  const { i18n } = useTranslation()

  const [page, setPage] = useState<number>(1)
  const [pages, setPages] = useState<number>()
  const [receivedMessages, setReceivedMessages] = useState<
    Result<MessageThread[]>
  >(Loading.of())

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
    switch (view) {
      case 'RECEIVED':
        loadReceivedMessages(accountId, page, PAGE_SIZE)
        break
      case 'SENT':
        setReceivedMessages(Loading.of())
    }
  }, [accountId, view, page, loadReceivedMessages])

  return (
    <Container>
      <H1>{i18n.messages.messageList.titles[view]}</H1>
      {view === 'RECEIVED' ? (
        <ReceivedMessages messages={receivedMessages} />
      ) : (
        <div>TODO</div>
      )}
      <Pagination
        pages={pages}
        currentPage={page}
        setPage={setPage}
        label={i18n.common.page}
      />
    </Container>
  )
})

const Container = styled.div`
  min-width: 35%;
  max-width: 400px;
  min-height: 500px;
  background-color: ${colors.greyscale.white};

  @media (max-width: 750px) {
    min-width: 50%;
  }

  @media (max-width: ${tabletMin}) {
    width: 100%;
    max-width: 100%;
  }

  &.desktop-only {
    @media (max-width: ${tabletMin}) {
      display: none;
    }
  }
`
