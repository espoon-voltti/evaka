// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import uniqBy from 'lodash/uniqBy'
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Failure, isLoading, Loading, Result, Success } from 'lib-common/api'
import { User } from 'lib-common/api-types/employee-auth'
import {
  InvoiceSortParam,
  InvoiceSummaryResponse,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import { formatCents } from 'lib-common/money'
import Pagination from 'lib-components/Pagination'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import {
  SortableTh,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from 'lib-components/layout/Table'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faExclamation, faSync } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import ChildrenCell from '../common/ChildrenCell'
import NameWithSsn from '../common/NameWithSsn'
import { StatusIconContainer } from '../common/StatusIconContainer'

import { InvoicesActions } from './invoices-state'

interface Props {
  user: User | undefined
  actions: InvoicesActions
  invoices: Result<InvoiceSummaryResponse[]>
  refreshInvoices: () => Promise<void>
  total?: number
  pages?: number
  currentPage: number
  sortBy: InvoiceSortParam
  sortDirection: SortDirection
  showCheckboxes: boolean
  checked: Record<string, true>
  allInvoicesToggle: boolean
  allInvoicesToggleDisabled: boolean
}

export default React.memo(function Invoices({
  user,
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
  const [refreshResult, setRefreshResult] = useState<Result<void>>(
    Success.of(undefined)
  )

  return (
    <div className="invoices" data-isloading={isLoading(invoices)}>
      {user?.accessibleFeatures.createDraftInvoices && (
        <RefreshInvoices>
          {refreshResult.isFailure ? (
            <RefreshError>{i18n.common.error.unknown}</RefreshError>
          ) : null}
          <Button
            appearance="inline"
            icon={faSync}
            disabled={refreshResult.isLoading}
            onClick={() => {
              setRefreshResult(Loading.of())
              refreshInvoices()
                .then(() => setRefreshResult(Success.of()))
                // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
                .catch((err) => setRefreshResult(Failure.of(err)))
            }}
            text={i18n.invoices.buttons.createInvoices}
            data-qa="create-invoices"
          />
        </RefreshInvoices>
      )}
      <TitleRowContainer>
        <SectionTitle size={1}>{i18n.invoices.table.title}</SectionTitle>
        {invoices?.isSuccess && (
          <ResultsContainer>
            <div>{total ? i18n.common.resultCount(total) : null}</div>
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
            {showCheckboxes && (
              <Checkbox
                checked={allInvoicesToggle}
                label={i18n.invoices.table.toggleAll}
                onChange={actions.allInvoicesToggle}
                disabled={allInvoicesToggleDisabled}
              />
            )}
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
      ?.map((is) => is.length > 0 && is.every((i) => checked[i.data.id]))
      .getOrElse(false) ?? false

  const isSorted = (column: InvoiceSortParam) =>
    sortBy === column ? sortDirection : undefined

  const toggleSort = (column: InvoiceSortParam) => () => {
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
        <SortableTh
          sorted={isSorted('CREATED_AT')}
          onClick={toggleSort('CREATED_AT')}
        >
          {i18n.invoices.table.createdAt}
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
              data-qa="toggle-all-invoices"
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
  invoices: InvoiceSummaryResponse[]
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <Tbody>
      {invoices.map(
        ({ data: item, permittedActions }: InvoiceSummaryResponse) => (
          <Tr
            key={item.id}
            onClick={() => navigate(`/finance/invoices/${item.id}`)}
            data-qa="table-invoice-row"
          >
            <Td>
              <NameWithSsn {...item.headOfFamily} i18n={i18n} />
            </Td>
            <Td>
              <ChildrenCell
                people={uniqBy(
                  item.rows.map(({ child }) => child),
                  ({ id }) => id
                )}
              />
            </Td>
            <Td>{`${item.periodStart.format()} - ${item.periodEnd.format()}`}</Td>
            <Td data-qa="invoice-created-at">
              {item.createdAt?.toLocalDate().format() ?? ''}
            </Td>
            <Td data-qa="invoice-total">{formatCents(item.totalPrice)}</Td>
            <Td>
              {item.headOfFamily.restrictedDetailsEnabled && (
                <Tooltip
                  tooltip={i18n.personProfile.restrictedDetails}
                  position="right"
                >
                  <StatusIconContainer color={colors.status.danger}>
                    <FontAwesomeIcon icon={faExclamation} inverse />
                  </StatusIconContainer>
                </Tooltip>
              )}
            </Td>
            <Td>{i18n.invoice.status[item.status]}</Td>
            {showCheckboxes && permittedActions.includes('SEND') ? (
              <Td onClick={(e) => e.stopPropagation()}>
                <Checkbox
                  hiddenLabel
                  label=""
                  checked={!!checked[item.id] || allInvoicesToggle}
                  disabled={allInvoicesToggle}
                  onChange={() => actions.toggleChecked(item.id)}
                  data-qa="toggle-invoice"
                />
              </Td>
            ) : null}
          </Tr>
        )
      )}
    </Tbody>
  )
})
