// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Button from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import EmptyMessageFolder from 'lib-components/messages/EmptyMessageFolder'
import { H1 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employeeMobile'
import { faPlus } from 'lib-icons'

import { renderResult } from '../async-rendering'
import BottomNavbar, { bottomNavBarHeight } from '../common/BottomNavbar'
import { PageWithNavigation } from '../common/PageWithNavigation'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'
import { UnitContext } from '../common/unit'

import MessageEditor from './MessageEditor'
import { MessagePreview } from './MessagePreview'
import ThreadView from './ThreadView'
import { recipientsQuery } from './queries'
import { MessageContext } from './state'

type UiState =
  | { type: 'threadList' }
  | { type: 'thread'; threadId: UUID }
  | { type: 'newMessage' }

export default function MessagesPage() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const { unitId } = useNonNullableParams<{
    unitId: UUID
  }>()

  const { unitInfoResponse } = useContext(UnitContext)
  const { groupRoute } = useSelectedGroup()

  const { groupAccounts, receivedMessages, selectedAccount, markThreadAsRead } =
    useContext(MessageContext)

  const recipients = useQueryResult(recipientsQuery)

  const [uiState, setUiState] = useState<UiState>({ type: 'threadList' })
  const selectThread = useCallback(
    (threadId: UUID | undefined) => {
      if (threadId === undefined) {
        setUiState({ type: 'threadList' })
      } else {
        setUiState({ type: 'thread', threadId })
        markThreadAsRead(threadId)
      }
    },
    [markThreadAsRead]
  )

  const changeGroup = useCallback(
    (group: GroupInfo | undefined) => {
      if (group) navigate(`/units/${unitId}/groups/${group.id}/messages`)
    },
    [navigate, unitId]
  )
  const onBack = useCallback(() => selectThread(undefined), [selectThread])

  return selectedAccount
    ? renderResult(
        combine(receivedMessages, recipients),
        ([threads, availableRecipients], isReloading) => {
          switch (uiState.type) {
            case 'threadList':
              return (
                <PageWithNavigation
                  selected="messages"
                  selectedGroup={
                    selectedAccount?.daycareGroup
                      ? {
                          id: selectedAccount.daycareGroup.id,
                          name: selectedAccount.daycareGroup.name,
                          utilization: 0
                        }
                      : undefined
                  }
                  onChangeGroup={changeGroup}
                  allowedGroupIds={groupAccounts.flatMap(
                    (ga) => ga.daycareGroup?.id || []
                  )}
                  includeSelectAll={false}
                >
                  <ContentArea
                    opaque
                    paddingVertical="zero"
                    paddingHorizontal="zero"
                    data-qa="messages-page-content-area"
                  >
                    <HeaderContainer>
                      <H1 noMargin={true}>{i18n.messages.title}</H1>
                    </HeaderContainer>
                    {threads.length > 0 ? (
                      threads.map((thread) => (
                        <MessagePreview
                          thread={thread}
                          hasUnreadMessages={thread.messages.some(
                            (item) =>
                              !item.readAt &&
                              item.sender.id !== selectedAccount?.account.id &&
                              !groupAccounts.some(
                                (ga) => ga.account.id === item.sender.id
                              )
                          )}
                          onClick={() => {
                            selectThread(thread.id)
                          }}
                          key={thread.id}
                        />
                      ))
                    ) : (
                      <EmptyMessageFolder
                        loading={isReloading}
                        iconColor={colors.grayscale.g35}
                        text={i18n.messages.emptyInbox}
                      />
                    )}
                    {featureFlags.employeeMobileGroupMessages && (
                      <HoverButton
                        primary
                        onClick={() => setUiState({ type: 'newMessage' })}
                        data-qa="new-message-btn"
                      >
                        <FontAwesomeIcon icon={faPlus} />
                        {i18n.messages.newMessage}
                      </HoverButton>
                    )}
                  </ContentArea>
                </PageWithNavigation>
              )
            case 'thread': {
              const selectedThread = threads.find(
                (t) => t.id === uiState.threadId
              )
              if (!selectedThread) {
                // Data for selected thread not found, should not happen
                setUiState({ type: 'threadList' })
                return null
              }
              return (
                <ContentArea
                  opaque={false}
                  fullHeight
                  paddingHorizontal="zero"
                  paddingVertical="zero"
                  data-qa="messages-page-content-area"
                >
                  <ThreadView
                    thread={selectedThread}
                    onBack={onBack}
                    accountId={selectedAccount.account.id}
                  />
                </ContentArea>
              )
            }
            case 'newMessage':
              return (
                <MessageEditor
                  availableRecipients={availableRecipients}
                  account={selectedAccount.account}
                  onClose={() => setUiState({ type: 'threadList' })}
                />
              )
          }
        }
      )
    : renderResult(unitInfoResponse, (unit) => (
        <ContentArea
          opaque
          paddingVertical="zero"
          paddingHorizontal="zero"
          data-qa="messages-page-content-area"
        >
          <TopBar title={unit.name} />
          <HeaderContainer>
            <H1 noMargin={true}>{i18n.messages.title}</H1>
          </HeaderContainer>
          {groupAccounts.length === 0 ? (
            <NoAccounts data-qa="info-no-account-access">
              {i18n.messages.noAccountAccess}
            </NoAccounts>
          ) : (
            <Navigate to={`${groupRoute}/messages/unread`} replace={true} />
          )}
          <BottomNavbar selected="messages" />
        </ContentArea>
      ))
}

export const HeaderContainer = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
`

const NoAccounts = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
`

const HoverButton = styled(Button)`
  position: fixed;
  bottom: calc(${defaultMargins.s} + ${bottomNavBarHeight}px);
  right: ${defaultMargins.s};
  border-radius: 40px;
  box-shadow: 0 4px 4px 0 ${(p) => p.theme.colors.grayscale.g15};
`
