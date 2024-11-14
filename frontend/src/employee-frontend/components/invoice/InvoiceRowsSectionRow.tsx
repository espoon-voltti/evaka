// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import {
  InvoiceDaycare,
  ProductWithName
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import { UUID } from 'lib-common/types'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { faCommentAlt, fasCommentAltLines } from 'lib-icons'

export interface InvoiceRowStub {
  product: string
  description: string
  unitId: UUID | null
  savedCostCenter: string | null
  periodStart: LocalDate
  periodEnd: LocalDate
  amount: number
  unitPrice: number
  price: number
  note: string | null
}

interface Props {
  row: InvoiceRowStub
  products: ProductWithName[]
  unitDetails: Record<UUID, InvoiceDaycare>
}

export default React.memo(function InvoiceRowSectionRow({
  row: {
    product,
    description,
    unitId,
    savedCostCenter,
    periodStart,
    periodEnd,
    amount,
    unitPrice,
    price,
    note
  },
  products,
  unitDetails
}: Props) {
  const theme = useTheme()

  const unit = unitId ? unitDetails[unitId] : null

  return (
    <Tr data-qa="invoice-row">
      <Td>
        <div data-qa="product">
          {products.find(({ key }) => key === product)?.nameFi ?? ''}
        </div>
      </Td>
      <Td>
        <div data-qa="description">{description}</div>
      </Td>
      <Td>
        <div data-qa="unit">
          <span>{unit?.name}</span>
          {!!savedCostCenter && (
            <UnitCostCenter>{savedCostCenter}</UnitCostCenter>
          )}
        </div>
      </Td>
      <Td>
        <span data-qa="period">
          {periodStart.format()} - {periodEnd.format()}
        </span>
      </Td>
      <Td>
        <span data-qa="amount">{amount}</span>
      </Td>
      <Td align="right">
        <span data-qa="unit-price">{formatCents(unitPrice)}</span>
      </Td>
      <Td align="right" data-qa="total-price">
        {formatCents(price)}
      </Td>
      <Td>
        <FixedSpaceRow spacing="s" justifyContent="flex-end">
          {note !== null ? (
            <Tooltip tooltip={note} data-qa="note-tooltip">
              <IconButtonWrapper margin={false}>
                <FontAwesomeIcon
                  icon={note ? fasCommentAltLines : faCommentAlt}
                  color={theme.colors.main.m2}
                  data-qa="note-icon"
                />
              </IconButtonWrapper>
            </Tooltip>
          ) : null}
        </FixedSpaceRow>
      </Td>
    </Tr>
  )
})

const IconButtonWrapper = styled.div<{ margin: boolean }>`
  ${({ margin }) => (margin ? 'margin: 6px 0;' : '')}
`

const UnitCostCenter = styled.span`
  font-style: italic;
  color: ${(p) => p.theme.colors.grayscale.g70};
  padding-left: 8px;
`
