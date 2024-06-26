// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faX } from '@fortawesome/free-solid-svg-icons'
import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import {
  CalendarEventTime,
  CitizenCalendarEvent
} from 'lib-common/generated/api-types/calendarevent'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatFirstName } from 'lib-common/names'
import { UUID } from 'lib-common/types'
import { StaticChip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  cancelMutation,
  MutateButton
} from 'lib-components/atoms/buttons/MutateButton'
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
import { useLang, useTranslation } from '../../localization'
import {
  CalendarModalBackground,
  CalendarModalCloseButton,
  CalendarModalSection
} from '../CalendarModal'
import { getStartOfDiscussionReservationWindow } from '../CalendarPage'
import { deleteCalendarEventTimeReservationMutation } from '../queries'

interface ChildWithSurveys {
  childId: string
  surveys: CitizenCalendarEvent[]
  firstName: string
}

interface Props {
  close: () => void
  surveys: CitizenCalendarEvent[]
  childData: ReservationChild[]
  openDiscussionReservations: (
    selectedChildId: UUID | undefined,
    selectedEventId: UUID | undefined
  ) => void
}

export const DiscussionHeader = styled.div`
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
  childData,
  openDiscussionReservations
}: Props) {
  const i18n = useTranslation()

  const [infoOpen, setInfoOpen] = useState(false)
  const onInfoClick = useCallback(() => setInfoOpen((prev) => !prev), [])

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
              <H3>{i18n.calendar.discussionTimeReservation.surveyListTitle}</H3>
              <FixedSpaceColumn>
                {childSurveys.map((cs) => (
                  <DiscussionChildElement
                    childWithSurveys={cs}
                    openDiscussionReservations={openDiscussionReservations}
                    key={`child-${cs.childId}`}
                    data-qa={`discussion-child-${cs.childId}`}
                  />
                ))}
              </FixedSpaceColumn>
              <InfoContainer>
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
                      i18n.calendar.discussionTimeReservation
                        .reservationInfoText
                    }
                    info={
                      i18n.calendar.discussionTimeReservation
                        .reservationInfoText
                    }
                    width="full"
                    close={onInfoClick}
                  />
                )}
              </InfoContainer>
            </CalendarModalSection>
            <Gap size="m" sizeOnMobile="s" />
          </div>
        </CalendarModalBackground>
      </PlainModal>
    </ModalAccessibilityWrapper>
  )
})

interface DiscussionChildElementProps {
  childWithSurveys: ChildWithSurveys
  openDiscussionReservations: (
    selectedChildId: UUID,
    selectedEventId: UUID
  ) => void
}

const DiscussionChildElement = React.memo(function DiscussionChildElement({
  childWithSurveys,
  openDiscussionReservations
}: DiscussionChildElementProps) {
  const today = LocalDate.todayInHelsinkiTz()
  return (
    <div>
      <StaticChip color={colors.main.m1}>
        {formatFirstName(childWithSurveys)}
      </StaticChip>
      {childWithSurveys.surveys.map((s) => {
        const reservations = s.timesByChild[childWithSurveys.childId].filter(
          (r) =>
            r.childId === childWithSurveys.childId &&
            r.date.isEqualOrAfter(today)
        )
        const sortedReservations = orderBy(reservations, [
          'date',
          'startTime',
          'endTime'
        ])
        return (
          <ChildSurveyElement
            survey={s}
            reservations={sortedReservations}
            openDiscussionReservations={() =>
              openDiscussionReservations(childWithSurveys.childId, s.id)
            }
            key={`${childWithSurveys.childId}-${s.id}`}
          />
        )
      })}
    </div>
  )
})

interface ChildSurveyElementProps {
  survey: CitizenCalendarEvent
  reservations: CalendarEventTime[]
  openDiscussionReservations: () => void
}
const ChildSurveyElement = React.memo(function ChildSurveyElement({
  survey,
  reservations,
  openDiscussionReservations
}: ChildSurveyElementProps) {
  const i18n = useTranslation()
  const [lang] = useLang()

  return (
    <SurveyElementContainer>
      {reservations.length > 0 ? (
        <ReservationListContainer>
          <p>
            <Strong>{survey.title}</Strong>
          </p>
          <SurveyReservationElement spacing="s">
            <span>
              {i18n.calendar.discussionTimeReservation.reservationLabelText}
            </span>
            {reservations.map((r) => (
              <FixedSpaceColumn key={r.id} alignItems="flex-start">
                <div>
                  <Bold>
                    {`${r.date.format('EEEEEE d.M.', lang)} 
                    ${i18n.calendar.discussionTimeReservation.timePreDescriptor} 
                    ${r.startTime.format()} - ${r.endTime.format()}`}
                  </Bold>
                </div>
                <MutateButton
                  appearance="inline"
                  icon={faX}
                  text={
                    i18n.calendar.discussionTimeReservation.cancelTimeButtonText
                  }
                  disabled={getStartOfDiscussionReservationWindow().isEqualOrAfter(
                    r.date
                  )}
                  mutation={deleteCalendarEventTimeReservationMutation}
                  onClick={() => {
                    if (r.childId !== null) {
                      return { childId: r.childId, calendarEventTimeId: r.id }
                    } else {
                      return cancelMutation
                    }
                  }}
                />
              </FixedSpaceColumn>
            ))}
          </SurveyReservationElement>
        </ReservationListContainer>
      ) : (
        <FixedSpaceRow alignItems="baseline" justifyContent="space-between">
          <SurveyTitleLabel>
            <p>
              <Strong>{survey.title}</Strong>
            </p>
          </SurveyTitleLabel>
          <ReservationButtonContainer>
            <Button
              appearance="link"
              onClick={openDiscussionReservations}
              text={
                i18n.calendar.discussionTimeReservation
                  .reservationModalButtonText
              }
              data-qa="open-discussion-reservations-modal"
            />
          </ReservationButtonContainer>
        </FixedSpaceRow>
      )}
    </SurveyElementContainer>
  )
})

const SurveyReservationElement = styled(FixedSpaceColumn)`
  margin-left: 10px;
  padding-left: 15px;
  border-left: 1px dashed black;
`

const SurveyTitleLabel = styled.div`
  word-break: break-all;
  max-width: 350px;
`

const ReservationListContainer = styled.div`
  word-break: break-word;
`

const InfoContainer = styled.div`
  margin-top: 16px;
`

const ReservationButtonContainer = styled.div`
  min-width: 126px;
`

const SurveyElementContainer = styled(FixedSpaceColumn)`
  margin: 0 10px;
`
