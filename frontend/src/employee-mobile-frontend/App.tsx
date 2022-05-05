// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import React, { useEffect } from 'react'
import {
  BrowserRouter as Router,
  Navigate,
  Route,
  Routes,
  useNavigate
} from 'react-router-dom'
import { ThemeProvider } from 'styled-components'

import useNonNullableParams from 'lib-common/useNonNullableParams'
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
                <Route index element={<Navigate replace to="/landing" />} />
              </Routes>
            </Router>
            <MobileReloadNotification />
          </UserContextProvider>
        </ErrorBoundary>
      </ThemeProvider>
    </I18nContextProvider>
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
  return (
    <ChildAttendanceContextProvider>
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
        <Route index element={<Navigate replace to="list/coming" />} />
      </Routes>
    </ChildAttendanceContextProvider>
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
      <Route
        path=":childId/new-message"
        element={
          <RequireAuth strength="PIN">
            <MessageEditorPage />
          </RequireAuth>
        }
      />
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
