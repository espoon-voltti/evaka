// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import React, { lazy, Suspense, useEffect } from 'react'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { ThemeProvider } from 'styled-components'

import useNonNullableParams from 'lib-common/useNonNullableParams'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { theme } from 'lib-customizations/common'

import MobileReloadNotification from './components/MobileReloadNotification'
import RequireAuth from './components/RequireAuth'
import { ChildAttendanceContextProvider } from './state/child-attendance'
import { I18nContextProvider, useTranslation } from './state/i18n'
import { MessageContextProvider } from './state/messages'
import { StaffAttendanceContextProvider } from './state/staff-attendance'
import { UnitContextProvider } from './state/unit'
import { UserContextProvider } from './state/user'

const MobileLander = lazy(
  () =>
    import(
      /* webpackChunkName: "MobileLander" */ './components/mobile/MobileLander'
    )
)
const PairingWizard = lazy(() => import('./components/mobile/PairingWizard'))
const UnitList = lazy(() => import('./components/UnitList'))

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
            <Suspense fallback={<SpinnerSegment />}>
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
            </Suspense>
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

const AttendancePageWrapper = lazy(
  () => import('./components/attendances/AttendancePageWrapper')
)
const AttendanceChildPage = lazy(
  () => import('./components/attendances/child-info/AttendanceChildPage')
)
const MarkAbsent = lazy(
  () => import('./components/attendances/actions/MarkAbsent')
)
const MarkAbsentBeforehand = lazy(
  () => import('./components/attendances/actions/MarkAbsentBeforehand')
)
const MarkDeparted = lazy(
  () => import('./components/attendances/actions/MarkDeparted')
)
const MarkPresent = lazy(
  () => import('./components/attendances/actions/MarkPresent')
)
const ChildSensitiveInfoPage = lazy(
  () => import('./components/attendances/child-info/ChildSensitiveInfoPage')
)
const ChildNotes = lazy(
  () => import('./components/attendances/notes/ChildNotes')
)

function ChildAttendanceRouter() {
  return (
    <ChildAttendanceContextProvider>
      <Suspense fallback={<SpinnerSegment />}>
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
      </Suspense>
    </ChildAttendanceContextProvider>
  )
}

const StaffPage = lazy(() => import('./components/staff/StaffPage'))

function StaffRouter() {
  return (
    <Routes>
      <Suspense fallback={<SpinnerSegment />}>
        <Route index element={<StaffPage />} />
      </Suspense>
    </Routes>
  )
}

const StaffAttendancesPage = lazy(
  () => import('./components/staff-attendance/StaffAttendancesPage')
)
const MarkExternalStaffMemberArrivalPage = lazy(
  () =>
    import('./components/staff-attendance/MarkExternalStaffMemberArrivalPage')
)
const ExternalStaffMemberPage = lazy(
  () => import('./components/staff-attendance/ExternalStaffMemberPage')
)
const StaffMarkArrivedPage = lazy(
  () => import('./components/staff-attendance/StaffMarkArrivedPage')
)
const StaffMarkDepartedPage = lazy(
  () => import('./components/staff-attendance/StaffMarkDepartedPage')
)
const StaffMemberPage = lazy(
  () => import('./components/staff-attendance/StaffMemberPage')
)

function StaffAttendanceRouter() {
  return (
    <StaffAttendanceContextProvider>
      <Suspense fallback={<SpinnerSegment />}>
        <Routes>
          <Route
            path="absent"
            element={<StaffAttendancesPage tab="absent" />}
          />
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
      </Suspense>
    </StaffAttendanceContextProvider>
  )
}

const MessageEditorPage = lazy(
  () => import('./components/messages/MessageEditorPage')
)
const MessagesPage = lazy(() => import('./components/messages/MessagesPage'))
const UnreadMessagesPage = lazy(() =>
  import('./components/messages/UnreadMessagesPage').then(
    ({ UnreadMessagesPage }) => ({ default: UnreadMessagesPage })
  )
)

function MessagesRouter() {
  return (
    <Suspense fallback={<SpinnerSegment />}>
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
    </Suspense>
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
