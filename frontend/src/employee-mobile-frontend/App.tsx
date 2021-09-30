// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'
import {
  BrowserRouter as Router,
  Route,
  Switch,
  Redirect,
  useRouteMatch
} from 'react-router-dom'
import { idleTracker } from 'lib-common/utils/idleTracker'
import ensureAuthenticated from './components/ensureAuthenticated'
import { ChildAttendanceContextProvider } from './state/child-attendance'
import { I18nContextProvider } from './state/i18n'
import { UserContextProvider } from './state/user'
import MobileLander from './components/mobile/MobileLander'
import PairingWizard from './components/mobile/PairingWizard'
import AttendancePageWrapper from './components/attendances/AttendancePageWrapper'
import AttendanceChildPage from './components/attendances/child-info/ChildInfo'
import { getAuthStatus, AuthStatus } from './api/auth'
import { client } from './api/client'
import MarkPresent from './components/attendances/actions/MarkPresent'
import MarkDeparted from './components/attendances/actions/MarkDeparted'
import MarkAbsent from './components/attendances/actions/MarkAbsent'
import MarkAbsentBeforehand from './components/attendances/actions/MarkAbsentBeforehand'
import DailyNoteEditor from './components/attendances/notes/DailyNoteEditor'
import StaffPage from './components/staff/StaffPage'
import StaffPage2 from './components/staff-attendance/StaffPage2'
import PinLogin from './components/attendances/child-info/PinLogin'
import { ThemeProvider } from 'styled-components'
import { theme } from 'lib-customizations/common'
import StaffMemberPage from './components/staff-attendance/StaffMemberPage'
import StaffMarkArrivedPage from './components/staff-attendance/StaffMarkArrivedPage'
import StaffMarkDepartedPage from './components/staff-attendance/StaffMarkDepartedPage'
import { StaffContextProvider } from './state/staff'
import { UnitContextProvider } from './state/unit'

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
        <ThemeProvider theme={theme}>
          <Router basename="/employee/mobile">
            <Switch>
              <Route exact path="/landing" component={MobileLander} />
              <Route exact path="/pairing" component={PairingWizard} />
              <Route
                path="/units/:unitId"
                component={ensureAuthenticated(UnitRouter)}
              />
              <Route component={RedirectToMainPage} />
            </Switch>
          </Router>
        </ThemeProvider>
      </I18nContextProvider>
    </UserContextProvider>
  )
}

function UnitRouter() {
  const { path } = useRouteMatch()

  return (
    <UnitContextProvider>
      <Switch>
        <Route path={`${path}/groups/:groupId`} component={GroupRouter} />
      </Switch>
    </UnitContextProvider>
  )
}

function GroupRouter() {
  const { path } = useRouteMatch()

  return (
    <Switch>
      <Route
        path={`${path}/child-attendance`}
        component={ChildAttendanceRouter}
      />
      <Route path={`${path}/staff`} component={StaffRouter} />
      <Route
        path={`${path}/staff-attendance`}
        component={StaffAttendanceRouter}
      />
    </Switch>
  )
}

function ChildAttendanceRouter() {
  const { path } = useRouteMatch()

  return (
    <ChildAttendanceContextProvider>
      <Switch>
        <Route
          exact
          path={`${path}/list/:attendanceStatus`}
          component={AttendancePageWrapper}
        />
        <Route
          exact
          path={`${path}/:childId`}
          component={AttendanceChildPage}
        />
        <Route
          exact
          path={`${path}/:childId/mark-present`}
          component={MarkPresent}
        />
        <Route
          exact
          path={`${path}/:childId/mark-absent`}
          component={MarkAbsent}
        />
        <Route
          exact
          path={`${path}/:childId/mark-absent-beforehand`}
          component={MarkAbsentBeforehand}
        />
        <Route
          exact
          path={`${path}/:childId/mark-departed`}
          component={MarkDeparted}
        />
        <Route
          exact
          path={`${path}/:childId/note`}
          component={DailyNoteEditor}
        />
        <Route exact path={`${path}/:childId/pin`} component={PinLogin} />
        <Redirect to={`${path}/list/coming`} />
      </Switch>
    </ChildAttendanceContextProvider>
  )
}

function StaffRouter() {
  const { path } = useRouteMatch()

  return (
    <StaffContextProvider>
      <Switch>
        <Route exact path={path} component={StaffPage} />
      </Switch>
    </StaffContextProvider>
  )
}

function StaffAttendanceRouter() {
  const { path } = useRouteMatch()

  return (
    <Switch>
      <Route exact path={path} component={StaffPage2} />
      <Route exact path={`${path}/:employeeId`} component={StaffMemberPage} />
      <Route
        exact
        path={`${path}/:employeeId/mark-arrived`}
        component={StaffMarkArrivedPage}
      />
      <Route
        exact
        path={`${path}/:employeeId/mark-departed`}
        component={StaffMarkDepartedPage}
      />
    </Switch>
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
