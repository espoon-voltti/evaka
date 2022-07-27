// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React from 'react'
import { Link } from 'react-router-dom'

import type { Invoice } from 'lib-common/generated/api-types/invoicing'
import { formatCents } from 'lib-common/money'
import type { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { faChild } from 'lib-icons'

import { getPersonInvoices } from '../../api/invoicing'
import { useTranslation } from '../../state/i18n'
import { StatusTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function PersonInvoices({ id, open }: Props) {
  const { i18n } = useTranslation()
  const [invoices] = useApiState(() => getPersonInvoices(id), [id])

  return (
    <div>
      <CollapsibleSection
        icon={faChild}
        title={i18n.personProfile.invoices}
        data-qa="person-invoices-collapsible"
        startCollapsed={!open}
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
              {orderBy(invoices, ['sentAt'], ['desc']).map(
                (invoice: Invoice) => {
                  return (
                    <Tr key={`${invoice.id}`} data-qa="table-invoice-row">
                      <Td>
                        <Link to={`/finance/invoices/${invoice.id}`}>
                          Lasku{' '}
                          {`${invoice.periodStart.format()} - ${invoice.periodEnd.format()}`}
                        </Link>
                      </Td>
                      <Td>{formatCents(invoice.totalPrice)}</Td>
                      <StatusTd>{i18n.invoice.status[invoice.status]}</StatusTd>
                    </Tr>
                  )
                }
              )}
            </Tbody>
          </Table>
        ))}
      </CollapsibleSection>
    </div>
  )
})
