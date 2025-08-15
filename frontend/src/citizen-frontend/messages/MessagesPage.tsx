// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState
} from 'react'
import styled from 'styled-components'
import { useLocation, useParams, useSearchParams } from 'wouter'

import { combine } from 'lib-common/api'
import type { MessageThreadId } from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { NotificationsContext } from 'lib-components/Notifications'
import { Notifications } from 'lib-components/Notifications'
import Main from 'lib-components/atoms/Main'
import { desktopMin, tabletMin } from 'lib-components/breakpoints'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import Container from 'lib-components/layout/Container'
import { TabletAndDesktop } from 'lib-components/layout/responsive-layout'
import { defaultMargins, Gap } from 'lib-components/white-space'

import Footer, { footerHeightDesktop } from '../Footer'
import { renderResult } from '../async-rendering'
import { useUser } from '../auth/state'
import { AuthContext } from '../auth/state'
import { childrenQuery } from '../children/queries'
import { useTranslation } from '../localization'
import useTitle from '../useTitle'
import { focusElementAfterDelay, focusElementOnNextFrame } from '../utils/focus'

import EmptyThreadView from './EmptyThreadView'
import MessageEditor from './MessageEditor'
import RedactedThreadView from './RedactedThreadView'
import ThreadList from './ThreadList'
import { messageThreadIdAttr } from './ThreadListItem'
import type { ThreadViewApi } from './ThreadView'
import ThreadView from './ThreadView'
import { recipientsQuery, sendMessageMutation } from './queries'
import { MessageContext } from './state'
import { isRegularThread } from './utils'

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
  useTitle(i18n, i18n.messages.inboxTitle)
  const [, navigate] = useLocation()
  const [searchParams] = useSearchParams()
  const { messageAccount, selectedThread, setSelectedThread } =
    useContext(MessageContext)
  const { addTimedNotification } = useContext(NotificationsContext)
  const [editorVisible, setEditorVisible] = useState<boolean>(false)
  const [displaySendError, setDisplaySendError] = useState<boolean>(false)

  const children = useQueryResult(childrenQuery())
  const recipients = useQueryResult(recipientsQuery())

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

  const params = useParams<{ threadId: MessageThreadId | undefined }>()
  useEffect(() => {
    setSelectedThread(params.threadId)
  }, [setSelectedThread, params.threadId])

  const threadView = useRef<ThreadViewApi>(null)

  const selectThread = useCallback(
    (threadId: MessageThreadId | undefined) => {
      if (!threadId) {
        navigate('/messages')
      } else {
        if (params.threadId !== threadId) {
          navigate(`/messages/${threadId}`)
        } else {
          threadView.current?.focusThreadTitle()
        }
      }
    },
    [navigate, params.threadId]
  )

  const onSelectedThreadDeleted = useCallback(() => {
    changeEditorVisibility(false)
  }, [changeEditorVisibility])

  const canSendNewMessage =
    !editorVisible && !!user?.accessibleFeatures.composeNewMessage

  const { mutateAsync: sendMessage } = useMutationResult(sendMessageMutation)
  const { apiVersion } = useContext(AuthContext)

  const closeThread = useCallback(() => {
    const selectedThreadId = selectedThread?.id
    selectThread(undefined)
    if (selectedThreadId !== undefined) {
      focusElementOnNextFrame(messageThreadIdAttr(selectedThreadId))
    }
  }, [selectThread, selectedThread?.id])

  return (
    <FullWidthContainer>
      <Notifications apiVersion={apiVersion} sticky topOffset={80} />
      <Container data-qa="message-container">
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
                  closeThread={closeThread}
                  setEditorVisible={changeEditorVisibility}
                  newMessageButtonEnabled={canSendNewMessage}
                />
                {selectedThread ? (
                  isRegularThread(selectedThread) ? (
                    <ThreadView
                      accountId={messageAccount.accountId}
                      closeThread={closeThread}
                      thread={selectedThread}
                      allowedAccounts={
                        recipients.getOrElse(null)?.childrenToMessageAccounts ??
                        []
                      }
                      accountDetails={
                        recipients.getOrElse(null)?.messageAccounts ?? []
                      }
                      onThreadDeleted={() => {
                        onSelectedThreadDeleted()
                        addTimedNotification({
                          children: i18n.messages.confirmDelete.success,
                          dataQa: 'thread-deleted-notification'
                        })
                      }}
                      ref={threadView}
                    />
                  ) : (
                    <RedactedThreadView
                      thread={selectedThread}
                      closeThread={closeThread}
                    />
                  )
                ) : (
                  <EmptyThreadView />
                )}
              </StyledFlex>
              {editorVisible &&
                renderResult(
                  combine(children, recipients),
                  ([children, recipientOptions]) => (
                    <MessageEditor
                      children_={children}
                      recipientOptions={recipientOptions}
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
    </FullWidthContainer>
  )
})

const FullWidthContainer = styled.div`
  width: 100%;
  margin: 0;
`
