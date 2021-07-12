// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { MessageContextProvider } from '../components/messages/MessageContext'
import { ChildContextProvider } from './child'
import { PersonContextProvider } from './person'
import { CustomersContextProvider } from './customers'
import { UnitContextProvider } from './unit'
import { UnitsContextProvider } from './units'
import { UIContextProvider } from './ui'
import { I18nContextProvider } from './i18n'
import { InvoicingUIContextProvider } from './invoicing-ui'
import { AbsencesContextProvider } from './absence'
import { ApplicationUIContextProvider } from './application-ui'
import {
  PlacementDraftContextProvider,
  UnitsContextProvider as PDUnitsContextProvider
} from '../state/placementdraft'
import { DecisionDraftContextProvider } from './decision'
import { TitleContextProvider } from './title'
import { ThemeProvider } from 'styled-components'
import { theme } from 'lib-customizations/common'

const StateProvider = React.memo(function StateProvider({
  children
}: {
  children: JSX.Element
}) {
  return (
    <I18nContextProvider>
      <UIContextProvider>
        <UnitsContextProvider>
          <UnitContextProvider>
            <CustomersContextProvider>
              <PersonContextProvider>
                <ChildContextProvider>
                  <InvoicingUIContextProvider>
                    <AbsencesContextProvider>
                      <DecisionDraftContextProvider>
                        <PlacementDraftContextProvider>
                          <PDUnitsContextProvider>
                            <MessageContextProvider>
                              <TitleContextProvider>
                                <ApplicationUIContextProvider>
                                  <ThemeProvider theme={theme}>
                                    {children}
                                  </ThemeProvider>
                                </ApplicationUIContextProvider>
                              </TitleContextProvider>
                            </MessageContextProvider>
                          </PDUnitsContextProvider>
                        </PlacementDraftContextProvider>
                      </DecisionDraftContextProvider>
                    </AbsencesContextProvider>
                  </InvoicingUIContextProvider>
                </ChildContextProvider>
              </PersonContextProvider>
            </CustomersContextProvider>
          </UnitContextProvider>
        </UnitsContextProvider>
      </UIContextProvider>
    </I18nContextProvider>
  )
})

export default StateProvider
