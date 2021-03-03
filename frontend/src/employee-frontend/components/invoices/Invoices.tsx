// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { useHistory } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faExclamation, faSync } from '@evaka/lib-icons'
import { Gap } from '@evaka/lib-components/src/white-space'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import {
  Table,
  Tr,
  Th,
  Td,
  Thead,
  Tbody,
  SortableTh
} from '@evaka/lib-components/src/layout/Table'
import Title from '@evaka/lib-components/src/atoms/Title'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { useTranslation } from '../../state/i18n'
import NameWithSsn from '../common/NameWithSsn'
import ChildrenCell from '../common/ChildrenCell'
import { InvoiceSummary } from '../../types/invoicing'
import { Result } from '@evaka/lib-common/src/api'
import { formatCents } from '../../utils/money'
import Tooltip from '../../components/common/Tooltip'
import { StatusIconContainer } from '../../components/common/StatusIconContainer'
import colors from '@evaka/lib-components/src/colors'
import { SearchOrder } from '../../types'
import { SortByInvoices } from '../../api/invoicing'
import Pagination from '@evaka/lib-components/src/Pagination'
import { InvoicesActions } from './invoices-state'

interface Props {
  actions: InvoicesActions
  invoices?: Result<InvoiceSummary[]>
  refreshInvoices: () => Promise<void>
  total?: number
  pages?: number
  currentPage: number
  sortBy: SortByInvoices
  sortDirection: SearchOrder
  showCheckboxes: boolean
  checked: Record<string, true>
  allInvoicesToggle: boolean
  allInvoicesToggleDisabled: boolean
}

export default React.memo(function Invoices({
  actions,
  invoices,
  refreshInvoices,
  total,
  pages,
  currentPage,
  sortBy,
  sortDirection,
  showCheckboxes,
  checked,
  allInvoicesToggle,
  allInvoicesToggleDisabled
}: Props) {
  const { i18n } = useTranslation()
  const [refreshError, setRefreshError] = useState(false)

  return (
    <div className="invoices">
      <RefreshInvoices>
        {refreshError ? (
          <RefreshError>{i18n.common.error.unknown}</RefreshError>
        ) : null}
        <InlineButton
          icon={faSync}
          onClick={() => {
            setRefreshError(false)
            refreshInvoices().catch(() => setRefreshError(true))
          }}
          text={i18n.invoices.buttons.createInvoices}
          dataQa="create-invoices"
        />
      </RefreshInvoices>
      <TitleRowContainer>
        <SectionTitle size={1}>{i18n.invoices.table.title}</SectionTitle>
        {invoices?.isSuccess && (
          <ResultsContainer>
            <div>{total ? i18n.feeDecisions.table.rowCount(total) : null}</div>
            <Pagination
              pages={pages}
              currentPage={currentPage}
              setPage={actions.setPage}
              label={i18n.common.page}
            />
          </ResultsContainer>
        )}
      </TitleRowContainer>
      {invoices?.isLoading ? <Loader /> : null}
      {!invoices || invoices.isFailure ? (
        <div>{i18n.common.error.unknown}</div>
      ) : null}
      {invoices?.isSuccess && (
        <>
          <ResultsContainer>
            <Checkbox
              checked={allInvoicesToggle}
              label={i18n.invoices.table.toggleAll}
              onChange={actions.allInvoicesToggle}
              disabled={allInvoicesToggleDisabled}
            />
          </ResultsContainer>
          <Gap size="m" />
          <Table data-qa="table-of-invoices">
            <InvoiceTableHeader
              actions={actions}
              invoices={invoices}
              checked={checked}
              sortBy={sortBy}
              sortDirection={sortDirection}
              showCheckboxes={showCheckboxes}
              allInvoicesToggle={allInvoicesToggle}
            />
            <InvoiceTableBody
              actions={actions}
              invoices={invoices.value}
              showCheckboxes={showCheckboxes}
              checked={checked}
              allInvoicesToggle={allInvoicesToggle}
            />
          </Table>
          <ResultsContainer>
            <Pagination
              pages={pages}
              currentPage={currentPage}
              setPage={actions.setPage}
              label={i18n.common.page}
            />
          </ResultsContainer>
        </>
      )}
    </div>
  )
})

const RefreshInvoices = styled.div`
  position: absolute;
  top: -30px;
  right: 60px;
`

const RefreshError = styled.span`
  margin-right: 20px;
  font-size: 14px;
`

