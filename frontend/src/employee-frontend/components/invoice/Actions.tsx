// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import type { InvoiceDetailed } from 'lib-common/generated/api-types/invoicing'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import { updateInvoice, markInvoiceSent } from '../../api/invoicing'

type Props = {
  invoice: InvoiceDetailed
  loadInvoice(): void
  editable: boolean
}

const Actions = React.memo(function Actions({
  invoice,
  loadInvoice,
  editable
}: Props) {
  const { i18n } = useTranslation()
  const saveChanges = () => updateInvoice(invoice)
  const markSent = useCallback(
    () => markInvoiceSent([invoice.id]),
    [invoice.id]
  )

  return (
    <FixedSpaceRow justifyContent="flex-end">
      {invoice.status === 'WAITING_FOR_SENDING' ? (
        <AsyncButton
          primary
          text={i18n.invoice.form.buttons.markSent}
          onClick={markSent}
          onSuccess={loadInvoice}
          data-qa="invoice-actions-mark-sent"
        />
      ) : null}
      {editable ? (
        <AsyncButton
          primary
          text={i18n.common.save}
          textInProgress={i18n.common.saving}
          textDone={i18n.common.saved}
          onClick={saveChanges}
          onSuccess={loadInvoice}
          data-qa="invoice-actions-save-changes"
        />
      ) : null}
    </FixedSpaceRow>
  )
})

export default Actions
