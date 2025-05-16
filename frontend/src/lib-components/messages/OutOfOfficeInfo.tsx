// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import styled, { useTheme } from 'styled-components'

import type FiniteDateRange from 'lib-common/finite-date-range'
import type { MessageAccountWithPresence } from 'lib-common/generated/api-types/messaging'
import type { MessageAccountId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { defaultMargins } from 'lib-components/white-space'
import { faExclamation } from 'lib-icons'

import { useTranslations } from '../i18n'

export interface OutOfOfficeInfoProps {
  selectedAccountIds: MessageAccountId[]
  accounts: MessageAccountWithPresence[]
}

type OutOfOfficeAccount = Omit<MessageAccountWithPresence, 'outOfOffice'> & {
  outOfOffice: FiniteDateRange
}

export default React.memo(function OutOfOfficeInfo({
  selectedAccountIds,
  accounts
}: OutOfOfficeInfoProps) {
  const i18n = useTranslations()
  const { colors } = useTheme()

  const today = LocalDate.todayInSystemTz()
  const outOfOfficeAccounts = accounts.filter(
    (account): account is OutOfOfficeAccount =>
      selectedAccountIds.includes(account.account.id) &&
      account.outOfOffice !== null &&
      account.outOfOffice.includes(today)
  )

  return outOfOfficeAccounts.length > 0 ? (
    <OutOfOfficeInfoArea data-qa="out-of-office-info">
      <RoundIcon content={faExclamation} color={colors.main.m2} size="s" />
      <div>
        {outOfOfficeAccounts.length === 1 ? (
          i18n.messages.outOfOffice.singleRecipient(
            outOfOfficeAccounts[0].account.name,
            outOfOfficeAccounts[0].outOfOffice
          )
        ) : (
          <Fragment>
            <div>{i18n.messages.outOfOffice.multipleRecipientsHeader}</div>
            <OutOfOfficeList>
              {outOfOfficeAccounts.map((account) => (
                <li key={account.account.id}>
                  {account.account.name}: {account.outOfOffice.format()}
                </li>
              ))}
            </OutOfOfficeList>
          </Fragment>
        )}
      </div>
    </OutOfOfficeInfoArea>
  ) : null
})

const OutOfOfficeInfoArea = styled.div`
  border: 1px solid ${(p) => p.theme.colors.main.m2};
  border-radius: 4px;
  padding: ${defaultMargins.s};
  display: flex;
  align-items: baseline;
  gap: ${defaultMargins.xs};
  margin-top: ${defaultMargins.s};
`
const OutOfOfficeList = styled.ul`
  margin-block: 0;
  padding-inline-start: ${defaultMargins.m};
`
