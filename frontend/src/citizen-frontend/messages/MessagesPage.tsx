// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import Footer, { footerHeightDesktop } from 'citizen-frontend/Footer'
import { renderResult } from 'citizen-frontend/async-rendering'
import { useUser } from 'citizen-frontend/auth/state'
import { useTranslation } from 'citizen-frontend/localization'
import { focusElementAfterDelay } from 'citizen-frontend/utils/focus'
import { combine } from 'lib-common/api'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { NotificationsContext } from 'lib-components/Notifications'
import Main from 'lib-components/atoms/Main'
import { desktopMin, tabletMin } from 'lib-components/breakpoints'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import Container from 'lib-components/layout/Container'
import { TabletAndDesktop } from 'lib-components/layout/responsive-layout'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { childrenQuery } from '../children/queries'

import EmptyThreadView from './EmptyThreadView'
import MessageEditor from './MessageEditor'
import RedactedThreadView from './RedactedThreadView'
import ThreadList from './ThreadList'
import ThreadView from './ThreadView'
import { receiversQuery, sendMessageMutation } from './queries'
import { isRegularThread, MessageContext } from './state'

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
  const i18n = useTranslation()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { messageAccount, selectedThread, setSelectedThread } =
    useContext(MessageContext)
  const { addTimedNotification } = useContext(NotificationsContext)
  const [editorVisible, setEditorVisible] = useState<boolean>(false)
  const [displaySendError, setDisplaySendError] = useState<boolean>(false)

  const children = useQueryResult(childrenQuery())
  const receivers = useQueryResult(receiversQuery())

  const user = useUser()

  useEffect(() => {
    setEditorVisible(searchParams.get('editorVisible') === 'true')
  }, [setEditorVisible, searchParams])

  const changeEditorVisibility = useCallback(
    (setEditorVisible: boolean) => {
      if (!setEditorVisible) {
        navigate('/messages')
      } else {
        navigate(`/messages?editorVisible=true`)
      }
    },
    [navigate]
  )

  const params = useParams<{ threadId: UUID | undefined }>()
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

  const onSelectedThreadDeleted = useCallback(() => {
    changeEditorVisibility(false)
  }, [changeEditorVisibility])

  const canSendNewMessage =
    !editorVisible && !!user?.accessibleFeatures.composeNewMessage

  const { mutateAsync: sendMessage } = useMutationResult(sendMessageMutation)

  return (
    <Container>
      {renderResult(messageAccount, (messageAccount) => (
        <>
          <Main>
            <TabletAndDesktop>
              <Gap size="L" />
            </TabletAndDesktop>
            <StyledFlex breakpoint={tabletMin} horizontalSpacing="L">
              <ThreadList
                accountId={messageAccount.accountId}
                selectThread={selectThread}
                setEditorVisible={changeEditorVisibility}
                newMessageButtonEnabled={canSendNewMessage}
              />
              {selectedThread ? (
                isRegularThread(selectedThread) ? (
                  <ThreadView
                    accountId={messageAccount.accountId}
                    closeThread={() => selectThread(undefined)}
                    thread={selectedThread}
                    onThreadDeleted={() => onSelectedThreadDeleted()}
                  />
                ) : (
                  <RedactedThreadView
                    thread={selectedThread}
                    closeThread={() => selectThread(undefined)}
                  />
                )
              ) : (
                <EmptyThreadView />
              )}
            </StyledFlex>
            {editorVisible &&
              renderResult(
                combine(children, receivers),
                ([children, receiverOptions]) => (
                  <MessageEditor
                    children_={children}
                    receiverOptions={receiverOptions}
                    messageAttachmentsAllowed={
                      messageAccount.messageAttachmentsAllowed
                    }
                    onSend={(body) => sendMessage({ body })}
                    onSuccess={() => {
                      changeEditorVisibility(false)
                      addTimedNotification({
                        children:
                          i18n.messages.messageEditor.messageSentNotification,
                        dataQa: 'message-sent-notification'
                      })
                      focusElementAfterDelay('new-message-btn')
                    }}
                    onFailure={() => setDisplaySendError(true)}
                    onClose={() => {
                      changeEditorVisibility(false)
                      focusElementAfterDelay('new-message-btn')
                    }}
                    displaySendError={displaySendError}
                  />
                )
              )}
          </Main>
          <Footer />
        </>
      ))}
    </Container>
  )
})
