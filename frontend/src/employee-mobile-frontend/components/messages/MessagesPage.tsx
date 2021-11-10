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
import { HeaderContainer, MessagePreview } from './MessagePreview'
import { TopBarWithGroupSelector } from '../common/TopBarWithGroupSelector'
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
    refreshMessages,
    receivedMessages,
    selectedThread,
    selectThread
  } = useContext(MessageContext)

  useEffect(() => loadNestedAccounts(unitId), [loadNestedAccounts, unitId])

  useEffect(() => refreshMessages(), [refreshMessages])

  const { i18n } = useTranslation()

  return renderResult(unitInfoResponse, (unit) => (
    <>
      {!(selectedThread && selectedAccount) ? (
        <>
          <TopBarWithGroupSelector
            title={unit.name}
            selectedGroup={selectedGroup}
            onChangeGroup={changeGroup}
          />
          <ContentArea opaque fullHeight paddingHorizontal={'zero'}>
            <HeaderContainer>
              <H1>{i18n.messages.title}</H1>
            </HeaderContainer>
            {receivedMessages.isSuccess &&
              receivedMessages.value.map((thread) => (
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
              ))}
          </ContentArea>
          <BottomNavBar selected="messages" />
        </>
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
  ))
}
