// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { BrowserRouter, Route, Switch } from 'react-router-dom'
import { config } from './configs'
import Header from './Header'
import Decisions from './decisions/Decisions'

export default function App() {
  return (
    <BrowserRouter basename="/citizen">
      <Header />
      <Switch>
        <Route exact path="/decisions" component={Decisions} />
        <Route path="/" component={RedirectToEnduser} />
      </Switch>
    </BrowserRouter>
  )
}

function RedirectToEnduser() {
  window.location.href = config.enduserBaseUrl
  return null
}
