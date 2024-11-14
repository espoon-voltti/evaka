// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import sumBy from 'lodash/sumBy'
import uniq from 'lodash/uniq'
import React, { Fragment, useContext, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import {
  InvoiceCodes,
  InvoiceRowDetailed,
  PersonDetailed
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
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

  th,
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

  const rowsByChild: Partial<Record<string, InvoiceRowDetailed[]>> = useMemo(
    () => groupBy(rows, (row) => row.child.id),
    [rows]
  )

  const replacedRowsByChild: Partial<Record<string, InvoiceRowDetailed[]>> =
    useMemo(() => groupBy(replacedRows, (row) => row.child.id), [replacedRows])

  const allChildIds = useMemo(
    () =>
      uniq([
        ...Object.keys(rowsByChild),
        ...(replacedRowsByChild !== undefined
          ? Object.keys(replacedRowsByChild)
          : [])
      ]),
    [rowsByChild, replacedRowsByChild]
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
        data-qa="invoice-rows"
      >
        {allChildIds.map((childId) => {
          const rows = rowsByChild[childId]
          const replacedRows = replacedRowsByChild?.[childId]

          const firstRow = rows?.[0] ?? replacedRows?.[0]
          if (firstRow === undefined) {
            // Does not happen: we have either rows or replacedRows
            return null
          }

          const sum = rows !== undefined ? totalPrice(rows) : 0
          const previousSum =
            replacedRows !== undefined
              ? sumBy(replacedRows, (row) => row.amount * row.price)
              : undefined

          return (
            <div key={childId} data-qa={`child-${childId}`}>
              <TitleContainer>
                <Title size={4}>
                  <Link to={`/child-information/${childId}`}>
                    <span data-qa="child-name">
                      {formatName(
                        firstRow.child.firstName,
                        firstRow.child.lastName,
                        i18n,
                        true
                      )}
                    </span>{' '}
                    <span data-qa="child-ssn">{firstRow.child.ssn}</span>
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
              {rows !== undefined ? (
                <InvoiceRowsTable data-qa="table-of-invoice-rows">
                  <Thead>
                    <Tr>
                      <Th>{i18n.invoice.form.rows.product}</Th>
                      <Th>{i18n.invoice.form.rows.description}</Th>
                      <UnitTh>{i18n.invoice.form.rows.unitId}</UnitTh>
                      <Th>{i18n.invoice.form.rows.daterange}</Th>
                      <AmountTh>{i18n.invoice.form.rows.amount}</AmountTh>
                      <UnitPriceTh align="right">
                        {i18n.invoice.form.rows.unitPrice}
                      </UnitPriceTh>
                      <TotalPriceTh align="right">
                        {i18n.invoice.form.rows.price}
                      </TotalPriceTh>
                      <Th />
                    </Tr>
                  </Thead>
                  <Tbody>
                    {rows.map((row, index) => (
                      <InvoiceRowsSectionRow
                        key={index}
                        row={row}
                        products={invoiceCodes.products}
                        unitDetails={unitDetails}
                      />
                    ))}
                  </Tbody>
                </InvoiceRowsTable>
              ) : null}
              <Sum
                title="rowSubTotal"
                sum={sum}
                previousSum={previousSum}
                data-qa="child-sum"
              />
            </div>
          )
        })}
      </CollapsibleSection>
      {uiMode == 'invoices-absence-modal' && child !== undefined && (
        <AbsencesModal child={child} date={absenceModalDate} />
      )}
    </Fragment>
  )
})
