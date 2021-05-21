// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { ReactNode, useState } from 'react'
import styled from 'styled-components'
import { defaultMargins } from '../../../lib-components/white-space'
import colors from '../../../lib-customizations/common'
import { faChevronDown, faChevronUp } from '../../../lib-icons'
import MessageBox, { MessageBoxRow } from './MessageBox'
import { GroupMessageAccount, messageBoxes } from './types'
import { AccountView } from './types-view'

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
  accounts,
  activeView,
  setView
}: {
  accounts: GroupMessageAccount[]
  activeView: AccountView | undefined
  setView: (view: AccountView) => void
}) {
  return (
    <CollapsibleMessageBoxesContainer>
      {accounts.map((acc, i) => (
        <CollapsibleRow
          key={acc.id}
          startCollapsed={i > 0}
          title={acc.daycareGroup.name}
        >
          {messageBoxes.map((view) => (
            <MessageBox
              key={view}
              view={view}
              account={acc}
              activeView={activeView}
              setView={setView}
            />
          ))}
        </CollapsibleRow>
      ))}
    </CollapsibleMessageBoxesContainer>
  )
}
