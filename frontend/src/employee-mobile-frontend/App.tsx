// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import React, { useCallback, useEffect, useState } from 'react'
import {
  BrowserRouter as Router,
  Route,
  Switch,
  Redirect,
  useRouteMatch
} from 'react-router-dom'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { ErrorPage } from 'lib-components/molecules/ErrorPage'
import ensureAuthenticated from './components/ensureAuthenticated'
import ExternalStaffMemberPage from './components/staff-attendance/ExternalStaffMemberPage'
import { ChildAttendanceContextProvider } from './state/child-attendance'
import { I18nContextProvider, useTranslation } from './state/i18n'
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
import ChildNotes from './components/attendances/notes/ChildNotes'
import StaffPage from './components/staff/StaffPage'
import StaffAttendancesPage from './components/staff-attendance/StaffAttendancesPage'
import MarkExternalStaffMemberArrivalPage from './components/staff-attendance/MarkExternalStaffMemberArrivalPage'
import PinLogin from './components/attendances/child-info/PinLogin'
import { ThemeProvider } from 'styled-components'
import { theme } from 'lib-customizations/common'
import StaffMemberPage from './components/staff-attendance/StaffMemberPage'
import StaffMarkArrivedPage from './components/staff-attendance/StaffMarkArrivedPage'
import StaffMarkDepartedPage from './components/staff-attendance/StaffMarkDepartedPage'
import { UnitContextProvider } from './state/unit'
import { StaffAttendanceContextProvider } from './state/staff-attendance'

export default function App() {
  const { i18n } = useTranslation()
  const [authStatus, refreshAuthStatus] = useAuthState()

  if (authStatus === undefined) {
    return null
  }

  return (
    <I18nContextProvider>
      <ThemeProvider theme={theme}>
        <ErrorBoundary
          fallback={() => (
            <ErrorPage basePath="/employee/mobile" labels={i18n.errorPage} />
          )}
        >
          <UserContextProvider
            user={authStatus?.user}
            refreshAuthStatus={refreshAuthStatus}
          >
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
          </UserContextProvider>
        </ErrorBoundary>
      </ThemeProvider>
    </I18nContextProvider>
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
        <Route exact path={`${path}/:childId/note`} component={ChildNotes} />
        <Route exact path={`${path}/:childId/pin`} component={PinLogin} />
        <Redirect to={`${path}/list/coming`} />
      </Switch>
    </ChildAttendanceContextProvider>
  )
}

function StaffRouter() {
  const { path } = useRouteMatch()

  return (
    <Switch>
      <Route exact path={path} component={StaffPage} />
      <Redirect to={path} />
    </Switch>
  )
}

function StaffAttendanceRouter() {
  const { path } = useRouteMatch()

  return (
    <StaffAttendanceContextProvider>
      <Switch>
        <Route exact path={path} component={StaffAttendancesPage} />
        <Route
          exact
          path={`${path}/external`}
          component={MarkExternalStaffMemberArrivalPage}
        />
        <Route
          exact
          path={`${path}/external/:attendanceId`}
          component={ExternalStaffMemberPage}
        />
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
        <Redirect to={path} />
      </Switch>
    </StaffAttendanceContextProvider>
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
