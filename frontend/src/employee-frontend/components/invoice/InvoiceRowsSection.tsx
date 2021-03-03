// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useState } from 'react'
import { Link } from 'react-router-dom'
import { groupBy, get } from 'lodash/fp'
import styled from 'styled-components'

import { faAbacus, faCoins } from '@evaka/lib-icons'
import LocalDate from '@evaka/lib-common/local-date'
import {
  Table,
  Tbody,
  Th,
  Thead,
  Tr
} from '@evaka/lib-components/layout/Table'
import InlineButton from '@evaka/lib-components/atoms/buttons/InlineButton'
import Title from '@evaka/lib-components/atoms/Title'
import CollapsibleSection from '@evaka/lib-components/molecules/CollapsibleSection'
import InvoiceRowsSectionRow from './InvoiceRowsSectionRow'
import Sum from './Sum'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import {
  InvoiceRowDetailed,
  InvoiceCodes,
  PersonDetailed
} from '../../types/invoicing'
import { Result } from '@evaka/lib-common/api'
import { totalPrice } from '../../utils/pricing'
import AbsencesModal from './AbsencesModal'
import { formatName } from '../../utils'

const TitleContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: baseline;
`

const updateInvoiceRow = (
  update: (rows: InvoiceRowDetailed[]) => void,
  invoiceRows: InvoiceRowDetailed[],
  invoiceRow: InvoiceRowDetailed
) => (value: Partial<InvoiceRowDetailed>) =>
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
  costCenter: '',
  subCostCenter: '',
  amount: 0,
  unitPrice: 0,
  price: 0
})

interface Props {
  rows: InvoiceRowDetailed[]
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

const CostCenterTh = styled(Th)`
  width: 110px;
`

const AmountTh = styled(Th)`
  width: 110px;
`

const UnitPriceTh = styled(Th)`
  width: 110px;
`

const TotalPriceTh = styled(Th)`
  width: 110px;
`

export default React.memo(function InvoiceRowsSection({
  rows,
  updateRows,
  invoiceCodes,
  editable
}: Props) {
  const { i18n } = useTranslation()
  const [child, setChild] = useState<PersonDetailed>()
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const [absenceModalDate, setAbsenceModalDate] = useState<LocalDate>(
    LocalDate.today()
  )

  const openAbsences = (child: PersonDetailed, date: LocalDate) => {
    setChild(child)
    setAbsenceModalDate(date)
    toggleUiMode('invoices-absence-modal')
  }

  const getAddInvoiceRow = (invoiceRow: InvoiceRowDetailed) => () =>
    updateRows([...rows, emptyInvoiceRow(invoiceRow)])

  const groupedRows = groupBy(get('child.id'), rows)

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
                      <CostCenterTh data-qa="invoice-row-costcenter">
                        {i18n.invoice.form.rows.costCenter}
                      </CostCenterTh>
                      <Th data-qa="invoice-row-subcostcenter">
                        {i18n.invoice.form.rows.subCostCenter}
                      </Th>
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
                      remove={() =>
                        updateRows(rows.filter(({ id }) => id !== firstRow.id))
                      }
                      invoiceCodes={invoiceCodes}
                      editable={editable}
                    />
                    {otherRows.map((row, index) => (
                      <InvoiceRowsSectionRow
                        key={index}
                        row={row}
                        update={updateInvoiceRow(updateRows, rows, row)}
                        remove={() =>
                          updateRows(rows.filter(({ id }) => id !== row.id))
                        }
                        invoiceCodes={invoiceCodes}
                        editable={editable}
                      />
                    ))}
                  </Tbody>
                </InvoiceRowsTable>
                <InlineButton
                  disabled={!editable}
                  onClick={getAddInvoiceRow(firstRow)}
                  dataQa="invoice-button-add-row"
                  text={i18n.invoice.form.rows.addRow}
                />
                <Sum title={'rowSubTotal'} sum={totalPrice(rows)} />
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
