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
import { ServiceWorkerContextProvider } from './common/service-worker'
import { UnitOrGroup, toUnitOrGroup } from './common/unit-or-group'
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
  const unitOrGroup: UnitOrGroup = useMemo(
    () => toUnitOrGroup({ unitId, groupId }),
    [unitId, groupId]
  )

  const { saveGroupId } = useContext(RememberContext)
  useEffect(
    () =>
      saveGroupId(unitOrGroup.type === 'group' ? unitOrGroup.id : undefined),
    [saveGroupId, unitOrGroup]
  )

  const unitInfoResponse = useQueryResult(
    unitInfoQuery({ unitId: unitOrGroup.unitId })
  )

  const navigate = useNavigate()
  useEffect(() => {
    // If we somehow end up with a groupId that doesn't exist in the current unit,
    // just navigate to some "default page" instead of allowing things to break
    if (unitOrGroup.type === 'group' && unitInfoResponse.isSuccess) {
      const validGroupId = unitInfoResponse.value.groups.some(
        (group) => group.id === unitOrGroup.id
      )
      if (!validGroupId) {
        navigate(
          routes.childAttendances(
            toUnitOrGroup({
              unitId: unitOrGroup.unitId,
              groupId: undefined
            })
          ).value
        )
      }
    }
  }, [navigate, unitOrGroup, unitInfoResponse])

  return (
    <MessageContextProvider unitOrGroup={unitOrGroup}>
      <Routes>
        <Route
          path="child-attendance/*"
          element={<ChildAttendanceRouter unitOrGroup={unitOrGroup} />}
        />
        <Route
          path="staff/*"
          element={<StaffRouter unitOrGroup={unitOrGroup} />}
        />
        <Route
          path="staff-attendance/*"
          element={<StaffAttendanceRouter unitOrGroup={unitOrGroup} />}
        />
        <Route
          path="messages/*"
          element={<MessagesRouter unitOrGroup={unitOrGroup} />}
        />
        <Route index element={<Navigate replace to="child-attendance" />} />
      </Routes>
    </MessageContextProvider>
  )
}

function ChildAttendanceRouter({ unitOrGroup }: { unitOrGroup: UnitOrGroup }) {
  // Re-fetch child data when navigating to the attendance section
  useQuery(childrenQuery(unitOrGroup.unitId), { refetchOnMount: 'always' })
  useQuery(attendanceStatusesQuery({ unitId: unitOrGroup.unitId }), {
    refetchOnMount: 'always'
  })

  return (
    <Routes>
      <Route
        path="/"
        element={<AttendancePageWrapper unitOrGroup={unitOrGroup} />}
      >
        <Route
          path="list/:attendanceStatus"
          element={<AttendanceTodayWrapper unitOrGroup={unitOrGroup} />}
        />
        <Route
          path="daylist"
          id="daylist"
          element={<ConfimedReservationDaysWrapper unitOrGroup={unitOrGroup} />}
        />
        <Route path="list" element={<Navigate replace to="/" />} />
      </Route>

      <Route
        path=":childId"
        element={<AttendanceChildPage unitOrGroup={unitOrGroup} />}
      />
      <Route
        path=":childId/mark-present"
        element={<MarkPresent unitId={unitOrGroup.unitId} />}
      />
      <Route
        path=":childId/mark-absent"
        element={<MarkAbsent unitId={unitOrGroup.unitId} />}
      />
      <Route
        path=":childId/mark-reservations"
        element={<MarkReservations unitOrGroup={unitOrGroup} />}
      />
      <Route
        path=":childId/mark-absent-beforehand"
        element={<MarkAbsentBeforehand unitId={unitOrGroup.unitId} />}
      />
      <Route
        path=":childId/mark-departed"
        element={<MarkDeparted unitId={unitOrGroup.unitId} />}
      />
      <Route
        path=":childId/note"
        element={<ChildNotes unitOrGroup={unitOrGroup} />}
      />
      <Route
        path=":childId/info"
        element={
          <RequirePinAuth unitId={unitOrGroup.unitId}>
            <ChildSensitiveInfoPage unitId={unitOrGroup.unitId} />
          </RequirePinAuth>
        }
      />
      <Route
        path=":childId/new-message"
        element={
          <RequirePinAuth unitId={unitOrGroup.unitId}>
            <NewChildMessagePage unitId={unitOrGroup.unitId} />
          </RequirePinAuth>
        }
      />
      <Route index element={<Navigate replace to="list/coming" />} />
    </Routes>
  )
}

