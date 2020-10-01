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
import { round } from '../utils'
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

  const producOpts: SelectOptionProps[] = isSuccess(invoiceCodes)
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
        <div>
          {editable ? (
            <Select
              name="product"
              placeholder=" "
              value={producOpts.filter((elem) => elem.value == product)}
              options={producOpts}
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
        </div>
      </Td>
      <Td>
        <div>
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
        </div>
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
                ? { text: 'Virheellinen arvo', status: 'warning' }
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
            placeholder=" "
            value={subCostCenterOpts.filter(
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
          <InputField
            value={amount ? amount.toString() : ''}
            type={'number'}
            onChange={(value) =>
              update({
                amount: round(Math.max(1, Math.min(1000, Number(value))))
              })
            }
            dataQa="input-amount"
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
        {formatCents(editable ? amount * unitPrice : price)}
      </Td>
      <Td>
        {editable ? (
          <IconButton
            icon={faTrash}
            onClick={remove}
            dataQa="delete-invoice-row-button"
          />
        ) : null}
      </Td>
    </Tr>
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
