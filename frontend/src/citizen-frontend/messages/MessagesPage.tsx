// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import Footer, { footerHeightDesktop } from 'citizen-frontend/Footer'
import { UnwrapResult } from 'citizen-frontend/async-rendering'
import { combine } from 'lib-common/api'
import { useApiState } from 'lib-common/utils/useRestApi'
import { desktopMin, tabletMin } from 'lib-components/breakpoints'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import Container from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'

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
              <ThreadList
                accountId={id}
                setEditorVisible={setEditorVisible}
                newMessageButtonEnabled={!editorVisible}
              />
              {selectedThread ? (
                <ThreadView accountId={id} thread={selectedThread} />
              ) : (
                <EmptyThreadView inboxEmpty={threads.length == 0} />
              )}
            </StyledFlex>
            {editorVisible && (
              <MessageEditor
                receiverOptions={receivers}
                onSend={(message) => sendMessage(message)}
                onSuccess={() => {
                  refreshThreads()
                  setEditorVisible(false)
                }}
                onFailure={() => setDisplaySendError(true)}
                onClose={() => setEditorVisible(false)}
                displaySendError={displaySendError}
              />
            )}
            <Footer />
          </>
        )}
      </UnwrapResult>
    </FullHeightContainer>
  )
})
