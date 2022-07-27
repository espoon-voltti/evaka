// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { lazy, Suspense } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import styled from 'styled-components'

import Footer from 'citizen-frontend/Footer'
import Main from 'lib-components/atoms/Main'
import SkipToContent from 'lib-components/atoms/buttons/SkipToContent'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import Tabs from 'lib-components/molecules/Tabs'
import { Gap } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'
import { faLockAlt } from 'lib-icons'

import RequireAuth from '../RequireAuth'
import { useUser } from '../auth/state'
import { useTranslation } from '../localization'

const MapView = lazy(
  () => import(/* webpackChunkName: "Applying-MapView" */ '../map/MapView')
)
const Decisions = lazy(
  () =>
    import(
      /* webpackChunkName: "Applying-Decisions" */ '../decisions/decisions-page/Decisions'
    )
)
const Applications = lazy(
  () =>
    import(
      /* webpackChunkName: "Applying-Applications" */ '../applications/Applications'
    )
)

const WhiteBg = styled.div`
  background-color: ${colors.grayscale.g0};
`

export interface Props {
  scrollToTop: () => void
}

export default React.memo(function ApplyingRouter({ scrollToTop }: Props) {
  const t = useTranslation()
  const user = useUser()
  const isEndUser = user?.userType === 'ENDUSER'
  const { pathname } = useLocation()

  const maybeLockElem = !isEndUser && (
    <FontAwesomeIcon icon={faLockAlt} size="xs" />
  )

  const tabs = [
    {
      id: 'applications',
      link: '/applying/applications',
      label: (
        <>
          {t.header.nav.applications} {maybeLockElem}
        </>
      )
    },
    {
      id: 'map',
      link: '/applying/map',
      label: t.header.nav.map
    },
    {
      id: 'decisions',
      link: '/applying/decisions',
      label: (
        <>
          {t.header.nav.decisions} {maybeLockElem}
        </>
      )
    }
  ]

  return (
    <>
      <SkipToContent target="applying-subnavigation">
        {t.skipLinks.applyingSubNav}
      </SkipToContent>
      <Gap size="s" />
      <WhiteBg>
        <Tabs
          tabs={tabs}
          data-qa="applying-subnavigation"
          id="applying-subnavigation"
        />
      </WhiteBg>
      <Main>
        <Suspense fallback={<SpinnerSegment />}>
          <Routes>
            <Route
              path="applications"
              element={
                <RequireAuth>
                  <Applications />
                </RequireAuth>
              }
            />
            <Route
              path="map"
              element={
                <>
                  <Gap size="s" />
                  <MapView scrollToTop={scrollToTop} />
                </>
              }
            />
            <Route
              path="decisions"
              element={
                <RequireAuth>
                  <Decisions />
                </RequireAuth>
              }
            />
            <Route
              path="/"
              element={
                <Navigate
                  to={isEndUser ? '/applying/applications' : '/applying/map'}
                />
              }
            />
          </Routes>
        </Suspense>
      </Main>
      {pathname !== '/applying/map' && <Footer />}
    </>
  )
})
