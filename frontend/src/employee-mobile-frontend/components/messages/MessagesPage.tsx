// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
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
import { combine } from 'lib-common/api'

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
    selectedAccount,
    loadMessagesWhenGroupChanges,
    receivedMessages,
    selectedThread,
    selectThread
  } = useContext(MessageContext)

  useEffect(() => loadNestedAccounts(unitId), [loadNestedAccounts, unitId])

  useEffect(() => {
    loadMessagesWhenGroupChanges(selectedGroup)
  }, [selectedGroup, loadMessagesWhenGroupChanges])

  const { i18n } = useTranslation()

  return renderResult(
    combine(unitInfoResponse, receivedMessages),
    ([unit, messages]) => (
      <>
        {!(selectedThread && selectedAccount) ? (
          <ContentArea opaque fullHeight paddingHorizontal={'zero'}>
            <TopBarWithGroupSelector
              title={unit.name}
              selectedGroup={selectedGroup}
              onChangeGroup={changeGroup}
            />
            <ContentArea opaque paddingHorizontal={'zero'}>
              <HeaderContainer>
                <H1>{i18n.messages.title}</H1>
              </HeaderContainer>
              {messages.length > 0 ? (
                messages.map((thread) => (
                  <MessagePreview
                    thread={thread}
                    hasUnreadMessages={thread.messages.some(
                      (item) =>
                        !item.readAt && item.sender.id !== selectedAccount?.id
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
            </ContentArea>
            <BottomNavBar selected="messages" />
          </ContentArea>
        ) : (
          <ContentArea
            opaque
            fullHeight
            paddingHorizontal={'zero'}
            paddingVertical={'zero'}
          >
            <ThreadView
              accountId={selectedAccount.id}
              thread={selectedThread}
              onBack={() => selectThread(undefined)}
            />
          </ContentArea>
        )}
      </>
    )
  )
}

export const HeaderContainer = styled.div`
  padding: 0 ${defaultMargins.s};
  border-bottom: 1px solid ${colors.greyscale.lighter};
`
