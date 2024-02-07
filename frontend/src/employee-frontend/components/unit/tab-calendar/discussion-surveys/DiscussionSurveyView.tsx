// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPen, faQuestion, faTrash } from 'Icons'
import orderBy from 'lodash/orderBy'
import partition from 'lodash/partition'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UIContext } from 'employee-frontend/state/ui'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { UnitGroupDetails } from 'lib-common/generated/api-types/daycare'
import { ChildBasics } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { useMutation, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
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
  TimesCalendarContainer
} from './survey-editor/DiscussionTimesForm'
import { DiscussionReservationCalendar } from './times-calendar/TimesCalendar'

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
    calendarPeriodEnd
  }: {
    unitId: UUID
    groupId: UUID
    eventData: CalendarEvent
    invitees: ChildBasics[]
    calendarPeriodEnd: LocalDate
  }) {
    const [calendarHorizonInMonths, setCalendarHorizonInMonths] =
      useState<number>(0)
    const { i18n } = useTranslation()

    const calendarRange = useMemo(() => {
      const today = LocalDate.todayInSystemTz()
      const previousMondayFromStart = today.subDays(today.getIsoDayOfWeek() - 1)
      const lastDayOfEndMonth = LocalDate.of(
        calendarPeriodEnd.year,
        calendarPeriodEnd.month,
        1
      )
        .addMonths(1 + calendarHorizonInMonths)
        .subDays(1)
      const standardizedPeriod = new FiniteDateRange(
        previousMondayFromStart,
        lastDayOfEndMonth
      )
      return standardizedPeriod
    }, [calendarPeriodEnd, calendarHorizonInMonths])

    return (
      <TimesCalendarContainer>
        <DiscussionReservationCalendar
          unitId={unitId}
          groupId={groupId}
          eventData={eventData}
          calendarRange={calendarRange}
          invitees={invitees}
        />
        <Gap size="L" />
        <FixedSpaceRow fullWidth alignItems="center" justifyContent="center">
          <ExpandHorizonButton
            onClick={() =>
              setCalendarHorizonInMonths(calendarHorizonInMonths + 1)
            }
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

  const unitDetails = useQueryResult(
    unitGroupDetailsQuery(unitId, eventData.period.start, eventData.period.end)
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

  const getInvitedChildren = useCallback(
    (unitDetails: UnitGroupDetails) => {
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
          const isPartOfIndividualSelections = childSelections.includes(
            p.child.id
          )
          return isPartOfFullGroupSelection || isPartOfIndividualSelections
        })
        .map((p) => p.child)

      const sortedResults = orderBy(allInvitedChildren, [
        (c) => c.lastName,
        (c) => c.firstName,
        (c) => c.id
      ])
      return sortedResults
    },
    [eventData.groups, eventData.individualChildren, eventData.period]
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
            const sortedInvitees = getInvitedChildren(unitDetailsResult)
            const reservations = eventData.times.filter(
              (t) => t.childId !== null
            )
            const [reserved, unreserved] = partition(sortedInvitees, (e) =>
              reservations.some((r) => r.childId === e.id)
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
                    invitees={sortedInvitees}
                    calendarPeriodEnd={eventData.period.end}
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
