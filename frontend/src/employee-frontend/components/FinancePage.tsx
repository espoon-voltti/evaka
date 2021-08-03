// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Tabs from 'lib-components/molecules/Tabs'
import { Gap } from 'lib-components/white-space'
import React, { useMemo } from 'react'
import { Redirect, Route, Switch } from 'react-router-dom'
import { useTranslation } from '../state/i18n'
import FeeDecisionsPage from './fee-decisions/FeeDecisionsPage'
import InvoicesPage from './invoices/InvoicesPage'
import { RouteWithTitle } from './RouteWithTitle'
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
      }
    ],
    [i18n]
  )

  return (
    <>
      <Gap size="s" />
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
        <Route path="/" component={RedirectToFeeDecisions} />
      </Switch>
    </>
  )
})

function RedirectToFeeDecisions() {
  return <Redirect to="/finance/fee-decisions" />
}