const TitleRowContainer = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 1.5rem;
`

const ResultsContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-end;
`

const SectionTitle = styled(Title)`
  margin-bottom: 0 !important;
`

const InvoiceTableHeader = React.memo(function InvoiceTableHeader({
  actions,
  invoices,
  checked,
  sortBy,
  sortDirection,
  showCheckboxes,
  allInvoicesToggle
}: Pick<
  Props,
  | 'actions'
  | 'invoices'
  | 'checked'
  | 'sortBy'
  | 'sortDirection'
  | 'showCheckboxes'
  | 'allInvoicesToggle'
>) {
  const { i18n } = useTranslation()

  const allChecked =
    invoices
      ?.map((is) => is.length > 0 && is.every((i) => checked[i.id]))
      .getOrElse(false) ?? false

  const isSorted = (column: SortByInvoices) =>
    sortBy === column ? sortDirection : undefined

  const toggleSort = (column: SortByInvoices) => () => {
    if (sortBy === column) {
      actions.setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
    } else {
      actions.setSortBy(column)
      actions.setSortDirection('ASC')
    }
  }

  return (
    <Thead>
      <Tr>
        <SortableTh
          sorted={isSorted('HEAD_OF_FAMILY')}
          onClick={toggleSort('HEAD_OF_FAMILY')}
        >
          {i18n.invoices.table.head}
        </SortableTh>
        <SortableTh
          sorted={isSorted('CHILDREN')}
          onClick={toggleSort('CHILDREN')}
        >
          {i18n.invoices.table.children}
        </SortableTh>
        <SortableTh sorted={isSorted('START')} onClick={toggleSort('START')}>
          {i18n.invoices.table.period}
        </SortableTh>
        <SortableTh sorted={isSorted('SUM')} onClick={toggleSort('SUM')}>
          {i18n.invoices.table.totalPrice}
        </SortableTh>
        <Th>{i18n.invoices.table.nb}</Th>
        <SortableTh sorted={isSorted('STATUS')} onClick={toggleSort('STATUS')}>
          {i18n.invoices.table.status}
        </SortableTh>
        {showCheckboxes ? (
          <Th>
            <Checkbox
              hiddenLabel
              label=""
              checked={allChecked || allInvoicesToggle}
              disabled={allInvoicesToggle}
              onChange={() =>
                allChecked ? actions.clearChecked() : actions.checkAll()
              }
              dataQa="toggle-all-invoices"
            />
          </Th>
        ) : null}
      </Tr>
    </Thead>
  )
})

const InvoiceTableBody = React.memo(function InvoiceTableBody({
  actions,
  invoices,
  showCheckboxes,
  checked,
  allInvoicesToggle
}: Pick<
  Props,
  'actions' | 'showCheckboxes' | 'checked' | 'allInvoicesToggle'
> & {
  invoices: InvoiceSummary[]
}) {
  const { i18n } = useTranslation()
  const history = useHistory()

  return (
    <Tbody>
      {invoices.map((item: InvoiceSummary) => (
        <Tr
          key={item.id}
          onClick={() => history.push(`/finance/invoices/${item.id}`)}
          data-qa="table-invoice-row"
        >
          <Td>
            <NameWithSsn {...item.headOfFamily} i18n={i18n} />
          </Td>
          <Td>
            <ChildrenCell people={item.rows.map(({ child }) => child)} />
          </Td>
          <Td>{`${item.periodStart.format()} - ${item.periodEnd.format()}`}</Td>
          <Td>{formatCents(item.totalPrice)}</Td>
          <Td>
            {item.headOfFamily.restrictedDetailsEnabled && (
              <Tooltip
                tooltipId={`restricted-details-${item.id}`}
                tooltipText={`${i18n.personProfile.restrictedDetails}`}
                place={'right'}
              >
                <StatusIconContainer color={colors.accents.red}>
                  <FontAwesomeIcon icon={faExclamation} inverse />
                </StatusIconContainer>
              </Tooltip>
            )}
          </Td>
          <Td>{i18n.invoice.status[item.status]}</Td>
          {showCheckboxes ? (
            <Td onClick={(e) => e.stopPropagation()}>
              <Checkbox
                hiddenLabel
                label=""
                checked={!!checked[item.id] || allInvoicesToggle}
                disabled={allInvoicesToggle}
                onChange={() => actions.toggleChecked(item.id)}
                dataQa="toggle-invoice"
              />
            </Td>
          ) : null}
        </Tr>
      ))}
    </Tbody>
  )
})
