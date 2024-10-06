// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import uniqBy from 'lodash/uniqBy'
import React, { useContext, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { formatPersonName } from 'employee-frontend/utils'
import { combine } from 'lib-common/api'
import { localDate, localDateRange } from 'lib-common/form/fields'
import {
  object,
  oneOf,
  required,
  transformed,
  value
} from 'lib-common/form/form'
import { useBoolean, useForm, useFormFields } from 'lib-common/form/hooks'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import { Action } from 'lib-common/generated/action'
import {
  InvoiceCorrection,
  InvoiceCorrectionWithPermittedActions,
  InvoiceDaycare,
  ProductWithName
} from 'lib-common/generated/api-types/invoicing'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { formatCents, parseCents } from 'lib-common/money'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Tooltip from 'lib-components/atoms/Tooltip'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import TextArea, { TextAreaF } from 'lib-components/atoms/form/TextArea'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, H4, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faCommentAlt, fasCommentAltLines, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { PersonContext } from '../../state/person'
import { renderResult } from '../async-rendering'
import { invoiceCodesQuery } from '../invoices/queries'

import {
  createInvoiceCorrectionMutation,
  deleteInvoiceCorrectionMutation,
  invoiceCorrectionsQuery,
  updateInvoiceCorrectionNoteMutation
} from './queries'

interface EditTarget {
  childId: UUID
  correctionId: UUID | null // null for new
}

export default React.memo(function PersonInvoiceCorrections({
  id,
  open: startOpen
}: {
  id: string
  open: boolean
}) {
  const { i18n } = useTranslation()
  const { fridgeChildren, permittedActions } = useContext(PersonContext)
  const [open, setOpen] = useState(startOpen)
  const invoiceCodes = useQueryResult(invoiceCodesQuery())
  const corrections = useQueryResult(invoiceCorrectionsQuery({ personId: id }))

  const [editTarget, setEditTarget] = useState<EditTarget | null>(null)

  const children = useMemo(
    () =>
      fridgeChildren.map((children) =>
        uniqBy(children, ({ data: { childId } }) => childId).map(
          ({ data: { child } }) => child
        )
      ),
    [fridgeChildren]
  )

  const groupedCorrections = useMemo(
    () =>
      combine(children, corrections).map(([children, corrections]) => {
        const pairs: [string, InvoiceCorrectionWithPermittedActions[]][] =
          children.map((child) => [
            child.id,
            corrections.filter(
              ({ data: correction }) => correction.childId === child.id
            )
          ])
        return Object.fromEntries(pairs)
      }),
    [children, corrections]
  )

  const products = useMemo(
    () => invoiceCodes.map(({ products }) => products),
    [invoiceCodes]
  )
  const unitIds = useMemo(
    () => invoiceCodes.map(({ units }) => units.map(({ id }) => id)),
    [invoiceCodes]
  )
  const unitDetails = useMemo(
    () =>
      invoiceCodes.map(({ units }) =>
        Object.fromEntries(units.map((unit) => [unit.id, unit]))
      ),
    [invoiceCodes]
  )

  return (
    <CollapsibleContentArea
      title={<H2>{i18n.personProfile.invoiceCorrections.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="person-invoice-corrections-collapsible"
    >
      {renderResult(
        combine(children, groupedCorrections, products, unitIds, unitDetails),
        ([children, groupedCorrections, products, unitIds, unitDetails]) => (
          <FixedSpaceColumn spacing="L">
            {children.length === 0 ? (
              <div>{i18n.invoiceCorrections.noChildren}</div>
            ) : (
              children.map((child) => (
                <ChildSection
                  key={child.id}
                  personId={id}
                  permittedPersonActions={permittedActions}
                  child={child}
                  corrections={groupedCorrections[child.id] ?? []}
                  products={products}
                  unitIds={unitIds}
                  unitDetails={unitDetails}
                  editTarget={editTarget}
                  onStartCreate={() =>
                    setEditTarget({ childId: child.id, correctionId: null })
                  }
                  onStartEdit={(correctionId) =>
                    setEditTarget({ childId: child.id, correctionId })
                  }
                  onEditorCancel={() => setEditTarget(null)}
                />
              ))
            )}
          </FixedSpaceColumn>
        )
      )}
    </CollapsibleContentArea>
  )
})

const ChildSection = React.memo(function ChildSection({
  personId,
  permittedPersonActions,
  child,
  corrections,
  products,
  unitDetails,
  editTarget,
  onStartCreate,
  onEditorCancel
}: {
  personId: UUID
  permittedPersonActions: Set<Action.Person>
  child: PersonJSON
  corrections: InvoiceCorrectionWithPermittedActions[]
  products: ProductWithName[]
  unitIds: UUID[]
  unitDetails: Record<UUID, InvoiceDaycare>
  editTarget: EditTarget | null
  onStartCreate: () => void
  onStartEdit: (correctionId: UUID) => void
  onEditorCancel: () => void
}) {
  const { i18n } = useTranslation()
  const editorOpen = editTarget !== null
  const editingThisChild = editTarget?.childId === child.id

  return (
    <FixedSpaceColumn spacing="s">
      <FixedSpaceRow justifyContent="space-between" alignItems="center">
        <ChildNameLink to={`/child-information/${child.id}`}>
          <H4 noMargin>{formatPersonName(child, i18n)}</H4>
        </ChildNameLink>

        {permittedPersonActions.has('CREATE_INVOICE_CORRECTION') && (
          <AddButton
            onClick={onStartCreate}
            text={i18n.invoiceCorrections.addRow}
            disabled={editorOpen}
            data-qa="create-invoice-correction"
          />
        )}
      </FixedSpaceRow>

      <Table>
        <Thead>
          <Tr>
            <Th>{i18n.invoiceCorrections.targetMonth}</Th>
            <Th>{i18n.invoice.form.rows.product}</Th>
            <Th>{i18n.invoice.form.rows.description}</Th>
            <Th>{i18n.invoice.form.rows.unitId}</Th>
            <Th>{i18n.invoice.form.rows.daterange}</Th>
            <Th>{i18n.invoice.form.rows.amount}</Th>
            <Th align="right">{i18n.invoice.form.rows.unitPrice}</Th>
            <Th align="right">{i18n.invoice.form.rows.price}</Th>
            <Th>{i18n.personProfile.invoiceCorrections.invoiceStatusHeader}</Th>
            <Th />
          </Tr>
        </Thead>
        <Tbody>
          {editingThisChild && (
            <InvoiceCorrectionEditModal
              personId={personId}
              childId={child.id}
              row={
                editTarget?.correctionId
                  ? corrections.find(
                      (c) => c.data.id === editTarget.correctionId
                    )?.data ?? null
                  : null
              }
              products={products}
              units={unitDetails}
              onEditorClose={onEditorCancel}
            />
          )}
          {corrections.map((row) => (
            <InvoiceCorrectionRowReadView
              key={row.data.id}
              row={row}
              products={products}
              units={unitDetails}
            />
          ))}
        </Tbody>
      </Table>
    </FixedSpaceColumn>
  )
})

const InvoiceCorrectionRowReadView = React.memo(
  function InvoiceCorrectionRowReadView({
    row: { data: correction, permittedActions },
    units,
    products
  }: {
    row: InvoiceCorrectionWithPermittedActions
    units: Record<string, InvoiceDaycare | undefined>
    products: ProductWithName[]
  }) {
    const { i18n } = useTranslation()

    const productName = useMemo(
      () => products.find((p) => p.key === correction.product)?.nameFi,
      [products, correction.product]
    )

    const [noteEditorOpen, { on: openNote, off: closeNote }] = useBoolean(false)

    const statusText = i18n.personProfile.invoiceCorrections.invoiceStatus(
      correction.invoice?.status ?? null
    )

    return (
      <>
        {noteEditorOpen && (
          <NoteQuickEditor
            initialValue={correction.note}
            correctionId={correction.id}
            personId={correction.headOfFamilyId}
            closeNote={closeNote}
          />
        )}
        <Tr data-qa="invoice-details-invoice-row">
          <Td data-qa="target-month">
            {correction.targetMonth.month.toString().padStart(2, '0')} /{' '}
            {correction.targetMonth.year}
          </Td>
          <Td data-qa="product">{productName}</Td>
          <Td data-qa="description">{correction.description}</Td>
          <Td data-qa="unit">{units[correction.unitId]?.name ?? ''}</Td>
          <Td data-qa="period">{correction.period.format()}</Td>
          <Td data-qa="amount" align="right">
            {correction.amount}
          </Td>
          <Td data-qa="unit-price" align="right">
            <OneLine>{formatCents(correction.unitPrice, true)} €</OneLine>
          </Td>
          <Td data-qa="total-price" align="right">
            <OneLine>
              {formatCents(correction.amount * correction.unitPrice, true)} €
            </OneLine>
          </Td>
          <Td data-qa="status">
            {correction.invoice ? (
              <Link to={`/finance/invoices/${correction.invoice.id}`}>
                {statusText}
              </Link>
            ) : (
              <div>{statusText}</div>
            )}
          </Td>
          <Td>
            <FixedSpaceRow justifyContent="flex-end">
              {permittedActions.includes('DELETE') &&
                (correction.invoice === null ||
                  correction.invoice.status === 'DRAFT') && (
                  <ConfirmedMutation
                    buttonStyle="ICON"
                    icon={faTrash}
                    buttonAltText={i18n.common.remove}
                    confirmationTitle={
                      i18n.invoiceCorrections.deleteConfirmTitle
                    }
                    confirmLabel={i18n.common.remove}
                    mutation={deleteInvoiceCorrectionMutation}
                    onClick={() => ({
                      id: correction.id,
                      personId: correction.headOfFamilyId
                    })}
                    data-qa="delete-invoice-row-button"
                  />
                )}
              <Tooltip tooltip={correction.note} data-qa="note-tooltip">
                <IconOnlyButton
                  icon={
                    correction.note.trim() ? fasCommentAltLines : faCommentAlt
                  }
                  onClick={openNote}
                  aria-label={i18n.common.addNew}
                  data-qa="note-icon"
                />
              </Tooltip>
            </FixedSpaceRow>
          </Td>
        </Tr>
      </>
    )
  }
)

const correctionForm = object({
  targetMonth: localDate(),
  product: required(oneOf<string>()),
  description: required(value<string>()),
  unit: required(oneOf<string>()),
  period: required(localDateRange()),
  amount: transformed(value<string>(), (value) => {
    if (value.trim() === '') return ValidationError.of('required')

    if (!/^[0-9]+$/.test(value)) return ValidationError.of('format')

    const parsed = parseInt(value)
    if (isNaN(parsed) || parsed < 1) return ValidationError.of('generic')

    return ValidationSuccess.of(parsed)
  }),
  unitPrice: transformed(value<string>(), (value) => {
    const cents = parseCents(value)
    if (cents === undefined) return ValidationError.of('format')
    if (cents === 0) return ValidationError.of('required')

    return ValidationSuccess.of(cents)
  }),
  note: required(value<string>())
})

const InvoiceCorrectionEditModal = React.memo(
  function InvoiceCorrectionEditModal({
    personId,
    childId,
    row,
    units,
    products,
    onEditorClose
  }: {
    personId: UUID
    childId: UUID
    row: InvoiceCorrection | null
    units: Record<string, InvoiceDaycare | undefined>
    products: ProductWithName[]
    onEditorClose: () => void
  }) {
    const { i18n, lang } = useTranslation()
    const creatingNew = row === null

    const form = useForm(
      correctionForm,
      () => ({
        targetMonth: row
          ? localDate.fromDate(row.targetMonth.atDay(1))
          : localDate.empty(),
        product: {
          domValue: row?.product ?? '',
          options: products.map((p) => ({
            value: p.key,
            domValue: p.key,
            label: p.nameFi
          }))
        },
        description: row?.description ?? '',
        unit: {
          domValue: row?.unitId ?? '',
          options: Object.entries(units).map(([id, unit]) => ({
            value: id,
            domValue: id,
            label: unit?.name ?? ''
          }))
        },
        period: row
          ? localDateRange.fromRange(row.period)
          : localDateRange.empty(),
        amount: row?.amount?.toFixed(0) ?? '',
        unitPrice: row?.unitPrice?.toFixed(2) ?? '',
        note: row?.note ?? ''
      }),
      i18n.validationErrors
    )

    const {
      targetMonth,
      product,
      description,
      unit,
      period,
      amount,
      unitPrice,
      note
    } = useFormFields(form)

    const month = targetMonth.value()?.getYearMonth() ?? null

    return (
      <MutateFormModal
        title={
          creatingNew
            ? i18n.invoiceCorrections.addTitle
            : i18n.invoiceCorrections.editTitle
        }
        resolveMutation={createInvoiceCorrectionMutation}
        resolveDisabled={!form.isValid()}
        resolveAction={() => ({
          body: {
            headOfFamilyId: personId,
            childId,
            targetMonth: null, // backend fills for now
            product: product.value(),
            description: description.value(),
            unitId: unit.value(),
            period: period.value(),
            amount: amount.value(),
            unitPrice: unitPrice.value(),
            note: note.value()
          }
        })}
        resolveLabel={i18n.common.save}
        onSuccess={onEditorClose}
        rejectAction={onEditorClose}
        rejectLabel={i18n.common.cancel}
      >
        <FixedSpaceColumn spacing="m">
          <FixedSpaceColumn spacing="xs">
            <Label>{i18n.invoiceCorrections.targetMonth} *</Label>
            <div>{month?.format() ?? 'Seuraava laskutuskausi'}</div>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xs">
            <Label>{i18n.invoice.form.rows.product} *</Label>
            <SelectF
              bind={product}
              disabled={!creatingNew}
              placeholder={i18n.common.select}
              hideErrorsBeforeTouched
              data-qa="select-product"
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xs">
            <Label>{i18n.invoice.form.rows.description}</Label>
            <InputFieldF
              bind={description}
              readonly={!creatingNew}
              hideErrorsBeforeTouched
              data-qa="input-description"
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xs">
            <Label>{i18n.invoice.form.rows.unitId} *</Label>
            {/*TODO: ComboboxF?*/}
            <SelectF
              bind={unit}
              disabled={!creatingNew}
              placeholder={i18n.common.select}
              hideErrorsBeforeTouched
              data-qa="input-unit"
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xs">
            <Label>{i18n.invoice.form.rows.daterange} *</Label>
            <DateRangePickerF
              bind={period}
              locale={lang}
              hideErrorsBeforeTouched
              data-qa="date-range-input"
            />
          </FixedSpaceColumn>
          <FixedSpaceRow>
            <AmountColumn spacing="xs">
              <Label>{i18n.invoice.form.rows.amount} *</Label>
              <InputFieldF
                bind={amount}
                readonly={!creatingNew}
                type="numeric"
                step={1}
                hideErrorsBeforeTouched
                data-qa="input-amount"
              />
            </AmountColumn>
            <OperatorColumn>x</OperatorColumn>
            <UnitPriceColumn spacing="xs">
              <Label>{i18n.invoice.form.rows.unitPrice} *</Label>
              <InputFieldF
                bind={unitPrice}
                readonly={!creatingNew}
                hideErrorsBeforeTouched
                symbol="€"
                data-qa="input-price"
              />
            </UnitPriceColumn>
            <OperatorColumn>=</OperatorColumn>
            <FixedSpaceColumn spacing="xs">
              <Label>{i18n.invoice.form.rows.price}</Label>
              <SumDiv data-qa="total-price">
                {amount.isValid() && unitPrice.isValid()
                  ? `${formatCents(amount.value() * unitPrice.value())} €`
                  : ''}
              </SumDiv>
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <FixedSpaceColumn spacing="xs">
            <Label>
              {i18n.personProfile.invoiceCorrections.noteModalTitle}
            </Label>
            <TextAreaF
              bind={note}
              hideErrorsBeforeTouched
              data-qa="input-note"
            />
          </FixedSpaceColumn>
        </FixedSpaceColumn>
      </MutateFormModal>
    )
  }
)

const NoteQuickEditor = React.memo(function NoteQuickEditor({
  initialValue,
  correctionId,
  personId,
  closeNote
}: {
  initialValue: string
  correctionId: UUID
  personId: UUID
  closeNote: () => void
}) {
  const { i18n } = useTranslation()
  const [note, setNote] = useState(initialValue)

  return (
    <MutateFormModal
      title={i18n.personProfile.invoiceCorrections.noteModalTitle}
      resolveMutation={updateInvoiceCorrectionNoteMutation}
      resolveAction={() => ({ id: correctionId, personId, body: { note } })}
      resolveLabel={i18n.common.save}
      onSuccess={closeNote}
      rejectAction={closeNote}
      rejectLabel={i18n.common.cancel}
    >
      <P noMargin centered>
        {i18n.personProfile.invoiceCorrections.noteModalInfo}
      </P>
      <Gap size="s" />
      <TextArea value={note} onChange={setNote} data-qa="note-textarea" />
    </MutateFormModal>
  )
})

const ChildNameLink = styled(Link)`
  width: fit-content;

  ${H4} {
    color: ${(p) => p.theme.colors.main.m2};
  }
`

const AmountColumn = styled(FixedSpaceColumn)`
  width: 80px;
`

const UnitPriceColumn = styled(FixedSpaceColumn)`
  width: 120px;
`

const OperatorColumn = styled.div`
  padding-top: 36px;
`

const SumDiv = styled.div`
  padding-top: 8px;
`

const OneLine = styled.span`
  white-space: nowrap;
  overflow: hidden;
`
