// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import mapValues from 'lodash/mapValues'
import sumBy from 'lodash/sumBy'
import React, { Fragment, useContext, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import {
  InvoiceCodes,
  InvoiceRowDetailed,
  PersonDetailed
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
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

interface Props {
  rows: InvoiceRowDetailed[]
  replacedRows: InvoiceRowDetailed[] | undefined
  invoiceCodes: InvoiceCodes
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
  replacedRows,
  invoiceCodes
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

  const childRows = groupBy(rows, (row) => row.child.id)

  const replacedChildSums: Partial<Record<UUID, number>> | undefined =
    useMemo(() => {
      if (!replacedRows) return undefined
      return mapValues(
        groupBy(replacedRows, (row) => row.child.id),
        (rows) => sumBy(rows, (row) => row.amount * row.unitPrice)
      )
    }, [replacedRows])

  const unitIds = useMemo(
    () => invoiceCodes.units.map((unit) => unit.id),
    [invoiceCodes]
  )
  const unitDetails = useMemo(
    () => Object.fromEntries(invoiceCodes.units.map((unit) => [unit.id, unit])),
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
          {Object.entries(childRows).map(([childId, childRows]) => {
            const firstRow = childRows[0]
            const previousSum =
              replacedChildSums !== undefined
                ? replacedChildSums[childId] ?? 0
                : undefined
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
                    appearance="inline"
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
                    {childRows.map((row, index) => (
                      <InvoiceRowsSectionRow
                        key={index}
                        row={row}
                        products={invoiceCodes.products}
                        unitIds={unitIds}
                        unitDetails={unitDetails}
                      />
                    ))}
                  </Tbody>
                </InvoiceRowsTable>
                <Sum
                  title="rowSubTotal"
                  sum={totalPrice(childRows)}
                  previousSum={previousSum}
                />
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
