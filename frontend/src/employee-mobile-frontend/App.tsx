// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isPropValid from '@emotion/is-prop-valid'
import { ErrorBoundary } from '@sentry/react'
import React, { useContext, useEffect } from 'react'
import {
  BrowserRouter as Router,
  Navigate,
  Route,
  Routes,
  useNavigate
} from 'react-router-dom'
import { StyleSheetManager, ThemeProvider } from 'styled-components'

import { useQuery } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import {
  Notifications,
  NotificationsContextProvider
} from 'lib-components/Notifications'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { theme } from 'lib-customizations/common'

import RequireAuth from './RequireAuth'
import { RequirePinAuth } from './RequirePinAuth'
import { UserContext, UserContextProvider } from './auth/state'
import AttendancePageWrapper from './child-attendance/AttendancePageWrapper'
import AttendanceTodayWrapper from './child-attendance/AttendanceTodayWrapper'
import ConfimedReservationDaysWrapper from './child-attendance/ConfimedReservationDaysWrapper'
import MarkAbsent from './child-attendance/actions/MarkAbsent'
import MarkAbsentBeforehand from './child-attendance/actions/MarkAbsentBeforehand'
import MarkDeparted from './child-attendance/actions/MarkDeparted'
import MarkPresent from './child-attendance/actions/MarkPresent'
import MarkReservations from './child-attendance/actions/MarkReservations'
import {
  attendanceStatusesQuery,
  childrenQuery
} from './child-attendance/queries'
import AttendanceChildPage from './child-info/AttendanceChildPage'
import ChildSensitiveInfoPage from './child-info/ChildSensitiveInfoPage'
import ChildNotes from './child-notes/ChildNotes'
import { I18nContextProvider, useTranslation } from './common/i18n'
import { ServiceWorkerContextProvider } from './common/service-worker'
import MessagesPage from './messages/MessagesPage'
import NewChildMessagePage from './messages/NewChildMessagePage'
import { UnreadMessagesPage } from './messages/UnreadMessagesPage'
import { MessageContextProvider } from './messages/state'
import MobileLander from './pairing/MobileLander'
import PairingWizard from './pairing/PairingWizard'
import { queryClient, QueryClientProvider } from './query'
import { SettingsPage } from './settings/SettingsPage'
import StaffPage from './staff/StaffPage'
import ExternalStaffMemberPage from './staff-attendance/ExternalStaffMemberPage'
import MarkExternalStaffMemberArrivalPage from './staff-attendance/MarkExternalStaffMemberArrivalPage'
import StaffAttendanceEditPage from './staff-attendance/StaffAttendanceEditPage'
import StaffAttendancesPage from './staff-attendance/StaffAttendancesPage'
import StaffMarkArrivedPage from './staff-attendance/StaffMarkArrivedPage'
import StaffMarkDepartedPage from './staff-attendance/StaffMarkDepartedPage'
import StaffMemberPage from './staff-attendance/StaffMemberPage'
import UnitList from './units/UnitList'

export default function App() {
  const { i18n } = useTranslation()
  const { apiVersion } = useContext(UserContext)

  return (
    <QueryClientProvider client={queryClient}>
      <I18nContextProvider>
        <StyleSheetManager shouldForwardProp={shouldForwardProp}>
          <ThemeProvider theme={theme}>
            <ErrorBoundary
              fallback={() => (
                <ErrorPage
                  basePath="/employee/mobile"
                  labels={i18n.errorPage}
                />
              )}
            >
              <UserContextProvider>
                <ServiceWorkerContextProvider>
                  <NotificationsContextProvider>
                    <Notifications apiVersion={apiVersion} />
                    <Router basename="/employee/mobile">
                      <Routes>
                        <Route path="/landing" element={<MobileLander />} />
                        <Route path="/pairing" element={<PairingWizard />} />
                        <Route
                          path="/units"
                          element={
                            <RequireAuth>
                              <UnitList />
                            </RequireAuth>
                          }
                        />
                        <Route
                          path="/units/:unitId/*"
                          element={
                            <RequireAuth>
                              <UnitRouter />
                            </RequireAuth>
                          }
                        />
                        <Route
                          index
                          element={<Navigate replace to="/landing" />}
                        />
                      </Routes>
                    </Router>
                  </NotificationsContextProvider>
                </ServiceWorkerContextProvider>
              </UserContextProvider>
            </ErrorBoundary>
          </ThemeProvider>
        </StyleSheetManager>
      </I18nContextProvider>
    </QueryClientProvider>
  )
}

// This implements the default behavior from styled-components v5
// TODO: Prefix all custom props with $, then remove this
function shouldForwardProp(propName: string, target: unknown) {
  if (typeof target === 'string') {
    // For HTML elements, forward the prop if it is a valid HTML attribute
    return isPropValid(propName)
  }
  // For other elements, forward all props
  return true
}

function UnitRouter() {
  const { unitId } = useRouteParams(['unitId'])

  return (
    <Routes>
      <Route path="/settings" element={<SettingsPage unitId={unitId} />} />
      <Route
        path="/groups/:groupId/*"
        element={<GroupRouter unitId={unitId} />}
      />
      <Route index element={<Navigate replace to="groups/all" />} />
    </Routes>
  )
}

