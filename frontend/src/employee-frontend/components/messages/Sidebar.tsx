// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getReceivers } from 'employee-frontend/components/messages/api'
import {
  SelectorNode,
  unitAsSelectorNode
} from 'employee-frontend/components/messages/SelectorNode'
import { Result } from 'lib-common/api'
import Button from 'lib-components/atoms/buttons/Button'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { defaultMargins } from 'lib-components/white-space'
import { sortBy, uniqBy } from 'lodash'
import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import colors from '../../../lib-customizations/common'
import { useTranslation } from '../../state/i18n'
import Select, { SelectOptionProps } from '../common/Select'
import GroupMessageAccountList from './GroupMessageAccountList'
import MessageBox from './MessageBox'
import {
  isGroupMessageAccount,
  MessageAccount,
  messageBoxes,
  ReceiverGroup
} from './types'
import { AccountView } from './types-view'

const AccountSection = styled.section`
  padding: 12px 0;

  & + & {
    border-top: 1px dashed ${colors.greyscale.dark};
  }
`

const AccountHeader = styled.div`
  padding: 12px ${defaultMargins.m};
  color: ${colors.greyscale.dark};
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
  setSelectedReceivers: React.Dispatch<
    React.SetStateAction<SelectorNode | undefined>
  >
  accountView: AccountView | undefined
}

function Accounts({
  accounts,
  setView,
  setSelectedReceivers,
  accountView
}: AccountsParams) {
  const { i18n } = useTranslation()
  const personalAccount = accounts.find((a) => a.personal)
  const groupAccounts = accounts.filter(isGroupMessageAccount)

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

  useEffect(() => {
    const { label: unitName, value: unitId } = selectedUnit
    void getReceivers(unitId).then((result: Result<ReceiverGroup[]>) => {
      if (result.isSuccess)
        setSelectedReceivers(() =>
          unitAsSelectorNode({ id: unitId, name: unitName }, result.value)
        )
    })
  }, [selectedUnit, setSelectedReceivers])

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
          {messageBoxes.map((view) => (
            <MessageBox
              key={view}
              view={view}
              account={personalAccount}
              activeView={accountView}
              setView={setView}
            />
          ))}
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
            activeView={accountView}
            setView={setView}
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
  setSelectedReceivers: React.Dispatch<
    React.SetStateAction<SelectorNode | undefined>
  >
  showEditor: () => void
}

export default React.memo(function Sidebar({
  accounts,
  setView,
  setSelectedReceivers,
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
          return (
            <Accounts
              accounts={accounts}
              accountView={view}
              setView={setView}
              setSelectedReceivers={setSelectedReceivers}
            />
          )
        }
      })}
      <Received
        active={!!view && view.view === 'RECEIVERS'}
        onClick={() => view && setView({ ...view, view: 'RECEIVERS' })}
      >
        {i18n.messages.receiverSelection.title}
      </Received>
      <Button
        text={i18n.messages.messageBoxes.newBulletin}
        onClick={showEditor}
      />
    </Container>
  )
})

export const Received = styled.div<{ active: boolean }>`
  cursor: pointer;
  padding: 12px ${defaultMargins.m};
  font-weight: ${(p) => (p.active ? '600;' : 'unset')}
  background-color: ${(p) =>
    p.active ? colors.brandEspoo.espooTurquoiseLight : 'unset'}
`

const Container = styled.div`
  width: 20%;
  min-width: 20%;
  max-width: 20%;
  min-height: 500px;
  background-color: ${colors.greyscale.white};
  overflow-y: auto;
  margin-right: ${defaultMargins.m};
`
