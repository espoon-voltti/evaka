// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'
import {
  BrowserRouter as Router,
  Route,
  Switch,
  Redirect
} from 'react-router-dom'
import { idleTracker } from '@evaka/lib-common/utils/idleTracker'
import ensureAuthenticated from './components/ensureAuthenticated'
import { AttendanceUIContextProvider } from './state/attendance-ui'
import { I18nContextProvider } from './state/i18n'
import { UserContextProvider } from './state/user'
import MobileLander from './components/mobile/MobileLander'
import PairingWizard from './components/mobile/PairingWizard'
import AttendancePageWrapper from './components/attendances/AttendancePageWrapper'
import AttendanceChildPage from './components/attendances/AttendanceChildPage'
import { getAuthStatus, AuthStatus } from './api/auth'
import { client } from './api/client'
import MarkPresent from './components/attendances/actions/MarkPresent'
import MarkDeparted from './components/attendances/actions/MarkDeparted'
import MarkAbsent from './components/attendances/actions/MarkAbsent'
import DailyNoteEditor from './components/attendances/notes/DailyNoteEditor'

export default function App() {
  const [authStatus, refreshAuthStatus] = useAuthState()

  if (authStatus === undefined) {
    return null
  }

  return (
    <UserContextProvider
      user={authStatus?.user}
      refreshAuthStatus={refreshAuthStatus}
    >
      <I18nContextProvider>
        <AttendanceUIContextProvider>
          <Router basename="/employee/mobile">
            <Switch>
              <Route exact path="/landing" component={MobileLander} />
              <Route exact path="/pairing" component={PairingWizard} />
              <Route
                path="/units/:unitId/attendance/:groupId"
                component={ensureAuthenticated(AttendancePageWrapper)}
              />
              <Route
                path="/units/:unitId/groups/:groupId/childattendance/:childId/markpresent"
                component={ensureAuthenticated(MarkPresent)}
              />
              <Route
                path="/units/:unitId/groups/:groupId/childattendance/:childId/markabsent"
                component={ensureAuthenticated(MarkAbsent)}
              />
              <Route
                path="/units/:unitId/groups/:groupId/childattendance/:childId/markdeparted"
                component={ensureAuthenticated(MarkDeparted)}
              />
              <Route
                path="/units/:unitId/groups/:groupId/childattendance/:childId/note"
                component={ensureAuthenticated(DailyNoteEditor)}
              />
              <Route
                path="/units/:unitId/groups/:groupId/childattendance/:childId"
                component={ensureAuthenticated(AttendanceChildPage)}
              />
              <Route path="" component={RedirectToMainPage} />
            </Switch>
          </Router>
        </AttendanceUIContextProvider>
      </I18nContextProvider>
    </UserContextProvider>
  )
}

const RedirectToMainPage = React.memo(function RedirectToMainPage() {
  return <Redirect to="/landing" />
})

function useAuthState(): [AuthStatus | undefined, () => Promise<void>] {
  const [authStatus, setAuthStatus] = useState<AuthStatus>()

  useEffect(() => {
    void getAuthStatus().then(setAuthStatus)
  }, [])

  useEffect(() => {
    return idleTracker(
      client,
      () => {
        void getAuthStatus().then(setAuthStatus)
      },
      { thresholdInMinutes: 20 }
    )
  }, [])

  const refreshAuthStatus = useCallback(
    () => getAuthStatus().then(setAuthStatus),
    [setAuthStatus]
  )

  return [authStatus, refreshAuthStatus]
}
