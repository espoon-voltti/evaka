// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router-dom'

import { PersonContext } from 'employee-frontend/state/person'
import { UserContext } from 'employee-frontend/state/user'
import { formatCents } from 'lib-common/money'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H2 } from 'lib-components/typography'
import { faRefresh } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { StatusTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'
import { formatInvoicePeriod } from '../invoice/utils'

import {
  createReplacementDraftsForHeadOfFamilyMutation,
  headOfFamilyInvoicesQuery
} from './queries'

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function PersonInvoices({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const user = useContext(UserContext)
  const { permittedActions } = useContext(PersonContext)
  const [open, setOpen] = useState(startOpen)
  const invoices = useQueryResult(headOfFamilyInvoicesQuery({ id }))

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
        {user?.user?.accessibleFeatures.replacementInvoices &&
        permittedActions.has('CREATE_REPLACEMENT_DRAFT_INVOICES') ? (
          <FixedSpaceColumn alignItems="flex-end">
            <MutateButton
              icon={faRefresh}
              appearance="inline"
              mutation={createReplacementDraftsForHeadOfFamilyMutation}
              onClick={() => ({ headOfFamilyId: id })}
              text={i18n.personProfile.invoice.createReplacementDrafts}
            />
          </FixedSpaceColumn>
        ) : null}
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
                      {formatInvoicePeriod(invoice, i18n)}
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
