// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, {
  ReactNode,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled, { useTheme } from 'styled-components'

import { UpdateStateFn } from 'lib-common/form-state'
import {
  InvoiceDaycare,
  ProductWithName
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { formatCents, parseCents } from 'lib-common/money'
import { UUID } from 'lib-common/types'
import Tooltip from 'lib-components/atoms/Tooltip'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Combobox, {
  MenuItemProps
} from 'lib-components/atoms/dropdowns/Combobox'
import Select from 'lib-components/atoms/dropdowns/Select'
import InputField from 'lib-components/atoms/form/InputField'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { faCommentAlt, fasCommentAltLines, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import DateRangeInput from '../common/DateRangeInput'
import EuroInput from '../common/EuroInput'

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
  update: UpdateStateFn<InvoiceRowStub>
  remove: (() => void) | undefined
  products: ProductWithName[]
  unitIds: UUID[]
  unitDetails: Record<UUID, InvoiceDaycare>
  editable: boolean
  addNote?: () => void
  status?: ReactNode
}

function InvoiceRowSectionRow({
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
  update,
  remove,
  editable,
  products,
  unitIds,
  unitDetails,
  addNote,
  status
}: Props) {
  const theme = useTheme()
  const { i18n } = useTranslation()

  const productOpts = useMemo(() => products.map(({ key }) => key), [products])

  const unit = unitId ? unitDetails[unitId] : null

  return (
    <Tr data-qa="invoice-details-invoice-row">
      <Td>
        {editable ? (
          <Select
            name="product"
            selectedItem={product}
            items={productOpts}
            onChange={(product) => (product ? update({ product }) : undefined)}
            getItemLabel={(product) =>
              products.find(({ key }) => key === product)?.nameFi ?? ''
            }
            data-qa="select-product"
          />
        ) : (
          <div>{products.find(({ key }) => key === product)?.nameFi ?? ''}</div>
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
          <UnitCombobox
            items={unitIds}
            selectedItem={unitId}
            unitDetails={unitDetails}
            update={update}
          ></UnitCombobox>
        ) : (
          <div>
            <span>{unit?.name}</span>
            {savedCostCenter && (
              <UnitCostCenter>{savedCostCenter}</UnitCostCenter>
            )}
          </div>
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
          `${formatCents(unitPrice)} €`
        )}
      </Td>
      <Td align="right">
        {editable ? (
          <TotalPrice>{`${formatCents(amount * unitPrice)} €`}</TotalPrice>
        ) : (
          `${formatCents(price)} €`
        )}
      </Td>
      {status !== undefined && <Td>{status}</Td>}
      <Td>
        <FixedSpaceRow spacing="s" justifyContent="flex-end">
          {note !== null || addNote ? (
            <Tooltip tooltip={note}>
              <IconButtonWrapper margin={editable}>
                {addNote ? (
                  <IconButton
                    icon={note ? fasCommentAltLines : faCommentAlt}
                    onClick={addNote}
                  />
                ) : (
                  <FontAwesomeIcon
                    icon={note ? fasCommentAltLines : faCommentAlt}
                    color={theme.colors.main.m2}
                  />
                )}
              </IconButtonWrapper>
            </Tooltip>
          ) : null}
          {remove ? (
            <IconButtonWrapper margin={editable}>
              <IconButton
                icon={faTrash}
                onClick={remove}
                data-qa="delete-invoice-row-button"
              />
            </IconButtonWrapper>
          ) : null}
        </FixedSpaceRow>
      </Td>
    </Tr>
  )
}

const IconButtonWrapper = styled.div<{ margin: boolean }>`
  ${({ margin }) => (margin ? 'margin: 6px 0;' : '')}
`

const TotalPrice = styled.div`
  padding: 6px 0;
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
      inputMode="numeric"
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

type UnitComboboxProps = Pick<Props, 'unitDetails' | 'update'> & {
  items: UUID[]
  selectedItem: UUID | null
}

const UnitCombobox = React.memo(function UnitCombobox({
  items,
  selectedItem,
  unitDetails,
  update
}: UnitComboboxProps) {
  const { i18n } = useTranslation()

  const unitMenuItem = useCallback(
    ({ item: unitId, highlighted }: MenuItemProps<UUID>) => {
      const unit = unitDetails[unitId]
      return (
        <UnitMenuItem className={classNames({ highlighted, clickable: true })}>
          <span>{unit?.name ?? unitId}</span>
          {unit?.costCenter && (
            <UnitCostCenter>{unit.costCenter}</UnitCostCenter>
          )}
        </UnitMenuItem>
      )
    },
    [unitDetails]
  )
  const onChange = useCallback(
    (value: UUID | null) => update({ unitId: value }),
    [update]
  )
  const getItemLabel = useCallback(
    (unitId: UUID) => unitDetails[unitId]?.name ?? unitId,
    [unitDetails]
  )
  const filterItems = useCallback(
    (inputValue: string, items: UUID[]) => {
      const filter = inputValue.toLowerCase()
      return items.filter((unitId) => {
        const unit = unitDetails[unitId]
        if (!unit) return false
        return (
          unit.name.toLowerCase().startsWith(filter) ||
          unit.costCenter?.startsWith(filter)
        )
      })
    },
    [unitDetails]
  )

  return (
    <Combobox
      data-qa="input-unit"
      items={items}
      selectedItem={selectedItem}
      onChange={onChange}
      placeholder={i18n.invoice.form.rows.unitId}
      getItemLabel={getItemLabel}
      filterItems={filterItems}
    >
      {{ menuItem: unitMenuItem }}
    </Combobox>
  )
})

const UnitMenuItem = styled.div`
  padding: 8px 10px;

  &.highlighted {
    background-color: ${(p) => p.theme.colors.main.m4};
  }

  &.clickable {
    cursor: pointer;
  }

  white-space: pre-line;
`

const UnitCostCenter = styled.span`
  font-style: italic;
  color: ${(p) => p.theme.colors.grayscale.g70};
  padding-left: 8px;
`

export default InvoiceRowSectionRow
