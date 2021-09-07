// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { MessageThread } from 'lib-common/api-types/messaging/message'
import { UUID } from 'lib-common/types'
import useIntersectionObserver from 'lib-common/utils/useIntersectionObserver'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { tabletMin } from 'lib-components/breakpoints'
import { H1 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowLeft } from 'lib-icons'
import React, { useContext } from 'react'
import styled from 'styled-components'
import { useTranslation } from '../localization'
import { MessageContext } from './state'
import ThreadListItem from './ThreadListItem'

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
        <Gap size={'s'} />
        <HeaderContainer>
          <H1 noMargin>{t.messages.inboxTitle}</H1>
        </HeaderContainer>
        <Gap size={'xs'} />
        <DottedLine />
        <Gap size={'s'} />
        <HeaderContainer>
          <Button
            text={t.messages.messageEditor.newMessage}
            onClick={() => setEditorVisible(true)}
            primary
            data-qa="new-message-btn"
            disabled={!newMessageButtonEnabled}
          />
        </HeaderContainer>
        <Gap size={'s'} />

        {threadLoadingResult.isSuccess && threads.length === 0 && (
          <>
            <SolidLine />
            <ThreadListContainer>
              <Gap size={'s'} />
              <span style={{ color: `${colors.greyscale.dark}` }}>
                {t.messages.noMessagesInfo}
              </span>
            </ThreadListContainer>
          </>
        )}

        {threads.map((thread) => (
          <ThreadListItem
            key={thread.id}
            thread={thread}
            onClick={() => selectThread(thread)}
            active={selectedThread?.id === thread.id}
            hasUnreadMessages={hasUnreadMessages(thread, accountId)}
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

const DottedLine = styled.hr`
  width: 100%;
  border: 1px dashed ${colors.greyscale.medium};
  border-top-width: 0px;
`

const SolidLine = styled.hr`
  width: 100%;
  border: 1px solid ${colors.greyscale.lighter};
  border-top-width: 0px;
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
  padding-left: 5%;
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
