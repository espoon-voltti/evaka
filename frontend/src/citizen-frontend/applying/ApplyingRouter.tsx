// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Applications from '../applications/Applications'
import requireAuth from '../auth/requireAuth'
import { useUser } from '../auth/state'
import Decisions from '../decisions/decisions-page/Decisions'
import { useTranslation } from '../localization'
import MapView from '../map/MapView'
import Tabs from 'lib-components/molecules/Tabs'
import { Gap } from 'lib-components/white-space'
import React from 'react'
import { Redirect, Route, Switch } from 'react-router-dom'

export default React.memo(function ApplyingRouter() {
  const t = useTranslation()
  const user = useUser()

  const tabs = [
    {
      id: 'map',
      link: '/applying/map',
      label: t.header.nav.map
    },
    {
      id: 'applications',
      link: '/applying/applications',
      label: t.header.nav.applications
    },
    {
      id: 'decisions',
      link: '/applying/decisions',
      label: t.header.nav.decisions
    }
  ]

  return (
    <>
      {user && (
        <>
          <Gap size="s" />
          <Tabs tabs={tabs} dataQa="applying-subnavigation" />
        </>
      )}
      <Switch>
        <Route exact path="/applying/map" component={MapView} />
        <Route
          exact
          path="/applying/applications"
          component={requireAuth(Applications)}
        />
        <Route
          exact
          path="/applying/decisions"
          component={requireAuth(Decisions)}
        />
        <Redirect to="/applying/map" />
      </Switch>
    </>
  )
})
