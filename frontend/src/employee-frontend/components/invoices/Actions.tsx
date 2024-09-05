// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { InvoiceStatus } from 'lib-common/generated/api-types/invoicing'
import { UUID } from 'lib-common/types'
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
  status: InvoiceStatus
  canSend: boolean
  canDelete: boolean
  checkedInvoices: Set<UUID>
  clearCheckedInvoices: () => void
  checkedAreas: string[]
  fullAreaSelection: boolean
}

const Actions = React.memo(function Actions({
  openModal,
  status,
  canSend,
  canDelete,
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
  ) : null
})

export default Actions
