// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  getReceivers,
  sendMessage,
  SendMessageParams
} from 'citizen-frontend/messages/api'
import MessageEditor from 'citizen-frontend/messages/MessageEditor'
import { Loading, Result } from 'lib-common/api'
import { MessageAccount } from 'lib-common/api-types/messaging/message'
import { useRestApi } from 'lib-common/utils/useRestApi'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { tabletMin } from 'lib-components/breakpoints'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import Container from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'
import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { headerHeight } from '../header/const'
import { MessageContext } from './state'
import ThreadList from './ThreadList'
import ThreadView from './ThreadView'

const FullHeightContainer = styled(Container)`
  height: calc(100vh - ${headerHeight});
`

const StyledFlex = styled(AdaptiveFlex)`
  align-items: stretch;
  position: absolute;
  top: ${defaultMargins.s};
  right: 0;
  bottom: 0;
  left: 0;
`

export default React.memo(function MessagesPage() {
  const { accountId, loadAccount, selectedThread, refreshThreads } =
    useContext(MessageContext)
  useEffect(() => {
    if (!accountId.isSuccess) {
      loadAccount()
    }
  }, [accountId, loadAccount])
  const [editorVisible, setEditorVisible] = useState<boolean>(false)
  const [displaySendError, setDisplaySendError] = useState<boolean>(false)
  const [receivers, setReceivers] = useState<Result<MessageAccount[]>>(
    Loading.of()
  )
  const loadReceivers = useRestApi(getReceivers, setReceivers)

  useEffect(() => {
    loadReceivers()
  }, [loadReceivers])

  return (
    <FullHeightContainer>
      <StyledFlex breakpoint={tabletMin} horizontalSpacing="L">
        {accountId.mapAll({
          failure() {
            return <ErrorSegment />
          },
          loading() {
            return <SpinnerSegment />
          },
          success(id) {
            return (
              <>
                <ThreadList
                  accountId={id}
                  setEditorVisible={setEditorVisible}
                  newMessageButtonEnabled={receivers.isSuccess}
                />
                {selectedThread && (
                  <ThreadView accountId={id} thread={selectedThread} />
                )}
              </>
            )
          }
        })}
      </StyledFlex>
      {editorVisible && receivers.isSuccess && (
        <MessageEditor
          receiverOptions={receivers.value}
          onSend={(message: SendMessageParams) =>
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
    </FullHeightContainer>
  )
})
