// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import {
  CalendarEventTime,
  CitizenCalendarEvent
} from 'lib-common/generated/api-types/calendarevent'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import { formatFirstName } from 'lib-common/names'
import { StaticChip } from 'lib-components/atoms/Chip'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { Bold, H1, H2, H3, P, Strong } from 'lib-components/typography'
import { Gap, defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes } from 'lib-icons'

import ModalAccessibilityWrapper from '../../ModalAccessibilityWrapper'
import { useTranslation } from '../../localization'
import {
  CalendarModalBackground,
  CalendarModalCloseButton,
  CalendarModalSection
} from '../CalendarModal'

interface ChildWithSurveys {
  childId: string
  surveys: CitizenCalendarEvent[]
  firstName: string
}

interface Props {
  close: () => void
  surveys: CitizenCalendarEvent[]
  childData: ReservationChild[]
}

const DiscussionHeader = styled.div`
  position: sticky;
  z-index: 100;
  border-bottom: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  text-transform: capitalize;
  padding: ${defaultMargins.m};
  top: 0;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`

export default React.memo(function DiscussionSurveyModal({
  close,
  surveys,
  childData
}: Props) {
  const i18n = useTranslation()

  const sortedSurveys = useMemo(
    () => orderBy(surveys, (s) => s.title),
    [surveys]
  )
  const sortedChildrenWithSurveys = useMemo(
    () =>
      orderBy(
        childData.filter((c) =>
          sortedSurveys.some((s) => s.timesByChild[c.id])
        ),
        (c) => c.firstName
      ),
    [childData, sortedSurveys]
  )
  const childSurveys: ChildWithSurveys[] = useMemo(
    () =>
      sortedChildrenWithSurveys.map((c) => ({
        surveys: sortedSurveys.filter((s) => s.timesByChild[c.id]),
        childId: c.id,
        firstName: c.firstName
      })),
    [sortedChildrenWithSurveys, sortedSurveys]
  )

  return (
    <ModalAccessibilityWrapper>
      <PlainModal mobileFullScreen margin="auto">
        <CalendarModalBackground>
          <div>
            <DiscussionHeader>
              <CalendarModalCloseButton
                onClick={close}
                aria-label={i18n.common.closeModal}
                icon={faTimes}
              />
              <H1 noMargin>
                {i18n.calendar.discussionTimeReservation.surveyModalTitle}
              </H1>
            </DiscussionHeader>
            <CalendarModalSection>
              <H2>
                {i18n.calendar.discussionTimeReservation.surveyModalSubTitle}
              </H2>
              <P>
                {i18n.calendar.discussionTimeReservation.surveyModalInstruction}
              </P>
              <Gap size="xs" />
              <H3>{i18n.calendar.discussionTimeReservation.surveyListTitle}</H3>
              <FixedSpaceColumn>
                {childSurveys.map((cs) => (
                  <div key={cs.childId}>
                    <StaticChip color={colors.main.m1}>
                      {formatFirstName(cs)}
                    </StaticChip>
                    {cs.surveys.map((s) => {
                      const reservations = s.timesByChild[cs.childId].filter(
                        (r) => r.childId === cs.childId
                      )
                      return (
                        <ChildSurveyElement
                          survey={s}
                          reservations={reservations}
                          key={`${cs.childId}-${s.id}`}
                        />
                      )
                    })}
                  </div>
                ))}
              </FixedSpaceColumn>
            </CalendarModalSection>
            <Gap size="m" sizeOnMobile="s" />
          </div>
        </CalendarModalBackground>
      </PlainModal>
    </ModalAccessibilityWrapper>
  )
})

interface ChildSurveyElementProps {
  survey: CitizenCalendarEvent
  reservations: CalendarEventTime[]
}

const ChildSurveyElement = React.memo(function ChildSurveyElement({
  survey,
  reservations
}: ChildSurveyElementProps) {
  const i18n = useTranslation()
  const [infoOpen, setInfoOpen] = useState(false)
  const onInfoClick = useCallback(() => setInfoOpen((prev) => !prev), [])

  return (
    <FixedSpaceColumn spacing="xs">
      <p>
        <Strong>{survey.title}</Strong>
      </p>
      {reservations.length > 0 && (
        <>
          <SurveyReservationElement spacing="s">
            <span>
              {i18n.calendar.discussionTimeReservation.reservationLabelText}
            </span>
            {reservations.map((r) => (
              <Bold
                key={r.id}
              >{`${r.date.format()} ${i18n.calendar.discussionTimeReservation.timePreDescriptor} ${r.startTime.format()} - ${r.endTime.format()}`}</Bold>
            ))}
          </SurveyReservationElement>
          <FixedSpaceRow gap="m">
            <span>{`${i18n.calendar.discussionTimeReservation.reservationInfoButtonText}`}</span>
            <InfoButton
              aria-label={i18n.common.openExpandingInfo}
              margin="zero"
              data-qa="discussion-time-reservation-info-button"
              open={infoOpen}
              onClick={onInfoClick}
            />
          </FixedSpaceRow>

          {infoOpen && (
            <ExpandingInfoBox
              data-qa="reservation-info-box"
              aria-label={
                i18n.calendar.discussionTimeReservation.reservationInfoText
              }
              info={i18n.calendar.discussionTimeReservation.reservationInfoText}
              width="full"
              close={onInfoClick}
            />
          )}
        </>
      )}
    </FixedSpaceColumn>
  )
})

const SurveyReservationElement = styled(FixedSpaceColumn)`
  margin-left: 10px;
  padding-left: 15px;
  border-left: 1px dashed black;
`
