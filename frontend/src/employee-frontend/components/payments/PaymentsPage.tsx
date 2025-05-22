// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'

import type {
  PaymentSortParam,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import type { PaymentId } from 'lib-common/generated/api-types/shared'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import { useCheckedState } from '../../state/invoicing'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { renderResult } from '../async-rendering'

import Actions from './Actions'
import PaymentFilters from './PaymentFilters'
import Payments from './Payments'
import { searchPaymentsQuery } from './queries'
import { selectablePaymentStatuses } from './utils'

export default React.memo(function PaymentsPage() {
  const {
    payments: { confirmedSearchFilters: searchFilters, page }
  } = useContext(InvoicingUiContext)

  const [sortBy, setSortBy] = useState<PaymentSortParam>('UNIT')
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')

  const payments = useQueryResult(
    searchFilters &&
      (!searchFilters.paymentDateStart ||
        !searchFilters.paymentDateEnd ||
        searchFilters.paymentDateEnd.isEqualOrAfter(
          searchFilters.paymentDateStart
        ))
      ? searchPaymentsQuery({
          body: {
            page,
            sortBy,
            sortDirection,
            area: searchFilters.area,
            distinctions: searchFilters.distinctions,
            paymentDateStart: searchFilters.paymentDateStart ?? null,
            paymentDateEnd: searchFilters.paymentDateEnd ?? null,
            searchTerms: searchFilters.searchTerms,
            status: searchFilters.status,
            unit: searchFilters.unit ?? null
          }
        })
      : constantQuery({ data: [], pages: 0, total: 0 })
  )

  const checkedState = useCheckedState<PaymentId>()

  const checkAll = useCallback(() => {
    if (payments && payments.isSuccess) {
      checkedState.checkIds(payments.value.data.map((payment) => payment.id))
    }
  }, [payments, checkedState.checkIds]) // eslint-disable-line react-hooks/exhaustive-deps

  const checkedIds = checkedState.getCheckedIds()

  return (
    <Container data-qa="payments-page">
      <ContentArea opaque>
        <PaymentFilters />
      </ContentArea>
      <Gap size="XL" />
      {searchFilters &&
        renderResult(payments, (payments) => (
          <>
            <ContentArea opaque>
              <Payments
                payments={payments.data}
                total={payments.total}
                pages={payments.pages}
                sortBy={sortBy}
                setSortBy={setSortBy}
                sortDirection={sortDirection}
                setSortDirection={setSortDirection}
                showCheckboxes={selectablePaymentStatuses.includes(
                  searchFilters.status
                )}
                isChecked={checkedState.isChecked}
                toggleChecked={checkedState.toggleChecked}
                checkAll={checkAll}
                clearChecked={checkedState.clearChecked}
              />
            </ContentArea>
            <Actions
              status={searchFilters.status}
              checkedIds={checkedIds}
              clearChecked={checkedState.clearChecked}
            />
          </>
        ))}
    </Container>
  )
})
