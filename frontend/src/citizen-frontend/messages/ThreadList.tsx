// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import useIntersectionObserver from 'lib-common/utils/useIntersectionObserver'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { tabletMin } from 'lib-components/breakpoints'
import { H1 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowLeft } from 'lib-icons'
import React, { useContext } from 'react'
import styled from 'styled-components'
import { UUID } from '../../lib-common/types'
import { useTranslation } from '../localization'
import { MessageContext } from './state'
import ThreadListItem from './ThreadListItem'
import { MessageAccount, MessageThread } from './types'

const hasUnreadMessages = (thread: MessageThread, accountId: UUID) =>
  thread.messages.some((m) => !m.readAt && m.senderId !== accountId)

interface Props {
  account: MessageAccount
}

export default React.memo(function ThreadList({ account }: Props) {
  const t = useTranslation()
  const {
    selectedThread,
    selectThread,
    threads,
    threadLoadingResult,
    loadMoreThreads
  } = useContext(MessageContext)

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
      <Container className={selectedThread ? 'desktop-only' : undefined}>
        <HeaderContainer>
          <H1 noMargin>{t.messages.inboxTitle}</H1>
          {threadLoadingResult.isSuccess && threads.length === 0 && (
            <span>{t.messages.noMessages}</span>
          )}
        </HeaderContainer>

        {threads.map((thread) => (
          <ThreadListItem
            key={thread.id}
            thread={thread}
            onClick={() => selectThread(thread)}
            active={selectedThread?.id === thread.id}
            hasUnreadMessages={hasUnreadMessages(thread, account.id)}
          />
        ))}
        {threadLoadingResult.mapAll({
          failure() {
            return <ErrorSegment title={t.common.errors.genericGetError} />
          },
          loading() {
            return <SpinnerSegment />
          },
          success() {
            return <OnEnterView onEnter={loadMoreThreads} />
          }
        })}
      </Container>
    </>
  )
})

const MobileOnly = styled.div`
  display: none;

  @media (max-width: ${tabletMin}) {
    display: block;
  }
`

const Return = styled(IconButton)`
  margin-left: ${defaultMargins.xs};
`

const Container = styled.div`
  min-width: 35%;
  max-width: 400px;
  min-height: 500px;
  background-color: ${colors.greyscale.white};
  overflow-y: auto;

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

const HeaderContainer = styled.div`
  padding: ${defaultMargins.m};
`

const OnEnterView = React.memo(function IsInView({
  onEnter
}: {
  onEnter: () => void
}) {
  const ref = useIntersectionObserver<HTMLDivElement>(onEnter)
  return <div ref={ref} />
})
