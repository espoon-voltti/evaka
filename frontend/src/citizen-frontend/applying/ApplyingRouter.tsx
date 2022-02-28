// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Redirect, Route, Switch } from 'react-router-dom'
import styled from 'styled-components'

import Tabs from 'lib-components/molecules/Tabs'
import { Gap } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'

import RequireAuth from '../RequireAuth'
import Applications from '../applications/Applications'
import Decisions from '../decisions/decisions-page/Decisions'
import { useTranslation } from '../localization'
import MapView from '../map/MapView'

const WhiteBg = styled.div`
  background-color: ${colors.grayscale.g0};
`

export default React.memo(function ApplyingRouter() {
  const t = useTranslation()

  const tabs = [
    {
      id: 'applications',
      link: '/applying/applications',
      label: t.header.nav.applications
    },
    {
      id: 'map',
      link: '/applying/map',
      label: t.header.nav.map
    },
    {
      id: 'decisions',
      link: '/applying/decisions',
      label: t.header.nav.decisions
    }
  ]

  return (
    <>
      <Gap size="s" />
      <WhiteBg>
        <Tabs tabs={tabs} data-qa="applying-subnavigation" />
      </WhiteBg>
      <Switch>
        <Route
          exact
          path="/applying/applications"
          render={() => (
            <RequireAuth>
              <Applications />
            </RequireAuth>
          )}
        />
        <Route exact path="/applying/map" render={() => <MapView />} />
        <Route
          exact
          path="/applying/decisions"
          render={() => (
            <RequireAuth>
              <Decisions />
            </RequireAuth>
          )}
        />
        <Route
          path="/"
          render={() => <Redirect to="/applying/applications" />}
        />
      </Switch>
    </>
  )
})
