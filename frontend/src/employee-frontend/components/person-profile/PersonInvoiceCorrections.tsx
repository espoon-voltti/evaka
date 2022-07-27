// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import uniqBy from 'lodash/uniqBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { formatPersonName } from 'employee-frontend/utils'
import { combine, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { UpdateStateFn } from 'lib-common/form-state'
import {
  InvoiceCorrection,
  InvoiceDaycare,
  ProductWithName
} from 'lib-common/generated/api-types/invoicing'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import { InlineAsyncButton } from 'lib-components/employee/notes/InlineAsyncButton'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H4, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faChild } from 'lib-icons'

import {
  createInvoiceCorrection,
  deleteInvoiceCorrection,
  getInvoiceCodes,
  getPersonInvoiceCorrections,
  updateInvoiceCorrectionNote
} from '../../api/invoicing'
import { Translations, useTranslation } from '../../state/i18n'
import { PersonContext } from '../../state/person'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import InvoiceRowSectionRow from '../invoice/InvoiceRowsSectionRow'

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
  const [corrections, reloadCorrections] = useApiState(
    () => getPersonInvoiceCorrections(id),
    [id]
  )
  const { editState, updateState, cancelEditing, addNewRow } =
    useCorrectionEditState(id)
  const [noteModalState, setNoteModalState] = useState<{
    id: UUID
    note: string
  }>()

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
        const pairs: [string, InvoiceCorrection[]][] = children.map((child) => [
          child.id,
          corrections.filter((correction) => correction.childId === child.id)
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

  const save = useMemo(() => {
    if (!editState) return undefined

    const unitId = editState.unitId
    if (!unitId) return undefined
    if (editState.periodStart.isAfter(editState.periodEnd)) return undefined

    return () =>
      createInvoiceCorrection({
        ...editState,
        headOfFamilyId: id,
        unitId,
        period: new FiniteDateRange(editState.periodStart, editState.periodEnd)
      })
  }, [id, editState])

  const onSaveSuccess = useCallback(() => {
    cancelEditing()
    reloadCorrections()
  }, [cancelEditing, reloadCorrections])

  const deleteCorrection = useCallback(
    (id: UUID) => void deleteInvoiceCorrection(id).then(reloadCorrections),
    [reloadCorrections]
  )

  const editNote = useCallback(
    (id: UUID, note: string) => setNoteModalState({ id, note }),
    []
  )

  const saveNote = useCallback(() => {
    if (!noteModalState) return
    if (noteModalState.id === 'new') {
      updateState({ note: noteModalState.note })
      return
    } else {
      return updateInvoiceCorrectionNote(noteModalState.id, noteModalState.note)
    }
  }, [noteModalState, updateState])

  const onNoteUpdateSuccess = useCallback(() => {
    setNoteModalState(undefined)
    if (noteModalState && noteModalState.id !== 'new') reloadCorrections()
  }, [noteModalState, reloadCorrections])

  return (
    <CollapsibleSection
      icon={faChild}
      title={i18n.personProfile.invoiceCorrections.title}
      data-qa="person-invoice-corrections-collapsible"
      startCollapsed={!open}
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
                  i18n={i18n}
                  child={child}
                  corrections={groupedCorrections[child.id] ?? []}
                  products={products}
                  unitIds={unitIds}
                  unitDetails={unitDetails}
                  editState={editState}
                  updateState={updateState}
                  cancelEditing={cancelEditing}
                  save={save}
                  onSaveSuccess={onSaveSuccess}
                  addNewRow={addNewRow}
                  deleteCorrection={deleteCorrection}
                  editNote={editNote}
                />
              ))
            )}
          </FixedSpaceColumn>
        )
      )}
      {noteModalState ? (
        <AsyncFormModal
          title={i18n.personProfile.invoiceCorrections.noteModalTitle}
          resolveAction={saveNote}
          resolveLabel={i18n.common.save}
          onSuccess={onNoteUpdateSuccess}
          rejectAction={() => setNoteModalState(undefined)}
          rejectLabel={i18n.common.cancel}
        >
          <P noMargin centered>
            {i18n.personProfile.invoiceCorrections.noteModalInfo}
          </P>
          <Gap size="s" />
          <TextArea
            value={noteModalState.note}
            onChange={(note) =>
              setNoteModalState((previous) => previous && { ...previous, note })
            }
          />
        </AsyncFormModal>
      ) : null}
    </CollapsibleSection>
  )
})

