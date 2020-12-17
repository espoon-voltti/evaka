// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import AsyncButton from '@evaka/lib-components/src/atoms/buttons/AsyncButton'
import { useTranslation } from '../../state/i18n'
import { updateInvoice, markInvoiceSent } from '../../api/invoicing'
import { InvoiceDetailed } from '../../types/invoicing'
import { EspooColours } from '../../utils/colours'
import { FixedSpaceRow } from '~components/shared/layout/flex-helpers'

const ErrorMessage = styled.div`
  color: ${EspooColours.red};
  margin-right: 20px;
`

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
  const [error, setError] = useState(false)

  const saveChanges = () =>
    updateInvoice(invoice)
      .then(() => void setError(false))
      .catch(() => void setError(true))

  const markSent = () =>
    markInvoiceSent([invoice.id])
      .then(() => void setError(false))
      .catch(() => void setError(true))

  return (
    <FixedSpaceRow justifyContent="flex-end">
      {error ? <ErrorMessage>{i18n.common.error.unknown}</ErrorMessage> : null}
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
