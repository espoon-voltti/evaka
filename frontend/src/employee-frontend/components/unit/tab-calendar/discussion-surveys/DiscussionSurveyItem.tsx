// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled from 'styled-components'

import type { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'
import { StaticChip } from 'lib-components/atoms/Chip'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3 } from 'lib-components/typography'
import { theme } from 'lib-customizations/common'

import { useTranslation } from '../../../../state/i18n'

const SurveyItemContainer = styled(FixedSpaceRow)`
  border-bottom: 1px dashed ${(p) => p.theme.colors.grayscale.g35};
  padding: 10px;
  margin: 0px;
`

const ModifiedAtText = styled.span`
  font-weight: lighter;
`

export default React.memo(function DiscussionReservationSurveyListItem({
  eventData
}: {
  eventData: CalendarEvent
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events.discussionReservation
  const status = useMemo(
    () =>
      eventData.period.end.isBefore(LocalDate.todayInHelsinkiTz())
        ? 'ENDED'
        : 'SENT',
    [eventData.period.end]
  )

  const statusColor = {
    SENT: theme.colors.accents.a3emerald,
    ENDED: theme.colors.grayscale.g15
  }
  return (
    <SurveyItemContainer
      data-qa={`survey-${eventData.id}`}
      justifyContent="space-between"
      alignItems="center"
    >
      <FixedSpaceRow>
        <H3 data-qa="survey-title">{eventData.title}</H3>
      </FixedSpaceRow>
      <FixedSpaceRow justifyContent="flex-end" spacing="L" alignItems="center">
        <ModifiedAtText data-qa="survey-modified-at">
          {`${t.surveyModifiedAt} ${eventData.contentModifiedAt.toLocalDate().format()}`}
        </ModifiedAtText>
        <StaticChip
          data-qa="survey-status"
          fitContent
          color={statusColor[status]}
        >
          {t.surveyStatus[status]}
        </StaticChip>
      </FixedSpaceRow>
    </SurveyItemContainer>
  )
})
