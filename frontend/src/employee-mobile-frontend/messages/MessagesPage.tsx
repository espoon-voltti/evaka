// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { MessageThread } from 'lib-common/generated/api-types/messaging'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Button from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import { Tabs } from 'lib-components/molecules/Tabs'
import { defaultMargins } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employeeMobile'
import { faPlus } from 'lib-icons'

import { renderResult } from '../async-rendering'
import BottomNavbar, { bottomNavBarHeight } from '../common/BottomNavbar'
import { PageWithNavigation } from '../common/PageWithNavigation'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'
import { UnitContext } from '../common/unit'

import DraftMessagesList from './DraftMessagesList'
import MessageEditor from './MessageEditor'
import ReceivedThreadsList from './ReceivedThreadsList'
import SentMessagesList from './SentMessagesList'
import ThreadView from './ThreadView'
import { recipientsQuery } from './queries'
import { MessageContext } from './state'

type Tab = 'received' | 'sent' | 'drafts'

type UiState =
  | { type: 'threadList' }
  | { type: 'thread'; thread: MessageThread }
  | { type: 'newMessage' }

export default function MessagesPage() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const { unitId } = useNonNullableParams<{
    unitId: UUID
  }>()

  const { unitInfoResponse } = useContext(UnitContext)
  const { groupRoute } = useSelectedGroup()

  const { groupAccounts, selectedAccount, markThreadAsRead } =
    useContext(MessageContext)

  const recipients = useQueryResult(recipientsQuery, {
    enabled: selectedAccount !== undefined
  })

  const threadListTabs = useMemo(
    () => [
      {
        id: 'received',
        onClick: () => setActiveTab('received'),
        label: i18n.messages.tabs.received
      },
      {
        id: 'sent',
        onClick: () => setActiveTab('sent'),
        label: i18n.messages.tabs.sent
      },
      {
        id: 'drafts',
        onClick: () => setActiveTab('drafts'),
        label: i18n.messages.tabs.drafts
      }
    ],
    [i18n]
  )
  const [activeTab, setActiveTab] = useState<Tab>('received')
  const [uiState, setUiState] = useState<UiState>({ type: 'threadList' })

  const selectThread = useCallback(
    (thread: MessageThread | undefined) => {
      if (thread === undefined) {
        setUiState({ type: 'threadList' })
      } else {
        setUiState({ type: 'thread', thread })
        markThreadAsRead(thread.id)
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
    ? (() => {
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
                  <Tabs mobile active={activeTab} tabs={threadListTabs} />
                  {activeTab === 'received' ? (
                    <ReceivedThreadsList onSelectThread={selectThread} />
                  ) : activeTab === 'sent' ? (
                    <SentMessagesList />
                  ) : (
                    <DraftMessagesList />
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
            return (
              <ContentArea
                opaque={false}
                fullHeight
                paddingHorizontal="zero"
                paddingVertical="zero"
                data-qa="messages-page-content-area"
              >
                <ThreadView
                  thread={uiState.thread}
                  onBack={onBack}
                  accountId={selectedAccount.account.id}
                />
              </ContentArea>
            )
          }
          case 'newMessage':
            return renderResult(recipients, (availableRecipients) => (
              <MessageEditor
                availableRecipients={availableRecipients}
                account={selectedAccount.account}
                onClose={() => setUiState({ type: 'threadList' })}
              />
            ))
        }
      })()
    : renderResult(unitInfoResponse, (unit) => (
        <ContentArea
          opaque
          paddingVertical="zero"
          paddingHorizontal="zero"
          data-qa="messages-page-content-area"
        >
          <TopBar title={unit.name} />
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
