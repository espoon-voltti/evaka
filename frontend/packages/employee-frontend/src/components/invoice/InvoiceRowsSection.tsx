// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useState } from 'react'
import { Link } from 'react-router-dom'
import { faAbacus, faCoins } from '@evaka/icons'
import styled from 'styled-components'
import { groupBy, get } from 'lodash/fp'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Button, Collapsible, Table, Title } from '~components/shared/alpha'
import InvoiceRowsSectionRow from './InvoiceRowsSectionRow'
import Sum from './Sum'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '~state/ui'
import {
  InvoiceRowDetailed,
  InvoiceCodes,
  PersonDetailed
} from '../../types/invoicing'
import { Result } from '../../api'
import { totalPrice } from '../../utils/pricing'
import AbsencesModal from './AbsencesModal'
import { formatName } from '~utils'

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
  amount: 1,
  unitPrice: 0,
  price: 0
})

interface Props {
  rows: InvoiceRowDetailed[]
  updateRows: (rows: InvoiceRowDetailed[]) => void
  invoiceCodes: Result<InvoiceCodes>
  editable: boolean
}

const InvoiceRowsSection = React.memo(function InvoiceRowsSection({
  rows,
  updateRows,
  invoiceCodes,
  editable
}: Props) {
  const { i18n } = useTranslation()
  const [toggled, setToggled] = useState(true)
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
      <Collapsible
        title={i18n.invoice.form.rows.title}
        icon={faCoins}
        open={toggled}
        onToggle={() => setToggled((prev) => !prev)}
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
                  <Button
                    plain
                    icon={faAbacus}
                    iconSize="lg"
                    onClick={() =>
                      openAbsences(firstRow.child, firstRow.periodStart)
                    }
                  >
                    {i18n.invoice.openAbsenceSummary}
                  </Button>
                </TitleContainer>
                <Table.Table dataQa="table-of-invoice-rows">
                  <Table.Head>
                    <Table.Row>
                      <Table.Th dataQa="invoice-row-product">
                        {i18n.invoice.form.rows.product}
                      </Table.Th>
                      <Table.Th dataQa="invoice-row-description">
                        {i18n.invoice.form.rows.description}
                      </Table.Th>
                      <Table.Th dataQa="invoice-row-costcenter">
                        {i18n.invoice.form.rows.costCenter}
                      </Table.Th>
                      <Table.Th dataQa="invoice-row-subcostcenter">
                        {i18n.invoice.form.rows.subCostCenter}
                      </Table.Th>
                      <Table.Th dataQa="invoice-row-daterange">
                        {i18n.invoice.form.rows.daterange}
                      </Table.Th>
                      <Table.Th dataQa="invoice-row-amount">
                        {i18n.invoice.form.rows.amount}
                      </Table.Th>
                      <Table.Th align="right" dataQa="invoice-row-unitprice">
                        {i18n.invoice.form.rows.unitPrice}
                      </Table.Th>
                      <Table.Th align="right" dataQa="invoice-row-totalprice">
                        {i18n.invoice.form.rows.price}
                      </Table.Th>
                      <Table.Th />
                    </Table.Row>
                  </Table.Head>
                  <Table.Body>
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
                  </Table.Body>
                </Table.Table>
                <Button
                  plain
                  disabled={!editable}
                  onClick={getAddInvoiceRow(firstRow)}
                  dataQa="invoice-button-add-row"
                >
                  {i18n.invoice.form.rows.addRow}
                </Button>
                <Sum title={'rowSubTotal'} sum={totalPrice(rows)} />
              </div>
            )
          })}
        </div>
      </Collapsible>
      {uiMode == 'invoices-absence-modal' && child !== undefined && (
        <AbsencesModal child={child} date={absenceModalDate} />
      )}
    </Fragment>
  )
})

export default InvoiceRowsSection
