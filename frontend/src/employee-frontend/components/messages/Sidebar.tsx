// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import _ from 'lodash'
import colors from 'lib-components/colors'
import { defaultMargins } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import { Result } from 'lib-common/api'
import { EnrichedMessageAccount } from 'employee-frontend/components/messages/types'
import { UUID } from 'employee-frontend/types'

type Props = {
  accounts: Result<EnrichedMessageAccount[]>,
  selectedAccount: UUID | undefined,
  setSelectedAccount: (id: UUID) => void,
}

export default React.memo(function Sidebar({
  accounts
}: Props) {
  const t = useTranslation()
  console.log(t)

  return (
    <>
      <Container>
        <HeaderContainer>
        </HeaderContainer>
        {accounts.isSuccess && (
        <ul>
          {accounts.value.map(account => (<li key={account.accountId}>{account.accountName}</li>))}
        </ul>
        )

        }
      </Container>
    </>
  )
})

const Container = styled.div`
  width: 20%;
  min-width: 20%;
  max-width: 20%;
  min-height: 500px;
  background-color: ${colors.greyscale.white};
  overflow-y: auto;
`

const HeaderContainer = styled.div`
  padding: ${defaultMargins.m};
`
