// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import {
  CitizenCalendarEvent,
  CitizenCalendarEventTime
} from 'lib-common/generated/api-types/calendarevent'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatFirstName } from 'lib-common/names'
import { UUID } from 'lib-common/types'
import { StaticChip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { cancelMutation } from 'lib-components/atoms/buttons/MutateButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Bold, H1, H2, H3, Light, P, Strong } from 'lib-components/typography'
import { Gap, defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faQuestion } from 'lib-icons'
import { faTimes } from 'lib-icons'

import ModalAccessibilityWrapper from '../../ModalAccessibilityWrapper'
import { useLang, useTranslation } from '../../localization'
import {
  CalendarModalBackground,
  CalendarModalCloseButton,
  CalendarModalSection
} from '../CalendarModal'
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
export interface ConfirmModalState {
  visible: boolean
  childId: UUID | null
  eventTimeId: UUID | null
}
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

  const [confirmationModalState, setConfirmationModalState] =
    useState<ConfirmModalState>({
      visible: false,
      childId: null,
      eventTimeId: null
    })
  const onCancelClick = useCallback(
    (childId: UUID, eventTimeId: UUID) => {
      setConfirmationModalState({ visible: true, childId, eventTimeId })
    },
    [setConfirmationModalState]
  )

  const onConfirmClose = useCallback(
    () =>
      setConfirmationModalState({
        visible: false,
        childId: null,
        eventTimeId: null
      }),
    [setConfirmationModalState]
  )

  return (
    <ModalAccessibilityWrapper>
      {confirmationModalState.visible && (
        <MutateFormModal
          resolveMutation={deleteCalendarEventTimeReservationMutation}
          data-qa="confirm-cancel-modal"
          resolveAction={() => {
            if (
              confirmationModalState.childId &&
              confirmationModalState.eventTimeId
            ) {
              return {
                childId: confirmationModalState.childId,
                calendarEventTimeId: confirmationModalState.eventTimeId
              }
            } else return cancelMutation
          }}
          rejectAction={onConfirmClose}
          title={i18n.calendar.discussionTimeReservation.confirmCancel.title}
          onSuccess={onConfirmClose}
          rejectLabel={
            i18n.calendar.discussionTimeReservation.confirmCancel.cancel
          }
          resolveLabel={
            i18n.calendar.discussionTimeReservation.cancelTimeButtonText
          }
          type="warning"
          icon={faQuestion}
          zIndex={1000}
        />
      )}
      <PlainModal
        mobileFullScreen
        margin="auto"
        zIndex={100}
        data-qa="discussions-modal"
      >
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
                    onCancelClick={onCancelClick}
                    key={`child-${cs.childId}`}
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
  onCancelClick: (childId: UUID, eventTimeId: UUID) => void
  openDiscussionReservations: (
    selectedChildId: UUID,
    selectedEventId: UUID
  ) => void
}

const DiscussionChildElement = React.memo(function DiscussionChildElement({
  childWithSurveys,
  onCancelClick,
  openDiscussionReservations
}: DiscussionChildElementProps) {
  return (
    <div data-qa={`discussion-child-${childWithSurveys.childId}`}>
      <StaticChip color={colors.main.m1}>
        {formatFirstName(childWithSurveys)}
      </StaticChip>
      {childWithSurveys.surveys.map((s) => {
        const reservations = s.timesByChild[childWithSurveys.childId].filter(
          (r) => r.childId === childWithSurveys.childId
        )
        const sortedReservations = orderBy(reservations, [
          'date',
          'startTime',
          'endTime'
        ])
        return (
          <ChildSurveyElement
            onCancelClick={onCancelClick}
            survey={s}
            reservations={sortedReservations}
            childId={childWithSurveys.childId}
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
  reservations: CitizenCalendarEventTime[]
  childId: UUID
  openDiscussionReservations: () => void
  onCancelClick: (childId: UUID, eventTimeId: UUID) => void
}
const ChildSurveyElement = React.memo(function ChildSurveyElement({
  survey,
  reservations,
  childId,
  openDiscussionReservations,
  onCancelClick
}: ChildSurveyElementProps) {
  const i18n = useTranslation()
  const [lang] = useLang()
  const today = LocalDate.todayInHelsinkiTz()
  return (
    <SurveyElementContainer data-qa={`child-survey-${childId}-${survey.id}`}>
      {reservations.length > 0 ? (
        <>
          <P data-qa={`survey-title-${survey.id}`}>
            <Strong>{survey.title}</Strong>
          </P>
          <SurveyReservationElement spacing="s">
            <span>
              {i18n.calendar.discussionTimeReservation.reservationLabelText}
            </span>
            {reservations.map((r) => (
              <FixedSpaceColumn key={r.id} alignItems="flex-start">
                <div data-qa={`reservation-${r.id}`}>
                  {r.date.isBefore(today) ? (
                    <Light>
                      {`${r.date.format('EEEEEE d.M.', lang)} 
                    ${i18n.calendar.discussionTimeReservation.timePreDescriptor} 
                    ${r.startTime.format()} - ${r.endTime.format()}`}
                    </Light>
                  ) : (
                    <Bold>
                      {`${r.date.format('EEEEEE d.M.', lang)} 
                    ${i18n.calendar.discussionTimeReservation.timePreDescriptor} 
                    ${r.startTime.format()} - ${r.endTime.format()}`}
                    </Bold>
                  )}
                </div>
                {r.date.isEqualOrAfter(today) && (
                  <>
                    <Button
                      appearance="inline"
                      icon={faTimes}
                      text={
                        i18n.calendar.discussionTimeReservation
                          .cancelTimeButtonText
                      }
                      disabled={!r.isEditable}
                      onClick={() => {
                        if (r.childId !== null) {
                          onCancelClick(r.childId, r.id)
                        }
                      }}
                      data-qa="reservation-cancel-button"
                    />
                    {!r.isEditable && (
                      <InfoBox
                        aria-label={
                          i18n.calendar.discussionTimeReservation
                            .cancellationDeadlineInfoMessage
                        }
                        message={
                          i18n.calendar.discussionTimeReservation
                            .cancellationDeadlineInfoMessage
                        }
                      />
                    )}
                  </>
                )}
              </FixedSpaceColumn>
            ))}
          </SurveyReservationElement>
        </>
      ) : (
        <FixedSpaceRow alignItems="baseline" justifyContent="space-between">
          <SurveyTitleLabel>
            <p data-qa={`survey-title-${survey.id}`}>
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
              data-qa="open-survey-reservations-button"
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

const InfoContainer = styled.div`
  margin-top: 16px;
`

const ReservationButtonContainer = styled.div`
  min-width: 126px;
`

const SurveyElementContainer = styled(FixedSpaceColumn)`
  margin: 0 10px;
  word-break: break-word;
`
