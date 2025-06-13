// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isPropValid from '@emotion/is-prop-valid'
import { ErrorBoundary } from '@sentry/react'
import React, { useContext, useEffect, useMemo } from 'react'
import { StyleSheetManager, ThemeProvider } from 'styled-components'
import { Router, Route, Redirect, useLocation, Switch } from 'wouter'

import type { ChildId, DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQuery, useQueryResult } from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import type { Uri } from 'lib-common/uri'
import { uri } from 'lib-common/uri'
import useRouteParams, { useIdRouteParam } from 'lib-common/useRouteParams'
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
import type { UnitOrGroup } from './common/unit-or-group'
import { toUnitOrGroup } from './common/unit-or-group'
import ChildMessagesPage from './messages/ChildMessagesPage'
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
import ExternalStaffMemberPage from './staff-attendance/ExternalStaffMemberPage'
import MarkExternalStaffMemberArrivalPage from './staff-attendance/MarkExternalStaffMemberArrivalPage'
import StaffAttendanceEditPage from './staff-attendance/StaffAttendanceEditPage'
import StaffAttendancesPage from './staff-attendance/StaffAttendancesPage'
import StaffMarkArrivedPage from './staff-attendance/StaffMarkArrivedPage'
import StaffMarkDepartedPage from './staff-attendance/StaffMarkDepartedPage'
import StaffMemberPage from './staff-attendance/StaffMemberPage'
import StaffMemberPlannedAttendancesPage from './staff-attendance/StaffMemberPlannedAttendancesPage'
import StaffPreviousAttendancesPage from './staff-attendance/StaffPreviousAttendancesPage'
import type { ChildAttendanceUIState } from './types'
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
                      <Router base="/employee/mobile">
                        <Switch>
                          <Route path="/landing">
                            <MobileLander />
                          </Route>
                          <Route path="/pairing">
                            <PairingWizard />
                          </Route>
                          <Route path="/units">
                            <RequireAuth>
                              <UnitList />
                            </RequireAuth>
                          </Route>
                          <Route path="/units/:unitId" nest>
                            <RequireAuth>
                              <UnitRouter />
                            </RequireAuth>
                          </Route>
                          <Route>
                            <Redirect replace to="/landing" />
                          </Route>
                        </Switch>
                        {!!featureFlags.environmentLabel && (
                          <EnvironmentLabel>
                            {featureFlags.environmentLabel}
                          </EnvironmentLabel>
                        )}
                      </Router>
                      <div id="datepicker-container" />
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
  const unitId = useIdRouteParam<DaycareId>('unitId')

  return (
    <Switch>
      <Route path="/settings">
        <SettingsPage unitId={unitId} />
      </Route>
      <Route path="/groups/:groupId" nest>
        <GroupRouter unitId={unitId} />
      </Route>
      <Route path="/children/:childId" nest>
        <ChildRouter unitId={unitId} />
      </Route>
      <Route path="/mark-present">
        <MarkPresent unitId={unitId} />
      </Route>
      <Route path="/mark-departed">
        <MarkDeparted unitId={unitId} />
      </Route>
      <Route>
        <Redirect replace to="/groups/all" />
      </Route>
    </Switch>
  )
}

function GroupRouter({ unitId }: { unitId: DaycareId }) {
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

  const [, navigate] = useLocation()
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
      <Switch>
        <Route path="/child-attendance" nest>
          <ChildAttendanceRouter unitOrGroup={unitOrGroup} />
        </Route>
        <Route path="/staff-attendance" nest>
          <StaffAttendanceRouter unitOrGroup={unitOrGroup} />
        </Route>
        <Route path="/messages" nest>
          <MessagesRouter unitOrGroup={unitOrGroup} />
        </Route>
        <Route>
          <Redirect replace to="/child-attendance" />
        </Route>
      </Switch>
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
    <AttendancePageWrapper unitOrGroup={unitOrGroup}>
      <Switch>
        <Route path="/list/:attendanceStatus">
          <AttendanceTodayWrapper unitOrGroup={unitOrGroup} />
        </Route>
        <Route path="/daylist">
          <ConfimedReservationDaysWrapper unitOrGroup={unitOrGroup} />
        </Route>
        <Route>
          <Redirect replace to="/list/coming" />
        </Route>
      </Switch>
    </AttendancePageWrapper>
  )
}

function ChildRouter({ unitId }: { unitId: DaycareId }) {
  const childId = useIdRouteParam<ChildId>('childId')

  return (
    <Switch>
      <Route path="/">
        <AttendanceChildPage unitId={unitId} childId={childId} />
      </Route>
      <Route path="/mark-absent">
        <MarkAbsent unitId={unitId} childId={childId} />
      </Route>
      <Route path="/mark-reservations">
        <MarkReservations unitId={unitId} childId={childId} />
      </Route>
      <Route path="/mark-absent-beforehand">
        <MarkAbsentBeforehand unitId={unitId} childId={childId} />
      </Route>
      <Route path="/note">
        <ChildNotes unitId={unitId} childId={childId} />
      </Route>
      <Route path="/info">
        <RequirePinAuth unitId={unitId}>
          <ChildSensitiveInfoPage unitId={unitId} childId={childId} />
        </RequirePinAuth>
      </Route>
      <Route path="/new-message">
        <RequirePinAuth unitId={unitId}>
          <NewChildMessagePage unitId={unitId} childId={childId} />
        </RequirePinAuth>
      </Route>
      <Route path="/received-messages">
        <RequirePinAuth unitId={unitId}>
          <ChildMessagesPage unitId={unitId} childId={childId} />
        </RequirePinAuth>
      </Route>
    </Switch>
  )
}

