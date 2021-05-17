// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import useIntersectionObserver from 'lib-common/utils/useIntersectionObserver'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { tabletMin } from 'lib-components/breakpoints'
import { H1 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowLeft } from 'lib-icons'
import React from 'react'
import styled from 'styled-components'
import { useTranslation } from '../localization'
import ThreadListItem from './ThreadListItem'
import { MessageThread } from './types'

type Props = {
  threads: MessageThread[]
  nextPage: Result<void>
  activeThread: MessageThread | undefined
  onClickThread: (target: MessageThread) => void
  onReturn: () => void
  loadNextPage: () => void
}

export default React.memo(function ThreadList({
  threads,
  nextPage,
  activeThread,
  onClickThread,
  onReturn,
  loadNextPage
}: Props) {
  const t = useTranslation()

  return (
    <>
      {activeThread && (
        <MobileOnly>
          <Return
            icon={faArrowLeft}
            onClick={onReturn}
            altText={t.common.return}
          />
        </MobileOnly>
      )}
      <Container className={activeThread ? 'desktop-only' : undefined}>
        <HeaderContainer>
          <H1 noMargin>{t.messages.inboxTitle}</H1>
          {nextPage.isSuccess && threads.length === 0 && (
            <span>{t.messages.noMessages}</span>
          )}
        </HeaderContainer>

        {threads.map((thread) => (
          <ThreadListItem
            key={thread.id}
            thread={thread}
            onClick={() => onClickThread(thread)}
            active={activeThread?.id === thread.id}
          />
        ))}
        {nextPage.isFailure && (
          <ErrorSegment title={t.common.errors.genericGetError} />
        )}
        {nextPage.isLoading && <SpinnerSegment />}
        {nextPage.isSuccess && <OnEnterView onEnter={loadNextPage} />}
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
