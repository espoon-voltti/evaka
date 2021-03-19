// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faChild } from 'lib-icons'
import * as _ from 'lodash'
import React, { useContext, useEffect } from 'react'
import { Link } from 'react-router-dom'

import { Loading } from 'lib-common/api'
import { getPersonInvoices } from '../../api/invoicing'
import { StatusTd } from '../../components/PersonProfile'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import Loader from 'lib-components/atoms/Loader'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { useTranslation } from '../../state/i18n'
import { PersonContext } from '../../state/person'
import { UUID } from '../../types'
import { Invoice } from '../../types/invoicing'
import { formatCents } from '../../utils/money'

interface Props {
  id: UUID
  open: boolean
}

const PersonInvoices = React.memo(function PersonInvoices({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { invoices, setInvoices } = useContext(PersonContext)

  useEffect(() => {
    setInvoices(Loading.of())
    void getPersonInvoices(id).then((response) => {
      setInvoices(response)
    })
  }, [id, setInvoices])

  const renderInvoices = () =>
    invoices.isSuccess
      ? _.orderBy(invoices.value, ['sentAt'], ['desc']).map(
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
        )
      : null

  return (
    <div>
      <CollapsibleSection
        icon={faChild}
        title={i18n.personProfile.invoices}
        dataQa="person-invoices-collapsible"
        startCollapsed={!open}
      >
        <Table data-qa="table-of-invoices">
          <Thead>
            <Tr>
              <Th>{i18n.personProfile.invoice.validity}</Th>
              <Th>{i18n.personProfile.invoice.price}</Th>
              <Th>{i18n.personProfile.invoice.status}</Th>
            </Tr>
          </Thead>
          <Tbody>{renderInvoices()}</Tbody>
        </Table>
        {invoices.isLoading && <Loader />}
        {invoices.isFailure && <div>{i18n.common.loadingFailed}</div>}
      </CollapsibleSection>
    </div>
  )
})

export default PersonInvoices
