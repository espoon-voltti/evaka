// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { faTrash } from '@evaka/icons'
import LocalDate from '@evaka/lib-common/src/local-date'
import { IconButton, Input, Table } from '~components/shared/alpha'
import Select, { SelectOptionProps } from '../common/Select'
import DateRangeInput from '../common/DateRangeInput'
import EuroInput from '../common/EuroInput'
import { useTranslation } from '../../state/i18n'
import { isSuccess, Result } from '../../api'
import { Product, InvoiceCodes } from '../../types/invoicing'
import { round } from '../utils'
import { EspooColours } from '../../utils/colours'
import { formatCents, parseCents } from '../../utils/money'

const CostCenterInput = styled.input`
  width: 4em;

  &.invalid {
    border-color: ${EspooColours.orange};
  }
`

interface InvoiceRowStub {
  product: Product
  description: string
  costCenter: string
  subCostCenter: string | null
  periodStart: LocalDate
  periodEnd: LocalDate
  amount: number
  unitPrice: number
  price: number
}

interface Props {
  row: InvoiceRowStub
  update: (v: Partial<InvoiceRowStub>) => void
  remove: () => void
  invoiceCodes: Result<InvoiceCodes>
  editable: boolean
}

function InvoiceRowSectionRow({
  row: {
    product,
    description,
    costCenter,
    subCostCenter,
    periodStart,
    periodEnd,
    amount,
    unitPrice,
    price
  },
  update,
  remove,
  invoiceCodes,
  editable
}: Props) {
  const { i18n } = useTranslation()

  const producOpts: SelectOptionProps[] = isSuccess(invoiceCodes)
    ? invoiceCodes.data.products.map((product) => ({
        id: product,
        label: i18n.product[product]
      }))
    : []

  const subCostCenterOpts: SelectOptionProps[] = isSuccess(invoiceCodes)
    ? invoiceCodes.data.subCostCenters.map((subCostCenter) => ({
        id: subCostCenter,
        label: subCostCenter
      }))
    : []

  const costCenterValueIsValid =
    costCenter === undefined ||
    !isSuccess(invoiceCodes) ||
    invoiceCodes.data.costCenters.includes(costCenter)

  return (
    <Table.Row dataQa="invoice-details-invoice-row">
      <Table.Td>
        <div>
          {editable ? (
            <Select
              name="product"
              placeholder=" "
              options={producOpts}
              value={product}
              onChange={(event: React.ChangeEvent<HTMLSelectElement>) =>
                update({ product: event.target.value as Product })
              }
              dataQa="select-product"
            />
          ) : (
            <div>{i18n.product[product]}</div>
          )}
        </div>
      </Table.Td>
      <Table.Td>
        <div>
          {editable ? (
            <Input
              name="description"
              placeholder={i18n.invoice.form.rows.description}
              type="text"
              value={description}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                update({ description: event.target.value.substring(0, 52) })
              }
              dataQa="input-description"
            />
          ) : (
            <div>{description}</div>
          )}
        </div>
      </Table.Td>
      <Table.Td>
        {editable ? (
          <CostCenterInput
            className={classNames('input', {
              invalid: !costCenterValueIsValid
            })}
            name="costCenter"
            type="text"
            placeholder={i18n.invoice.form.rows.costCenter}
            value={costCenter}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
              update({ costCenter: event.target.value })
            }
            data-qa="input-cost-center"
          />
        ) : (
          <div>{costCenter}</div>
        )}
      </Table.Td>
      <Table.Td>
        {editable ? (
          <Select
            name="subCostCenter"
            placeholder=" "
            options={subCostCenterOpts}
            value={subCostCenter ?? undefined}
            onChange={(event: React.ChangeEvent<HTMLSelectElement>) =>
              update({ subCostCenter: event.target.value })
            }
            dataQa="select-sub-cost-center"
          />
        ) : (
          <div>{subCostCenter}</div>
        )}
      </Table.Td>
      <Table.Td>
        {editable ? (
          <DateRangeInput
            start={periodStart}
            end={periodEnd}
            onChange={(start: LocalDate, end: LocalDate) =>
              update({
                periodStart: start,
                periodEnd: end
              })
            }
            onValidationResult={() => undefined}
          />
        ) : (
          `${periodStart.format()} - ${periodEnd.format()}`
        )}
      </Table.Td>
      <Table.Td>
        {editable ? (
          <Input
            value={amount ? amount.toString() : ''}
            type={'number'}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
              update({
                amount: round(
                  Math.max(1, Math.min(1000, Number(event.target.value)))
                )
              })
            }
            dataQa="input-amount"
          />
        ) : (
          amount
        )}
      </Table.Td>
      <Table.Td align="right">
        {editable ? (
          <UnitPriceInput
            value={unitPrice}
            onChange={(unitPrice) => void update({ unitPrice })}
          />
        ) : (
          formatCents(unitPrice)
        )}
      </Table.Td>
      <Table.Td align="right">
        {formatCents(editable ? amount * unitPrice : price)}
      </Table.Td>
      <Table.Td>
        {editable ? (
          <IconButton
            icon={faTrash}
            onClick={remove}
            dataQa="delete-invoice-row-button"
          />
        ) : null}
      </Table.Td>
    </Table.Row>
  )
}

const NarrowEuroInput = styled(EuroInput)`
  width: 5em;
`

const UnitPriceInput = React.memo(function UnitPriceInput({
  value,
  onChange
}: {
  value: number
  onChange: (v: number) => void
}) {
  const [stringValue, setStringValue] = useState(formatCents(value) ?? '')

  useEffect(() => {
    const parsed = parseCents(stringValue)
    if (parsed !== undefined) {
      onChange(parsed)
    }
  }, [stringValue])

  return (
    <NarrowEuroInput
      value={stringValue}
      onChange={setStringValue}
      allowEmpty={false}
      dataQa="input-price"
    />
  )
})

export default InvoiceRowSectionRow
