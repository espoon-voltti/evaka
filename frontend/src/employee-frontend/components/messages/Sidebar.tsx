// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import Button from 'lib-components/atoms/buttons/Button'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import colors, { greyscale } from 'lib-components/colors'
import { defaultMargins } from 'lib-components/white-space'
import { sortBy, uniqBy } from 'lodash'
import React, { useState } from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import Select, { SelectOptionProps } from '../common/Select'
import GroupMessageAccountList from './GroupMessageAccountList'
import MessageBox from './MessageBox'
import { isGroupMessageAccount, MessageAccount } from './types'
import { AccountView } from './types-view'

const AccountSection = styled.section`
  padding: 12px 0;

  & + & {
    border-top: 1px dashed ${greyscale.dark};
  }
`

const AccountHeader = styled.div`
  padding: 12px ${defaultMargins.m};
  color: ${greyscale.dark};
  font-family: 'Montserrat', sans-serif;
  font-size: 20px;
  font-weight: 600;
`

const UnitSelection = styled.div`
  padding: 0 ${defaultMargins.s};
`

interface AccountsParams {
  accounts: MessageAccount[]
  setView: (view: AccountView) => void
  view: AccountView | undefined
}

function Accounts(props: AccountsParams) {
  const { i18n } = useTranslation()
  const personalAccount = props.accounts.find((a) => a.personal)
  const groupAccounts = props.accounts.filter(isGroupMessageAccount)

  const unitOptions = sortBy(
    uniqBy(
      groupAccounts.map(({ daycareGroup }) => ({
        value: daycareGroup.unitId,
        label: daycareGroup.unitName
      })),
      (val) => val.value
    ),
    (u) => u.label
  )
  const unitSelectionEnabled = unitOptions.length > 1

  const [selectedUnit, setSelectedUnit] = useState<SelectOptionProps>(
    unitOptions[0]
  )

  const visibleGroupAccounts = sortBy(
    groupAccounts.filter(
      (acc) => acc.daycareGroup.unitId === selectedUnit.value
    ),
    (val) => val.daycareGroup.name
  )

  return (
    <>
      {personalAccount && (
        <AccountSection>
          <AccountHeader>{i18n.messages.sidePanel.ownMessages}</AccountHeader>
          <MessageBox
            view="RECEIVED"
            account={personalAccount}
            activeView={props.view}
            setView={props.setView}
          />
          <MessageBox
            view="SENT"
            account={personalAccount}
            activeView={props.view}
            setView={props.setView}
          />
        </AccountSection>
      )}

      {groupAccounts.length > 0 && (
        <AccountSection>
          <AccountHeader>
            {i18n.messages.sidePanel.groupsMessages}
          </AccountHeader>
          {unitSelectionEnabled && (
            <UnitSelection>
              <Select
                options={unitOptions}
                onChange={(val) =>
                  val && 'value' in val && setSelectedUnit(val)
                }
                value={selectedUnit}
              />
            </UnitSelection>
          )}
          <GroupMessageAccountList
            accounts={visibleGroupAccounts}
            activeView={props.view}
            setView={props.setView}
          />
        </AccountSection>
      )}
    </>
  )
}

interface Props {
  accounts: Result<MessageAccount[]>
  view: AccountView | undefined
  setView: (view: AccountView) => void
  showEditor: () => void
}

export default React.memo(function Sidebar({
  accounts,
  setView,
  showEditor,
  view
}: Props) {
  const { i18n } = useTranslation()

  return (
    <Container>
      {accounts.mapAll({
        loading() {
          return <Loader />
        },
        failure() {
          return <ErrorSegment />
        },
        success(accounts) {
          return <Accounts accounts={accounts} view={view} setView={setView} />
        }
      })}
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
