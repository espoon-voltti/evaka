// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { ReactNode, useContext, useState } from 'react'
import styled from 'styled-components'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronDown, faChevronUp } from 'lib-icons'
import MessageBox, { MessageBoxRow } from './MessageBox'
import { NestedGroupMessageAccount, NestedMessageAccount } from './types'
import { AccountView, messageBoxes } from './types-view'
import { MessageContext } from 'employee-frontend/components/messages/MessageContext'

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
          color={colors.greyscale.darkest}
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
  nestedGroupAccounts,
  activeView,
  setView
}: {
  nestedGroupAccounts: NestedGroupMessageAccount[]
  activeView: AccountView | undefined
  setView: (view: AccountView) => void
}) {
  const { unreadCountsByAccount } = useContext(MessageContext)
  const startCollapsed = (nestedAccount: NestedMessageAccount, i: number) =>
    i > 0 &&
    ((unreadCountsByAccount.isSuccess &&
      !unreadCountsByAccount.value.find(
        (x) => x.accountId === nestedAccount.account.id
      )?.unreadCount) ||
      !unreadCountsByAccount.isSuccess)

  return (
    <CollapsibleMessageBoxesContainer>
      {nestedGroupAccounts.map((nestedAcc, i) => (
        <CollapsibleRow
          key={nestedAcc.account.id}
          startCollapsed={startCollapsed(nestedAcc, i)}
          title={nestedAcc.daycareGroup.name}
        >
          {messageBoxes.map((view) => (
            <MessageBox
              key={view}
              view={view}
              account={nestedAcc.account}
              activeView={activeView}
              setView={setView}
            />
          ))}
        </CollapsibleRow>
      ))}
    </CollapsibleMessageBoxesContainer>
  )
}
