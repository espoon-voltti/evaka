// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'
import { espooBrandColors } from 'lib-customizations/common'
import { defaultMargins } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import { AccountView, View } from './types-view'
import { MessageContext } from './MessageContext'
import { MessageAccount } from 'lib-common/api-types/messaging/message'

export const MessageBoxRow = styled.div<{ active: boolean }>`
  cursor: pointer;
  padding: 12px ${defaultMargins.m};
  font-weight: ${(p) => (p.active ? '600;' : 'unset')}
  background-color: ${(p) =>
    p.active ? espooBrandColors.espooTurquoiseLight : 'unset'}
`

const UnreadCount = styled.span`
  color: ${espooBrandColors.espooBlue};
  font-size: 14px;
  font-weight: 600;
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
