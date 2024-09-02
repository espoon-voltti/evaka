// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import { wrapResult } from 'lib-common/api'
import { InvoiceDetailedResponse } from 'lib-common/generated/api-types/invoicing'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import { markInvoicesSent } from '../../generated/api-clients/invoicing'

const markInvoicesSentResult = wrapResult(markInvoicesSent)

type Props = {
  invoice: InvoiceDetailedResponse
  loadInvoice: () => void
}

const Actions = React.memo(function Actions({ invoice, loadInvoice }: Props) {
  const { i18n } = useTranslation()
  const markSent = useCallback(
    () => markInvoicesSentResult({ body: [invoice.data.id] }),
    [invoice.data.id]
  )

  return (
    <FixedSpaceRow justifyContent="flex-end">
      {invoice.permittedActions.includes('MARK_SENT') &&
      invoice.data.status === 'WAITING_FOR_SENDING' ? (
        <AsyncButton
          primary
          text={i18n.invoice.form.buttons.markSent}
          onClick={markSent}
          onSuccess={loadInvoice}
          data-qa="invoice-actions-mark-sent"
        />
      ) : null}
    </FixedSpaceRow>
  )
})

export default Actions
