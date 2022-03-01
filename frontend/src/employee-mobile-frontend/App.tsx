// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import React, { useEffect } from 'react'
import {
  BrowserRouter as Router,
  generatePath,
  Redirect,
  Route,
  Switch,
  useHistory,
  useParams,
  useRouteMatch
} from 'react-router-dom'
import { ThemeProvider } from 'styled-components'

import ErrorPage from 'lib-components/molecules/ErrorPage'
import { theme } from 'lib-customizations/common'

import MobileReloadNotification from './components/MobileReloadNotification'
import RequireAuth from './components/RequireAuth'
import UnitList from './components/UnitList'
import AttendancePageWrapper from './components/attendances/AttendancePageWrapper'
import MarkAbsent from './components/attendances/actions/MarkAbsent'
import MarkAbsentBeforehand from './components/attendances/actions/MarkAbsentBeforehand'
import MarkDeparted from './components/attendances/actions/MarkDeparted'
import MarkPresent from './components/attendances/actions/MarkPresent'
import AttendanceChildPage from './components/attendances/child-info/AttendanceChildPage'
import ChildSensitiveInfoPage from './components/attendances/child-info/ChildSensitiveInfoPage'
import ChildNotes from './components/attendances/notes/ChildNotes'
import MessageEditorPage from './components/messages/MessageEditorPage'
import MessagesPage from './components/messages/MessagesPage'
import { UnreadMessagesPage } from './components/messages/UnreadMessagesPage'
import MobileLander from './components/mobile/MobileLander'
import PairingWizard from './components/mobile/PairingWizard'
import ExternalStaffMemberPage from './components/staff-attendance/ExternalStaffMemberPage'
import MarkExternalStaffMemberArrivalPage from './components/staff-attendance/MarkExternalStaffMemberArrivalPage'
import StaffAttendancesPage from './components/staff-attendance/StaffAttendancesPage'
import StaffMarkArrivedPage from './components/staff-attendance/StaffMarkArrivedPage'
import StaffMarkDepartedPage from './components/staff-attendance/StaffMarkDepartedPage'
import StaffMemberPage from './components/staff-attendance/StaffMemberPage'
import StaffPage from './components/staff/StaffPage'
import { ChildAttendanceContextProvider } from './state/child-attendance'
import { I18nContextProvider, useTranslation } from './state/i18n'
import { MessageContextProvider } from './state/messages'
import { StaffAttendanceContextProvider } from './state/staff-attendance'
import { UnitContextProvider } from './state/unit'
import { UserContextProvider } from './state/user'

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
                <Route exact path="/landing" render={() => <MobileLander />} />
                <Route exact path="/pairing" render={() => <PairingWizard />} />
                <Route
                  exact
                  path="/units"
                  render={() => (
                    <RequireAuth>
                      <UnitList />
                    </RequireAuth>
                  )}
                />
                <Route
                  path="/units/:unitId"
                  render={() => (
                    <RequireAuth>
                      <UnitRouter />
                    </RequireAuth>
                  )}
                />
                <Route path="/" render={() => <Redirect to="/landing" />} />
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
  const params = useParams<{ unitId: string }>()

  return (
    <UnitContextProvider unitId={params.unitId}>
      <Switch>
        <Route
          path={`${path}/groups/:groupId`}
          render={() => <GroupRouter />}
        />
        <Route
          path="/"
          render={() => (
            <Redirect to={`${generatePath(path, params)}/groups/all`} />
          )}
        />
      </Switch>
    </UnitContextProvider>
  )
}

function GroupRouter() {
  const { path } = useRouteMatch()
  const params = useParams()

  useGroupIdInLocalStorage()

  return (
    <MessageContextProvider>
      <Switch>
        <Route
          path={`${path}/child-attendance`}
          render={() => <ChildAttendanceRouter />}
        />
        <Route path={`${path}/staff`} render={() => <StaffRouter />} />
        <Route
          path={`${path}/staff-attendance`}
          render={() => <StaffAttendanceRouter />}
        />
        <Route path={`${path}/messages`} render={() => <MessagesRouter />} />
        <Route
          path="/"
          render={() => (
            <Redirect to={`${generatePath(path, params)}/child-attendance`} />
          )}
        />
      </Switch>
    </MessageContextProvider>
  )
}

