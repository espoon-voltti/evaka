// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Failure, Loading, Result, Success } from 'lib-common/api'
import {
  Payment,
  PaymentSortParam,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import { formatCents } from 'lib-common/money'
import Pagination from 'lib-components/Pagination'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
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
import { faExclamationTriangle, faSync } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import { PaymentsActions } from './payments-state'

interface Props {
  actions: PaymentsActions
  payments?: Result<Payment[]>
  createPayments: () => Promise<void>
  total?: number
  pages?: number
  currentPage: number
  sortBy: PaymentSortParam
  sortDirection: SortDirection
  showCheckboxes: boolean
  checked: Record<string, true>
}

export default React.memo(function Payments({
  actions,
  payments,
  createPayments,
  total,
  pages,
  currentPage,
  sortBy,
  sortDirection,
  showCheckboxes,
  checked
}: Props) {
  const { i18n } = useTranslation()
  const [createResult, setCreateResult] = useState<Result<void>>(
    Success.of(undefined)
  )

  return (
    <div className="payments">
      <CreatePayments>
        {createResult.isFailure ? (
          <CreateError>{i18n.common.error.unknown}</CreateError>
        ) : null}
        <Button
          appearance="inline"
          icon={faSync}
          disabled={createResult.isLoading}
          onClick={() => {
            setCreateResult(Loading.of())
            createPayments()
              .then(() => setCreateResult(Success.of()))
              // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
              .catch((err) => setCreateResult(Failure.of(err)))
          }}
          text={i18n.payments.buttons.createPaymentDrafts}
          data-qa="create-payment-drafts"
        />
      </CreatePayments>
      <TitleRowContainer>
        <SectionTitle size={1}>{i18n.payments.table.title}</SectionTitle>
        {payments?.isSuccess && (
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
      {payments?.isLoading ? <Loader /> : null}
      {!payments || payments.isFailure ? (
        <div>{i18n.common.error.unknown}</div>
      ) : null}
      {payments?.isSuccess && (
        <>
          <Table>
            <PaymentTableHeader
              actions={actions}
              payments={payments}
              checked={checked}
              sortBy={sortBy}
              sortDirection={sortDirection}
              showCheckboxes={showCheckboxes}
            />
            <PaymentTableBody
              actions={actions}
              payments={payments.value}
              showCheckboxes={showCheckboxes}
              checked={checked}
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

const CreatePayments = styled.div`
  position: absolute;
  top: -30px;
  right: 60px;
`

const CreateError = styled.span`
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

const MissingDetailsWarning = styled.div`
  color: ${(p) => p.theme.colors.status.warning};
`

const PaymentTableHeader = React.memo(function PaymentTableHeader({
  actions,
  payments,
  checked,
  sortBy,
  sortDirection,
  showCheckboxes
}: Pick<
  Props,
  | 'actions'
  | 'payments'
  | 'checked'
  | 'sortBy'
  | 'sortDirection'
  | 'showCheckboxes'
>) {
  const { i18n } = useTranslation()

  const allChecked =
    payments
      ?.map((ps) => ps.length > 0 && ps.every((i) => checked[i.id]))
      .getOrElse(false) ?? false

  const isSorted = (column: PaymentSortParam) =>
    sortBy === column ? sortDirection : undefined

  const toggleSort = (column: PaymentSortParam) => () => {
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
        <SortableTh sorted={isSorted('UNIT')} onClick={toggleSort('UNIT')}>
          {i18n.payments.table.unit}
        </SortableTh>
        <SortableTh sorted={isSorted('PERIOD')} onClick={toggleSort('PERIOD')}>
          {i18n.payments.table.period}
        </SortableTh>
        <SortableTh
          sorted={isSorted('CREATED')}
          onClick={toggleSort('CREATED')}
        >
          {i18n.payments.table.createdAt}
        </SortableTh>
        <SortableTh sorted={isSorted('NUMBER')} onClick={toggleSort('NUMBER')}>
          {i18n.payments.table.number}
        </SortableTh>
        <SortableTh sorted={isSorted('AMOUNT')} onClick={toggleSort('AMOUNT')}>
          {i18n.payments.table.amount}
        </SortableTh>
        <Th>{i18n.payments.table.status}</Th>
        <Th>{i18n.payments.table.nb}</Th>
        {showCheckboxes ? (
          <Th>
            <Checkbox
              hiddenLabel
              label=""
              checked={allChecked}
              onChange={actions.toggleCheckAll}
              data-qa="toggle-all-payments"
            />
          </Th>
        ) : null}
      </Tr>
    </Thead>
  )
})

const PaymentTableBody = React.memo(function PaymentTableBody({
  actions,
  payments,
  showCheckboxes,
  checked
}: Pick<Props, 'actions' | 'showCheckboxes' | 'checked'> & {
  payments: Payment[]
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <Tbody>
      {payments.map((item) => (
        <Tr
          key={item.id}
          onClick={() =>
            navigate(
              `/reports/voucher-service-providers/${item.unit.id}?year=${item.period.start.year}&month=${item.period.start.month}`
            )
          }
          data-qa="table-payment-row"
        >
          <Td>{item.unit.name}</Td>
          <Td>{item.period.format()}</Td>
          <Td>{item.created.toLocalDate().formatExotic('d.M.yyyy')}</Td>
          <Td>{item.number}</Td>
          <Td>{formatCents(item.amount)}</Td>
          <Td>{i18n.payments.status[item.status]}</Td>
          <Td>
            {!item.unit.businessId ||
            !item.unit.iban ||
            !item.unit.providerId ? (
              <MissingDetailsWarning>
                <FontAwesomeIcon icon={faExclamationTriangle} />
                {i18n.payments.table.missingPaymentDetails}
              </MissingDetailsWarning>
            ) : null}
          </Td>
          {showCheckboxes ? (
            <Td onClick={(e) => e.stopPropagation()}>
              <Checkbox
                hiddenLabel
                label=""
                checked={!!checked[item.id]}
                onChange={() => actions.toggleChecked(item.id)}
              />
            </Td>
          ) : null}
        </Tr>
      ))}
    </Tbody>
  )
})
