// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ReactNode } from 'react'
import React from 'react'

import { NotificationsContextProvider } from 'lib-components/Notifications'

import { MessageContextProvider } from '../components/messages/MessageContext'
import { CustomersContextProvider } from '../components/person-search/customers'
import { ReportNotificationContextProvider } from '../components/reports/ReportNotificationContext'

import { ApplicationUIContextProvider } from './application-ui'
import { InvoicingUIContextProvider } from './invoicing-ui'
import { TitleContextProvider } from './title'
import { UIContextProvider } from './ui'
import { UnitContextProvider } from './unit'
import { UnitsContextProvider } from './units'

const StateProvider = React.memo(function StateProvider({
  children
}: {
  children: ReactNode | ReactNode[]
}) {
  return (
    <NotificationsContextProvider>
      <UIContextProvider>
        <UnitsContextProvider>
          <UnitContextProvider>
            <CustomersContextProvider>
              <InvoicingUIContextProvider>
                <MessageContextProvider>
                  <TitleContextProvider>
                    <ApplicationUIContextProvider>
                      <ReportNotificationContextProvider>
                        {children}
                      </ReportNotificationContextProvider>
                    </ApplicationUIContextProvider>
                  </TitleContextProvider>
                </MessageContextProvider>
              </InvoicingUIContextProvider>
            </CustomersContextProvider>
          </UnitContextProvider>
        </UnitsContextProvider>
      </UIContextProvider>
    </NotificationsContextProvider>
  )
})

export default StateProvider
