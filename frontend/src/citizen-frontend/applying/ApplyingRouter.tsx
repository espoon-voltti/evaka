// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import styled from 'styled-components'

import Tabs from 'lib-components/molecules/Tabs'
import { Gap } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'
import { faLockAlt } from 'lib-icons'

import RequireAuth from '../RequireAuth'
import Applications from '../applications/Applications'
import { useUser } from '../auth/state'
import Decisions from '../decisions/decisions-page/Decisions'
import { useTranslation } from '../localization'
import MapView from '../map/MapView'

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
      <Gap size="s" />
      <WhiteBg>
        <Tabs tabs={tabs} data-qa="applying-subnavigation" />
      </WhiteBg>
      <Routes>
        <Route
          path="applications"
          element={
            <RequireAuth>
              <Applications />
            </RequireAuth>
          }
        />
        <Route path="map" element={<MapView scrollToTop={scrollToTop} />} />
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
    </>
  )
})
