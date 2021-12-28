// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  MessageReceiversResponse,
  NestedMessageAccount
} from 'lib-common/generated/api-types/messaging'
import { getReceivers } from './api'
import {
  SelectorNode,
  unitAsSelectorNode
} from 'lib-components/employee/messages/SelectorNode'
import { Result } from 'lib-common/api'
import Button from 'lib-components/atoms/buttons/Button'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { sortBy, uniqBy } from 'lodash'
import React, { useContext, useEffect, useMemo } from 'react'
import styled from 'styled-components'
import colors from 'lib-customizations/common'
import { useTranslation } from '../../state/i18n'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import GroupMessageAccountList from './GroupMessageAccountList'
import MessageBox from './MessageBox'
import { MessageContext } from './MessageContext'
import { messageBoxes } from './types-view'
import { fontWeights, H1 } from 'lib-components/typography'
import { renderResult } from '../async-rendering'
import { isNestedGroupMessageAccount } from 'lib-components/employee/messages/types'

const Container = styled.div`
  flex: 0 1 260px;
  display: flex;
  flex-direction: column;
  margin-right: ${defaultMargins.m};

  & > div {
    background-color: ${colors.greyscale.white};
  }
`
const AccountContainer = styled.div`
  flex: 1;
  overflow-y: auto;
`

const HeaderContainer = styled.div`
  padding: 12px ${defaultMargins.m};
  padding-top: 0px;
  padding-bottom: 0px;
`

const DashedLine = styled.hr`
  width: 100%;
  border: 1px dashed ${colors.greyscale.medium};
  border-top-width: 0px;
`

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
  font-weight: ${fontWeights.semibold};
`

const NoAccounts = styled.div`
  padding: ${defaultMargins.s};
`

const UnitSelection = styled.div`
  padding: 0 ${defaultMargins.s};
`

const Receivers = styled.div<{ active: boolean }>`
  cursor: pointer;
  padding: 12px ${defaultMargins.m};
  font-weight: ${(p) => (p.active ? fontWeights.semibold : 'unset')};
  background-color: ${(p) => (p.active ? colors.main.lighter : 'unset')};
`

interface AccountsParams {
  nestedAccounts: NestedMessageAccount[]
  setSelectedReceivers: React.Dispatch<
    React.SetStateAction<SelectorNode | undefined>
  >
}

function Accounts({ nestedAccounts, setSelectedReceivers }: AccountsParams) {
  const { i18n } = useTranslation()
  const { setSelectedAccount, selectedAccount, selectedUnit, setSelectedUnit } =
    useContext(MessageContext)

  const [personalAccount, groupAccounts, unitOptions] = useMemo(() => {
    const nestedPersonalAccount = nestedAccounts.find(
      (a) => !isNestedGroupMessageAccount(a)
    )
    const nestedGroupAccounts = nestedAccounts.filter(
      isNestedGroupMessageAccount
    )
    const unitOptions = sortBy(
      uniqBy(
        nestedGroupAccounts.map(({ daycareGroup }) => ({
          value: daycareGroup.unitId,
          label: daycareGroup.unitName
        })),
        (val) => val.value
      ),
      (u) => u.label
    )
    return [nestedPersonalAccount, nestedGroupAccounts, unitOptions]
  }, [nestedAccounts])

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
      {nestedAccounts.length === 0 && (
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
            nestedGroupAccounts={visibleGroupAccounts}
            activeView={selectedAccount}
            setView={setSelectedAccount}
          />
        </AccountSection>
      )}
    </>
  )
}

interface Props {
  setSelectedReceivers: React.Dispatch<
    React.SetStateAction<SelectorNode | undefined>
  >
  showEditor: () => void
}

export default React.memo(function Sidebar({
  setSelectedReceivers,
  showEditor
}: Props) {
  const { i18n } = useTranslation()
  const { nestedAccounts, selectedAccount, setSelectedAccount } =
    useContext(MessageContext)

  const newMessageEnabled =
    nestedAccounts.isSuccess && nestedAccounts.value.length > 0
  return (
    <Container>
      <AccountContainer>
        <Gap size={'s'} />
        <HeaderContainer>
          <H1 noMargin>{i18n.messages.inboxTitle}</H1>
        </HeaderContainer>
        <DashedLine />
        <Gap size={'s'} />
        <HeaderContainer>
          <Button
            primary
            disabled={!newMessageEnabled}
            text={i18n.messages.messageBoxes.newMessage}
            onClick={showEditor}
            data-qa="new-message-btn"
          />
        </HeaderContainer>
        {renderResult(nestedAccounts, (nestedAccounts) => (
          <Accounts
            nestedAccounts={nestedAccounts}
            setSelectedReceivers={setSelectedReceivers}
          />
        ))}
        <Receivers
          active={selectedAccount?.view === 'RECEIVERS'}
          onClick={() =>
            selectedAccount &&
            setSelectedAccount({ ...selectedAccount, view: 'RECEIVERS' })
          }
          style={{ display: 'none' }}
        >
          {i18n.messages.receiverSelection.title}
        </Receivers>
      </AccountContainer>
    </Container>
  )
})
