// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { AssistanceNeedDecisionReportContextProvider } from 'employee-frontend/components/reports/AssistanceNeedDecisionReportContext'

import { MessageContextProvider } from '../components/messages/MessageContext'

import { ApplicationUIContextProvider } from './application-ui'
import { CustomersContextProvider } from './customers'
import { InvoicingUIContextProvider } from './invoicing-ui'
import { TitleContextProvider } from './title'
import { UIContextProvider } from './ui'
import { UnitsContextProvider } from './units'

const StateProvider = React.memo(function StateProvider({
  children
}: {
  children: JSX.Element
}) {
  return (
    <UIContextProvider>
      <UnitsContextProvider>
        <CustomersContextProvider>
          <InvoicingUIContextProvider>
            <MessageContextProvider>
              <TitleContextProvider>
                <ApplicationUIContextProvider>
                  <AssistanceNeedDecisionReportContextProvider>
                    {children}
                  </AssistanceNeedDecisionReportContextProvider>
                </ApplicationUIContextProvider>
              </TitleContextProvider>
            </MessageContextProvider>
          </InvoicingUIContextProvider>
        </CustomersContextProvider>
      </UnitsContextProvider>
    </UIContextProvider>
  )
})

export default StateProvider
