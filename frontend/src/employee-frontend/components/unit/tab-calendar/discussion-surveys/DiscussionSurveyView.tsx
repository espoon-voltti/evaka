// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPen, faQuestion, faTrash } from 'Icons'
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
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { BoundForm, useForm, useFormFields } from 'lib-common/form/hooks'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { UnitGroupDetails } from 'lib-common/generated/api-types/daycare'
import { ChildBasics } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { useMutation, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import { StaticChip } from 'lib-components/atoms/Chip'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
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

import { unitGroupDetailsQuery } from '../../queries'
import { deleteCalendarEventMutation } from '../queries'

import InviteeSection from './InviteeSection'
import {
  BorderedBox,
  NewEventTimeForm,
  TimesCalendarContainer
} from './survey-editor/DiscussionTimesForm'
import { eventTimeArray, timesForm } from './survey-editor/form'
import { DiscussionReservationCalendar } from './times-calendar/TimesCalendar'

export interface ChildGroupInfo {
  child: ChildBasics
  groupPlacements: DateRange[]
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
  const allInvitedChildren = unitDetails.placements
    .filter((p) => {
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
    .map((p) => ({
      child: p.child,
      groupPlacements: p.groupPlacements
        .filter((gp) => gp.groupId === groupId)
        .map((gp) => new DateRange(gp.startDate, gp.endDate))
    }))

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
    unitId,
    groupId,
    eventData,
    invitees,
    calendarRange,
    expandCalendarAction,
    times,
    addAction,
    removeAction,
    horizonRef
  }: {
    unitId: UUID
    groupId: UUID
    eventData: CalendarEvent
    invitees: ChildGroupInfo[]
    calendarRange: FiniteDateRange
    expandCalendarAction: () => void
    times: BoundForm<typeof eventTimeArray>
    addAction: (et: NewEventTimeForm) => void
    removeAction: (id: UUID) => void
    horizonRef: MutableRefObject<HTMLDivElement | null>
  }) {
    const { i18n } = useTranslation()

    return (
      <TimesCalendarContainer>
        <DiscussionReservationCalendar
          unitId={unitId}
          groupId={groupId}
          eventData={eventData}
          calendarRange={calendarRange}
          invitees={invitees}
          times={times}
          addAction={addAction}
          removeAction={removeAction}
          horizonRef={horizonRef}
        />
        <Gap size="L" />
        <FixedSpaceRow fullWidth alignItems="center" justifyContent="center">
          <ExpandHorizonButton
            onClick={expandCalendarAction}
            text={
              i18n.unit.calendar.events.discussionReservation.calendar
                .addTimeButton
            }
          />
        </FixedSpaceRow>
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

  const getCalendarHorizon = useCallback(() => {
    const today = LocalDate.todayInSystemTz()
    const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)

    const defaultHorizonDate = previousMonday.addMonths(3).lastDayOfMonth()

    const eventDataHorizonDate = eventData.period.end.lastDayOfMonth()

    return defaultHorizonDate.isAfter(eventDataHorizonDate)
      ? defaultHorizonDate
      : eventDataHorizonDate
  }, [eventData.period.end])

  const horizonRef = useRef<HTMLDivElement | null>(null)
  const [calendarHorizonDate, setCalendarHorizonDate] =
    useState<LocalDate>(getCalendarHorizon())

  const calendarRange = useMemo(() => {
    const today = LocalDate.todayInSystemTz()

    const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)

    return new FiniteDateRange(previousMonday, calendarHorizonDate)
  }, [calendarHorizonDate])

  const unitDetails = useQueryResult(
    unitGroupDetailsQuery(unitId, calendarRange.start, calendarRange.end)
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
              deleteCalendarEvent({ groupId: groupId, eventId: eventData.id })
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
        />
      )}
      <Container>
        <ReturnButton
          label={i18n.common.goBack}
          onClick={() => {
            navigate('..', { relative: 'path' })
          }}
        />
        <ContentArea opaque>
          <FixedSpaceRow alignItems="center" justifyContent="space-between">
            <H2>{eventData.title}</H2>
            <FixedSpaceRow alignItems="center" spacing="L">
              <InlineButton
                icon={faTrash}
                text={t.discussionReservation.deleteSurveyButton}
                onClick={() => setDeleteConfirmModalVisible(true)}
                data-qa={`button-delete-survey-${eventData.id}`}
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
            <InlineButton
              icon={faPen}
              text={t.discussionReservation.editSurveyButton}
              onClick={onEdit}
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
                <p>{eventData?.description}</p>
              </WidthLimiter>
            </FormFieldGroup>
          </FormSectionGroup>
          {renderResult(unitDetails, (unitDetailsResult) => {
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
                    <H3 noMargin>
                      {t.discussionReservation.surveyDiscussionTimesTitle}
                    </H3>
                  </BorderedBox>
                  <ReservationCalendarSection
                    unitId={unitId}
                    groupId={groupId}
                    eventData={eventData}
                    invitees={sortedCalendarInvitees}
                    calendarRange={calendarRange}
                    expandCalendarAction={() => {
                      setCalendarHorizonDate(
                        calendarHorizonDate.addMonths(1).lastDayOfMonth()
                      )
                      scrollRefIntoView(horizonRef, 200)
                    }}
                    times={times}
                    addAction={addTime}
                    removeAction={removeTimeById}
                    horizonRef={horizonRef}
                  />
                </FormSectionGroup>
              </>
            )
          })}
        </ContentArea>
      </Container>
    </>
  )
})

const ExpandHorizonButton = styled(Button)`
  margin-bottom: 10px;
`
const WidthLimiter = styled.div`
  max-width: 400px;
`
const FormFieldGroup = styled(FixedSpaceColumn).attrs({ spacing: 'S' })``
const FormSectionGroup = styled(FixedSpaceColumn).attrs({ spacing: 'L' })`
  margin-bottom: 60px;
`
