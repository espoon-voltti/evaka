// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isPropValid from '@emotion/is-prop-valid'
import { ErrorBoundary } from '@sentry/react'
import React, { useContext, useEffect, useMemo } from 'react'
import {
  BrowserRouter as Router,
  Navigate,
  Route,
  Routes,
  useNavigate
} from 'react-router-dom'
import { StyleSheetManager, ThemeProvider } from 'styled-components'

import { useQuery, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { Uri, uri } from 'lib-common/uri'
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
import { SelectedGroupId, toSelectedGroupId } from './common/selected-group'
import { ServiceWorkerContextProvider } from './common/service-worker'
import MessagesPage from './messages/MessagesPage'
import NewChildMessagePage from './messages/NewChildMessagePage'
import { UnreadMessagesPage } from './messages/UnreadMessagesPage'
import { MessageContextProvider } from './messages/state'
import MobileLander from './pairing/MobileLander'
import PairingWizard from './pairing/PairingWizard'
import { queryClient, QueryClientProvider } from './query'
import { RememberContext, RememberContextProvider } from './remember'
import { SettingsPage } from './settings/SettingsPage'
import StaffPage from './staff/StaffPage'
import ExternalStaffMemberPage from './staff-attendance/ExternalStaffMemberPage'
import MarkExternalStaffMemberArrivalPage from './staff-attendance/MarkExternalStaffMemberArrivalPage'
import StaffAttendanceEditPage from './staff-attendance/StaffAttendanceEditPage'
import StaffAttendancesPage from './staff-attendance/StaffAttendancesPage'
import StaffMarkArrivedPage from './staff-attendance/StaffMarkArrivedPage'
import StaffMarkDepartedPage from './staff-attendance/StaffMarkDepartedPage'
import StaffMemberPage from './staff-attendance/StaffMemberPage'
import { ChildAttendanceUIState } from './types'
import UnitList from './units/UnitList'
import { unitInfoQuery } from './units/queries'

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
                    <RememberContextProvider>
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
                    </RememberContextProvider>
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
  const { groupId } = useRouteParams([], ['groupId'])
  const selectedGroupId: SelectedGroupId = useMemo(
    () => toSelectedGroupId({ unitId, groupId }),
    [unitId, groupId]
  )

  const { saveGroupId } = useContext(RememberContext)
  useEffect(
    () =>
      saveGroupId(
        selectedGroupId.type === 'one' ? selectedGroupId.id : undefined
      ),
    [saveGroupId, selectedGroupId]
  )

  const unitInfoResponse = useQueryResult(
    unitInfoQuery({ unitId: selectedGroupId.unitId })
  )

  const navigate = useNavigate()
  useEffect(() => {
    // If we somehow end up with a groupId that doesn't exist in the current unit,
    // just navigate to some "default page" instead of allowing things to break
    if (selectedGroupId.type === 'one' && unitInfoResponse.isSuccess) {
      const validGroupId = unitInfoResponse.value.groups.some(
        (group) => group.id === selectedGroupId.id
      )
      if (!validGroupId) {
        navigate(
          routes.childAttendances(
            toSelectedGroupId({
              unitId: selectedGroupId.unitId,
              groupId: undefined
            })
          ).value
        )
      }
    }
  }, [navigate, selectedGroupId, unitInfoResponse])

  return (
    <MessageContextProvider selectedGroupId={selectedGroupId}>
      <Routes>
        <Route
          path="child-attendance/*"
          element={<ChildAttendanceRouter selectedGroupId={selectedGroupId} />}
        />
        <Route
          path="staff/*"
          element={<StaffRouter selectedGroupId={selectedGroupId} />}
        />
        <Route
          path="staff-attendance/*"
          element={<StaffAttendanceRouter selectedGroupId={selectedGroupId} />}
        />
        <Route
          path="messages/*"
          element={<MessagesRouter selectedGroupId={selectedGroupId} />}
        />
        <Route index element={<Navigate replace to="child-attendance" />} />
      </Routes>
    </MessageContextProvider>
  )
}

