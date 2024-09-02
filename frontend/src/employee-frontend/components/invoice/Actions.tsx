// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import { InvoiceDetailedResponse } from 'lib-common/generated/api-types/invoicing'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import { markInvoicesSentMutation } from '../invoices/queries'

type Props = {
  invoice: InvoiceDetailedResponse
}

const Actions = React.memo(function Actions({ invoice }: Props) {
  const { i18n } = useTranslation()

  return (
    <FixedSpaceRow justifyContent="flex-end">
      {invoice.permittedActions.includes('MARK_SENT') &&
      invoice.data.status === 'WAITING_FOR_SENDING' ? (
        <MutateButton
          primary
          text={i18n.invoice.form.buttons.markSent}
          mutation={markInvoicesSentMutation}
          onClick={() => ({body: [invoice.data.id]})}
          data-qa="invoice-actions-mark-sent"
        />
      ) : null}
    </FixedSpaceRow>
  )
})

export default Actions
