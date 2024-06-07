// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { InvoiceStatus } from 'lib-common/generated/api-types/invoicing'
import { LegacyAsyncButton } from 'lib-components/atoms/buttons/LegacyAsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { deleteDraftInvoices } from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'
import StickyActionBar from '../common/StickyActionBar'

import { InvoicesActions } from './invoices-state'

const deleteDraftInvoicesResult = wrapResult(deleteDraftInvoices)

const ErrorMessage = styled.div`
  color: ${colors.status.danger};
`

const CheckedRowsInfo = styled.div`
  color: ${colors.grayscale.g35};
  font-style: italic;
  font-weight: ${fontWeights.bold};
`

type Props = {
  actions: InvoicesActions
  reloadInvoices: () => void
  status: InvoiceStatus
  canSend: boolean
  canDelete: boolean
  checkedInvoices: Record<string, true>
  checkedAreas: string[]
  allInvoicesToggle: boolean
}

const Actions = React.memo(function Actions({
  actions,
  reloadInvoices,
  status,
  canSend,
  canDelete,
  checkedInvoices,
  checkedAreas,
  allInvoicesToggle
}: Props) {
  const { i18n } = useTranslation()
  const [error, setError] = useState<string>()
  const checkedIds = Object.keys(checkedInvoices)

  return status === 'DRAFT' ? (
    <StickyActionBar align="right">
      {error ? (
        <>
          <ErrorMessage>{error}</ErrorMessage>
          <Gap size="s" horizontal />
        </>
      ) : null}
      {checkedIds.length > 0 ? (
        <>
          <CheckedRowsInfo>
            {i18n.invoices.buttons.checked(checkedIds.length)}
          </CheckedRowsInfo>
          <Gap size="s" horizontal />
        </>
      ) : null}
      {canDelete && (
        <LegacyAsyncButton
          text={i18n.invoices.buttons.deleteInvoice(checkedIds.length)}
          disabled={checkedIds.length === 0}
          onClick={() => deleteDraftInvoicesResult({ body: checkedIds })}
          onSuccess={() => {
            setError(undefined)
            actions.clearChecked()
            reloadInvoices()
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
            (!allInvoicesToggle && checkedIds.length === 0) ||
            (allInvoicesToggle && checkedAreas.length === 0)
          }
          text={i18n.invoices.buttons.sendInvoice(checkedIds.length)}
          onClick={actions.openModal}
          data-qa="open-send-invoices-dialog"
        />
      )}
    </StickyActionBar>
  ) : null
})

export default Actions
