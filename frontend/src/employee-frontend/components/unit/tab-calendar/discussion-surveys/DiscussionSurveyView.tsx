// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPen, faTrash } from 'Icons'
import React, { useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useTranslation } from 'employee-frontend/state/i18n'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { StaticChip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { H2, H3, Label } from 'lib-components/typography'
import { theme } from 'lib-customizations/common'

import { unitGroupDetailsQuery } from '../../queries'

import InviteeSection from './InviteeSection'

const WidthLimiter = styled.div`
  max-width: 400px;
`
const FormFieldGroup = styled(FixedSpaceColumn).attrs({ spacing: 'S' })``
const FormSectionGroup = styled(FixedSpaceColumn).attrs({ spacing: 'L' })`
  margin-bottom: 60px;
`

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
  const onDelete = (eventId: UUID) => eventId
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

  const status = useMemo(
    () =>
      eventData.period.end.isBefore(LocalDate.todayInHelsinkiTz())
        ? 'ENDED'
        : 'SENT',
    [eventData.period]
  )

  return (
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
              onClick={() => onDelete(eventData.id)}
              data-qa={`button-delete-survey-${eventData.id}`}
            />
            <SurveyStatusChip status={status} />
          </FixedSpaceRow>
        </FixedSpaceRow>

        <FormFieldGroup>
          <Label>{t.discussionReservation.surveyModifiedAt}</Label>
          <p>Muokkausaika tähän!!</p>
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

        <FormSectionGroup>
          <H3>{t.discussionReservation.surveyInviteeTitle}</H3>
          <FormFieldGroup>
            {renderResult(unitDetails, (unitDetailsResult) => (
              <InviteeSection
                eventData={eventData}
                unitDetails={unitDetailsResult}
              />
            ))}
          </FormFieldGroup>
        </FormSectionGroup>

        <FormSectionGroup>
          <H3>{t.discussionReservation.surveyDiscussionTimesTitle}</H3>
          <HorizontalLine />

          <HorizontalLine />
        </FormSectionGroup>
      </ContentArea>
    </Container>
  )
})
