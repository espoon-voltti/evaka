// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import type { InvoiceStatus } from 'lib-common/generated/api-types/invoicing'
import type { InvoiceId } from 'lib-common/generated/api-types/shared'
import { Button } from 'lib-components/atoms/buttons/Button'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import StickyActionBar from '../common/StickyActionBar'

import { deleteDraftInvoicesMutation } from './queries'

const ErrorMessage = styled.div`
  color: ${colors.status.danger};
`

const CheckedRowsInfo = styled.div`
  color: ${colors.grayscale.g35};
  font-style: italic;
  font-weight: ${fontWeights.bold};
`

type Props = {
  openModal: () => void
  openResendModal: () => void
  status: InvoiceStatus
  canSend: boolean
  canDelete: boolean
  canResend: boolean
  checkedInvoices: Set<InvoiceId>
  clearCheckedInvoices: () => void
  checkedAreas: string[]
  fullAreaSelection: boolean
}

const Actions = React.memo(function Actions({
  openModal,
  openResendModal,
  status,
  canSend,
  canDelete,
  canResend,
  checkedInvoices,
  clearCheckedInvoices,
  checkedAreas,
  fullAreaSelection
}: Props) {
  const { i18n } = useTranslation()
  const [error, setError] = useState<string>()

  return status === 'DRAFT' ? (
    <StickyActionBar align="right">
      {error ? (
        <>
          <ErrorMessage>{error}</ErrorMessage>
          <Gap size="s" horizontal />
        </>
      ) : null}
      {checkedInvoices.size > 0 ? (
        <>
          <CheckedRowsInfo>
            {i18n.invoices.buttons.checked(checkedInvoices.size)}
          </CheckedRowsInfo>
          <Gap size="s" horizontal />
        </>
      ) : null}
      {canDelete && (
        <MutateButton
          text={i18n.invoices.buttons.deleteInvoice(checkedInvoices.size)}
          disabled={checkedInvoices.size === 0}
          mutation={deleteDraftInvoicesMutation}
          onClick={() => ({ body: [...checkedInvoices] })}
          onSuccess={() => {
            setError(undefined)
            clearCheckedInvoices()
          }}
          onFailure={() => setError(i18n.common.error.unknown)}
          data-qa="delete-invoices"
        />
      )}
      <Gap size="s" horizontal />
      {canSend && (
        <LegacyButton
          primary
          disabled={
            (!fullAreaSelection && checkedInvoices.size === 0) ||
            (fullAreaSelection && checkedAreas.length === 0)
          }
          text={i18n.invoices.buttons.sendInvoice(checkedInvoices.size)}
          onClick={openModal}
          data-qa="open-send-invoices-dialog"
        />
      )}
    </StickyActionBar>
  ) : status === 'SENT' ? (
    <StickyActionBar align="right">
      {checkedInvoices.size > 0 ? (
        <>
          <CheckedRowsInfo>
            {i18n.invoices.buttons.checked(checkedInvoices.size)}
          </CheckedRowsInfo>
          <Gap size="s" horizontal />
        </>
      ) : null}
      {canResend && (
        <Button
          primary
          text={i18n.invoices.buttons.resendInvoice(checkedInvoices.size)}
          onClick={openResendModal}
          disabled={
            (!fullAreaSelection && checkedInvoices.size === 0) ||
            (fullAreaSelection && checkedAreas.length === 0)
          }
          data-qa="send-again"
        />
      )}
    </StickyActionBar>
  ) : null
})

export default Actions
