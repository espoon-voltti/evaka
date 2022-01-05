// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { theme } from 'lib-customizations/common'
import React, { useEffect } from 'react'
import {
  BrowserRouter as Router,
  Redirect,
  Route,
  Switch,
  useHistory,
  useParams,
  useRouteMatch
} from 'react-router-dom'
import { ThemeProvider } from 'styled-components'
import MarkAbsent from './components/attendances/actions/MarkAbsent'
import MarkAbsentBeforehand from './components/attendances/actions/MarkAbsentBeforehand'
import MarkDeparted from './components/attendances/actions/MarkDeparted'
import MarkPresent from './components/attendances/actions/MarkPresent'
import AttendancePageWrapper from './components/attendances/AttendancePageWrapper'
import AttendanceChildPage from './components/attendances/child-info/AttendanceChildPage'
import ChildSensitiveInfoPage from './components/attendances/child-info/ChildSensitiveInfoPage'
import ChildNotes from './components/attendances/notes/ChildNotes'
import requirePinAuth from './components/auth/requirePinAuth'
import EnsureAuthenticated from './components/EnsureAuthenticated'
import MobileLander from './components/mobile/MobileLander'
import PairingWizard from './components/mobile/PairingWizard'
import MobileReloadNotification from './components/MobileReloadNotification'
import ExternalStaffMemberPage from './components/staff-attendance/ExternalStaffMemberPage'
import MarkExternalStaffMemberArrivalPage from './components/staff-attendance/MarkExternalStaffMemberArrivalPage'
import StaffAttendancesPage from './components/staff-attendance/StaffAttendancesPage'
import StaffMarkArrivedPage from './components/staff-attendance/StaffMarkArrivedPage'
import StaffMarkDepartedPage from './components/staff-attendance/StaffMarkDepartedPage'
import StaffMemberPage from './components/staff-attendance/StaffMemberPage'
import StaffPage from './components/staff/StaffPage'
import { ChildAttendanceContextProvider } from './state/child-attendance'
import { I18nContextProvider, useTranslation } from './state/i18n'
import { StaffAttendanceContextProvider } from './state/staff-attendance'
import { UnitContextProvider } from './state/unit'
import { UserContextProvider } from './state/user'
import MessagesPage from './components/messages/MessagesPage'
import { MessageContextProvider } from './state/messages'
import MessageEditorPage from './components/messages/MessageEditorPage'
import UnitList from './components/UnitList'
import { UnreadMessagesPage } from './components/messages/UnreadMessagesPage'

export default function App() {
  const { i18n } = useTranslation()

  return (
    <I18nContextProvider>
      <ThemeProvider theme={theme}>
        <ErrorBoundary
          fallback={() => (
            <ErrorPage basePath="/employee/mobile" labels={i18n.errorPage} />
          )}
        >
          <UserContextProvider>
            <Router basename="/employee/mobile">
              <Switch>
                <Route exact path="/landing" component={MobileLander} />
                <Route exact path="/pairing" component={PairingWizard} />
                <Route exact path="/units">
                  <EnsureAuthenticated>
                    <UnitList />
                  </EnsureAuthenticated>
                </Route>
                <Route path="/units/:unitId">
                  <EnsureAuthenticated>
                    <UnitRouter />
                  </EnsureAuthenticated>
                </Route>
                <Redirect to="/landing" />
              </Switch>
            </Router>
            <MobileReloadNotification />
          </UserContextProvider>
        </ErrorBoundary>
      </ThemeProvider>
    </I18nContextProvider>
  )
}

function UnitRouter() {
  const { path } = useRouteMatch()
  const { unitId } = useParams<{ unitId: string }>()

  return (
    <UnitContextProvider unitId={unitId}>
      <Switch>
        <Route path={`${path}/groups/:groupId`} component={GroupRouter} />
        <Redirect to={`${path}/groups/all`} />
      </Switch>
    </UnitContextProvider>
  )
}

function GroupRouter() {
  const { path } = useRouteMatch()
  useGroupIdInLocalStorage()

  return (
    <MessageContextProvider>
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
        <Route path={`${path}/messages`} component={MessagesRouter} />
        <Redirect to={`${path}/child-attendance`} />
      </Switch>
    </MessageContextProvider>
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
        <Route
          exact
          path={`${path}/:childId/info`}
          component={requirePinAuth(ChildSensitiveInfoPage)}
        />
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
        <Route
          exact
          path={`${path}/:tab(absent|present)`}
          component={StaffAttendancesPage}
        />
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
        <Redirect to={`${path}/absent`} />
      </Switch>
    </StaffAttendanceContextProvider>
  )
}

function MessagesRouter() {
  const { path } = useRouteMatch()

  return (
    <Switch>
      <Route exact path={path} component={requirePinAuth(MessagesPage)} />
      <Route
        exact
        path={`${path}/unread-messages`}
        component={UnreadMessagesPage}
      />
      <Route
        exact
        path={`${path}/:childId/new-message`}
        component={requirePinAuth(MessageEditorPage)}
      />
      <Redirect to={path} />
    </Switch>
  )
}

const groupIdKey = 'evakaEmployeeMobileGroupId'

function useGroupIdInLocalStorage() {
  const history = useHistory()
  const { unitId, groupId } = useParams<{ unitId: string; groupId: string }>()

  useEffect(() => {
    try {
      const storedGroupId = window.localStorage?.getItem(groupIdKey)

      if (groupId === 'all' && storedGroupId && groupId !== storedGroupId) {
        history.replace(`/units/${unitId}/groups/${storedGroupId}`)
      }
    } catch (e) {
      // do nothing
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    try {
      window.localStorage?.setItem(groupIdKey, groupId)
    } catch (e) {
      // do nothing
    }
  }, [groupId])
}