function StaffRouter({ unitOrGroup }: { unitOrGroup: UnitOrGroup }) {
  return (
    <Routes>
      <Route index element={<StaffPage unitOrGroup={unitOrGroup} />} />
    </Routes>
  )
}

function StaffAttendanceRouter({ unitOrGroup }: { unitOrGroup: UnitOrGroup }) {
  return (
    <Routes>
      <Route
        path="absent"
        element={
          <StaffAttendancesPage tab="absent" unitOrGroup={unitOrGroup} />
        }
      />
      <Route
        path="present"
        element={
          <StaffAttendancesPage tab="present" unitOrGroup={unitOrGroup} />
        }
      />
      <Route
        path="external"
        element={
          <MarkExternalStaffMemberArrivalPage unitOrGroup={unitOrGroup} />
        }
      />
      <Route
        path="external/:attendanceId"
        element={<ExternalStaffMemberPage unitId={unitOrGroup.unitId} />}
      />
      <Route
        path=":employeeId"
        element={<StaffMemberPage unitOrGroup={unitOrGroup} />}
      />
      <Route
        path=":employeeId/edit"
        element={<StaffAttendanceEditPage unitOrGroup={unitOrGroup} />}
      />
      <Route
        path=":employeeId/mark-arrived"
        element={<StaffMarkArrivedPage unitOrGroup={unitOrGroup} />}
      />
      <Route
        path=":employeeId/mark-departed"
        element={<StaffMarkDepartedPage unitOrGroup={unitOrGroup} />}
      />
      <Route index element={<Navigate replace to="absent" />} />
    </Routes>
  )
}

function MessagesRouter({ unitOrGroup }: { unitOrGroup: UnitOrGroup }) {
  return (
    <Routes>
      <Route
        index
        element={
          <RequirePinAuth unitId={unitOrGroup.unitId}>
            <MessagesPage unitOrGroup={unitOrGroup} />
          </RequirePinAuth>
        }
      />
      <Route
        path="unread-messages"
        element={<UnreadMessagesPage unitOrGroup={unitOrGroup} />}
      />
    </Routes>
  )
}

export const routes = {
  unitOrGroup(unitOrGroup: UnitOrGroup): Uri {
    const id = unitOrGroup.type === 'unit' ? 'all' : unitOrGroup.id
    return uri`/units/${unitOrGroup.unitId}/groups/${id}`
  },
  settings(unitId: UUID): Uri {
    return uri`/units/${unitId}/settings`
  },
  childAttendanceList(
    unitOrGroup: UnitOrGroup,
    state: ChildAttendanceUIState
  ): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/list/${state}`
  },
  childAttendance(unitOrGroup: UnitOrGroup, child: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/${child}`
  },
  childAttendances(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/`
  },
  childAttendanceDaylist(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/daylist`
  },
  markAbsentBeforehand(unitOrGroup: UnitOrGroup, child: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/${child}/mark-absent-beforehand`
  },
  markPresent(unitOrGroup: UnitOrGroup, child: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/${child}/mark-present`
  },
  markAbsent(unitOrGroup: UnitOrGroup, child: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/${child}/mark-absent`
  },
  markDeparted(unitOrGroup: UnitOrGroup, child: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/${child}/mark-departed`
  },
  markReservations(unitOrGroup: UnitOrGroup, child: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/${child}/mark-reservations`
  },
  childNotes(unitOrGroup: UnitOrGroup, child: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/${child}/note`
  },
  newChildMessage(unitOrGroup: UnitOrGroup, child: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/${child}/new-message`
  },
  childSensitiveInfo(unitOrGroup: UnitOrGroup, child: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance/${child}/info`
  },
  staff(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/staff`
  },
  staffAttendances(unitOrGroup: UnitOrGroup, tab: 'absent' | 'present'): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/staff-attendance/${tab}`
  },
  externalStaffAttendances(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/staff-attendance/external`
  },
  externalStaffAttendance(unitOrGroup: UnitOrGroup, attendanceId: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/staff-attendance/external/${attendanceId}`
  },
  staffAttendance(unitOrGroup: UnitOrGroup, employeeId: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/staff-attendance/${employeeId}`
  },
  staffAttendanceEdit(unitOrGroup: UnitOrGroup, employeeId: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/staff-attendance/${employeeId}/edit`
  },
  staffMarkArrived(unitOrGroup: UnitOrGroup, employeeId: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/staff-attendance/${employeeId}/mark-arrived`
  },
  staffMarkDeparted(unitOrGroup: UnitOrGroup, employeeId: UUID): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/staff-attendance/${employeeId}/mark-departed`
  },
  messages(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/messages`
  },
  unreadMessages(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/messages/unread-messages`
  }
}
