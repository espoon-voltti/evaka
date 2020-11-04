// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'

import { faTrash } from 'icon-set'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Td, Tr } from 'components/shared/layout/Table'
import InputField from '~components/shared/atoms/form/InputField'
import Select, { SelectOptionProps } from '../common/Select'
import DateRangeInput from '../common/DateRangeInput'
import EuroInput from '../common/EuroInput'
import { useTranslation } from '../../state/i18n'
import { isSuccess, Result } from '../../api'
import { Product, InvoiceCodes } from '../../types/invoicing'
import { formatCents, parseCents } from '../../utils/money'
import IconButton from '~components/shared/atoms/buttons/IconButton'

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

  const productOpts: SelectOptionProps[] = isSuccess(invoiceCodes)
    ? invoiceCodes.data.products.map((product) => ({
        value: product,
        label: i18n.product[product]
      }))
    : []

  const subCostCenterOpts: SelectOptionProps[] = isSuccess(invoiceCodes)
    ? invoiceCodes.data.subCostCenters.map((subCostCenter) => ({
        value: subCostCenter,
        label: subCostCenter
      }))
    : []

  const costCenterValueIsValid =
    costCenter === undefined ||
    !isSuccess(invoiceCodes) ||
    invoiceCodes.data.costCenters.includes(costCenter)

  return (
    <Tr data-qa="invoice-details-invoice-row">
      <Td>
        {editable ? (
          <ProductSelect
            name="product"
            placeholder=" "
            value={productOpts.find((elem) => elem.value === product)}
            options={productOpts}
            onChange={(value) =>
              value && 'value' in value
                ? update({ product: value.value as Product })
                : undefined
            }
            data-qa="select-product"
          />
        ) : (
          <div>{i18n.product[product]}</div>
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
            dataQa="input-description"
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
                ? { text: 'Virhe', status: 'warning' }
                : undefined
            }
          />
        ) : (
          <div>{costCenter}</div>
        )}
      </Td>
      <Td>
        {editable ? (
          <SubCostCenterSelect
            name="subCostCenter"
            placeholder=" "
            value={subCostCenterOpts.find(
              (elem) => elem.value == subCostCenter
            )}
            options={subCostCenterOpts}
            onChange={(value) =>
              value && 'value' in value
                ? update({ subCostCenter: value.value })
                : undefined
            }
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
            dataQa="delete-invoice-row-button"
          />
        ) : null}
      </Td>
    </Tr>
  )
}

const ProductSelect = styled(Select)`
  min-width: 280px;
`

const SubCostCenterSelect = styled(Select)`
  min-width: 80px;
`

const DeleteButton = styled(IconButton)`
  margin: 6px 0;
`

const TotalPrice = styled.div`
  padding: 6px 12px 6px 12px;
`

const NarrowEuroInput = styled(EuroInput)`
  width: 5em;
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
  }, [stringValue])

  return (
    <InputField
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
      dataQa="input-amount"
      info={invalid ? { status: 'warning', text: 'Virhe' } : undefined}
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
  }, [stringValue])

  return (
    <NarrowEuroInput
      invalidText="Virhe"
      value={stringValue}
      onChange={setStringValue}
      allowEmpty={false}
      dataQa="input-price"
    />
  )
})

export default InvoiceRowSectionRow