function ChildAttendanceRouter({
  selectedGroupId
}: {
  selectedGroupId: SelectedGroupId
}) {
  // Re-fetch child data when navigating to the attendance section
  useQuery(childrenQuery(selectedGroupId.unitId), { refetchOnMount: 'always' })
  useQuery(attendanceStatusesQuery(selectedGroupId.unitId), {
    refetchOnMount: 'always'
  })

  return (
    <Routes>
      <Route
        path="/"
        element={<AttendancePageWrapper selectedGroupId={selectedGroupId} />}
      >
        <Route
          path="list/:attendanceStatus"
          element={<AttendanceTodayWrapper selectedGroupId={selectedGroupId} />}
        />
        <Route
          path="daylist"
          id="daylist"
          element={
            <ConfimedReservationDaysWrapper selectedGroupId={selectedGroupId} />
          }
        />
        <Route path="list" element={<Navigate replace to="/" />} />
      </Route>

      <Route
        path=":childId"
        element={<AttendanceChildPage selectedGroupId={selectedGroupId} />}
      />
      <Route
        path=":childId/mark-present"
        element={<MarkPresent unitId={selectedGroupId.unitId} />}
      />
      <Route
        path=":childId/mark-absent"
        element={<MarkAbsent unitId={selectedGroupId.unitId} />}
      />
      <Route
        path=":childId/mark-reservations"
        element={<MarkReservations selectedGroupId={selectedGroupId} />}
      />
      <Route
        path=":childId/mark-absent-beforehand"
        element={<MarkAbsentBeforehand unitId={selectedGroupId.unitId} />}
      />
      <Route
        path=":childId/mark-departed"
        element={<MarkDeparted unitId={selectedGroupId.unitId} />}
      />
      <Route
        path=":childId/note"
        element={<ChildNotes selectedGroupId={selectedGroupId} />}
      />
      <Route
        path=":childId/info"
        element={
          <RequirePinAuth unitId={selectedGroupId.unitId}>
            <ChildSensitiveInfoPage unitId={selectedGroupId.unitId} />
          </RequirePinAuth>
        }
      />
      <Route
        path=":childId/new-message"
        element={
          <RequirePinAuth unitId={selectedGroupId.unitId}>
            <NewChildMessagePage unitId={selectedGroupId.unitId} />
          </RequirePinAuth>
        }
      />
      <Route index element={<Navigate replace to="list/coming" />} />
    </Routes>
  )
}

function StaffRouter({
  selectedGroupId
}: {
  selectedGroupId: SelectedGroupId
}) {
  return (
    <Routes>
      <Route index element={<StaffPage selectedGroupId={selectedGroupId} />} />
    </Routes>
  )
}

function StaffAttendanceRouter({
  selectedGroupId
}: {
  selectedGroupId: SelectedGroupId
}) {
  return (
    <Routes>
      <Route
        path="absent"
        element={
          <StaffAttendancesPage
            tab="absent"
            selectedGroupId={selectedGroupId}
          />
        }
      />
      <Route
        path="present"
        element={
          <StaffAttendancesPage
            tab="present"
            selectedGroupId={selectedGroupId}
          />
        }
      />
      <Route
        path="external"
        element={
          <MarkExternalStaffMemberArrivalPage
            selectedGroupId={selectedGroupId}
          />
        }
      />
      <Route
        path="external/:attendanceId"
        element={<ExternalStaffMemberPage unitId={selectedGroupId.unitId} />}
      />
      <Route
        path=":employeeId"
        element={<StaffMemberPage selectedGroupId={selectedGroupId} />}
      />
      <Route
        path=":employeeId/edit"
        element={<StaffAttendanceEditPage selectedGroupId={selectedGroupId} />}
      />
      <Route
        path=":employeeId/mark-arrived"
        element={<StaffMarkArrivedPage selectedGroupId={selectedGroupId} />}
      />
      <Route
        path=":employeeId/mark-departed"
        element={<StaffMarkDepartedPage selectedGroupId={selectedGroupId} />}
      />
      <Route index element={<Navigate replace to="absent" />} />
    </Routes>
  )
}

