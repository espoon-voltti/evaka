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
import React, { useContext, useEffect } from 'react'
import styled from 'styled-components'
import colors from '../../../lib-customizations/common'
import { useTranslation } from '../../state/i18n'
import Select from '../common/Select'
import GroupMessageAccountList from './GroupMessageAccountList'
import MessageBox from './MessageBox'
import { MessageContext } from './MessageContext'
import {
  isGroupMessageAccount,
  isPersonalMessageAccount,
  MessageAccount,
  ReceiverGroup
} from './types'
import { messageBoxes } from './types-view'

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
const ButtonContainer = styled.div`
  flex: 0 1 auto;
  margin-top: ${defaultMargins.s};
  padding: 12px ${defaultMargins.m};
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
  font-weight: 600;
`

const UnitSelection = styled.div`
  padding: 0 ${defaultMargins.s};
`

const Receivers = styled.div<{ active: boolean }>`
  cursor: pointer;
  padding: 12px ${defaultMargins.m};
  font-weight: ${(p) => (p.active ? '600;' : 'unset')}
  background-color: ${(p) =>
    p.active ? colors.brandEspoo.espooTurquoiseLight : 'unset'}
`

interface AccountsParams {
  accounts: MessageAccount[]
  setSelectedReceivers: React.Dispatch<
    React.SetStateAction<SelectorNode | undefined>
  >
}

function Accounts({ accounts, setSelectedReceivers }: AccountsParams) {
  const { i18n } = useTranslation()
  const {
    setSelectedAccount,
    selectedAccount,
    selectedUnit,
    setSelectedUnit
  } = useContext(MessageContext)

  const personalAccount = accounts.find(isPersonalMessageAccount)
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

  useEffect(() => {
    !selectedUnit && setSelectedUnit(unitOptions[0])
  }, [selectedUnit, setSelectedUnit, unitOptions])

  useEffect(() => {
    if (!selectedUnit) {
      return
    }
    const { label: unitName, value: unitId } = selectedUnit
    void getReceivers(unitId).then((result: Result<ReceiverGroup[]>) => {
      if (result.isSuccess)
        setSelectedReceivers(() =>
          unitAsSelectorNode({ id: unitId, name: unitName }, result.value)
        )
    })
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
      {personalAccount && (
        <AccountSection>
          <AccountHeader>{i18n.messages.sidePanel.ownMessages}</AccountHeader>
          {messageBoxes.map((view) => (
            <MessageBox
              key={view}
              view={view}
              account={personalAccount}
              activeView={selectedAccount}
              setView={setSelectedAccount}
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
  const { accounts, selectedAccount, setSelectedAccount } = useContext(
    MessageContext
  )

  return (
    <Container>
      <AccountContainer>
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
                setSelectedReceivers={setSelectedReceivers}
              />
            )
          }
        })}
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
      <ButtonContainer>
        <Button
          primary
          text={i18n.messages.messageBoxes.newMessage}
          onClick={showEditor}
          data-qa="new-message-btn"
        />
      </ButtonContainer>
    </Container>
  )
})
