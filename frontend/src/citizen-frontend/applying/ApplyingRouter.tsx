// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Tabs from 'lib-components/molecules/Tabs'
import { Gap } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'
import React from 'react'
import { Redirect, Route, Switch } from 'react-router-dom'
import styled from 'styled-components'
import Applications from '../applications/Applications'
import requireAuth from '../auth/requireAuth'
import { useUser } from '../auth/state'
import Decisions from '../decisions/decisions-page/Decisions'
import { useTranslation } from '../localization'
import MapView from '../map/MapView'

const WhiteBg = styled.div`
  background-color: ${colors.greyscale.white};
`

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
          <WhiteBg>
            <Tabs tabs={tabs} data-qa="applying-subnavigation" />
          </WhiteBg>
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
