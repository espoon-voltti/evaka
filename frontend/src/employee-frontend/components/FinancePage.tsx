// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import { Redirect, Route, Switch } from 'react-router-dom'
import Tabs from 'lib-components/molecules/Tabs'
import { Gap } from 'lib-components/white-space'
import { useTranslation } from '../state/i18n'
import { RouteWithTitle } from './RouteWithTitle'
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
        <RouteWithTitle
          exact
          path="/finance/fee-decisions"
          component={FeeDecisionsPage}
          title={i18n.titles.feeDecisions}
        />
        <RouteWithTitle
          exact
          path="/finance/value-decisions"
          component={VoucherValueDecisionsPage}
          title={i18n.titles.valueDecisions}
        />
        <RouteWithTitle
          exact
          path="/finance/invoices"
          component={InvoicesPage}
          title={i18n.titles.invoices}
        />
        <RouteWithTitle
          exact
          path="/finance/income-statements"
          component={IncomeStatementsPage}
          title={i18n.titles.incomeStatements}
        />
        <Route path="/" component={RedirectToFeeDecisions} />
      </Switch>
    </>
  )
})

function RedirectToFeeDecisions() {
  return <Redirect to="/finance/fee-decisions" />
}
