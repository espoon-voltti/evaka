// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { PaymentStatus } from 'lib-common/generated/api-types/invoicing'
import Button from 'lib-components/atoms/buttons/Button'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import StickyActionBar from '../common/StickyActionBar'

import { PaymentsActions } from './payments-state'

const CheckedRowsInfo = styled.div`
  color: ${colors.grayscale.g35};
  font-style: italic;
  font-weight: ${fontWeights.bold};
`

type Props = {
  actions: PaymentsActions
  status: PaymentStatus
  checkedPayments: Record<string, true>
}

const Actions = React.memo(function Actions({
  actions,
  status,
  checkedPayments
}: Props) {
  const { i18n } = useTranslation()
  const checkedIds = Object.keys(checkedPayments)

  return status === 'DRAFT' ? (
    <StickyActionBar align="right">
      {checkedIds.length > 0 ? (
        <>
          <CheckedRowsInfo>
            {i18n.payments.buttons.checked(checkedIds.length)}
          </CheckedRowsInfo>
          <Gap size="s" horizontal />
        </>
      ) : null}
      <Gap size="s" horizontal />
      <Button
        primary
        disabled={checkedIds.length === 0}
        text={i18n.payments.buttons.sendPayments(checkedIds.length)}
        onClick={actions.openModal}
        data-qa="open-send-invoices-dialog"
      />
    </StickyActionBar>
  ) : null
})

export default Actions
