// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import Footer, { footerHeightDesktop } from 'citizen-frontend/Footer'
import { UnwrapResult } from 'citizen-frontend/async-rendering'
import { combine } from 'lib-common/api'
import { useApiState } from 'lib-common/utils/useRestApi'
import Button from 'lib-components/atoms/buttons/Button'
import { desktopMin, tabletMin } from 'lib-components/breakpoints'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import Container from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { headerHeightDesktop } from '../header/const'
import { useTranslation } from '../localization'

import EmptyThreadView from './EmptyThreadView'
import MessageEditor from './MessageEditor'
import ThreadList from './ThreadList'
import ThreadView from './ThreadView'
import { getReceivers, sendMessage } from './api'
import { MessageContext } from './state'

const FullHeightContainer = styled(Container)`
  height: calc(100vh - ${headerHeightDesktop}px);
`

const StyledFlex = styled(AdaptiveFlex)`
  align-items: stretch;
  top: ${defaultMargins.s};
  right: 0;
  bottom: 0;
  left: 0;
  height: calc(100% - ${footerHeightDesktop});

  @media (max-width: ${desktopMin}) {
    height: auto;
  }
`

export default React.memo(function MessagesPage() {
  const { accountId, loadAccount, selectedThread, refreshThreads, threads } =
    useContext(MessageContext)
  useEffect(() => {
    if (!accountId.isSuccess) {
      loadAccount()
    }
  }, [accountId, loadAccount])
  const [editorVisible, setEditorVisible] = useState<boolean>(false)
  const [displaySendError, setDisplaySendError] = useState<boolean>(false)
  const t = useTranslation()
  const [receivers] = useApiState(
    () => getReceivers(t.messages.staffAnnotation),
    [t.messages.staffAnnotation]
  )

  return (
    <FullHeightContainer>
      <UnwrapResult result={combine(accountId, receivers)}>
        {([id, receivers]) => (
          <>
            <StyledFlex breakpoint={tabletMin} horizontalSpacing="L">
              <Gap size="s" />
              <Threads>
                <Gap size="s" />
                <HeaderContainer>
                  <H1 noMargin>{t.messages.inboxTitle}</H1>
                </HeaderContainer>
                <Gap size="xs" />
                <DottedLine />
                <Gap size="s" />
                <HeaderContainer>
                  <Button
                    text={t.messages.messageEditor.newMessage}
                    onClick={() => setEditorVisible(true)}
                    primary
                    data-qa="new-message-btn"
                    disabled={editorVisible}
                  />
                </HeaderContainer>
                <Gap size="s" />
                {editorVisible && (
                  <MessageEditor
                    receiverOptions={receivers}
                    onSend={(message) =>
                      sendMessage(message).then((result) => {
                        if (result.isSuccess) {
                          refreshThreads()
                        } else {
                          setDisplaySendError(true)
                        }
                      })
                    }
                    onClose={() => setEditorVisible(false)}
                    displaySendError={displaySendError}
                  />
                )}
                <ThreadList accountId={id} />
              </Threads>
              {selectedThread ? (
                <ThreadView accountId={id} thread={selectedThread} />
              ) : (
                <EmptyThreadView inboxEmpty={threads.length == 0} />
              )}
            </StyledFlex>
            <Footer />
          </>
        )}
      </UnwrapResult>
    </FullHeightContainer>
  )
})

const Threads = styled.div`
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
  }

  &.desktop-only {
    @media (max-width: ${tabletMin}) {
      display: none;
    }
  }
`

export const HeaderContainer = styled.div`
  padding-left: 5%;
`

export const DottedLine = styled.hr`
  width: 100%;
  border: 1px dashed ${colors.grayscale.g35};
  border-top-width: 0px;
`
