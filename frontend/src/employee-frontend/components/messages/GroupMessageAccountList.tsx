// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { ReactNode, useContext, useState } from 'react'
import styled from 'styled-components'

import { AuthorizedMessageAccount } from 'lib-common/generated/api-types/messaging'
import { GroupMessageAccount } from 'lib-components/messages/types'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronDown, faChevronUp } from 'lib-icons'

import MessageBox, { MessageBoxRow } from './MessageBox'
import { MessageContext } from './MessageContext'
import { AccountView, groupMessageBoxes } from './types-view'

const AccountContainer = styled.div`
  margin: 12px 0;
`
const CollapseToggle = styled.div`
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px ${defaultMargins.m};
`
const Content = styled.div<{ visible: boolean }>`
  display: ${(p) => (p.visible ? 'block' : 'none')};
`

function CollapsibleRow({
  startCollapsed,
  children,
  title
}: {
  children: ReactNode
  startCollapsed: boolean
  title: string
}) {
  const [expanded, setExpanded] = useState(!startCollapsed)
  return (
    <AccountContainer>
      <CollapseToggle onClick={() => setExpanded(!expanded)}>
        <div>{title}</div>
        <FontAwesomeIcon
          icon={expanded ? faChevronUp : faChevronDown}
          size="xs"
          color={colors.grayscale.g100}
        />
      </CollapseToggle>
      <Content visible={expanded}>{children}</Content>
    </AccountContainer>
  )
}

const CollapsibleMessageBoxesContainer = styled.div`
  margin: ${defaultMargins.s} 0;

  ${MessageBoxRow} {
    padding-left: 40px;
  }
`

export default function GroupMessageAccountList({
  accounts,
  activeView,
  selectAccount
}: {
  accounts: GroupMessageAccount[]
  activeView: AccountView | undefined
  selectAccount: (v: AccountView) => void
}) {
  const { unreadCountsByAccount } = useContext(MessageContext)
  const startCollapsed = (acc: AuthorizedMessageAccount, i: number) =>
    i > 0 &&
    unreadCountsByAccount
      .map((counts) => {
        const accountUnreadCounts = counts.find(
          (a) => a.accountId === acc.account.id
        )
        return accountUnreadCounts ? accountUnreadCounts.totalUnreadCount : 0
      })
      .getOrElse(0) === 0

  return (
    <CollapsibleMessageBoxesContainer>
      {accounts.map((acc, i) => (
        <CollapsibleRow
          key={acc.account.id}
          startCollapsed={startCollapsed(acc, i)}
          title={acc.daycareGroup.name}
        >
          {groupMessageBoxes.map((view) => (
            <MessageBox
              key={view}
              view={view}
              account={acc.account}
              unitId={acc.daycareGroup.unitId}
              activeView={activeView}
              selectAccount={selectAccount}
            />
          ))}
        </CollapsibleRow>
      ))}
    </CollapsibleMessageBoxesContainer>
  )
}