function StaffAttendanceRouter({ unitOrGroup }: { unitOrGroup: UnitOrGroup }) {
  return (
    <Switch>
      <Route path="/today/absent">
        <StaffAttendancesPage
          primaryTab="today"
          statusTab="absent"
          unitOrGroup={unitOrGroup}
        />
      </Route>
      <Route path="/today/present">
        <StaffAttendancesPage
          primaryTab="today"
          statusTab="present"
          unitOrGroup={unitOrGroup}
        />
      </Route>
      <Route path="/today">
        <Redirect replace to="/today/absent" />
      </Route>
      <Route path="/planned">
        <StaffAttendancesPage primaryTab="planned" unitOrGroup={unitOrGroup} />
      </Route>
      <Route path="/external">
        <MarkExternalStaffMemberArrivalPage unitOrGroup={unitOrGroup} />
      </Route>
      <Route path="/external/:attendanceId">
        <ExternalStaffMemberPage unitId={unitOrGroup.unitId} />
      </Route>
      <Route path="/:employeeId">
        <StaffMemberPage unitOrGroup={unitOrGroup} />
      </Route>
      <Route path="/:employeeId/previous">
        <StaffPreviousAttendancesPage unitOrGroup={unitOrGroup} />
      </Route>
      <Route path="/:employeeId/planned">
        <StaffMemberPlannedAttendancesPage unitOrGroup={unitOrGroup} />
      </Route>
      <Route path="/:employeeId/edit">
        <StaffAttendanceEditPage unitOrGroup={unitOrGroup} />
      </Route>
      <Route path="/:employeeId/mark-arrived">
        <StaffMarkArrivedPage unitOrGroup={unitOrGroup} />
      </Route>
      <Route path="/:employeeId/mark-departed">
        <StaffMarkDepartedPage unitOrGroup={unitOrGroup} />
      </Route>
      <Route>
        <Redirect replace to="/today/absent" />
      </Route>
    </Switch>
  )
}

function MessagesRouter({ unitOrGroup }: { unitOrGroup: UnitOrGroup }) {
  return (
    <Switch>
      <Route path="/">
        <RequirePinAuth unitId={unitOrGroup.unitId}>
          <MessagesPage unitOrGroup={unitOrGroup} />
        </RequirePinAuth>
      </Route>
      <Route path="unread-messages">
        <UnreadMessagesPage unitOrGroup={unitOrGroup} />
      </Route>
      <Route path="thread/:threadId">
        <ReceivedThreadPage unitOrGroup={unitOrGroup} />
      </Route>
      <Route path="new">
        <NewMessagePage unitOrGroup={unitOrGroup} />
      </Route>
    </Switch>
  )
}

const root = uri`~/employee/mobile`

export const routes = {
  unitList(): Uri {
    return uri`${root}/mobile/units/`
  },
  unit(unitId: UUID): Uri {
    return uri`${root}/units/${unitId}`
  },
  settings(unitId: UUID): Uri {
    return uri`${this.unit(unitId)}/settings`
  },
  markPresent(unitId: UUID, childIds: UUID[], multiselect: boolean): Uri {
    const params = new URLSearchParams()
    params.set('children', childIds.join(','))
    if (multiselect) params.set('multiselect', 'true')
    return uri`${this.unit(unitId)}/mark-present`.appendQuery(params)
  },
  markDeparted(unitId: UUID, childIds: UUID[], multiselect: boolean): Uri {
    const params = new URLSearchParams()
    params.set('children', childIds.join(','))
    if (multiselect) params.set('multiselect', 'true')
    return uri`${this.unit(unitId)}/mark-departed`.appendQuery(params)
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
  markAbsent(unitId: UUID, child: UUID): Uri {
    return uri`${this.child(unitId, child)}/mark-absent`
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
  childMessages(unitId: UUID, child: UUID): Uri {
    return uri`${this.child(unitId, child)}/received-messages`
  },
  childSensitiveInfo(unitId: UUID, child: UUID): Uri {
    return uri`${this.child(unitId, child)}/info`
  },
  staffAttendanceRoot(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.unitOrGroup(unitOrGroup)}/staff-attendance`
  },
  staffAttendancesToday(
    unitOrGroup: UnitOrGroup,
    tab: 'absent' | 'present'
  ): Uri {
    return uri`${this.staffAttendanceRoot(unitOrGroup)}/today/${tab}`
  },
  staffAttendancesPlanned(unitOrGroup: UnitOrGroup): Uri {
    return uri`${this.staffAttendanceRoot(unitOrGroup)}/planned`
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
  staffPlannedAttendances(unitOrGroup: UnitOrGroup, employeeId: UUID): Uri {
    return uri`${this.staffAttendance(unitOrGroup, employeeId)}/planned`
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
