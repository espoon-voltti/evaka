// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import Button from '~components/shared/atoms/buttons/Button'
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
  goToInvoices(): void
  editable: boolean
}

const Actions = React.memo(function Actions({
  invoice,
  goToInvoices,
  editable
}: Props) {
  const { i18n } = useTranslation()
  const [actionInFlight, setActionInFlight] = useState(false)
  const [error, setError] = useState(false)

  const saveChanges = () => {
    setActionInFlight(true)
    updateInvoice(invoice)
      .then(() => void setError(false))
      .then(goToInvoices)
      .catch(() => void setError(true))
      .finally(() => void setActionInFlight(false))
  }

  const markSent = () => {
    setActionInFlight(true)
    markInvoiceSent([invoice.id])
      .then(() => void setError(false))
      .then(goToInvoices)
      .catch(() => void setError(true))
      .finally(() => void setActionInFlight(false))
  }

  return (
    <FixedSpaceRow>
      {error ? <ErrorMessage>{i18n.common.error.unknown}</ErrorMessage> : null}
      {[
        invoice.status === 'WAITING_FOR_SENDING' ? (
          <Button
            key="invoice-actions-mark-sent"
            primary
            onClick={markSent}
            disabled={actionInFlight}
            dataQa="invoice-actions-mark-sent"
            text={i18n.invoice.form.buttons.markSent}
          />
        ) : null,
        editable ? (
          <Button
            key="invoice-actions-save-changes"
            primary
            onClick={saveChanges}
            disabled={actionInFlight}
            dataQa="invoice-actions-save-changes"
            text={i18n.invoice.form.buttons.saveChanges}
          />
        ) : null
      ]}
    </FixedSpaceRow>
  )
})

export default Actions
