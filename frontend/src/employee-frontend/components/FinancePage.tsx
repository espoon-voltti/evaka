// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'

import Tabs from 'lib-components/molecules/Tabs'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'

import { useTranslation } from '../state/i18n'
import { UserContext } from '../state/user'

import EmployeeRoute from './EmployeeRoute'
import FeeDecisionsPage from './fee-decisions/FeeDecisionsPage'
import IncomeStatementsPage from './income-statements/IncomeStatementsPage'
import InvoicesPage from './invoices/InvoicesPage'
import PaymentsPage from './payments/PaymentsPage'
import VoucherValueDecisionsPage from './voucher-value-decisions/VoucherValueDecisionsPage'

export default React.memo(function FinancePage() {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)

  const tabs = useMemo(
    () =>
      [
        user?.permittedGlobalActions?.includes('SEARCH_FEE_DECISIONS')
          ? {
              id: 'fee-decisions',
              link: '/finance/fee-decisions',
              label: i18n.header.feeDecisions
            }
          : null,
        user?.permittedGlobalActions?.includes('SEARCH_VOUCHER_VALUE_DECISIONS')
          ? {
              id: 'value-decisions',
              link: '/finance/value-decisions',
              label: i18n.header.valueDecisions
            }
          : null,
        user?.permittedGlobalActions?.includes('SEARCH_INVOICES')
          ? {
              id: 'invoices',
              link: '/finance/invoices',
              label: i18n.header.invoices
            }
          : null,
        user?.permittedGlobalActions?.includes(
          'SEARCH_VOUCHER_VALUE_DECISIONS'
        ) && featureFlags.experimental?.voucherUnitPayments
          ? {
              id: 'payments',
              link: '/finance/payments',
              label: i18n.header.payments
            }
          : null,
        user?.permittedGlobalActions?.includes(
          'FETCH_INCOME_STATEMENTS_AWAITING_HANDLER'
        )
          ? {
              id: 'income-statements',
              link: '/finance/income-statements',
              label: i18n.header.incomeStatements
            }
          : null
      ].flatMap((tab) => (tab !== null ? tab : [])),
    [i18n, user]
  )

  return (
    <>
      <Tabs tabs={tabs} />
      <Gap size="s" />
      <Routes>
        <Route
          path="fee-decisions"
          element={
            <EmployeeRoute title={i18n.titles.feeDecisions}>
              <FeeDecisionsPage />
            </EmployeeRoute>
          }
        />
        <Route
          path="value-decisions"
          element={
            <EmployeeRoute title={i18n.titles.valueDecisions}>
              <VoucherValueDecisionsPage />
            </EmployeeRoute>
          }
        />
        <Route
          path="invoices"
          element={
            <EmployeeRoute title={i18n.titles.invoices}>
              <InvoicesPage />
            </EmployeeRoute>
          }
        />
        {featureFlags.experimental?.voucherUnitPayments && (
          <Route
            path="payments"
            element={
              <EmployeeRoute title={i18n.titles.payments}>
                <PaymentsPage />
              </EmployeeRoute>
            }
          />
        )}
        <Route
          path="income-statements"
          element={
            <EmployeeRoute title={i18n.titles.incomeStatements}>
              <IncomeStatementsPage />
            </EmployeeRoute>
          }
        />
        <Route index element={<Navigate replace to="fee-decisions" />} />
      </Routes>
    </>
  )
})
