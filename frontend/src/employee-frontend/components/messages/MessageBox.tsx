// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'
import { MessageAccount } from 'lib-common/generated/api-types/messaging'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { useTranslation } from '../../state/i18n'
import { MessageContext } from './MessageContext'
import { AccountView, View } from './types-view'

export const MessageBoxRow = styled.div<{ active: boolean }>`
  cursor: pointer;
  padding: 12px ${defaultMargins.m};
  font-weight: ${(p) => (p.active ? fontWeights.semibold : 'unset')};
  background-color: ${(p) => (p.active ? colors.main.lighter : 'unset')};
`

const UnreadCount = styled.span`
  color: ${colors.main.primary};
  font-size: 14px;
  font-weight: ${fontWeights.semibold};
  padding-left: ${defaultMargins.xxs};
`

interface MessageBoxProps {
  account: MessageAccount
  activeView: AccountView | undefined
  setView: (view: AccountView) => void
  view: View
}

export default function MessageBox({
  account,
  activeView,
  setView,
  view
}: MessageBoxProps) {
  const { i18n } = useTranslation()
  const { unreadCountsByAccount } = useContext(MessageContext)
  const active = view == activeView?.view && account.id == activeView.account.id
  const unreadCount =
    (unreadCountsByAccount.isSuccess
      ? unreadCountsByAccount.value.find(
          ({ accountId }) => accountId === account.id
        )?.unreadCount
      : null) || 0
  return (
    <MessageBoxRow
      onClick={() => setView({ account: account, view: view })}
      active={active}
      data-qa={`message-box-row-${view}`}
    >
      {i18n.messages.messageBoxes.names[view]}{' '}
      {view === 'RECEIVED' && unreadCount > 0 && (
        <UnreadCount>{unreadCount}</UnreadCount>
      )}
    </MessageBoxRow>
  )
}
