// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { MessageContextProvider } from '../components/messages/MessageContext'
import { CustomersContextProvider } from './customers'
import { UnitsContextProvider } from './units'
import { UIContextProvider } from './ui'
import { InvoicingUIContextProvider } from './invoicing-ui'
import { TitleContextProvider } from './title'
import { ApplicationUIContextProvider } from './application-ui'

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
                  {children}
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
