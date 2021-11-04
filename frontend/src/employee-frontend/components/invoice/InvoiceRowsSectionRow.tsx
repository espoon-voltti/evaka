// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'

import { faTrash } from 'lib-icons'
import { UpdateStateFn } from 'lib-common/form-state'
import LocalDate from 'lib-common/local-date'
import { Td, Tr } from 'lib-components/layout/Table'
import InputField from 'lib-components/atoms/form/InputField'
import Select from 'lib-components/atoms/dropdowns/Select'
import DateRangeInput from '../common/DateRangeInput'
import EuroInput from '../common/EuroInput'
import { useTranslation } from '../../state/i18n'
import { Result } from 'lib-common/api'
import { Product, InvoiceCodes } from '../../types/invoicing'
import { formatCents, parseCents } from 'lib-common/money'
import IconButton from 'lib-components/atoms/buttons/IconButton'

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
  update: UpdateStateFn<InvoiceRowStub>
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

  const productOpts = invoiceCodes.map((codes) => codes.products).getOrElse([])

  const subCostCenterOpts = invoiceCodes
    .map((codes) => codes.subCostCenters)
    .getOrElse([])

  const costCenterValueIsValid =
    costCenter === undefined ||
    !invoiceCodes.isSuccess ||
    invoiceCodes.value.costCenters.includes(costCenter)

  return (
    <Tr data-qa="invoice-details-invoice-row">
      <Td>
        {editable ? (
          <Select
            name="product"
            selectedItem={product}
            items={productOpts}
            onChange={(product) => (product ? update({ product }) : undefined)}
            getItemLabel={(product) => i18n.product[product] ?? product}
            data-qa="select-product"
          />
        ) : (
          <div>{i18n.product[product] ?? product}</div>
        )}
      </Td>
      <Td>
        {editable ? (
          <InputField
            placeholder={i18n.invoice.form.rows.description}
            type="text"
            value={description}
            onChange={(value) =>
              update({ description: value.substring(0, 52) })
            }
            data-qa="input-description"
          />
        ) : (
          <div>{description}</div>
        )}
      </Td>
      <Td>
        {editable ? (
          <InputField
            value={costCenter}
            type="text"
            placeholder={i18n.invoice.form.rows.costCenter}
            onChange={(value) => update({ costCenter: value })}
            data-qa="input-cost-center"
            info={
              !costCenterValueIsValid
                ? { text: 'Tarkista', status: 'warning' }
                : undefined
            }
          />
        ) : (
          <div>{costCenter}</div>
        )}
      </Td>
      <Td>
        {editable ? (
          <Select
            name="subCostCenter"
            placeholder=""
            selectedItem={subCostCenter}
            items={subCostCenterOpts}
            onChange={(subCostCenter) => update({ subCostCenter })}
            data-qa="select-sub-cost-center"
          />
        ) : (
          <div>{subCostCenter}</div>
        )}
      </Td>
      <Td>
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
      </Td>
      <Td>
        {editable ? (
          <AmountInput
            value={amount}
            onChange={(amount) => void update({ amount })}
          />
        ) : (
          amount
        )}
      </Td>
      <Td align="right">
        {editable ? (
          <UnitPriceInput
            value={unitPrice}
            onChange={(unitPrice) => void update({ unitPrice })}
          />
        ) : (
          formatCents(unitPrice)
        )}
      </Td>
      <Td align="right">
        <TotalPrice>
          {formatCents(editable ? amount * unitPrice : price)}
        </TotalPrice>
      </Td>
      <Td>
        {editable ? (
          <DeleteButton
            icon={faTrash}
            onClick={remove}
            data-qa="delete-invoice-row-button"
          />
        ) : null}
      </Td>
    </Tr>
  )
}

const DeleteButton = styled(IconButton)`
  margin: 6px 0;
`

const TotalPrice = styled.div`
  padding: 6px 12px 6px 12px;
`

const NarrowEuroInput = styled(EuroInput)`
  width: 5em;
`

const NarrowInput = styled(InputField)`
  max-width: 80px;
`

const AmountInput = React.memo(function AmountInput({
  value,
  onChange
}: {
  value: number
  onChange: (v: number) => void
}) {
  const [stringValue, setStringValue] = useState(value ? value.toString() : '')
  const [invalid, setInvalid] = useState(false)

  useEffect(() => {
    const parsed = Number(stringValue)
    if (!Number.isNaN(parsed)) {
      onChange(parsed)
      setInvalid(false)
    }
  }, [stringValue]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <NarrowInput
      type={'number'}
      min={1}
      max={1000}
      value={stringValue}
      onChange={setStringValue}
      onBlur={() => {
        if (!stringValue || Number.isNaN(stringValue)) {
          setInvalid(true)
        }
      }}
      data-qa="input-amount"
      info={invalid ? { status: 'warning', text: 'Tarkista' } : undefined}
    />
  )
})

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
  }, [stringValue]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <NarrowEuroInput
      invalidText="Tarkista"
      value={stringValue}
      onChange={setStringValue}
      allowEmpty={false}
      data-qa="input-price"
    />
  )
})

export default InvoiceRowSectionRow
