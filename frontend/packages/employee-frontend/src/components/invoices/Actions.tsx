// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { Gap } from '@evaka/lib-components/src/white-space'
import AsyncButton from '~components/shared/atoms/buttons/AsyncButton'
import Button from '~components/shared/atoms/buttons/Button'
import { useTranslation } from '../../state/i18n'
import StickyActionBar from '../common/StickyActionBar'
import { deleteInvoices } from '../../api/invoicing'
import { InvoiceStatus } from '../../types/invoicing'
import { EspooColours } from '../../utils/colours'
import { InvoicesActions } from './invoices-state'

const ErrorMessage = styled.div`
  color: ${EspooColours.red};
`

const CheckedRowsInfo = styled.div`
  color: ${EspooColours.grey};
  font-style: italic;
  font-weight: bold;
`

type Props = {
  actions: InvoicesActions
  reloadInvoices: () => void
  status: InvoiceStatus
  checkedInvoices: Record<string, true>
  checkedAreas: string[]
  allInvoicesToggle: boolean
}

const Actions = React.memo(function Actions({
  actions,
  reloadInvoices,
  status,
  checkedInvoices,
  checkedAreas,
  allInvoicesToggle
}: Props) {
  const { i18n } = useTranslation()
  const [error, setError] = useState(false)
  const checkedIds = Object.keys(checkedInvoices)

  return status === 'DRAFT' ? (
    <StickyActionBar align={'right'}>
      {error ? (
        <>
          <ErrorMessage>{i18n.common.error.unknown}</ErrorMessage>
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
      <AsyncButton
        text={i18n.invoices.buttons.deleteInvoice(checkedIds.length)}
        disabled={checkedIds.length === 0}
        onClick={() =>
          deleteInvoices(checkedIds)
            .then(() => void setError(false))
            .catch(() => void setError(true))
        }
        onSuccess={() => {
          actions.clearChecked()
          reloadInvoices()
        }}
        data-qa="delete-invoices"
      />
      <Gap size="s" horizontal />
      <Button
        primary
        disabled={
          (!allInvoicesToggle && checkedIds.length === 0) ||
          (allInvoicesToggle && checkedAreas.length === 0)
        }
        text={i18n.invoices.buttons.sendInvoice(checkedIds.length)}
        onClick={actions.openModal}
        data-qa="open-send-invoices-dialog"
      />
    </StickyActionBar>
  ) : null
})

export default Actions
