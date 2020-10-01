// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faChild } from '@evaka/icons'
import * as _ from 'lodash'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { isFailure, isLoading, isSuccess, Loading } from '~api'
import { getPersonInvoices } from '~api/invoicing'
import { StatusTd } from '~components/PersonProfile'
import { Collapsible, Loader, Table } from '~components/shared/alpha'
import { useTranslation } from '~state/i18n'
import { PersonContext } from '~state/person'
import { UUID } from '~types'
import { Invoice } from '~types/invoicing'
import { formatCents } from '~utils/money'

interface Props {
  id: UUID
  open: boolean
}

const PersonInvoices = React.memo(function PersonInvoices({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { invoices, setInvoices } = useContext(PersonContext)
  const [toggled, setToggled] = useState(open)
  const toggle = useCallback(() => setToggled((toggled) => !toggled), [
    setToggled
  ])

  useEffect(() => {
    setInvoices(Loading())
    void getPersonInvoices(id).then((response) => {
      setInvoices(response)
    })
  }, [id, setInvoices])

  const renderInvoices = () =>
    isSuccess(invoices)
      ? _.orderBy(invoices.data, ['sentAt'], ['desc']).map(
          (invoice: Invoice) => {
            return (
              <Table.Row key={`${invoice.id}`} dataQa="table-invoice-row">
                <Table.Td>
                  <Link to={`/invoices/${invoice.id}`}>
                    Lasku{' '}
                    {`${invoice.periodStart.format()} - ${invoice.periodEnd.format()}`}
                  </Link>
                </Table.Td>
                <Table.Td>{formatCents(invoice.totalPrice)}</Table.Td>
                <StatusTd>{i18n.invoice.status[invoice.status]}</StatusTd>
              </Table.Row>
            )
          }
        )
      : null

  return (
    <div>
      <Collapsible
        icon={faChild}
        title={i18n.personProfile.invoices}
        open={toggled}
        onToggle={toggle}
        dataQa="person-invoices-collapsible"
      >
        <Table.Table dataQa="table-of-invoices">
          <Table.Head>
            <Table.Row>
              <Table.Th>{i18n.personProfile.invoice.validity}</Table.Th>
              <Table.Th>{i18n.personProfile.invoice.price}</Table.Th>
              <Table.Th>{i18n.personProfile.invoice.status}</Table.Th>
            </Table.Row>
          </Table.Head>
          <Table.Body>{renderInvoices()}</Table.Body>
        </Table.Table>
        {isLoading(invoices) && <Loader />}
        {isFailure(invoices) && <div>{i18n.common.loadingFailed}</div>}
      </Collapsible>
    </div>
  )
})

export default PersonInvoices
