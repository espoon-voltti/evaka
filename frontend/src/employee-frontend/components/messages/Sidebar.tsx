// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import uniqBy from 'lodash/uniqBy'
import React, { useContext, useEffect, useMemo } from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import {
  AuthorizedMessageAccount,
  MessageReceiversResponse
} from 'lib-common/generated/api-types/messaging'
import Button from 'lib-components/atoms/buttons/Button'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import {
  SelectorNode,
  unitAsSelectorNode
} from 'lib-components/employee/messages/SelectorNode'
import { isGroupMessageAccount } from 'lib-components/employee/messages/types'
import { fontWeights, H1 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import GroupMessageAccountList from './GroupMessageAccountList'
import MessageBox from './MessageBox'
import { MessageContext } from './MessageContext'
import { getReceivers } from './api'
import { messageBoxes } from './types-view'

const Container = styled.div`
  flex: 0 1 260px;
  display: flex;
  flex-direction: column;
  margin-right: ${defaultMargins.m};

  & > div {
    background-color: ${colors.grayscale.g0};
  }
`
const AccountContainer = styled.div`
  flex: 1;
  overflow-y: auto;
`

const HeaderContainer = styled.div`
  padding: 0 ${defaultMargins.m};
`

const DashedLine = styled.hr`
  width: 100%;
  border: 1px dashed ${colors.grayscale.g35};
  border-top-width: 0;
`

const AccountSection = styled.section`
  padding: 12px 0;

  & + & {
    border-top: 1px dashed ${colors.grayscale.g70};
  }
`

const AccountHeader = styled.div`
  padding: 12px ${defaultMargins.m};
  color: ${colors.grayscale.g70};
  font-family: 'Montserrat', sans-serif;
  font-size: 20px;
  font-weight: ${fontWeights.semibold};
`

const NoAccounts = styled.div`
  padding: ${defaultMargins.s};
`

const UnitSelection = styled.div`
  padding: 0 ${defaultMargins.s};
`

interface AccountsProps {
  accounts: AuthorizedMessageAccount[]
  setSelectedReceivers: React.Dispatch<
    React.SetStateAction<SelectorNode | undefined>
  >
}

function Accounts({ accounts, setSelectedReceivers }: AccountsProps) {
  const { i18n } = useTranslation()
  const { setSelectedAccount, selectedAccount, selectedUnit, setSelectedUnit } =
    useContext(MessageContext)

  const [personalAccount, groupAccounts, unitOptions] = useMemo(() => {
    const personal = accounts.find((a) => !isGroupMessageAccount(a))
    const groupAccs = accounts.filter(isGroupMessageAccount)
    const unitOpts = sortBy(
      uniqBy(
        groupAccs.map(({ daycareGroup }) => ({
          value: daycareGroup.unitId,
          label: daycareGroup.unitName
        })),
        (val) => val.value
      ),
      (u) => u.label
    )
    return [personal, groupAccs, unitOpts]
  }, [accounts])

  const unitSelectionEnabled = unitOptions.length > 1

  useEffect(() => {
    !selectedUnit && setSelectedUnit(unitOptions[0])
  }, [selectedUnit, setSelectedUnit, unitOptions])

  useEffect(() => {
    if (!selectedUnit) {
      return
    }
    const { label: unitName, value: unitId } = selectedUnit
    void getReceivers(unitId).then(
      (result: Result<MessageReceiversResponse[]>) => {
        if (result.isSuccess)
          setSelectedReceivers(() =>
            unitAsSelectorNode({ id: unitId, name: unitName }, result.value)
          )
      }
    )
  }, [selectedUnit, setSelectedReceivers])

  const visibleGroupAccounts = selectedUnit
    ? sortBy(
        groupAccounts.filter(
          (acc) => acc.daycareGroup.unitId === selectedUnit.value
        ),
        (val) => val.daycareGroup.name
      )
    : []

  return (
    <>
      {accounts.length === 0 && (
        <NoAccounts>{i18n.messages.sidePanel.noAccountAccess}</NoAccounts>
      )}

      {personalAccount && (
        <AccountSection data-qa="personal-account">
          <AccountHeader>{i18n.messages.sidePanel.ownMessages}</AccountHeader>
          {messageBoxes.map((view) => (
            <MessageBox
              key={view}
              view={view}
              account={personalAccount.account}
              activeView={selectedAccount}
              setView={setSelectedAccount}
            />
          ))}
        </AccountSection>
      )}

      {groupAccounts.length > 0 && (
        <AccountSection data-qa="unit-accounts">
          <AccountHeader>
            {i18n.messages.sidePanel.groupsMessages}
          </AccountHeader>
          {unitSelectionEnabled && (
            <UnitSelection>
              <Combobox
                items={unitOptions}
                onChange={(val) => (val ? setSelectedUnit(val) : undefined)}
                selectedItem={selectedUnit ?? null}
                getItemLabel={(val) => val.label}
              />
            </UnitSelection>
          )}
          <GroupMessageAccountList
            accounts={visibleGroupAccounts}
            activeView={selectedAccount}
            setView={setSelectedAccount}
          />
        </AccountSection>
      )}
    </>
  )
}

interface Props {
  showEditor: () => void
  setSelectedReceivers: React.Dispatch<
    React.SetStateAction<SelectorNode | undefined>
  >
}

export default React.memo(function Sidebar({
  showEditor,
  setSelectedReceivers
}: Props) {
  const { i18n } = useTranslation()
  const { accounts } = useContext(MessageContext)

  const newMessageEnabled = accounts.isSuccess && accounts.value.length > 0
  return (
    <Container>
      <AccountContainer>
        <Gap size="s" />
        <HeaderContainer>
          <H1 noMargin>{i18n.messages.inboxTitle}</H1>
        </HeaderContainer>
        <DashedLine />
        <Gap size="s" />
        <HeaderContainer>
          <Button
            primary
            disabled={!newMessageEnabled}
            text={i18n.messages.messageBoxes.newMessage}
            onClick={showEditor}
            data-qa="new-message-btn"
          />
        </HeaderContainer>
        {renderResult(accounts, (value) => (
          <Accounts
            accounts={value}
            setSelectedReceivers={setSelectedReceivers}
          />
        ))}
      </AccountContainer>
    </Container>
  )
})
