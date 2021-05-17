// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Paged, Result, Success } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { tabletMin } from 'lib-components/breakpoints'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import Container from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { getReceivedMessages, markThreadRead } from './api'
import ThreadReadView from './ThreadReadView'
import ThreadList from './ThreadList'
import { MessageThread } from './types'
import { HeaderContext, HeaderState } from './state'

export default React.memo(function MessagesPage() {
  const { refreshUnreadMessagesCount } = useContext<HeaderState>(HeaderContext)
  const [messagesState, setMessagesState] = useState<MessagesState>(
    initialState
  )
  const [activeMessage, setActiveThread] = useState<MessageThread>()
  const setMessagesResult = useCallback(
    (result: Result<Paged<MessageThread>>) =>
      setMessagesState((state) => {
        if (result.isSuccess) {
          return {
            ...state,
            threads: [...state.threads, ...result.value.data],
            nextPage: Success.of(undefined),
            total: result.value.total,
            pages: result.value.pages
          }
        }

        if (result.isFailure) {
          return {
            ...state,
            nextPage: result.map(() => undefined)
          }
        }

        return state
      }),
    []
  )

  const loadMessages = useRestApi(getReceivedMessages, setMessagesResult)
  useEffect(() => {
    setMessagesState((state) => ({ ...state, nextPage: Loading.of() }))
    loadMessages(messagesState.currentPage)
  }, [loadMessages, messagesState.currentPage])

  const loadNextPage = () =>
    setMessagesState((state) => {
      if (state.currentPage < state.pages) {
        return {
          ...state,
          currentPage: state.currentPage + 1
        }
      }
      return state
    })

  const openThread = ({ id, messages }: MessageThread) => {
    const hasUnreadMessages = messages.some((m) => !m.readAt)

    if (hasUnreadMessages) {
      setMessagesState(({ threads, ...state }) => ({
        ...state,
        threads: threads.map((t) =>
          t.id === id
            ? {
                ...t,
                messages: t.messages.map((m) => ({
                  ...m,
                  readAt: m.readAt || new Date()
                }))
              }
            : t
        )
      }))
    }

    setActiveThread(messagesState.threads.find((t) => t.id === id))

    if (hasUnreadMessages) {
      void markThreadRead(id).then(() => {
        refreshUnreadMessagesCount()
      })
    }
  }

  return (
    <Container>
      <Gap size="s" />
      <StyledFlex breakpoint={tabletMin} horizontalSpacing="L">
        <ThreadList
          threads={messagesState.threads}
          nextPage={messagesState.nextPage}
          activeThread={activeMessage}
          onClickThread={openThread}
          onReturn={() => setActiveThread(undefined)}
          loadNextPage={loadNextPage}
        />
        {activeMessage && <ThreadReadView thread={activeMessage} />}
      </StyledFlex>
    </Container>
  )
})

interface MessagesState {
  threads: MessageThread[]
  nextPage: Result<void>
  currentPage: number
  pages: number
  total?: number
}

const initialState: MessagesState = {
  threads: [],
  nextPage: Loading.of(),
  currentPage: 1,
  pages: 0
}

const StyledFlex = styled(AdaptiveFlex)`
  align-items: stretch;
`
