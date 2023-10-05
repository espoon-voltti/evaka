// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { ContentArea } from 'lib-components/layout/Container'
import EmptyMessageFolder from 'lib-components/messages/EmptyMessageFolder'
import { H1 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { renderResult } from '../async-rendering'
import BottomNavbar from '../common/BottomNavbar'
import { PageWithNavigation } from '../common/PageWithNavigation'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'
import { UnitContext } from '../common/unit'

import { MessagePreview } from './MessagePreview'
import ThreadView from './ThreadView'
import { MessageContext } from './state'

export default function MessagesPage() {
  const navigate = useNavigate()
  const { unitId } = useNonNullableParams<{
    unitId: UUID
  }>()

  const { unitInfoResponse } = useContext(UnitContext)
  const { groupRoute } = useSelectedGroup()

  function changeGroup(group: GroupInfo | undefined) {
    if (group) navigate(`/units/${unitId}/groups/${group.id}/messages`)
  }

  const {
    groupAccounts,
    receivedMessages,
    selectedThread,
    selectThread,
    selectedAccount
  } = useContext(MessageContext)

  const { i18n } = useTranslation()
  const onBack = useCallback(() => selectThread(undefined), [selectThread])

  return selectedAccount && selectedThread ? (
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
  ) : selectedAccount && !selectedThread ? (
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
      allowedGroupIds={groupAccounts.flatMap((ga) => ga.daycareGroup?.id || [])}
      includeSelectAll={false}
    >
      {renderResult(receivedMessages, (messages) => (
        <ContentArea
          opaque
          paddingVertical="zero"
          paddingHorizontal="zero"
          data-qa="messages-page-content-area"
        >
          <HeaderContainer>
            <H1 noMargin={true}>{i18n.messages.title}</H1>
          </HeaderContainer>
          {messages.length > 0 ? (
            messages.map((thread) => (
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
                  selectThread(thread)
                }}
                key={thread.id}
              />
            ))
          ) : (
            <EmptyMessageFolder
              loading={receivedMessages.isLoading}
              iconColor={colors.grayscale.g35}
              text={i18n.messages.emptyInbox}
            />
          )}
        </ContentArea>
      ))}
    </PageWithNavigation>
  ) : (
    renderResult(unitInfoResponse, (unit) => (
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
  )
}

export const HeaderContainer = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
`

const NoAccounts = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
`
