// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useTranslation } from 'employee-frontend/state/i18n'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import {
  CalendarEvent,
  GroupInfo,
  IndividualChild
} from 'lib-common/generated/api-types/calendarevent'
import { UnitGroupDetails } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { unitGroupDetailsQuery } from '../../../queries'
import { DiscussionTimesCalendar } from '../times-calendar/TimesCalendar'

import { DiscussionSurveyEditMode } from './DiscussionSurveyEditor'
import DiscussionSurveyForm from './DiscussionSurveyForm'
import { timesForm } from './form'

export const TimesCalendarContainer = styled.div`
  max-height: 680px;
  overflow-y: scroll;
  overflow-x: hidden;
`

export const BorderedBox = styled.div`
  position: relative;
  z-index: 2;
  box-shadow: 0 2px 0px rgba(0, 0, 0, 0.15);
  margin-bottom: 0;
  padding-top: 24px;
  padding-bottom: 24px;
`

//export type TreeNodeInfo = TreeNode & { lastName: string, firstName: string }
export interface TreeNodeInfo {
  key: string
  text: string
  checked: boolean
  children: TreeNodeInfo[]
  firstName: string
  lastName: string
}

export type NewEventTimeForm = {
  id: UUID
  date: LocalDate
  childId: null
  timeRange: { startTime: string; endTime: string }
}

const isChildSelected = (childId: string, selections: IndividualChild[]) =>
  selections.some((s) => s.id === childId)
const resolveInviteeTreeNodes = (
  invitedAttendees: {
    individualChildren: IndividualChild[]
    groups: GroupInfo[]
  },
  groupResult: UnitGroupDetails,
  groupId: UUID,
  period: FiniteDateRange
): TreeNodeInfo[] => {
  const { groups, placements } = groupResult
  const currentGroup = groups.filter((g) => g.id === groupId)
  return currentGroup.map((g) => {
    // individualChildren contains childSelections that are not a part of a full group selection
    // -> any children there means their full group is not selected
    const individualChildrenOfSelectedGroup =
      invitedAttendees.individualChildren.filter((c) => c.groupId === groupId)
    const groupChildren = placements.filter((p) =>
      p.groupPlacements.some(
        (gp) =>
          gp.groupId === g.id &&
          period.overlaps(new DateRange(gp.startDate, gp.endDate))
      )
    )

    const sortedGroupChildren: TreeNodeInfo[] = orderBy(groupChildren, [
      ({ child }) => child.lastName,
      ({ child }) => child.firstName
    ]).map(({ child: c }) => ({
      key: c.id,
      text: `${c.firstName} ${c.lastName}`,
      checked:
        individualChildrenOfSelectedGroup.length === 0 ||
        isChildSelected(c.id, individualChildrenOfSelectedGroup),
      children: [],
      firstName: c.firstName,
      lastName: c.lastName
    }))

    // group is always selected:
    // - the full group is selected by default for a new survey
    // - existing survey has to have at least one attendee for the group -> tree node selected
    return {
      text: g.name,
      key: g.id,
      checked: true,
      children: sortedGroupChildren,
      firstName: '',
      lastName: ''
    }
  })
}

export default React.memo(function DiscussionTimesForm({
  eventData,
  groupId,
  unitId,
  editMode
}: {
  eventData: CalendarEvent | null
  groupId: UUID
  unitId: UUID
  editMode: DiscussionSurveyEditMode
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events

  const getCalendarHorizon = useCallback(() => {
    const today = LocalDate.todayInSystemTz()
    const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)

    const defaultHorizonDate = previousMonday.addMonths(3).lastDayOfMonth()

    const eventDataHorizonDate = eventData
      ? eventData.period.end.lastDayOfMonth()
      : today

    return defaultHorizonDate.isAfter(eventDataHorizonDate)
      ? defaultHorizonDate
      : eventDataHorizonDate
  }, [eventData])

  const [calendarHorizonDate, setCalendarHorizonDate] =
    useState<LocalDate>(getCalendarHorizon())

  const discussionTimesForm = useForm(
    timesForm,
    () => ({
      times: []
    }),
    i18n.validationErrors
  )

  const { times } = useFormFields(discussionTimesForm)

  const period = useMemo(() => {
    const today = LocalDate.todayInSystemTz()

    if (eventData) {
      return eventData.period
    } else {
      if (times.state.length > 0) {
        const sortedTimes = orderBy(times.state, [(t) => t.date])
        return new FiniteDateRange(
          sortedTimes[0].date,
          sortedTimes[sortedTimes.length - 1].date
        )
      } else {
        return new FiniteDateRange(today, today)
      }
    }
  }, [times.state, eventData])

  const calendarRange = useMemo(() => {
    const today = LocalDate.todayInSystemTz()

    const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)

    return new FiniteDateRange(previousMonday, calendarHorizonDate)
  }, [calendarHorizonDate])

  const groupData = useQueryResult(
    unitGroupDetailsQuery(unitId, calendarRange.start, calendarRange.end)
  )

  const invitedAttendees = useMemo(
    () => ({
      individualChildren: eventData?.individualChildren ?? [],
      groups: eventData?.groups ?? []
    }),
    [eventData]
  )

  const addTimeForDay = useCallback(
    (et: NewEventTimeForm) => {
      times.set([...times.state, et])
    },
    [times]
  )
  const removeTimeById = useCallback(
    (id: UUID) => {
      times.set(times.state.filter((t) => t.id !== id))
    },
    [times]
  )

  return (
    <>
      <div>
        {renderResult(groupData, (groupResult) => {
          const attendeeTree: TreeNodeInfo[] = resolveInviteeTreeNodes(
            invitedAttendees,
            groupResult,
            groupId,
            period
          )

          return (
            <>
              <DiscussionSurveyForm
                eventData={eventData}
                period={period}
                groupId={groupId}
                attendeeNodes={attendeeTree}
                timesBind={discussionTimesForm}
                unitId={unitId}
              />

              {editMode === 'create' && (
                <>
                  <BorderedBox>
                    <H3 noMargin>
                      {t.discussionReservation.surveyDiscussionTimesTitle}
                    </H3>
                  </BorderedBox>
                  <TimesCalendarContainer>
                    <DiscussionTimesCalendar
                      unitId={unitId}
                      groupId={groupId}
                      times={discussionTimesForm}
                      calendarRange={calendarRange}
                      addAction={addTimeForDay}
                      removeAction={removeTimeById}
                    />

                    <Gap size="L" />
                    <FixedSpaceRow
                      fullWidth
                      alignItems="center"
                      justifyContent="center"
                    >
                      <Button
                        onClick={() =>
                          setCalendarHorizonDate(
                            calendarHorizonDate.addMonths(1).lastDayOfMonth()
                          )
                        }
                        text={
                          i18n.unit.calendar.events.discussionReservation
                            .calendar.addTimeButton
                        }
                      />
                    </FixedSpaceRow>
                  </TimesCalendarContainer>
                </>
              )}
            </>
          )
        })}
      </div>
    </>
  )
})
