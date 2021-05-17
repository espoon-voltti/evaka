// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { espooBrandColors } from 'lib-customizations/common'
import { defaultMargins } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import { MessageAccount } from './types'
import { AccountView, View } from './types-view'

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
  const active = view == activeView?.view && account.id == activeView.account.id
  return (
    <MessageBoxRow onClick={() => setView({ account, view })} active={active}>
      {i18n.messages.messageBoxes.names[view]}{' '}
      {view === 'RECEIVED' && account.unreadCount > 0 && (
        <UnreadCount>{account.unreadCount}</UnreadCount>
      )}
    </MessageBoxRow>
  )
}