function GroupRouter({ unitId }: { unitId: UUID }) {
  useGroupIdInLocalStorage()

  return (
    <MessageContextProvider unitId={unitId}>
      <Routes>
        <Route
          path="child-attendance/*"
          element={<ChildAttendanceRouter unitId={unitId} />}
        />
        <Route path="staff/*" element={<StaffRouter unitId={unitId} />} />
        <Route
          path="staff-attendance/*"
          element={<StaffAttendanceRouter unitId={unitId} />}
        />
        <Route path="messages/*" element={<MessagesRouter unitId={unitId} />} />
        <Route index element={<Navigate replace to="child-attendance" />} />
      </Routes>
    </MessageContextProvider>
  )
}

function ChildAttendanceRouter({ unitId }: { unitId: UUID }) {
  // Re-fetch child data when navigating to the attendance section
  useQuery(childrenQuery(unitId), { refetchOnMount: 'always' })
  useQuery(attendanceStatusesQuery(unitId), { refetchOnMount: 'always' })

  return (
    <Routes>
      <Route path="/" element={<AttendancePageWrapper unitId={unitId} />}>
        <Route
          path="list/:attendanceStatus"
          element={<AttendanceTodayWrapper />}
        />
        <Route
          path="daylist"
          id="daylist"
          element={<ConfimedReservationDaysWrapper unitId={unitId} />}
        />
        <Route path="list" element={<Navigate replace to="/" />} />
      </Route>

      <Route
        path=":childId"
        element={<AttendanceChildPage unitId={unitId} />}
      />
      <Route
        path=":childId/mark-present"
        element={<MarkPresent unitId={unitId} />}
      />
      <Route
        path=":childId/mark-absent"
        element={<MarkAbsent unitId={unitId} />}
      />
      <Route
        path=":childId/mark-reservations"
        element={<MarkReservations unitId={unitId} />}
      />
      <Route
        path=":childId/mark-absent-beforehand"
        element={<MarkAbsentBeforehand unitId={unitId} />}
      />
      <Route
        path=":childId/mark-departed"
        element={<MarkDeparted unitId={unitId} />}
      />
      <Route path=":childId/note" element={<ChildNotes unitId={unitId} />} />
      <Route
        path=":childId/info"
        element={
          <RequirePinAuth unitId={unitId}>
            <ChildSensitiveInfoPage unitId={unitId} />
          </RequirePinAuth>
        }
      />
      <Route
        path=":childId/new-message"
        element={
          <RequirePinAuth unitId={unitId}>
            <NewChildMessagePage unitId={unitId} />
          </RequirePinAuth>
        }
      />
      <Route index element={<Navigate replace to="list/coming" />} />
    </Routes>
  )
}

function StaffRouter({ unitId }: { unitId: UUID }) {
  return (
    <Routes>
      <Route index element={<StaffPage unitId={unitId} />} />
    </Routes>
  )
}

function StaffAttendanceRouter({ unitId }: { unitId: UUID }) {
  return (
    <Routes>
      <Route
        path="absent"
        element={<StaffAttendancesPage tab="absent" unitId={unitId} />}
      />
      <Route
        path="present"
        element={<StaffAttendancesPage tab="present" unitId={unitId} />}
      />
      <Route
        path="external"
        element={<MarkExternalStaffMemberArrivalPage unitId={unitId} />}
      />
      <Route
        path="external/:attendanceId"
        element={<ExternalStaffMemberPage unitId={unitId} />}
      />
      <Route path=":employeeId" element={<StaffMemberPage unitId={unitId} />} />
      <Route
        path=":employeeId/edit"
        element={<StaffAttendanceEditPage unitId={unitId} />}
      />
      <Route
        path=":employeeId/mark-arrived"
        element={<StaffMarkArrivedPage unitId={unitId} />}
      />
      <Route
        path=":employeeId/mark-departed"
        element={<StaffMarkDepartedPage unitId={unitId} />}
      />
      <Route index element={<Navigate replace to="absent" />} />
    </Routes>
  )
}

function MessagesRouter({ unitId }: { unitId: UUID }) {
  return (
    <Routes>
      <Route
        index
        element={
          <RequirePinAuth unitId={unitId}>
            <MessagesPage unitId={unitId} />
          </RequirePinAuth>
        }
      />
      <Route
        path="unread-messages"
        element={<UnreadMessagesPage unitId={unitId} />}
      />
    </Routes>
  )
}

const groupIdKey = 'evakaEmployeeMobileGroupId'

function useGroupIdInLocalStorage() {
  const navigate = useNavigate()
  const { unitId, groupId } = useRouteParams(['unitId', 'groupId'])

  useEffect(() => {
    try {
      const storedGroupId = window.localStorage?.getItem(groupIdKey)

      if (
        unitId &&
        groupId === 'all' &&
        storedGroupId &&
        groupId !== storedGroupId
      ) {
        navigate(`/units/${unitId}/groups/${storedGroupId}`, {
          replace: true
        })
      }
    } catch (e) {
      // do nothing
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    try {
      window.localStorage?.setItem(groupIdKey, groupId ?? 'all')
    } catch (e) {
      // do nothing
    }
  }, [groupId])
}
