// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { BrowserRouter, Route, Switch } from 'react-router-dom'
import { Authentication } from './auth'
import { Localization } from './localization'
import Header from './header/Header'
import Decisions from './decisions/Decisions'
import Footer from '~Footer'

export default function App() {
  return (
    <BrowserRouter basename="/citizen">
      <Authentication>
        <Localization>
          <Header />
          <Switch>
            <Route exact path="/decisions" component={Decisions} />
            <Route path="/" component={RedirectToEnduser} />
          </Switch>
          <Footer />
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
