// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { PaymentStatus } from 'lib-common/generated/api-types/invoicing'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { deleteDraftPayments } from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'
import StickyActionBar from '../common/StickyActionBar'

import { PaymentsActions } from './payments-state'

const deleteDraftPaymentsResult = wrapResult(deleteDraftPayments)

const CheckedRowsInfo = styled.div`
  color: ${colors.grayscale.g35};
  font-style: italic;
  font-weight: ${fontWeights.bold};
`

type Props = {
  actions: PaymentsActions
  reloadPayments: () => void
  status: PaymentStatus
  checkedPayments: Record<string, true>
}

const Actions = React.memo(function Actions({
  actions,
  reloadPayments,
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
      <AsyncButton
        text={i18n.payments.buttons.deletePayment(checkedIds.length)}
        disabled={checkedIds.length === 0}
        onClick={() => deleteDraftPaymentsResult({ body: checkedIds })}
        onSuccess={() => {
          actions.clearChecked()
          reloadPayments()
        }}
        data-qa="delete-payments"
      />

      <Gap size="s" horizontal />
      <Button
        primary
        disabled={checkedIds.length === 0}
        text={i18n.payments.buttons.sendPayments(checkedIds.length)}
        onClick={actions.openModal}
        data-qa="open-send-payments-dialog"
      />
    </StickyActionBar>
  ) : null
})

export default Actions
