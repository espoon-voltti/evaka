// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { BrowserRouter, Route, Switch } from 'react-router-dom'
import { Authentication } from '~auth'
import { Localization } from '~localization'
import Header from '~header/Header'
import Decisions from '~decisions/decisions-page/Decisions'
import Footer from '~Footer'
import DecisionResponseList from '~decisions/decision-response-page/DecisionResponseList'
import ApplicationEditor from '~applications/editor/ApplicationEditor'
import GlobalErrorDialog from '~overlay/Error'
import { OverlayContextProvider } from '~overlay/state'
import Applications from '~applications/Applications'

export default function App() {
  return (
    <BrowserRouter basename="/citizen">
      <Authentication>
        <Localization>
          <OverlayContextProvider>
            <Header />
            <Switch>
              <Route exact path="/applications" component={Applications} />
              <Route exact path="/decisions" component={Decisions} />
              <Route
                exact
                path="/decisions/by-application/:applicationId"
                component={DecisionResponseList}
              />
              <Route
                exact
                path="/applications/:applicationId/edit"
                component={ApplicationEditor}
              />
              <Route path="/" component={RedirectToEnduser} />
            </Switch>
            <Footer />
            <GlobalErrorDialog />
          </OverlayContextProvider>
        </Localization>
      </Authentication>
    </BrowserRouter>
  )
}

function RedirectToEnduser() {
  window.location.href =
    window.location.host === 'localhost:9094' ? 'http://localhost:9091' : '/'
  return null
}
