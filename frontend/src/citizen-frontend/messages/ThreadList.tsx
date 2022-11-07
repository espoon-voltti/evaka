// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { MessageThread } from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import useIntersectionObserver from 'lib-common/utils/useIntersectionObserver'
import Button from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { tabletMin } from 'lib-components/breakpoints'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { TabletAndDesktop } from 'lib-components/layout/responsive-layout'
import { H1 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faPlus } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'
import { mobileBottomNavHeight } from '../navigation/const'

import ThreadListItem from './ThreadListItem'
import { MessageContext } from './state'

const hasUnreadMessages = (thread: MessageThread, accountId: UUID) =>
  thread.messages.some((m) => !m.readAt && m.sender.id !== accountId)

interface Props {
  accountId: UUID
  setEditorVisible: (value: boolean) => void
  newMessageButtonEnabled: boolean
}

export default React.memo(function ThreadList({
  accountId,
  setEditorVisible,
  newMessageButtonEnabled
}: Props) {
  const navigate = useNavigate()
  const params = useParams<{ threadId: UUID | undefined }>()
  const t = useTranslation()
  const {
    selectedThread,
    setSelectedThread,
    threads,
    threadLoadingResult,
    loadMoreThreads,
    refreshThreads,
    refreshUnreadMessagesCount
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

  const refreshEverything = useCallback(() => {
    refreshThreads()
    refreshUnreadMessagesCount()
  }, [refreshThreads, refreshUnreadMessagesCount])

  return (
    <>
      {selectedThread && (
        <MobileOnly>
          <ReturnButton
            label={t.common.return}
            onClick={() => selectThread(undefined)}
            margin={defaultMargins.s}
          />
        </MobileOnly>
      )}
      <Container className={selectedThread ? 'desktop-only' : undefined}>
        <Gap size="s" sizeOnMobile="m" />
        <HeaderContainer>
          <H1 noMargin>{t.messages.inboxTitle}</H1>
        </HeaderContainer>
        <Gap size="xs" sizeOnMobile="m" />
        <TabletAndDesktop>
          <DottedLine />
          <Gap size="s" />
          <HeaderContainer>
            <Button
              text={t.messages.messageEditor.newMessage}
              onClick={() => setEditorVisible(true)}
              primary
              data-qa="new-message-btn"
              disabled={!newMessageButtonEnabled}
            />
          </HeaderContainer>
          <Gap size="s" />
        </TabletAndDesktop>
        <MobileOnly>
          <FloatingButton
            onClick={() => setEditorVisible(true)}
            primary
            data-qa="new-message-btn"
            disabled={!newMessageButtonEnabled}
          >
            <FixedSpaceRow spacing="xs" alignItems="center">
              <FontAwesomeIcon icon={faPlus} />
              <div>{t.messages.messageEditor.newMessage}</div>
            </FixedSpaceRow>
          </FloatingButton>
        </MobileOnly>

        {threadLoadingResult.isSuccess && threads.length === 0 && (
          <>
            <SolidLine />
            <ThreadListContainer>
              <Gap size="s" />
              <NoMessagesInfo>{t.messages.noMessagesInfo}</NoMessagesInfo>
              <Gap size="XXL" />
            </ThreadListContainer>
          </>
        )}

        <ThreadListItems>
          {threads.map((thread) => (
            <li key={thread.id}>
              <ThreadListItem
                thread={thread}
                onClick={() => selectThread(thread.id)}
                onDeleted={refreshEverything}
                active={selectedThread?.id === thread.id}
                hasUnreadMessages={hasUnreadMessages(thread, accountId)}
              />
            </li>
          ))}
        </ThreadListItems>
        {renderResult(threadLoadingResult, () => (
          <OnEnterView onEnter={loadMoreThreads} />
        ))}
      </Container>
    </>
  )
})

const MobileOnly = styled.div`
  display: none;

  @media (max-width: ${tabletMin}) {
    display: block;
    margin: 0;
  }
`

const DottedLine = styled.hr`
  width: 100%;
  border: 1px dashed ${colors.grayscale.g35};
  border-top-width: 0px;
`

const SolidLine = styled.hr`
  width: 100%;
  border: 1px solid ${colors.grayscale.g15};
  border-top-width: 0px;
`

const NoMessagesInfo = styled.div`
  color: ${colors.grayscale.g70};
`

const Container = styled.div`
  min-width: 35%;
  max-width: 400px;
  min-height: 500px;
  background-color: ${colors.grayscale.g0};
  overflow-y: auto;

  @media (max-width: 750px) {
    min-width: 50%;
  }

  @media (max-width: ${tabletMin}) {
    width: 100%;
    max-width: 100%;
    min-height: auto;
  }

  &.desktop-only {
    @media (max-width: ${tabletMin}) {
      display: none;
    }
  }
`

const HeaderContainer = styled.div`
  padding-left: 5%;
`

const ThreadListContainer = styled.div`
  padding-left: 5%;
`

const ThreadListItems = styled.ul`
  list-style: none;
  padding: 0;
  margin: 0;
`

const FloatingButton = styled(Button)`
  position: fixed;
  bottom: calc(${defaultMargins.s} + ${mobileBottomNavHeight}px);
  right: ${defaultMargins.s};
  border-radius: 40px;
  z-index: 10;
`

const OnEnterView = React.memo(function IsInView({
  onEnter
}: {
  onEnter: () => void
}) {
  const ref = useIntersectionObserver<HTMLDivElement>(onEnter)
  return <div ref={ref} />
})
