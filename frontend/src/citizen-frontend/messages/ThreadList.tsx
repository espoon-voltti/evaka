// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { renderResult } from 'citizen-frontend/async-rendering'
import { MessageThread } from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import useIntersectionObserver from 'lib-common/utils/useIntersectionObserver'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { tabletMin } from 'lib-components/breakpoints'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowLeft } from 'lib-icons'

import { useTranslation } from '../localization'
import { OverlayContext } from '../overlay/state'

import ThreadListItem from './ThreadListItem'
import { MessageContext } from './state'

const hasUnreadMessages = (thread: MessageThread, accountId: UUID) =>
  thread.messages.some((m) => !m.readAt && m.sender.id !== accountId)

interface Props {
  accountId: UUID
}

export default React.memo(function ThreadList({ accountId }: Props) {
  const navigate = useNavigate()
  const params = useParams<{ threadId: UUID | undefined }>()
  const t = useTranslation()
  const {
    selectedThread,
    setSelectedThread,
    threads,
    threadLoadingResult,
    loadMoreThreads
  } = useContext(MessageContext)

  useEffect(() => {
    setSelectedThread(params.threadId)
  }, [setSelectedThread, params.threadId])

  const selectThread = useCallback(
    (threadId: UUID | undefined) => {
      if (!threadId) {
        navigate('/messages')
      } else {
        navigate(`/messages/${threadId}`)
      }
    },
    [navigate]
  )

  const { setErrorMessage } = useContext(OverlayContext)
  const onAttachmentUnavailable = useCallback(
    () =>
      setErrorMessage({
        title: t.fileDownload.modalHeader,
        text: t.fileDownload.modalMessage,
        type: 'error'
      }),
    [t, setErrorMessage]
  )

  return (
    <>
      {selectedThread && (
        <MobileOnly>
          <Return
            icon={faArrowLeft}
            onClick={() => selectThread(undefined)}
            altText={t.common.return}
          />
        </MobileOnly>
      )}

      {threadLoadingResult.isSuccess && threads.length === 0 && (
        <>
          <SolidLine />
          <ThreadListContainer>
            <Gap size="s" />
            <span style={{ color: `${colors.grayscale.g70}` }}>
              {t.messages.noMessagesInfo}
            </span>
          </ThreadListContainer>
        </>
      )}

      {threads.map((thread) => (
        <ThreadListItem
          key={thread.id}
          thread={thread}
          onClick={() => selectThread(thread.id)}
          active={selectedThread?.id === thread.id}
          hasUnreadMessages={hasUnreadMessages(thread, accountId)}
          onAttachmentUnavailable={onAttachmentUnavailable}
        />
      ))}
      {renderResult(threadLoadingResult, () => (
        <OnEnterView onEnter={loadMoreThreads} />
      ))}
    </>
  )
})

const MobileOnly = styled.div`
  display: none;

  @media (max-width: ${tabletMin}) {
    display: block;
  }
`

const SolidLine = styled.hr`
  width: 100%;
  border: 1px solid ${colors.grayscale.g15};
  border-top-width: 0px;
`

const Return = styled(IconButton)`
  margin-left: ${defaultMargins.xs};
`

const ThreadListContainer = styled.div`
  padding-left: 5%;
`

const OnEnterView = React.memo(function IsInView({
  onEnter
}: {
  onEnter: () => void
}) {
  const ref = useIntersectionObserver<HTMLDivElement>(onEnter)
  return <div ref={ref} />
})