const ChildSection = React.memo(function ChildSection({
  i18n,
  child,
  corrections,
  products,
  unitIds,
  unitDetails,
  editState,
  updateState,
  cancelEditing,
  save,
  onSaveSuccess,
  addNewRow,
  deleteCorrection,
  editNote
}: {
  i18n: Translations
  child: PersonJSON
  corrections: InvoiceCorrection[]
  products: ProductWithName[]
  unitIds: UUID[]
  unitDetails: Record<UUID, InvoiceDaycare>
  editState: InvoiceCorrectionForm | undefined
  updateState: UpdateState
  cancelEditing: () => void
  save: (() => Promise<Result<void>>) | undefined
  onSaveSuccess: () => void
  addNewRow: (childId: UUID) => void
  deleteCorrection: (id: UUID) => void
  editNote: (id: UUID, note: string) => void
}) {
  return (
    <FixedSpaceColumn spacing="s">
      <ChildNameLink to={`/child-information/${child.id}`}>
        <H4 noMargin>{formatPersonName(child, i18n)}</H4>
      </ChildNameLink>
      <Table>
        <Thead>
          <Tr>
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
          {corrections.map((correction) => (
            <InvoiceRowSectionRow
              key={correction.id}
              row={{
                ...correction,
                periodStart: correction.period.start,
                periodEnd: correction.period.end,
                savedCostCenter: null,
                price: correction.amount * correction.unitPrice
              }}
              products={products}
              unitIds={unitIds}
              unitDetails={unitDetails}
              editable={false}
              update={() => undefined}
              remove={
                correction.invoiceStatus && correction.invoiceStatus !== 'DRAFT'
                  ? undefined
                  : () => deleteCorrection(correction.id)
              }
              addNote={() => editNote(correction.id, correction.note)}
              status={
                correction.invoiceId ? (
                  <Link to={`/finance/invoices/${correction.invoiceId}`}>
                    {i18n.personProfile.invoiceCorrections.invoiceStatus(
                      correction.invoiceStatus
                    )}
                  </Link>
                ) : (
                  <span>
                    {i18n.personProfile.invoiceCorrections.invoiceStatus(
                      correction.invoiceStatus
                    )}
                  </span>
                )
              }
            />
          ))}
          {editState?.childId === child.id && (
            <InvoiceRowSectionRow
              row={editState}
              products={products}
              unitIds={unitIds}
              unitDetails={unitDetails}
              editable={true}
              update={updateState}
              remove={undefined}
              addNote={() => editNote(editState.id, editState.note)}
              status={<span />}
            />
          )}
        </Tbody>
      </Table>
      <FlexRow justifyContent="space-between">
        <AddButton
          onClick={() => addNewRow(child.id)}
          text={i18n.invoiceCorrections.addRow}
          disabled={!!editState}
        />
        {editState?.childId === child.id && (
          <FixedSpaceRow spacing="L">
            <InlineButton text={i18n.common.cancel} onClick={cancelEditing} />
            <InlineAsyncButton
              text={i18n.common.save}
              onClick={save ?? (() => Promise.resolve(Success.of()))}
              onSuccess={onSaveSuccess}
              disabled={!save}
            />
          </FixedSpaceRow>
        )}
      </FlexRow>
    </FixedSpaceColumn>
  )
})

const ChildNameLink = styled(Link)`
  width: fit-content;

  ${H4} {
    color: ${(p) => p.theme.colors.main.m2};
  }
`

function useCorrectionEditState(headOfChildId: UUID) {
  const [state, setState] = useState<InvoiceCorrectionForm>()

  const cancelEditing = useCallback(() => setState(undefined), [])

  const addNewRow = useCallback(
    (childId: UUID) =>
      setState({
        id: 'new',
        headOfChildId,
        childId,
        unitId: null,
        product: 'DAYCARE',
        periodStart: LocalDate.todayInSystemTz().subMonths(1).startOfMonth(),
        periodEnd: LocalDate.todayInSystemTz().startOfMonth().subDays(1),
        amount: 0,
        unitPrice: 0,
        price: 0,
        description: '',
        note: '',
        savedCostCenter: null
      }),
    [headOfChildId]
  )

  const updateState: UpdateState = useCallback(
    (values) =>
      setState((previous) =>
        previous
          ? {
              ...previous,
              ...values
            }
          : undefined
      ),
    []
  )

  return {
    editState: state,
    updateState,
    cancelEditing,
    addNewRow
  }
}

interface InvoiceCorrectionForm {
  id: 'new' | UUID
  headOfChildId: UUID
  childId: UUID
  unitId: UUID | null
  product: string
  periodStart: LocalDate
  periodEnd: LocalDate
  amount: number
  unitPrice: number
  price: number
  description: string
  note: string
  savedCostCenter: null
}

type UpdateState = UpdateStateFn<
  Exclude<InvoiceCorrectionForm, 'id' | 'headOfChildId' | 'childId'>
>
