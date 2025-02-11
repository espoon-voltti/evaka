// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import React, { useCallback, useContext } from 'react'
import styled from 'styled-components'

import { MessageAccount } from 'lib-common/generated/api-types/messaging'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'

import { MessageContext } from './MessageContext'
import { AccountView, isStandardView, View } from './types-view'

export const MessageBoxRow = styled.div<{ active: boolean }>`
  cursor: pointer;
  padding: 12px ${defaultMargins.m};
  font-weight: ${(p) => (p.active ? fontWeights.semibold : 'unset')};
  background-color: ${(p) => (p.active ? colors.main.m4 : 'unset')};
`

interface MessageBoxProps {
  account: MessageAccount
  activeView: AccountView | undefined
  view: View
  unitId: string | null
  selectAccount: (v: AccountView) => void
}

export default function MessageBox({
  account,
  activeView,
  view,
  unitId,
  selectAccount
}: MessageBoxProps) {
  const { i18n } = useTranslation()
  const { unreadCountsByAccount } = useContext(MessageContext)
  const active =
    !!activeView &&
    isEqual(view, activeView.view) &&
    account.id === activeView.account.id
  const unreadCount = unreadCountsByAccount
    .map((unreadCounts) => {
      if (view === 'received') {
        return (
          unreadCounts.find(({ accountId }) => accountId === account.id)
            ?.unreadCount ?? 0
        )
      }
      if (view === 'copies') {
        return (
          unreadCounts.find(({ accountId }) => accountId === account.id)
            ?.unreadCopyCount ?? 0
        )
      }

      return 0
    })
    .getOrElse(0)
  const onClick = useCallback(
    () => selectAccount({ account, view, unitId }),
    [account, selectAccount, unitId, view]
  )
  return (
    <MessageBoxRow
      onClick={onClick}
      active={active}
      data-qa={`message-box-row-${isStandardView(view) ? view : view.id}`}
    >
      {isStandardView(view)
        ? i18n.messages.messageBoxes.names[view]
        : view.name}{' '}
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
