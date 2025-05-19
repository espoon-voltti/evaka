// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useContext, useEffect, useMemo } from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import { wrapResult } from 'lib-common/api'
import { sortSelectableRecipients } from 'lib-common/api-types/messaging'
import type { SelectableRecipientsResponse } from 'lib-common/generated/api-types/messaging'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { fontWeights, H1 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { getSelectableRecipients } from '../../generated/api-clients/messaging'
import { useTranslation } from '../../state/i18n'

import GroupMessageAccountList from './GroupMessageAccountList'
import MessageBox from './MessageBox'
import { MessageContext } from './MessageContext'
import {
  financeMessageBoxes,
  municipalMessageBoxes,
  personalMessageBoxes,
  serviceWorkerMessageBoxes
} from './types-view'

const getSeletableRecipientsResult = wrapResult(getSelectableRecipients)

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

const FoldersHeader = styled.div`
  padding: 12px ${defaultMargins.m};
  color: ${colors.grayscale.g70};
  font-family: 'Montserrat', sans-serif;
  font-size: 16px;
  font-weight: ${fontWeights.semibold};
`

const NoAccounts = styled.div`
  padding: ${defaultMargins.s};
`

const UnitSelection = styled.div`
  padding: 0 ${defaultMargins.s};
`

interface AccountsProps {
  setRecipients: React.Dispatch<
    React.SetStateAction<SelectableRecipientsResponse[] | undefined>
  >
}

function Accounts({ setRecipients }: AccountsProps) {
  const { i18n } = useTranslation()
  const {
    selectedAccount,
    municipalAccount,
    serviceWorkerAccount,
    financeAccount,
    folders,
    personalAccount,
    groupAccounts,
    unitOptions,
    selectAccount,
    selectUnit
  } = useContext(MessageContext)

  const unitSelectionEnabled = unitOptions.length > 1
  const selectedUnit = useMemo(
    () =>
      unitOptions.find((unit) => unit.value === selectedAccount?.unitId) ??
      unitOptions[0],
    [selectedAccount?.unitId, unitOptions]
  )

  useEffect(() => {
    void getSeletableRecipientsResult().then(
      (result: Result<SelectableRecipientsResponse[]>) => {
        if (result.isSuccess) {
          const sortedRecipients = result.value.map((account) => ({
            ...account,
            receivers: sortSelectableRecipients(account.receivers)
          }))
          setRecipients(sortedRecipients)
        }
      }
    )
  }, [setRecipients])

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
      {!municipalAccount &&
        !serviceWorkerAccount &&
        !financeAccount &&
        !personalAccount &&
        groupAccounts.length === 0 && (
          <NoAccounts>{i18n.messages.sidePanel.noAccountAccess}</NoAccounts>
        )}

      {municipalAccount && (
        <AccountSection data-qa="municipal-account">
          <AccountHeader>
            {i18n.messages.sidePanel.municipalMessages}
          </AccountHeader>
          {municipalMessageBoxes.map((view) => (
            <MessageBox
              key={view}
              view={view}
              account={municipalAccount.account}
              unitId={null}
              activeView={selectedAccount}
              selectAccount={selectAccount}
            />
          ))}
        </AccountSection>
      )}

      {serviceWorkerAccount && (
        <AccountSection data-qa="service-worker-account">
          <AccountHeader>
            {i18n.messages.sidePanel.serviceWorkerMessages}
          </AccountHeader>
          {serviceWorkerMessageBoxes.map((view) => (
            <MessageBox
              key={view}
              view={view}
              account={serviceWorkerAccount.account}
              unitId={null}
              activeView={selectedAccount}
              selectAccount={selectAccount}
            />
          ))}
          {folders.isSuccess &&
            folders.value.filter(
              (f) => f.ownerId === serviceWorkerAccount.account.id
            ).length > 0 && (
              <>
                <FoldersHeader>
                  {i18n.messages.sidePanel.serviceWorkerFolders}
                </FoldersHeader>
                {folders.value
                  .filter((f) => f.ownerId === serviceWorkerAccount.account.id)
                  .map((folder) => (
                    <MessageBox
                      key={folder.id}
                      view={folder}
                      account={serviceWorkerAccount.account}
                      unitId={null}
                      activeView={selectedAccount}
                      selectAccount={selectAccount}
                    />
                  ))}
              </>
            )}
        </AccountSection>
      )}

      {financeAccount && (
        <AccountSection data-qa="finance-account">
          <AccountHeader>
            {i18n.messages.sidePanel.financeMessages}
          </AccountHeader>
          {financeMessageBoxes.map((view) => (
            <MessageBox
              key={view}
              view={view}
              account={financeAccount.account}
              unitId={null}
              activeView={selectedAccount}
              selectAccount={selectAccount}
            />
          ))}
        </AccountSection>
      )}

      {personalAccount && (
        <AccountSection data-qa="personal-account">
          <AccountHeader>{i18n.messages.sidePanel.ownMessages}</AccountHeader>
          {personalMessageBoxes.map((view) => (
            <MessageBox
              key={view}
              view={view}
              account={personalAccount.account}
              unitId={null}
              activeView={selectedAccount}
              selectAccount={selectAccount}
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
                onChange={(val) => (val ? selectUnit(val.value) : undefined)}
                selectedItem={selectedUnit ?? null}
                getItemLabel={(val) => val.label}
              />
            </UnitSelection>
          )}
          <GroupMessageAccountList
            accounts={visibleGroupAccounts}
            activeView={selectedAccount}
            selectAccount={selectAccount}
          />
        </AccountSection>
      )}
    </>
  )
}

interface Props {
  showEditor: () => void
  setRecipients: React.Dispatch<
    React.SetStateAction<SelectableRecipientsResponse[] | undefined>
  >
  enableNewMessage?: boolean
}

export default React.memo(function Sidebar({
  showEditor,
  setRecipients,
  enableNewMessage = true
}: Props) {
  const { i18n } = useTranslation()
  const { accounts } = useContext(MessageContext)

  const newMessageEnabled =
    accounts.isSuccess && accounts.value.length > 0 && enableNewMessage
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
          <LegacyButton
            primary
            disabled={!newMessageEnabled}
            text={i18n.messages.messageBoxes.newMessage}
            onClick={showEditor}
            data-qa="new-message-btn"
          />
        </HeaderContainer>
        <Accounts setRecipients={setRecipients} />
      </AccountContainer>
    </Container>
  )
})
