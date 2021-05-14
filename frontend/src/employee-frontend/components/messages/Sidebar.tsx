// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import Button from 'lib-components/atoms/buttons/Button'
import colors, { greyscale } from 'lib-components/colors'
import { defaultMargins } from 'lib-components/white-space'
import React from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { MessageAccount } from './types'

interface Props {
  accounts: Result<MessageAccount[]>
  selectedAccount: MessageAccount | undefined
  setSelectedAccount: (acc: MessageAccount) => void
  showEditor: () => void
}

export default React.memo(function Sidebar({
  accounts,
  setSelectedAccount,
  showEditor
}: Props) {
  const { i18n } = useTranslation()

  return (
    <Container>
      <AccountHeader>{i18n.messages.sidePanel.ownMessages}</AccountHeader>
      <AccountHeader>{i18n.messages.sidePanel.groupsMessages}</AccountHeader>
      {accounts.isSuccess && (
        <ul>
          {accounts.value.map((account) => (
            <li key={account.id} onClick={() => setSelectedAccount(account)}>
              {account.name}
            </li>
          ))}
        </ul>
      )}
      <Button
        text={i18n.messages.messageBoxes.newBulletin}
        onClick={showEditor}
      />
    </Container>
  )
})

const Container = styled.div`
  width: 20%;
  min-width: 20%;
  max-width: 20%;
  min-height: 500px;
  background-color: ${colors.greyscale.white};
  overflow-y: auto;
  margin-right: ${defaultMargins.m};
`

const AccountHeader = styled.div`
  padding: ${defaultMargins.m};
  color: ${greyscale.dark};
  font-family: 'Montserrat', sans-serif;
  font-size: 20px;
  font-weight: 600;
`
