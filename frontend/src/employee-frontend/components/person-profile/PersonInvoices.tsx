// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext } from 'react'
import { Link } from 'wouter'

import type { PersonId } from 'lib-common/generated/api-types/shared'
import { formatCents } from 'lib-common/money'
import { useQueryResult } from 'lib-common/query'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { faRefresh } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import { formatInvoicePeriod } from '../invoice/utils'

import { StatusTd } from './common'
import {
  createReplacementDraftsForHeadOfFamilyMutation,
  headOfFamilyInvoicesQuery
} from './queries'
import { PersonContext } from './state'

interface Props {
  id: PersonId
}

export default React.memo(function PersonInvoices({ id }: Props) {
  const { i18n } = useTranslation()
  const user = useContext(UserContext)
  const { permittedActions } = useContext(PersonContext)
  const invoices = useQueryResult(headOfFamilyInvoicesQuery({ id }))

  return (
    <div>
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
    </div>
  )
})
