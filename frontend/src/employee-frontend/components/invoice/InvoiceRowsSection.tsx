// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { groupBy, get } from 'lodash/fp'
import React, { Fragment, useContext, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import {
  PersonDetailed,
  InvoiceCodes,
  InvoiceRowDetailed
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import Title from 'lib-components/atoms/Title'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { faAbacus, faCoins } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { formatName } from '../../utils'
import { totalPrice } from '../../utils/pricing'

import AbsencesModal from './AbsencesModal'
import InvoiceRowsSectionRow from './InvoiceRowsSectionRow'
import Sum from './Sum'

const TitleContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: baseline;
`

const updateInvoiceRow =
  (
    update: (rows: InvoiceRowDetailed[]) => void,
    invoiceRows: InvoiceRowDetailed[],
    invoiceRow: InvoiceRowDetailed
  ) =>
  (value: Partial<InvoiceRowDetailed>) =>
    update(
      invoiceRows.map((row) =>
        row === invoiceRow ? { ...invoiceRow, ...value } : row
      )
    )

const emptyInvoiceRow = (
  invoiceRow: InvoiceRowDetailed
): InvoiceRowDetailed => ({
  id: '',
  child: invoiceRow.child,
  periodStart: invoiceRow.periodStart,
  periodEnd: invoiceRow.periodEnd,
  product: 'DAYCARE' as const,
  description: '',
  unitId: '',
  unitName: '',
  unitProviderType: 'MUNICIPAL',
  daycareType: [],
  costCenter: '',
  subCostCenter: '',
  savedCostCenter: null,
  amount: 0,
  unitPrice: 0,
  price: 0,
  correctionId: null,
  note: null
})

interface Props {
  rows: InvoiceRowDetailed[]
  permittedActions: Action.Invoice[]
  updateRows: (rows: InvoiceRowDetailed[]) => void
  invoiceCodes: Result<InvoiceCodes>
  editable: boolean
}

const InvoiceRowsTable = styled(Table)`
  border-collapse: collapse;
  margin-bottom: 15px;
  td {
    padding: 16px 0 24px 16px;
    vertical-align: bottom;
  }
  td:nth-child(1) {
    padding-left: 0;
  }
`

const UnitTh = styled(Th)`
  width: 240px;
`

const AmountTh = styled(Th)`
  width: 60px;
`

const UnitPriceTh = styled(Th)`
  width: 96px;
`

const TotalPriceTh = styled(Th)`
  width: 110px;
`

export default React.memo(function InvoiceRowsSection({
  rows,
  permittedActions,
  updateRows,
  invoiceCodes,
  editable
}: Props) {
  const { i18n } = useTranslation()
  const [child, setChild] = useState<PersonDetailed>()
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const [absenceModalDate, setAbsenceModalDate] = useState<LocalDate>(
    LocalDate.todayInSystemTz()
  )

  const openAbsences = (child: PersonDetailed, date: LocalDate) => {
    setChild(child)
    setAbsenceModalDate(date)
    toggleUiMode('invoices-absence-modal')
  }

  const getAddInvoiceRow = (invoiceRow: InvoiceRowDetailed) => () =>
    updateRows([...rows, emptyInvoiceRow(invoiceRow)])

  const groupedRows = groupBy(get('child.id'), rows)

  const products = useMemo(
    () => invoiceCodes.map(({ products }) => products).getOrElse([]),
    [invoiceCodes]
  )
  const unitIds = useMemo(
    () =>
      invoiceCodes.map(({ units }) => units.map(({ id }) => id)).getOrElse([]),
    [invoiceCodes]
  )
  const unitDetails = useMemo(
    () =>
      invoiceCodes
        .map(({ units }) =>
          Object.fromEntries(units.map((unit) => [unit.id, unit]))
        )
        .getOrElse({}),
    [invoiceCodes]
  )

  return (
    <Fragment>
      <CollapsibleSection
        title={i18n.invoice.form.rows.title}
        icon={faCoins}
        startCollapsed={false}
      >
        <div className="invoice-rows">
          {Object.entries(groupedRows).map(([childId, childRows]) => {
            const [firstRow, ...otherRows] = childRows
            return (
              <div key={childId}>
                <TitleContainer>
                  <Title size={4}>
                    <Link to={`/child-information/${childId}`}>
                      {formatName(
                        firstRow.child.firstName,
                        firstRow.child.lastName,
                        i18n,
                        true
                      )}{' '}
                      {firstRow.child.ssn}
                    </Link>
                  </Title>
                  <InlineButton
                    icon={faAbacus}
                    onClick={() =>
                      openAbsences(firstRow.child, firstRow.periodStart)
                    }
                    text={i18n.invoice.openAbsenceSummary}
                  />
                </TitleContainer>
                <InvoiceRowsTable data-qa="table-of-invoice-rows">
                  <Thead>
                    <Tr>
                      <Th data-qa="invoice-row-product">
                        {i18n.invoice.form.rows.product}
                      </Th>
                      <Th data-qa="invoice-row-description">
                        {i18n.invoice.form.rows.description}
                      </Th>
                      <UnitTh data-qa="invoice-row-unit">
                        {i18n.invoice.form.rows.unitId}
                      </UnitTh>
                      <Th data-qa="invoice-row-daterange">
                        {i18n.invoice.form.rows.daterange}
                      </Th>
                      <AmountTh data-qa="invoice-row-amount">
                        {i18n.invoice.form.rows.amount}
                      </AmountTh>
                      <UnitPriceTh
                        align="right"
                        data-qa="invoice-row-unitprice"
                      >
                        {i18n.invoice.form.rows.unitPrice}
                      </UnitPriceTh>
                      <TotalPriceTh
                        align="right"
                        data-qa="invoice-row-totalprice"
                      >
                        {i18n.invoice.form.rows.price}
                      </TotalPriceTh>
                      <Th />
                    </Tr>
                  </Thead>
                  <Tbody>
                    <InvoiceRowsSectionRow
                      key={firstRow.id || ''}
                      row={firstRow}
                      update={updateInvoiceRow(updateRows, rows, firstRow)}
                      remove={
                        editable
                          ? () =>
                              updateRows(
                                rows.filter(({ id }) => id !== firstRow.id)
                              )
                          : undefined
                      }
                      products={products}
                      unitIds={unitIds}
                      unitDetails={unitDetails}
                      editable={editable && permittedActions.includes('UPDATE')}
                      deletable={permittedActions.includes('DELETE')}
                    />
                    {otherRows.map((row, index) => (
                      <InvoiceRowsSectionRow
                        key={index}
                        row={row}
                        update={updateInvoiceRow(updateRows, rows, row)}
                        remove={
                          editable
                            ? () =>
                                updateRows(
                                  rows.filter(({ id }) => id !== row.id)
                                )
                            : undefined
                        }
                        products={products}
                        unitIds={unitIds}
                        unitDetails={unitDetails}
                        editable={
                          editable && permittedActions.includes('UPDATE')
                        }
                        deletable={permittedActions.includes('DELETE')}
                      />
                    ))}
                  </Tbody>
                </InvoiceRowsTable>
                {permittedActions.includes('UPDATE') && (
                  <InlineButton
                    disabled={!editable}
                    onClick={getAddInvoiceRow(firstRow)}
                    data-qa="invoice-button-add-row"
                    text={i18n.invoice.form.rows.addRow}
                  />
                )}
                <Sum title="rowSubTotal" sum={totalPrice(childRows)} />
              </div>
            )
          })}
        </div>
      </CollapsibleSection>
      {uiMode == 'invoices-absence-modal' && child !== undefined && (
        <AbsencesModal child={child} date={absenceModalDate} />
      )}
    </Fragment>
  )
})
