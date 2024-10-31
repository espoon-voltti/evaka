// SPDX-FileCopyrightText: 2017-2024 City of Espoo
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

import LocalDate from 'lib-common/local-date'
import { useQuery, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { Uri, uri } from 'lib-common/uri'
import useRouteParams from 'lib-common/useRouteParams'
import {
  Notifications,
  NotificationsContextProvider
} from 'lib-components/Notifications'
import { EnvironmentLabel } from 'lib-components/atoms/EnvironmentLabel'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { theme } from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'

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
import ChildSensitiveInfoPage from './child-info/ChildInfoPage'
import ChildNotes from './child-notes/ChildNotes'
import { I18nContextProvider, useTranslation } from './common/i18n'
import { ServiceWorkerContextProvider } from './common/service-worker'
import { UnitOrGroup, toUnitOrGroup } from './common/unit-or-group'
import MessagesPage from './messages/MessagesPage'
import NewChildMessagePage from './messages/NewChildMessagePage'
import NewMessagePage from './messages/NewMessagePage'
import { ReceivedThreadPage } from './messages/ThreadView'
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
import StaffPreviousAttendancesPage from './staff-attendance/StaffPreviousAttendancesPage'
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
                        {!!featureFlags.environmentLabel && (
                          <EnvironmentLabel>
                            {featureFlags.environmentLabel}
                          </EnvironmentLabel>
                        )}
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
      <Route
        path="/children/:childId/*"
        element={<ChildRouter unitId={unitId} />}
      />
      <Route index element={<Navigate replace to="groups/all" />} />
    </Routes>
  )
}

function GroupRouter({ unitId }: { unitId: UUID }) {
  const { groupId } = useRouteParams([], ['groupId'])
  const unitOrGroup: UnitOrGroup = useMemo(
    () => toUnitOrGroup(unitId, groupId),
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
          routes.childAttendances(toUnitOrGroup(unitOrGroup.unitId)).value
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
      <Route index element={<Navigate replace to="list/coming" />} />
    </Routes>
  )
}

function ChildRouter({ unitId }: { unitId: UUID }) {
  const { childId } = useRouteParams(['childId'])

  return (
    <Routes>
      <Route
        path="/"
        index
        element={<AttendanceChildPage unitId={unitId} childId={childId} />}
      />
      <Route
        path="/mark-present"
        element={<MarkPresent unitId={unitId} childId={childId} />}
      />
      <Route
        path="/mark-absent"
        element={<MarkAbsent unitId={unitId} childId={childId} />}
      />
      <Route
        path="/mark-reservations"
        element={<MarkReservations unitId={unitId} childId={childId} />}
      />
      <Route
        path="/mark-absent-beforehand"
        element={<MarkAbsentBeforehand unitId={unitId} childId={childId} />}
      />
      <Route
        path="/mark-departed"
        element={<MarkDeparted unitId={unitId} childId={childId} />}
      />
      <Route
        path="/note"
        element={<ChildNotes unitId={unitId} childId={childId} />}
      />
      <Route
        path="/info"
        element={
          <RequirePinAuth unitId={unitId}>
            <ChildSensitiveInfoPage unitId={unitId} childId={childId} />
          </RequirePinAuth>
        }
      />
      <Route
        path="/new-message"
        element={
          <RequirePinAuth unitId={unitId}>
            <NewChildMessagePage unitId={unitId} childId={childId} />
          </RequirePinAuth>
        }
      />
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
        path=":employeeId/previous"
        element={<StaffPreviousAttendancesPage unitOrGroup={unitOrGroup} />}
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
      <Route
        path="thread/:threadId"
        element={<ReceivedThreadPage unitOrGroup={unitOrGroup} />}
      />
      <Route
        path="new"
        element={<NewMessagePage unitOrGroup={unitOrGroup} />}
      />
    </Routes>
  )
}