function MessagesRouter({
  selectedGroupId
}: {
  selectedGroupId: SelectedGroupId
}) {
  return (
    <Routes>
      <Route
        index
        element={
          <RequirePinAuth unitId={selectedGroupId.unitId}>
            <MessagesPage selectedGroupId={selectedGroupId} />
          </RequirePinAuth>
        }
      />
      <Route
        path="unread-messages"
        element={<UnreadMessagesPage selectedGroupId={selectedGroupId} />}
      />
    </Routes>
  )
}

export const routes = {
  group(group: SelectedGroupId): Uri {
    const id = group.type === 'all' ? 'all' : group.id
    return uri`/units/${group.unitId}/groups/${id}`
  },
  settings(unitId: UUID): Uri {
    return uri`/units/${unitId}/settings`
  },
  childAttendanceList(
    group: SelectedGroupId,
    state: ChildAttendanceUIState
  ): Uri {
    return uri`${this.group(group)}/child-attendance/list/${state}`
  },
  childAttendance(group: SelectedGroupId, child: UUID): Uri {
    return uri`${this.group(group)}/child-attendance/${child}`
  },
  childAttendances(group: SelectedGroupId): Uri {
    return uri`${this.group(group)}/child-attendance/`
  },
  childAttendanceDaylist(group: SelectedGroupId): Uri {
    return uri`${this.group(group)}/child-attendance/daylist`
  },
  markAbsentBeforehand(group: SelectedGroupId, child: UUID): Uri {
    return uri`${this.group(group)}/child-attendance/${child}/mark-absent-beforehand`
  },
  markPresent(group: SelectedGroupId, child: UUID): Uri {
    return uri`${this.group(group)}/child-attendance/${child}/mark-present`
  },
  markAbsent(group: SelectedGroupId, child: UUID): Uri {
    return uri`${this.group(group)}/child-attendance/${child}/mark-absent`
  },
  markDeparted(group: SelectedGroupId, child: UUID): Uri {
    return uri`${this.group(group)}/child-attendance/${child}/mark-departed`
  },
  markReservations(group: SelectedGroupId, child: UUID): Uri {
    return uri`${this.group(group)}/child-attendance/${child}/mark-reservations`
  },
  childNotes(group: SelectedGroupId, child: UUID): Uri {
    return uri`${this.group(group)}/child-attendance/${child}/note`
  },
  newChildMessage(group: SelectedGroupId, child: UUID): Uri {
    return uri`${this.group(group)}/child-attendance/${child}/new-message`
  },
  childSensitiveInfo(group: SelectedGroupId, child: UUID): Uri {
    return uri`${this.group(group)}/child-attendance/${child}/info`
  },
  staff(group: SelectedGroupId): Uri {
    return uri`${this.group(group)}/staff`
  },
  staffAttendances(group: SelectedGroupId, tab: 'absent' | 'present'): Uri {
    return uri`${this.group(group)}/staff-attendance/${tab}`
  },
  externalStaffAttendances(group: SelectedGroupId): Uri {
    return uri`${this.group(group)}/staff-attendance/external`
  },
  externalStaffAttendance(group: SelectedGroupId, attendanceId: UUID): Uri {
    return uri`${this.group(group)}/staff-attendance/external/${attendanceId}`
  },
  staffAttendance(group: SelectedGroupId, employeeId: UUID): Uri {
    return uri`${this.group(group)}/staff-attendance/${employeeId}`
  },
  staffAttendanceEdit(group: SelectedGroupId, employeeId: UUID): Uri {
    return uri`${this.group(group)}/staff-attendance/${employeeId}/edit`
  },
  staffMarkArrived(group: SelectedGroupId, employeeId: UUID): Uri {
    return uri`${this.group(group)}/staff-attendance/${employeeId}/mark-arrived`
  },
  staffMarkDeparted(group: SelectedGroupId, employeeId: UUID): Uri {
    return uri`${this.group(group)}/staff-attendance/${employeeId}/mark-departed`
  },
  messages(group: SelectedGroupId): Uri {
    return uri`${this.group(group)}/messages`
  },
  unreadMessages(group: SelectedGroupId): Uri {
    return uri`${this.group(group)}/messages/unread-messages`
  }
}
