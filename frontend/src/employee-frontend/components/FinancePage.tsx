// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import { Redirect, Route, Switch } from 'react-router-dom'

import Tabs from 'lib-components/molecules/Tabs'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../state/i18n'

import EmployeeRoute from './EmployeeRoute'
import FeeDecisionsPage from './fee-decisions/FeeDecisionsPage'
import IncomeStatementsPage from './income-statements/IncomeStatementsPage'
import InvoicesPage from './invoices/InvoicesPage'
import VoucherValueDecisionsPage from './voucher-value-decisions/VoucherValueDecisionsPage'

export default React.memo(function FinancePage() {
  const { i18n } = useTranslation()

  const tabs = useMemo(
    () => [
      {
        id: 'fee-decisions',
        link: '/finance/fee-decisions',
        label: i18n.header.feeDecisions
      },
      {
        id: 'value-decisions',
        link: '/finance/value-decisions',
        label: i18n.header.valueDecisions
      },
      {
        id: 'invoices',
        link: '/finance/invoices',
        label: i18n.header.invoices
      },
      {
        id: 'income-statements',
        link: '/finance/income-statements',
        label: i18n.header.incomeStatements
      }
    ],
    [i18n]
  )

  return (
    <>
      <Tabs tabs={tabs} />
      <Gap size="s" />
      <Switch>
        <Route
          exact
          path="/finance/fee-decisions"
          element={
            <EmployeeRoute title={i18n.titles.feeDecisions}>
              <FeeDecisionsPage />
            </EmployeeRoute>
          }
        />
        <Route
          exact
          path="/finance/value-decisions"
          element={
            <EmployeeRoute title={i18n.titles.valueDecisions}>
              <VoucherValueDecisionsPage />
            </EmployeeRoute>
          }
        />
        <Route
          exact
          path="/finance/invoices"
          element={
            <EmployeeRoute title={i18n.titles.invoices}>
              <InvoicesPage />
            </EmployeeRoute>
          }
        />
        <Route
          exact
          path="/finance/income-statements"
          element={
            <EmployeeRoute title={i18n.titles.incomeStatements}>
              <IncomeStatementsPage />
            </EmployeeRoute>
          }
        />
        <Route path="/" element={<Redirect to="/finance/fee-decisions" />} />
      </Switch>
    </>
  )
})
