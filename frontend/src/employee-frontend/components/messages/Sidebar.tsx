// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import Button from 'lib-components/atoms/buttons/Button'
import colors, { greyscale } from 'lib-components/colors'
import { defaultMargins } from 'lib-components/white-space'
import { sortBy, uniqBy } from 'lodash'
import React, { useState } from 'react'
import styled from 'styled-components'
import Loader from '../../../lib-components/atoms/Loader'
import ErrorSegment from '../../../lib-components/atoms/state/ErrorSegment'
import { useTranslation } from '../../state/i18n'
import Select, { SelectOptionProps } from '../common/Select'
import { MessageAccount } from './types'

type GroupMessageAccount = Required<MessageAccount>

function getGroupAccounts(accounts: MessageAccount[]): GroupMessageAccount[] {
  return accounts.filter((a) => a.daycareGroup) as GroupMessageAccount[]
}

interface AccountsParams {
  accounts: MessageAccount[]
  setSelectedAccount: (account: MessageAccount) => void
}

function Accounts(props: AccountsParams) {
  const { i18n } = useTranslation()
  const personalAccount = props.accounts.find((a) => a.personal)
  const groupAccounts = getGroupAccounts(props.accounts)

  const unitOptions = uniqBy(
    groupAccounts.map(({ daycareGroup }) => ({
      value: daycareGroup.unitId,
      label: daycareGroup.unitName
    })),
    (val) => val.value
  )

  const [selectedUnit, setSelectedUnit] = useState<SelectOptionProps>(
    unitOptions[0]
  )

  if (props.accounts.length < 1) {
    return null
  }
  return (
    <>
      {personalAccount && (
        <>
          <AccountHeader>{i18n.messages.sidePanel.ownMessages}</AccountHeader>
          <div onClick={() => props.setSelectedAccount(personalAccount)}>
            {i18n.messages.messageBoxes.names.RECEIVED}
          </div>{' '}
          <div>{i18n.messages.messageBoxes.names.SENT}</div>
        </>
      )}

      {groupAccounts.length > 0 && (
        <>
          <AccountHeader>
            {i18n.messages.sidePanel.groupsMessages}
          </AccountHeader>
          {unitOptions.length > 0 && (
            <Select
              options={unitOptions}
              onChange={(val) => val && 'value' in val && setSelectedUnit(val)}
              value={selectedUnit}
            />
          )}
          <ul>
            {sortBy(
              groupAccounts.filter(
                (acc) => acc.daycareGroup.unitId === selectedUnit.value
              ),
              (val) => val.daycareGroup.name
            ).map((account) => (
              <li
                key={account.id}
                onClick={() => props.setSelectedAccount(account)}
              >
                {account.daycareGroup.name}
              </li>
            ))}
          </ul>
        </>
      )}
    </>
  )
}

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
      {accounts.isLoading && <Loader />}
      {accounts.isFailure && <ErrorSegment />}
      {accounts.isSuccess && (
        <Accounts
          accounts={accounts.value}
          setSelectedAccount={setSelectedAccount}
        />
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
