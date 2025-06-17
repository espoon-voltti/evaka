// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'
import { Redirect, useLocation } from 'wouter'

import { combine } from 'lib-common/api'
import type { GroupInfo } from 'lib-common/generated/api-types/attendance'
import type {
  DraftContent,
  SentMessage
} from 'lib-common/generated/api-types/messaging'
import { useQueryResult } from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'
import { Tabs } from 'lib-components/molecules/Tabs'
import { defaultMargins } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import BottomNavbar from '../common/BottomNavbar'
import { PageWithNavigation } from '../common/PageWithNavigation'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import type { UnitOrGroup } from '../common/unit-or-group'
import { toUnitOrGroup } from '../common/unit-or-group'
import { FloatingPrimaryActionButton } from '../pairing/components'
import { unitInfoQuery } from '../units/queries'

import DraftMessagesList from './DraftMessagesList'
import MessageEditor from './MessageEditor'
import ReceivedThreadsList from './ReceivedThreadsList'
import SentMessagesList from './SentMessagesList'
import { SentMessageView } from './ThreadView'
import { selectableRecipientsQuery } from './queries'
import { MessageContext } from './state'

type Tab = 'received' | 'sent' | 'drafts'

type UiState =
  | { type: 'list' }
  | { type: 'sentMessage'; message: SentMessage }
  | { type: 'continueDraft'; draft: DraftContent }

export default function MessagesPage({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()

  const unitId = unitOrGroup.unitId
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  const { groupAccounts, groupAccount } = useContext(MessageContext)

  const recipients = useQueryResult(selectableRecipientsQuery(), {
    enabled: unitOrGroup.type === 'group'
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
  const [uiState, setUiState] = useState<UiState>({ type: 'list' })

  useEffect(() => {
    setUiState({ type: 'list' })
  }, [unitOrGroup])

  const onSelectThread = (threadId: UUID) => {
    navigate(routes.receivedThread(unitOrGroup, threadId).value)
  }

  const onNewMessageClick = () => {
    navigate(routes.newMessage(unitOrGroup).value)
  }

  const selectSentMessage = useCallback(
    (message: SentMessage) => setUiState({ type: 'sentMessage', message }),
    []
  )

  const selectDraftMessage = useCallback(
    (draft: DraftContent) => setUiState({ type: 'continueDraft', draft }),
    []
  )

  const changeGroup = useCallback(
    (group: GroupInfo | undefined) => {
      if (group)
        navigate(routes.messages(toUnitOrGroup(unitId, group.id)).value)
    },
    [navigate, unitId]
  )
  const onBack = useCallback(() => setUiState({ type: 'list' }), [])
  if (unitOrGroup.type === 'unit') {
    return renderResult(
      combine(unitInfoResponse, groupAccounts),
      ([unit, groupAccounts]) => (
        <ContentArea
          opaque
          paddingVertical="zero"
          paddingHorizontal="zero"
          data-qa="messages-page-content-area"
        >
          <TopBar title={unit.name} unitId={unitId} />
          {groupAccounts.length === 0 ? (
            <NoAccounts data-qa="info-no-account-access">
              {i18n.messages.noAccountAccess}
            </NoAccounts>
          ) : (
            <Redirect
              to={routes.unreadMessages(unitOrGroup).value}
              replace={true}
            />
          )}
          <BottomNavbar selected="messages" unitOrGroup={unitOrGroup} />
        </ContentArea>
      )
    )
  } else {
    return renderResult(
      combine(groupAccounts, groupAccount(unitOrGroup.id)),
      ([groupAccounts, selectedAccount]) => {
        if (!selectedAccount) {
          return <Redirect to={routes.messages(toUnitOrGroup(unitId)).value} />
        }
        switch (uiState.type) {
          case 'list':
            return (
              <PageWithNavigation
                selected="messages"
                selectedGroup={
                  selectedAccount.daycareGroup
                    ? {
                        id: selectedAccount.daycareGroup.id,
                        name: selectedAccount.daycareGroup.name,
                        childCapacity: 0,
                        staffCapacity: 0,
                        utilization: 0
                      }
                    : undefined
                }
                unitOrGroup={unitOrGroup}
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
                    <ReceivedThreadsList
                      groupAccounts={groupAccounts}
                      account={selectedAccount.account}
                      onSelectThread={onSelectThread}
                    />
                  ) : activeTab === 'sent' ? (
                    <SentMessagesList
                      account={selectedAccount.account}
                      onSelectMessage={selectSentMessage}
                    />
                  ) : (
                    <DraftMessagesList
                      account={selectedAccount.account}
                      onSelectDraft={selectDraftMessage}
                    />
                  )}
                  <FloatingPrimaryActionButton
                    text={i18n.messages.newMessage}
                    icon={faPlus}
                    onClick={onNewMessageClick}
                    data-qa="new-message-btn"
                  />
                </ContentArea>
              </PageWithNavigation>
            )
          case 'sentMessage': {
            return (
              <ContentArea
                opaque={false}
                fullHeight
                paddingHorizontal="zero"
                paddingVertical="zero"
                data-qa="messages-page-content-area"
              >
                <SentMessageView
                  unitId={unitId}
                  account={selectedAccount.account}
                  message={uiState.message}
                  onBack={onBack}
                />
              </ContentArea>
            )
          }
          case 'continueDraft':
            return renderResult(recipients, (availableRecipients) => (
              <MessageEditor
                unitId={unitId}
                availableRecipients={availableRecipients}
                account={selectedAccount.account}
                draft={uiState.draft}
                onClose={() => setUiState({ type: 'list' })}
              />
            ))
        }
      }
    )
  }
}

const NoAccounts = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
`