function ChildAttendanceRouter() {
  const { path } = useRouteMatch()
  const params = useParams()

  return (
    <ChildAttendanceContextProvider>
      <Switch>
        <Route
          exact
          path={`${path}/list/:attendanceStatus`}
          render={() => <AttendancePageWrapper />}
        />
        <Route
          exact
          path={`${path}/:childId`}
          render={() => <AttendanceChildPage />}
        />
        <Route
          exact
          path={`${path}/:childId/mark-present`}
          render={() => <MarkPresent />}
        />
        <Route
          exact
          path={`${path}/:childId/mark-absent`}
          render={() => <MarkAbsent />}
        />
        <Route
          exact
          path={`${path}/:childId/mark-absent-beforehand`}
          render={() => <MarkAbsentBeforehand />}
        />
        <Route
          exact
          path={`${path}/:childId/mark-departed`}
          render={() => <MarkDeparted />}
        />
        <Route
          exact
          path={`${path}/:childId/note`}
          render={() => <ChildNotes />}
        />
        <Route
          exact
          path={`${path}/:childId/info`}
          render={() => (
            <RequireAuth strength="PIN">
              <ChildSensitiveInfoPage />
            </RequireAuth>
          )}
        />
        <Route
          path="/"
          render={() => (
            <Redirect to={`${generatePath(path, params)}/list/coming`} />
          )}
        />
      </Switch>
    </ChildAttendanceContextProvider>
  )
}

function StaffRouter() {
  const { path } = useRouteMatch()
  const params = useParams()

  return (
    <Switch>
      <Route exact path={path} render={() => <StaffPage />} />
      <Route
        path="/"
        render={() => <Redirect to={generatePath(path, params)} />}
      />
    </Switch>
  )
}

function StaffAttendanceRouter() {
  const { path } = useRouteMatch()
  const params = useParams()

  return (
    <StaffAttendanceContextProvider>
      <Switch>
        <Route
          exact
          path={`${path}/:tab(absent|present)`}
          render={() => <StaffAttendancesPage />}
        />
        <Route
          exact
          path={`${path}/external`}
          render={() => <MarkExternalStaffMemberArrivalPage />}
        />
        <Route
          exact
          path={`${path}/external/:attendanceId`}
          render={() => <ExternalStaffMemberPage />}
        />
        <Route
          exact
          path={`${path}/:employeeId`}
          render={() => <StaffMemberPage />}
        />
        <Route
          exact
          path={`${path}/:employeeId/mark-arrived`}
          render={() => <StaffMarkArrivedPage />}
        />
        <Route
          exact
          path={`${path}/:employeeId/mark-departed`}
          render={() => <StaffMarkDepartedPage />}
        />
        <Route
          path="/"
          render={() => (
            <Redirect to={`${generatePath(path, params)}/absent`} />
          )}
        />
      </Switch>
    </StaffAttendanceContextProvider>
  )
}

function MessagesRouter() {
  const { path } = useRouteMatch()

  return (
    <Switch>
      <Route
        exact
        path={path}
        render={() => (
          <RequireAuth strength="PIN">
            <MessagesPage />
          </RequireAuth>
        )}
      />
      <Route
        exact
        path={`${path}/unread-messages`}
        render={() => <UnreadMessagesPage />}
      />
      <Route
        exact
        path={`${path}/:childId/new-message`}
        render={() => (
          <RequireAuth strength="PIN">
            <MessageEditorPage />
          </RequireAuth>
        )}
      />
      <Route path="/" render={() => <Redirect to={path} />} />
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
