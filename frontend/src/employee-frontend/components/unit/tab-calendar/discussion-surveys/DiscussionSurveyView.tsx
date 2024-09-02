// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import orderBy from 'lodash/orderBy'
import partition from 'lodash/partition'
import React, {
  MutableRefObject,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState
} from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UIContext } from 'employee-frontend/state/ui'
import { combine } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { BoundForm, useForm, useFormFields } from 'lib-common/form/hooks'
import {
  CalendarEvent,
  DiscussionReservationDay
} from 'lib-common/generated/api-types/calendarevent'
import { UnitGroupDetails } from 'lib-common/generated/api-types/daycare'
import {
  ChildBasics,
  DaycarePlacementWithDetails
} from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { useMutation, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import { StaticChip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2, H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { theme } from 'lib-customizations/common'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { unitGroupDetailsQuery } from '../../queries'
import {
  deleteCalendarEventMutation,
  groupDiscussionReservationDaysQuery
} from '../queries'

import InviteeSection from './InviteeSection'
import {
  BorderedBox,
  NewEventTimeForm,
  TimesCalendarContainer
} from './survey-editor/DiscussionTimesForm'
import { type eventTimeArray, timesForm } from './survey-editor/form'
import { DiscussionReservationCalendar } from './times-calendar/TimesCalendar'

export interface ChildGroupInfo {
  child: ChildBasics
  groupPlacements: DateRange[]
}

export const getCombinedChildPlacementsForGroup = (
  placements: DaycarePlacementWithDetails[],
  groupId: UUID
) => {
  const validPlacements = placements.filter((p) =>
    p.groupPlacements.some((gp) => gp.groupId === groupId)
  )
  return Object.values(groupBy(validPlacements, (p) => p.child.id)).map(
    (placements) => ({
      child: placements[0].child,
      groupPlacements: placements
        .flatMap((p) => p.groupPlacements)
        .filter((gp) => gp.groupId === groupId)
        .map((gp) => new DateRange(gp.startDate, gp.endDate))
    })
  )
}

export const getInvitedChildInfo = (
  unitDetails: UnitGroupDetails,
  eventData: CalendarEvent,
  groupId: UUID
) => {
  const fullGroupSelections = eventData.groups.filter((g) => {
    const anyIndividuals =
      eventData.individualChildren.length > 0 &&
      eventData.individualChildren.some((c) => c.groupId === g.id)
    return !anyIndividuals
  })
  const childSelections = eventData.individualChildren.map((c) => c.id)
  const invitedChildPlacements = unitDetails.placements.filter((p) => {
    const isPartOfFullGroupSelection = p.groupPlacements.some((gp) => {
      const placedGroupIsInFullGroups = fullGroupSelections.some(
        (gi) => gi.id === gp.groupId
      )
      const durationsOverlap = eventData.period.overlaps(
        new DateRange(gp.startDate, gp.endDate)
      )
      return placedGroupIsInFullGroups && durationsOverlap
    })
    const isPartOfIndividualSelections = childSelections.includes(p.child.id)
    return isPartOfFullGroupSelection || isPartOfIndividualSelections
  })

  const allInvitedChildren = getCombinedChildPlacementsForGroup(
    invitedChildPlacements,
    groupId
  )

  const sortedResults = orderBy(allInvitedChildren, [
    (c) => c.child.lastName,
    (c) => c.child.firstName,
    (c) => c.child.id
  ])
  return sortedResults
}

const SurveyStatusChip = React.memo(function SurveyStatusChip({
  status
}: {
  status: 'SENT' | 'ENDED'
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events.discussionReservation
  const statusColor = {
    SENT: theme.colors.accents.a3emerald,
    ENDED: theme.colors.grayscale.g15
  }
  return (
    <StaticChip fitContent color={statusColor[status]}>
      {t.surveyStatus[status]}
    </StaticChip>
  )
})

const ReservationCalendarSection = React.memo(
  function ReservationCalendarSection({
    eventData,
    invitees,
    calendarRange,
    maxCalendarRange,
    calendarDays,
    expandCalendarAction,
    times,
    addAction,
    removeAction,
    horizonRef
  }: {
    eventData: CalendarEvent
    invitees: ChildGroupInfo[]
    calendarRange: FiniteDateRange
    maxCalendarRange: FiniteDateRange
    calendarDays: DiscussionReservationDay[]
    expandCalendarAction: () => void
    times: BoundForm<typeof eventTimeArray>
    addAction: (et: NewEventTimeForm) => void
    removeAction: (id: UUID) => void
    horizonRef: MutableRefObject<HTMLDivElement | null>
  }) {
    const { i18n } = useTranslation()

    return (
      <TimesCalendarContainer data-qa="reservation-calendar">
        <DiscussionReservationCalendar
          eventData={eventData}
          calendarRange={calendarRange}
          calendarDays={calendarDays}
          invitees={invitees}
          times={times}
          addAction={addAction}
          removeAction={removeAction}
          horizonRef={horizonRef}
        />
        {calendarRange.end.isBefore(maxCalendarRange.end) && (
          <>
            <Gap size="L" />
            <FixedSpaceRow
              fullWidth
              alignItems="center"
              justifyContent="center"
            >
              <ExpandHorizonButton
                onClick={expandCalendarAction}
                text={
                  i18n.unit.calendar.events.discussionReservation.calendar
                    .addTimeButton
                }
                data-qa="expand-horizon-button"
              />
            </FixedSpaceRow>
            <Gap size="m" />
          </>
        )}
      </TimesCalendarContainer>
    )
  }
)

export default React.memo(function DiscussionReservationSurveyView({
  unitId,
  groupId,
  eventData
}: {
  unitId: UUID
  groupId: UUID
  eventData: CalendarEvent
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const { mutateAsync: deleteCalendarEvent } = useMutation(
    deleteCalendarEventMutation
  )

  const { setErrorMessage } = useContext(UIContext)
  const onEdit = useCallback(() => {
    navigate(
      `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys/${eventData.id}/edit`,
      { replace: true }
    )
  }, [navigate, eventData.id, groupId, unitId])
  const t = i18n.unit.calendar.events

  const today = LocalDate.todayInSystemTz()
  const getCalendarHorizon = useCallback(() => {
    const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)

    const defaultHorizonDate = previousMonday.addMonths(1).lastDayOfMonth()

    const eventDataHorizonDate = eventData.period.end.lastDayOfMonth()

    return defaultHorizonDate.isAfter(eventDataHorizonDate)
      ? defaultHorizonDate
      : eventDataHorizonDate
  }, [eventData.period.end, today])

  const horizonRef = useRef<HTMLDivElement | null>(null)

  const [calendarHorizonDate, setCalendarHorizonDate] =
    useState<LocalDate>(getCalendarHorizon())

  const maxCalendarRange = useMemo(
    () =>
      new FiniteDateRange(
        today.subDays(today.getIsoDayOfWeek() - 1),
        today.addMonths(5).lastDayOfMonth()
      ),
    [today]
  )

  const visibleCalendarRange = useMemo(() => {
    const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)
    return new FiniteDateRange(previousMonday, calendarHorizonDate)
  }, [calendarHorizonDate, today])

  const extendHorizon = useCallback(() => {
    const candidateHorizon = calendarHorizonDate.addMonths(1).lastDayOfMonth()
    if (candidateHorizon.isEqualOrBefore(maxCalendarRange.end)) {
      setCalendarHorizonDate(candidateHorizon)
      scrollRefIntoView(horizonRef, 80)
    }
  }, [maxCalendarRange, calendarHorizonDate])

  const groupData = useQueryResult(
    unitGroupDetailsQuery({
      unitId,
      from: maxCalendarRange.start,
      to: maxCalendarRange.end
    })
  )

  const calendarDays = useQueryResult(
    groupDiscussionReservationDaysQuery({
      unitId,
      groupId,
      start: maxCalendarRange.start,
      end: maxCalendarRange.end
    })
  )

  const [deleteConfirmModalVisible, setDeleteConfirmModalVisible] =
    useState(false)

  const status = useMemo(
    () =>
      eventData.period.end.isBefore(LocalDate.todayInHelsinkiTz())
        ? 'ENDED'
        : 'SENT',
    [eventData.period.end]
  )

  const discussionTimesForm = useForm(
    timesForm,
    () => ({
      times: []
    }),
    i18n.validationErrors
  )

  const { times } = useFormFields(discussionTimesForm)

  const addTime = useCallback(
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
      {deleteConfirmModalVisible && (
        <InfoModal
          type="warning"
          title={t.discussionReservation.deleteConfirmation.title}
          icon={faQuestion}
          reject={{
            action: () => setDeleteConfirmModalVisible(false),
            label: i18n.common.cancel
          }}
          resolve={{
            action: () => {
              deleteCalendarEvent({ groupId: groupId, id: eventData.id })
                .catch(() => {
                  setErrorMessage({
                    title: t.discussionReservation.deleteConfirmation.error,
                    type: 'error',
                    resolveLabel: i18n.common.close
                  })
                })
                .finally(() => {
                  setDeleteConfirmModalVisible(false)
                  navigate(
                    `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys`,
                    { replace: true }
                  )
                })
            },
            label: i18n.common.remove
          }}
          text={t.discussionReservation.deleteConfirmation.text}
          data-qa="deletion-confirm-modal"
        />
      )}
      <Container>
        <ReturnButton label={i18n.common.goBack} />
        <ContentArea opaque>
          <FixedSpaceRow alignItems="center" justifyContent="space-between">
            <H2 data-qa="survey-title">{eventData.title}</H2>
            <FixedSpaceRow alignItems="center" spacing="L">
              <Button
                appearance="inline"
                icon={faTrash}
                text={t.discussionReservation.deleteSurveyButton}
                onClick={() => setDeleteConfirmModalVisible(true)}
                data-qa="survey-delete-button"
              />
              <SurveyStatusChip status={status} />
            </FixedSpaceRow>
          </FixedSpaceRow>

          <FormFieldGroup>
            <Label>{t.discussionReservation.surveyModifiedAt}</Label>
            <p>{eventData.contentModifiedAt.format()}</p>
          </FormFieldGroup>
          <FixedSpaceRow justifyContent="space-between" alignItems="center">
            <H3>{t.discussionReservation.surveyBasicsTitle}</H3>
            <Button
              appearance="inline"
              icon={faPen}
              text={t.discussionReservation.editSurveyButton}
              onClick={onEdit}
              data-qa="survey-edit-button"
            />
          </FixedSpaceRow>

          <FormSectionGroup>
            <FormFieldGroup>
              <Label>{t.discussionReservation.surveyPeriod}</Label>
              <p>{eventData.period.format()}</p>
            </FormFieldGroup>

            <FormFieldGroup>
              <ExpandingInfo info={t.discussionReservation.surveySummaryInfo}>
                <Label>{t.discussionReservation.surveySummary}</Label>
              </ExpandingInfo>
              <WidthLimiter>
                <p data-qa="survey-description">{eventData?.description}</p>
              </WidthLimiter>
            </FormFieldGroup>
          </FormSectionGroup>
          {renderResult(
            combine(groupData, calendarDays),
            ([unitDetailsResult, calendarDaysResult]) => {
              const sortedCalendarInvitees = getInvitedChildInfo(
                unitDetailsResult,
                eventData,
                groupId
              )
              const sortedPeriodInvitees: ChildGroupInfo[] =
                sortedCalendarInvitees.filter((c) =>
                  c.groupPlacements.some((gp) =>
                    gp.overlapsWith(eventData.period.asDateRange())
                  )
                )
              const reservations = eventData.times.filter(
                (t) => t.childId !== null
              )
              const [reserved, unreserved] = partition(
                sortedPeriodInvitees,
                (e) => reservations.some((r) => r.childId === e.child.id)
              )

              return (
                <>
                  <FormSectionGroup>
                    <H3>{t.discussionReservation.surveyInviteeTitle}</H3>
                    <FormFieldGroup>
                      <InviteeSection
                        reserved={reserved}
                        unreserved={unreserved}
                      />
                    </FormFieldGroup>
                  </FormSectionGroup>

                  <FormSectionGroup>
                    <BorderedBox>
                      <H3 noMargin data-qa="survey-reservation-calendar-title">
                        {t.discussionReservation.surveyDiscussionTimesTitle}
                      </H3>
                    </BorderedBox>
                    <ReservationCalendarSection
                      eventData={eventData}
                      invitees={sortedCalendarInvitees}
                      calendarRange={visibleCalendarRange}
                      maxCalendarRange={maxCalendarRange}
                      calendarDays={calendarDaysResult}
                      expandCalendarAction={extendHorizon}
                      times={times}
                      addAction={addTime}
                      removeAction={removeTimeById}
                      horizonRef={horizonRef}
                    />
                  </FormSectionGroup>
                </>
              )
            }
          )}
        </ContentArea>
      </Container>
    </>
  )
})

const ExpandHorizonButton = styled(LegacyButton)`
  margin-bottom: 10px;
`
const WidthLimiter = styled.div`
  max-width: 400px;
`
const FormFieldGroup = styled(FixedSpaceColumn).attrs({ spacing: 'S' })``
const FormSectionGroup = styled(FixedSpaceColumn).attrs({ spacing: 'L' })`
  margin-bottom: 60px;
`
