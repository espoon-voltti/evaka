// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect } from 'react'
import { useTranslation } from '../../state/i18n'
import { UnitContext } from '../../state/unit'
import { useHistory, useParams } from 'react-router-dom'
import { UUID } from 'lib-common/types'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { renderResult } from '../async-rendering'
import { H1 } from 'lib-components/typography'
import { MessageContext } from '../../state/messages'
import BottomNavBar from '../common/BottomNavbar'
import { ContentArea } from 'lib-components/layout/Container'
import { ThreadView } from './ThreadView'
import { MessagePreview } from './MessagePreview'
import { TopBarWithGroupSelector } from '../common/TopBarWithGroupSelector'
import colors from 'lib-customizations/common'
import EmptyMessageFolder from 'lib-components/employee/messages/EmptyMessageFolder'
import styled from 'styled-components'
import { defaultMargins } from 'lib-components/white-space'

export default function MessagesPage() {
  const history = useHistory()
  const { unitId, groupId: groupIdOrAll } = useParams<{
    unitId: UUID
    groupId: UUID | 'all'
  }>()

  const { unitInfoResponse } = useContext(UnitContext)

  function changeGroup(group: GroupInfo | undefined) {
    const groupId = group === undefined ? 'all' : group.id
    history.push(`/units/${unitId}/groups/${groupId}/messages`)
  }

  const selectedGroup =
    groupIdOrAll === 'all'
      ? undefined
      : unitInfoResponse
          .map((res) => res.groups.find((g) => g.id === groupIdOrAll))
          .getOrElse(undefined)

  const {
    loadNestedAccounts,
    groupAccounts,
    selectedSender,
    loadMessagesWhenGroupChanges,
    receivedMessages,
    personalAccount,
    selectedThread,
    selectThread
  } = useContext(MessageContext)

  useEffect(() => loadNestedAccounts(unitId), [loadNestedAccounts, unitId])

  useEffect(() => {
    loadMessagesWhenGroupChanges(selectedGroup)
  }, [selectedGroup, loadMessagesWhenGroupChanges])

  const { i18n } = useTranslation()

  const onBack = useCallback(() => selectThread(undefined), [selectThread])

  return selectedThread && selectedSender ? (
    <ContentArea
      opaque
      fullHeight
      paddingHorizontal={'zero'}
      paddingVertical={'zero'}
    >
      <ThreadView thread={selectedThread} onBack={onBack} />
    </ContentArea>
  ) : (
    renderResult(receivedMessages, (messages) => (
      <ContentArea
        opaque
        fullHeight
        paddingVertical={'zero'}
        paddingHorizontal={'zero'}
      >
        <TopBarWithGroupSelector
          selectedGroup={selectedGroup}
          onChangeGroup={changeGroup}
        />
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
                  item.sender.id !== personalAccount?.id &&
                  !groupAccounts.some((ga) => ga.account.id === item.sender.id)
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
            iconColor={colors.greyscale.medium}
            text={i18n.messages.emptyInbox}
          />
        )}
        <BottomNavBar selected="messages" />
      </ContentArea>
    ))
  )
}

export const HeaderContainer = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
  border-bottom: 1px solid ${colors.greyscale.lighter};
`
