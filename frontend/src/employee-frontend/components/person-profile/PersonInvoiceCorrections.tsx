// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { uniqBy } from 'lodash'
import React, { useContext, useMemo } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { formatPersonName } from 'employee-frontend/utils'
import { combine } from 'lib-common/api'
import {
  InvoiceCodes,
  InvoiceCorrection
} from 'lib-common/generated/api-types/invoicing'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { formatCents } from 'lib-common/money'
import { useApiState } from 'lib-common/utils/useRestApi'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { H4 } from 'lib-components/typography'
import { faChild } from 'lib-icons'

import {
  getInvoiceCodes,
  getPersonInvoiceCorrections
} from '../../api/invoicing'
import { Translations, useTranslation } from '../../state/i18n'
import { PersonContext } from '../../state/person'
import { renderResult } from '../async-rendering'

interface Props {
  id: string
  open: boolean
}

export default React.memo(function PersonInvoiceCorrections({
  id,
  open
}: Props) {
  const { i18n } = useTranslation()
  const { fridgeChildren } = useContext(PersonContext)
  const [invoiceCodes] = useApiState(getInvoiceCodes, [])
  const [corrections] = useApiState(() => getPersonInvoiceCorrections(id), [id])

  const children = useMemo(
    () =>
      fridgeChildren.map((children) =>
        uniqBy(children, ({ childId }) => childId).map(({ child }) => child)
      ),
    [fridgeChildren]
  )

  const groupedCorrections = useMemo(
    () =>
      combine(children, corrections).map(([children, corrections]) => {
        const pairs: [string, InvoiceCorrection[]][] = children.map((child) => [
          child.id,
          corrections.filter((correction) => correction.childId === child.id)
        ])
        return Object.fromEntries(pairs)
      }),
    [children, corrections]
  )

  return (
    <CollapsibleSection
      icon={faChild}
      title={i18n.personProfile.invoiceCorrections}
      data-qa="person-invoice-corrections-collapsible"
      startCollapsed={!open}
    >
      {renderResult(
        combine(children, invoiceCodes, groupedCorrections),
        ([children, invoiceCodes, groupedCorrections]) => (
          <FixedSpaceColumn spacing="L">
            {children.length === 0 ? (
              <div>{i18n.invoiceCorrections.noChildren}</div>
            ) : (
              children.map((child) => (
                <ChildSection
                  key={child.id}
                  i18n={i18n}
                  child={child}
                  corrections={groupedCorrections[child.id] ?? []}
                  invoiceCodes={invoiceCodes}
                />
              ))
            )}
          </FixedSpaceColumn>
        )
      )}
    </CollapsibleSection>
  )
})

const ChildSection = React.memo(function ChildSection({
  i18n,
  child,
  corrections,
  invoiceCodes
}: {
  i18n: Translations
  child: PersonJSON
  corrections: InvoiceCorrection[]
  invoiceCodes: InvoiceCodes
}) {
  return (
    <FixedSpaceColumn spacing="s">
      <Link to={`/child-information/${child.id}`}>
        <ChildName noMargin>{formatPersonName(child, i18n)}</ChildName>
      </Link>
      <Table>
        <Thead>
          <Tr>
            <Th>{i18n.invoice.form.rows.product}</Th>
            <Th>{i18n.invoice.form.rows.description}</Th>
            <Th>{i18n.invoice.form.rows.unitId}</Th>
            <Th>{i18n.invoice.form.rows.daterange}</Th>
            <Th>{i18n.invoice.form.rows.amount}</Th>
            <Th>{i18n.invoice.form.rows.unitPrice}</Th>
            <Th>{i18n.invoice.form.rows.price}</Th>
            <Th />
          </Tr>
        </Thead>
        <Tbody>
          {corrections.map((correction) => (
            <ReadModeRow
              key={correction.id}
              row={correction}
              invoiceCodes={invoiceCodes}
            />
          ))}
        </Tbody>
      </Table>
    </FixedSpaceColumn>
  )
})

const ChildName = styled(H4)`
  color: ${(p) => p.theme.colors.main.m2};
`

const ReadModeRow = React.memo(function ReadModeRow({
  row,
  invoiceCodes
}: {
  row: InvoiceCorrection
  invoiceCodes: InvoiceCodes
}) {
  return (
    <Tr>
      <Td>
        {invoiceCodes.products.find((product) => product.key === row.product)
          ?.nameFi ?? ''}
      </Td>
      <Td>{row.description}</Td>
      <Td>
        {invoiceCodes.units.find((unit) => unit.id === row.unitId)?.name ?? ''}
      </Td>
      <Td>{row.period.format()}</Td>
      <Td>{row.amount}</Td>
      <Td>{formatCents(row.unitPrice)} €</Td>
      <Td>{formatCents(row.amount * row.unitPrice)} €</Td>
      <Td />
    </Tr>
  )
})
