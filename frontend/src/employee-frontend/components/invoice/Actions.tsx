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
  invoiceResponse: InvoiceDetailedResponse
}

export const MarkSent = React.memo(function MarkSent({
  invoiceResponse
}: Props) {
  const { i18n } = useTranslation()
  const { invoice, permittedActions } = invoiceResponse

  return permittedActions.includes('MARK_SENT') ? (
    <FixedSpaceRow justifyContent="flex-end">
      <MutateButton
        primary
        text={i18n.invoice.form.buttons.markSent}
        mutation={markInvoicesSentMutation}
        onClick={() => ({ body: [invoice.id] })}
        data-qa="invoice-actions-mark-sent"
      />
    </FixedSpaceRow>
  ) : null
})
