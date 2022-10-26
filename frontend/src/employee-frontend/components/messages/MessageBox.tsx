// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'

import { MessageAccount } from 'lib-common/generated/api-types/messaging'
import RoundIcon from 'lib-components/atoms/RoundIcon'
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
  background-color: ${(p) => (p.active ? colors.main.m4 : 'unset')};
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
  const unreadCount = unreadCountsByAccount
    .map((unreadCounts) => {
      if (view === 'RECEIVED') {
        return (
          unreadCounts.find(({ accountId }) => accountId === account.id)
            ?.unreadCount ?? 0
        )
      }
      if (view === 'COPIES') {
        return (
          unreadCounts.find(({ accountId }) => accountId === account.id)
            ?.unreadCopyCount ?? 0
        )
      }

      return 0
    })
    .getOrElse(0)
  return (
    <MessageBoxRow
      onClick={() => setView({ account: account, view: view })}
      active={active}
      data-qa={`message-box-row-${view}`}
    >
      {i18n.messages.messageBoxes.names[view]}{' '}
      {unreadCount > 0 && (
        <RoundIconWithMargin
          content={String(unreadCount)}
          color={colors.status.warning}
          size="s"
        />
      )}
    </MessageBoxRow>
  )
}

const RoundIconWithMargin = styled(RoundIcon)`
  margin-left: ${defaultMargins.xs};
`
