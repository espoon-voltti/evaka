// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Applications from 'citizen-frontend/applications/Applications'
import { useUser } from 'citizen-frontend/auth'
import Decisions from 'citizen-frontend/decisions/decisions-page/Decisions'
import { useTranslation } from 'citizen-frontend/localization'
import MapView from 'citizen-frontend/map/MapView'
import Tabs from 'lib-components/molecules/Tabs'
import { Gap } from 'lib-components/white-space'
import React from 'react'
import { Route, Switch } from 'react-router-dom'

export default React.memo(function Applying() {
  const t = useTranslation()
  const loggedIn = !!useUser()

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
      {loggedIn && (
        <>
          <Gap size="s" />
          <Tabs tabs={tabs} dataQa="applying-subnavigation" />
        </>
      )}
      <Switch>
        <Route exact path="/applying/map" component={MapView} />
        <Route exact path="/applying/applications" component={Applications} />
        <Route exact path="/applying/decisions" component={Decisions} />
      </Switch>
    </>
  )
})
