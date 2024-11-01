// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useState } from 'react'
import { Link } from 'react-router-dom'

import { wrapResult } from 'lib-common/api'
import { formatCents } from 'lib-common/money'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H2 } from 'lib-components/typography'

import { getHeadOfFamilyInvoices } from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'
import { StatusTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'

const getHeadOfFamilyInvoicesResult = wrapResult(getHeadOfFamilyInvoices)

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function PersonInvoices({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const [open, setOpen] = useState(startOpen)
  const [invoices] = useApiState(
    () => getHeadOfFamilyInvoicesResult({ id }),
    [id]
  )

  return (
    <div>
      <CollapsibleContentArea
        title={<H2>{i18n.personProfile.invoices}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="person-invoices-collapsible"
      >
        {renderResult(invoices, (invoices) => (
          <Table data-qa="table-of-invoices">
            <Thead>
              <Tr>
                <Th>{i18n.personProfile.invoice.validity}</Th>
                <Th>{i18n.personProfile.invoice.price}</Th>
                <Th>{i18n.personProfile.invoice.status}</Th>
              </Tr>
            </Thead>
            <Tbody>
              {orderBy(invoices, ['sentAt'], ['desc']).map((invoice) => (
                <Tr key={invoice.id} data-qa="table-invoice-row">
                  <Td>
                    <Link to={`/finance/invoices/${invoice.id}`}>
                      Lasku{' '}
                      {`${invoice.periodStart.format()} - ${invoice.periodEnd.format()}`}
                    </Link>
                  </Td>
                  <Td>{formatCents(invoice.totalPrice)}</Td>
                  <StatusTd>{i18n.invoice.status[invoice.status]}</StatusTd>
                </Tr>
              ))}
            </Tbody>
          </Table>
        ))}
      </CollapsibleContentArea>
    </div>
  )
})
