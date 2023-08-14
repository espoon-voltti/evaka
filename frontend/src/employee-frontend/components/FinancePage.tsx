// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'

import { Action } from 'lib-common/generated/action'
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

interface PageProps {
  path: string
  title: string
  Page: React.FunctionComponent
}

export default React.memo(function FinancePage() {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)

  const tabs = useMemo(
    () =>
      [
        user?.permittedGlobalActions.includes('SEARCH_FEE_DECISIONS')
          ? {
              id: 'fee-decisions',
              link: '/finance/fee-decisions',
              label: i18n.header.feeDecisions
            }
          : null,
        user?.permittedGlobalActions.includes('SEARCH_VOUCHER_VALUE_DECISIONS')
          ? {
              id: 'value-decisions',
              link: '/finance/value-decisions',
              label: i18n.header.valueDecisions
            }
          : null,
        user?.permittedGlobalActions.includes('SEARCH_INVOICES')
          ? {
              id: 'invoices',
              link: '/finance/invoices',
              label: i18n.header.invoices
            }
          : null,
        user?.permittedGlobalActions.includes(
          'SEARCH_VOUCHER_VALUE_DECISIONS'
        ) && featureFlags.experimental?.voucherUnitPayments
          ? {
              id: 'payments',
              link: '/finance/payments',
              label: i18n.header.payments
            }
          : null,
        user?.permittedGlobalActions.includes(
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

  const requireOneOfPermittedActionsForPage = (
    { path, title, Page }: PageProps,
    ...actions: Action.Global[]
  ): React.ReactNode => {
    if (
      actions.some((action) => user?.permittedGlobalActions.includes(action))
    ) {
      return (
        <Route
          key={path}
          path={path}
          element={
            <EmployeeRoute title={title}>
              <Page />
            </EmployeeRoute>
          }
        />
      )
    } else {
      return null
    }
  }

  const permittedRoutes = new Map([
    [
      'fee-decisions',
      requireOneOfPermittedActionsForPage(
        {
          path: 'fee-decisions',
          title: i18n.titles.feeDecisions,
          Page: FeeDecisionsPage
        },
        'SEARCH_FEE_DECISIONS'
      )
    ],
    [
      'value-decisions',
      requireOneOfPermittedActionsForPage(
        {
          path: 'value-decisions',
          title: i18n.titles.valueDecision,
          Page: VoucherValueDecisionsPage
        },
        'SEARCH_VOUCHER_VALUE_DECISIONS'
      )
    ],
    [
      'invoices',
      requireOneOfPermittedActionsForPage(
        {
          path: 'invoices',
          title: i18n.titles.invoices,
          Page: InvoicesPage
        },
        'SEARCH_INVOICES'
      )
    ],
    [
      'invoices',
      requireOneOfPermittedActionsForPage(
        {
          path: 'invoices',
          title: i18n.titles.invoices,
          Page: InvoicesPage
        },
        'SEARCH_INVOICES'
      )
    ],
    [
      'payments',
      featureFlags.experimental?.voucherUnitPayments &&
        requireOneOfPermittedActionsForPage(
          {
            path: 'payments',
            title: i18n.titles.payments,
            Page: PaymentsPage
          },
          'SEARCH_VOUCHER_VALUE_DECISIONS'
        )
    ],
    [
      'income-statements',
      requireOneOfPermittedActionsForPage(
        {
          path: 'income-statements',
          title: i18n.titles.incomeStatements,
          Page: IncomeStatementsPage
        },
        'FETCH_INCOME_STATEMENTS_AWAITING_HANDLER'
      )
    ]
  ])

  return (
    <>
      <Tabs tabs={tabs} />
      <Gap size="s" />
      <Routes>
        {tabs.map(
          ({ id }) => permittedRoutes.has(id) && permittedRoutes.get(id)
        )}
        <Route index element={<Navigate replace to="fee-decisions" />} />
      </Routes>
    </>
  )
})
