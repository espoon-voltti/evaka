// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import React, { useContext, useEffect } from 'react'
import {
  BrowserRouter as Router,
  Navigate,
  Route,
  Routes,
  useNavigate
} from 'react-router-dom'
import { ThemeProvider } from 'styled-components'

import { useQuery } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import {
  Notifications,
  NotificationsContextProvider
} from 'lib-components/Notifications'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { theme } from 'lib-customizations/common'

import RequireAuth from './RequireAuth'
import UnitList from './UnitList'
import { UserContext, UserContextProvider } from './auth/state'
import AttendancePageWrapper from './child-attendance/AttendancePageWrapper'
import MarkAbsent from './child-attendance/actions/MarkAbsent'
import MarkAbsentBeforehand from './child-attendance/actions/MarkAbsentBeforehand'
import MarkDeparted from './child-attendance/actions/MarkDeparted'
import MarkPresent from './child-attendance/actions/MarkPresent'
import {
  attendanceStatusesQuery,
  childrenQuery
} from './child-attendance/queries'
import AttendanceChildPage from './child-info/AttendanceChildPage'
import ChildSensitiveInfoPage from './child-info/ChildSensitiveInfoPage'
import ChildNotes from './child-notes/ChildNotes'
import { I18nContextProvider, useTranslation } from './common/i18n'
import { ServiceWorkerContextProvider } from './common/service-worker'
import { UnitContextProvider } from './common/unit'
import MessageEditorPage from './messages/MessageEditorPage'
import MessagesPage from './messages/MessagesPage'
import { UnreadMessagesPage } from './messages/UnreadMessagesPage'
import { MessageContextProvider } from './messages/state'
import MobileLander from './pairing/MobileLander'
import PairingWizard from './pairing/PairingWizard'
import { queryClient, QueryClientProvider } from './query'
import ExternalStaffMemberPage from './staff-attendance/ExternalStaffMemberPage'
import MarkExternalStaffMemberArrivalPage from './staff-attendance/MarkExternalStaffMemberArrivalPage'
import StaffAttendancesPage from './staff-attendance/StaffAttendancesPage'
import StaffMarkArrivedPage from './staff-attendance/StaffMarkArrivedPage'
import StaffMarkDepartedPage from './staff-attendance/StaffMarkDepartedPage'
import StaffMemberPage from './staff-attendance/StaffMemberPage'
import { StaffAttendanceContextProvider } from './staff-attendance/state'
import StaffPage from './staff/StaffPage'

export default function App() {
  const { i18n } = useTranslation()
  const { apiVersion } = useContext(UserContext)

  return (
    <QueryClientProvider client={queryClient}>
      <I18nContextProvider>
        <ThemeProvider theme={theme}>
          <ErrorBoundary
            fallback={() => (
              <ErrorPage basePath="/employee/mobile" labels={i18n.errorPage} />
            )}
          >
            <ServiceWorkerContextProvider>
              <UserContextProvider>
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
              </UserContextProvider>
            </ServiceWorkerContextProvider>
          </ErrorBoundary>
        </ThemeProvider>
      </I18nContextProvider>
    </QueryClientProvider>
  )
}

function UnitRouter() {
  const params = useNonNullableParams<{ unitId: string }>()

  return (
    <UnitContextProvider unitId={params.unitId ?? 'all'}>
      <Routes>
        <Route path="/groups/:groupId/*" element={<GroupRouter />} />
        <Route index element={<Navigate replace to="groups/all" />} />
      </Routes>
    </UnitContextProvider>
  )
}

function GroupRouter() {
  useGroupIdInLocalStorage()

  return (
    <MessageContextProvider>
      <Routes>
        <Route path="child-attendance/*" element={<ChildAttendanceRouter />} />
        <Route path="staff/*" element={<StaffRouter />} />
        <Route path="staff-attendance/*" element={<StaffAttendanceRouter />} />
        <Route path="messages/*" element={<MessagesRouter />} />
        <Route index element={<Navigate replace to="child-attendance" />} />
      </Routes>
    </MessageContextProvider>
  )
}

function ChildAttendanceRouter() {
  // Re-fetch child data when navigating to the attendance section
  const { unitId } = useNonNullableParams<{ unitId: UUID }>()
  useQuery(childrenQuery(unitId), { refetchOnMount: 'always' })
  useQuery(attendanceStatusesQuery(unitId), { refetchOnMount: 'always' })

  return (
    <Routes>
      <Route
        path="list/:attendanceStatus"
        element={<AttendancePageWrapper />}
      />
      <Route path=":childId" element={<AttendanceChildPage />} />
      <Route path=":childId/mark-present" element={<MarkPresent />} />
      <Route path=":childId/mark-absent" element={<MarkAbsent />} />
      <Route
        path=":childId/mark-absent-beforehand"
        element={<MarkAbsentBeforehand />}
      />
      <Route path=":childId/mark-departed" element={<MarkDeparted />} />
      <Route path=":childId/note" element={<ChildNotes />} />
      <Route
        path=":childId/info"
        element={
          <RequireAuth strength="PIN">
            <ChildSensitiveInfoPage />
          </RequireAuth>
        }
      />
      <Route
        path=":childId/new-message"
        element={
          <RequireAuth strength="PIN">
            <MessageEditorPage />
          </RequireAuth>
        }
      />
      <Route index element={<Navigate replace to="list/coming" />} />
    </Routes>
  )
}

function StaffRouter() {
  return (
    <Routes>
      <Route index element={<StaffPage />} />
    </Routes>
  )
}

function StaffAttendanceRouter() {
  return (
    <StaffAttendanceContextProvider>
      <Routes>
        <Route path="absent" element={<StaffAttendancesPage tab="absent" />} />
        <Route
          path="present"
          element={<StaffAttendancesPage tab="present" />}
        />
        <Route
          path="external"
          element={<MarkExternalStaffMemberArrivalPage />}
        />
        <Route
          path="external/:attendanceId"
          element={<ExternalStaffMemberPage />}
        />
        <Route path=":employeeId" element={<StaffMemberPage />} />
        <Route
          path=":employeeId/mark-arrived"
          element={<StaffMarkArrivedPage />}
        />
        <Route
          path=":employeeId/mark-departed"
          element={<StaffMarkDepartedPage />}
        />
        <Route index element={<Navigate replace to="absent" />} />
      </Routes>
    </StaffAttendanceContextProvider>
  )
}

function MessagesRouter() {
  return (
    <Routes>
      <Route
        index
        element={
          <RequireAuth strength="PIN">
            <MessagesPage />
          </RequireAuth>
        }
      />
      <Route path="unread-messages" element={<UnreadMessagesPage />} />
    </Routes>
  )
}

const groupIdKey = 'evakaEmployeeMobileGroupId'

function useGroupIdInLocalStorage() {
  const navigate = useNavigate()
  const { unitId, groupId } = useNonNullableParams<{
    unitId: string
    groupId: string
  }>()

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
