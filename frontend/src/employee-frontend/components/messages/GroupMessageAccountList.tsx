// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import partition from 'lodash/partition'
import type { ReactNode } from 'react'
import { useContext, useState } from 'react'
import styled from 'styled-components'

import type { AuthorizedMessageAccount } from 'lib-common/generated/api-types/messaging'
import type { GroupMessageAccount } from 'lib-components/messages/types'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronDown, faChevronUp } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import MessageBox, { MessageBoxRow } from './MessageBox'
import { MessageContext } from './MessageContext'
import type { AccountView } from './types-view'
import { groupMessageBoxes } from './types-view'

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
const Content = styled.div<{ $visible: boolean }>`
  display: ${(p) => (p.$visible ? 'block' : 'none')};
`
const ClosedGroupsToggle = styled.button`
  display: flex;
  align-items: center;
  gap: ${defaultMargins.xs};
  width: 100%;
  padding: 12px ${defaultMargins.m};
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
  font: inherit;
  font-weight: ${fontWeights.semibold};
  color: ${colors.main.m2};

  &:hover {
    color: ${colors.main.m1};
  }

  svg {
    flex: none;
  }
`

function CollapsibleRow({
  startCollapsed,
  children,
  title,
  'data-qa': dataQa
}: {
  children: ReactNode
  startCollapsed: boolean
  title: string
  'data-qa': string
}) {
  const [expanded, setExpanded] = useState(!startCollapsed)
  return (
    <AccountContainer data-qa={dataQa}>
      <CollapseToggle onClick={() => setExpanded(!expanded)}>
        <div data-qa="group-message-account-name">{title}</div>
        <FontAwesomeIcon
          icon={expanded ? faChevronUp : faChevronDown}
          size="xs"
          color={colors.grayscale.g100}
        />
      </CollapseToggle>
      <Content $visible={expanded}>{children}</Content>
    </AccountContainer>
  )
}

const CollapsibleMessageBoxesContainer = styled.div`
  margin: ${defaultMargins.s} 0;

  ${MessageBoxRow} {
    padding-left: 40px;
  }
`
const ClosedAccountsContainer = styled.div`
  ${CollapseToggle} {
    padding-left: 40px;
  }

  && ${MessageBoxRow} {
    padding-left: 56px;
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
  const { i18n } = useTranslation()
  const { unreadCountsByAccount } = useContext(MessageContext)
  const [openAccounts, closedAccounts] = partition(
    accounts,
    (acc) => !acc.daycareGroup.closed
  )
  const closedAccountSelected = closedAccounts.some(
    (acc) => acc.account.id === activeView?.account.id
  )
  const [showClosedAccounts, setShowClosedAccounts] = useState(
    closedAccountSelected
  )
  // Changing the unit or navigating back could cause an ended group to become
  // selected while the ended groups are hidden. Make them visible in that case.
  const [previousClosedAccountSelected, setPreviousClosedAccountSelected] =
    useState(closedAccountSelected)
  if (closedAccountSelected !== previousClosedAccountSelected) {
    setPreviousClosedAccountSelected(closedAccountSelected)
    if (closedAccountSelected) setShowClosedAccounts(true)
  }

  const startCollapsed = (acc: AuthorizedMessageAccount, i: number) =>
    i > 0 &&
    acc.account.id !== activeView?.account.id &&
    unreadCountsByAccount
      .map((counts) => {
        const accountUnreadCounts = counts.find(
          (a) => a.accountId === acc.account.id
        )
        return accountUnreadCounts ? accountUnreadCounts.totalUnreadCount : 0
      })
      .getOrElse(0) === 0

  const renderAccount = (acc: GroupMessageAccount, i: number) => (
    <CollapsibleRow
      key={acc.account.id}
      startCollapsed={startCollapsed(acc, i)}
      title={
        acc.daycareGroup.closed
          ? i18n.messages.sidePanel.closedGroupName(acc.daycareGroup.name)
          : acc.daycareGroup.name
      }
      data-qa={`group-account-${acc.daycareGroup.id}`}
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
  )

  return (
    <CollapsibleMessageBoxesContainer>
      {openAccounts.map((acc, i) => renderAccount(acc, i))}
      {closedAccounts.length > 0 && (
        <>
          <AccountContainer>
            <ClosedGroupsToggle
              onClick={() => setShowClosedAccounts(!showClosedAccounts)}
              data-qa="show-closed-groups-toggle"
            >
              {showClosedAccounts
                ? i18n.messages.sidePanel.hideClosedGroups
                : i18n.messages.sidePanel.showClosedGroups}
              <FontAwesomeIcon
                icon={showClosedAccounts ? faChevronUp : faChevronDown}
                size="xs"
              />
            </ClosedGroupsToggle>
          </AccountContainer>
          {showClosedAccounts && (
            <ClosedAccountsContainer>
              {closedAccounts.map((acc, i) =>
                renderAccount(acc, openAccounts.length + i)
              )}
            </ClosedAccountsContainer>
          )}
        </>
      )}
    </CollapsibleMessageBoxesContainer>
  )
}