export const routes = {
  unit(unitId: UUID): Uri {
    return uri`/units/${unitId}`
  },
  settings(unitId: UUID): Uri {
    return uri`${this.unit(unitId)}/settings`
  },
  unitOrGroup(unitOrGroup: UnitOrGroup): Uri {
    const id = unitOrGroup.type === 'unit' ? 'all' : unitOrGroup.id
    return uri`${this.unit(unitOrGroup.unitId)}/groups/${id}`
  },
  childAttendances(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/child-attendance`
  },
  childAttendanceList(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.childAttendances(unitOrGroup)}/list`
  },
  childAttendanceListState(
    unitOrGroup: UnitOrGroup,
    state: ChildAttendanceUIState
  ): Uri {
    return uri`${this.childAttendanceList(unitOrGroup)}/${state}`
  },
  childAttendanceDaylist(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.childAttendances(unitOrGroup)}/daylist`
  },
  child(unitId: UUID, child: UUID): Uri {
    return uri`${this.unit(unitId)}/children/${child}`
  },
  childMarkAbsentBeforehand(unitId: UUID, child: UUID): Uri {
    return uri`${this.child(unitId, child)}/mark-absent-beforehand`
  },
  markPresent(unitId: UUID, child: UUID): Uri {
    return uri`${this.child(unitId, child)}/mark-present`
  },
  markAbsent(unitId: UUID, child: UUID): Uri {
    return uri`${this.child(unitId, child)}/mark-absent`
  },
  markDeparted(unitId: UUID, child: UUID): Uri {
    return uri`${this.child(unitId, child)}/mark-departed`
  },
  markReservations(unitId: UUID, child: UUID): Uri {
    return uri`${this.child(unitId, child)}/mark-reservations`
  },
  childNotes(unitId: UUID, child: UUID): Uri {
    return uri`${this.child(unitId, child)}/note`
  },
  newChildMessage(unitId: UUID, child: UUID): Uri {
    return uri`${this.child(unitId, child)}/new-message`
  },
  childSensitiveInfo(unitId: UUID, child: UUID): Uri {
    return uri`${this.child(unitId, child)}/info`
  },
  staff(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/staff`
  },
  staffAttendanceRoot(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/staff-attendance`
  },
  staffAttendances(unitOrGroup: UnitOrGroup, tab: 'absent' | 'present'): Uri {
    return uri`${this.staffAttendanceRoot(unitOrGroup)}/${tab}`
  },
  externalStaffAttendances(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.staffAttendanceRoot(unitOrGroup)}/external`
  },
  externalStaffAttendance(unitOrGroup: UnitOrGroup, attendanceId: UUID): Uri {
    return uri`${this.externalStaffAttendances(unitOrGroup)}/${attendanceId}`
  },
  staffAttendance(unitOrGroup: UnitOrGroup, employeeId: UUID): Uri {
    return uri`${this.staffAttendanceRoot(unitOrGroup)}/${employeeId}`
  },
  staffAttendanceEdit(
    unitOrGroup: UnitOrGroup,
    employeeId: UUID,
    date?: LocalDate
  ): Uri {
    return uri`${this.staffAttendance(unitOrGroup, employeeId)}/edit?date=${(date ?? LocalDate.todayInHelsinkiTz()).formatIso()}`
  },
  staffMarkArrived(unitOrGroup: UnitOrGroup, employeeId: UUID): Uri {
    return uri`${this.staffAttendance(unitOrGroup, employeeId)}/mark-arrived`
  },
  staffMarkDeparted(unitOrGroup: UnitOrGroup, employeeId: UUID): Uri {
    return uri`${this.staffAttendance(unitOrGroup, employeeId)}/mark-departed`
  },
  staffPreviousAttendances(unitOrGroup: UnitOrGroup, employeeId: UUID): Uri {
    return uri`${this.staffAttendance(unitOrGroup, employeeId)}/previous`
  },
  messages(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/messages`
  },
  unreadMessages(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.messages(unitOrGroup)}/unread-messages`
  },
  receivedThread(unitOrGroup: UnitOrGroup, threadId: UUID): Uri {
    return uri`${this.messages(unitOrGroup)}/thread/${threadId}`
  },
  newMessage(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.messages(unitOrGroup)}/new`
  }
}
