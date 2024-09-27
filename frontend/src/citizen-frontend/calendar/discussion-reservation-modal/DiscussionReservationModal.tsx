// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import groupBy from 'lodash/groupBy'
import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Failure } from 'lib-common/api'
import { mapped, object, required, value } from 'lib-common/form/form'
import { BoundFormState, useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import {
  CalendarEventTime,
  CalendarEventTimeCitizenReservationForm,
  CitizenCalendarEvent
} from 'lib-common/generated/api-types/calendarevent'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatFirstName } from 'lib-common/names'
import { capitalizeFirstLetter } from 'lib-common/string'
import { UUID } from 'lib-common/types'
import { StaticChip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  MutateButton,
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { H1, H2, H3, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowLeft } from 'lib-icons'
import { faTimes } from 'lib-icons'

import ModalAccessibilityWrapper from '../../ModalAccessibilityWrapper'
import { useLang, useTranslation } from '../../localization'
import { BottomFooterContainer } from '../BottomFooterContainer'
import {
  CalendarModalBackground,
  CalendarModalButtons,
  CalendarModalCloseButton,
  CalendarModalSection
} from '../CalendarModal'
import { WordBreakContainer } from '../DayView'
import { addCalendarEventTimeReservationMutation, queryKeys } from '../queries'

import { DiscussionHeader } from './DiscussionSurveyModal'
import { showModalEventTime } from './discussion-survey'

const discussionTimeReservationForm = mapped(
  object({
    selectedChild: required(value<UUID | undefined>()),
    eventTimeId: required(value<UUID | null>())
  }),
  (output): CalendarEventTimeCitizenReservationForm => ({
    childId: output.selectedChild,
    calendarEventTimeId: output.eventTimeId ?? ''
  })
)

function initialFormState(
  selectedChild?: ReservationChild
): StateOf<typeof discussionTimeReservationForm> {
  return {
    selectedChild: selectedChild?.id,
    eventTimeId: null
  }
}

interface Props {
  close: () => void
  childData?: ReservationChild
  eventData?: CitizenCalendarEvent
}

interface DiscussionTimeDay {
  date: string
  times: CalendarEventTime[]
}

export default React.memo(function DiscussionReservationModal({
  close,
  childData,
  eventData
}: Props) {
  const i18n = useTranslation()
  const t = i18n.calendar.discussionTimeReservation

  const [infoOpen, setInfoOpen] = useState(false)
  const onInfoClick = useCallback(() => setInfoOpen((prev) => !prev), [])

  const eventTimeDays: DiscussionTimeDay[] = useMemo(() => {
    const today = LocalDate.todayInHelsinkiTz()
    const eventTimes =
      eventData && childData
        ? orderBy(
            eventData.timesByChild[childData.id].filter((et) =>
              showModalEventTime(et, today)
            ),
            ['date', 'startTime', 'endTime']
          )
        : []

    return Object.entries(groupBy(eventTimes, (et) => et.date.format())).map(
      ([key, value]) => ({ date: key, times: value })
    )
  }, [eventData, childData])

  const form = useForm(
    discussionTimeReservationForm,
    () => initialFormState(childData),
    i18n.validationErrors
  )
  const { eventTimeId } = useFormFields(form)
  const navigate = useNavigate()

  const [timeAlreadyReserved, setTimeAlreadyReserved] = useState(false)

  const hasReservations = useMemo(() => {
    return eventTimeDays.some((d) => d.times.some((t) => t.childId !== null))
  }, [eventTimeDays])

  const queryClient = useQueryClient()
  const invalidateEvents = useCallback(() => {
    void queryClient.invalidateQueries({
      queryKey: queryKeys.allEvents()
    })
  }, [queryClient])

  const returnToSurveyModal = useCallback(() => {
    close()
    navigate(`/calendar?modal=discussions`)
  }, [navigate, close])

  return (
    <ModalAccessibilityWrapper>
      <PlainModal
        mobileFullScreen
        margin="auto"
        data-qa="discussion-reservations-modal"
        onEscapeKey={close}
      >
        <CalendarModalBackground>
          <BottomFooterContainer>
            <div>
              <DiscussionHeader>
                <CalendarModalCloseButton
                  onClick={close}
                  aria-label={i18n.common.closeModal}
                  icon={faTimes}
                />
                <H1 noMargin>{t.surveyModalTitle}</H1>
              </DiscussionHeader>
              <div>
                <BackButtonInline
                  onClick={returnToSurveyModal}
                  icon={faArrowLeft}
                  text={t.backButtonText}
                  aria-label={t.backButtonText}
                  appearance="inline"
                />
              </div>
              <CalendarModalSection>
                <WordBreakContainer>
                  <H2>{eventData?.title}</H2>
                  <p>{eventData?.description}</p>
                  <H3>{t.reservationChildTitle}</H3>
                  <StaticChip color={colors.main.m1} translate="no">
                    {childData ? formatFirstName(childData) : ''}
                  </StaticChip>
                </WordBreakContainer>
              </CalendarModalSection>
              <Gap size="zero" sizeOnMobile="s" />
              <CalendarModalSection>
                <FixedSpaceColumn spacing="m">
                  <FixedSpaceRow justifyContent="space-between">
                    <div />
                    <FixedSpaceRow justifyContent="space-between" />
                  </FixedSpaceRow>
                  {eventTimeDays.length > 0 && !hasReservations ? (
                    <>
                      <div>
                        <FixedSpaceRow gap="m" alignItems="center">
                          <div>
                            <H2
                              noMargin
                            >{`${i18n.calendar.discussionTimeReservation.freeTimesInfoButtonText}`}</H2>
                          </div>
                          <InfoButton
                            aria-label={i18n.common.openExpandingInfo}
                            margin="zero"
                            data-qa="free-times-info-button"
                            open={infoOpen}
                            onClick={onInfoClick}
                          />
                        </FixedSpaceRow>

                        {infoOpen && (
                          <ExpandingInfoBox
                            data-qa="free-times-info-box"
                            aria-label={
                              i18n.calendar.discussionTimeReservation
                                .freeTimesInfoText
                            }
                            info={
                              i18n.calendar.discussionTimeReservation
                                .freeTimesInfoText
                            }
                            width="full"
                            close={onInfoClick}
                          />
                        )}
                      </div>
                      <ReservationGrid>
                        <Label />
                        <Label>{t.reservationTime}</Label>
                        <Label>{t.reservationSelect}</Label>
                        {eventTimeDays.map((etd) => (
                          <React.Fragment key={etd.date}>
                            {etd.times.map((t, i) => (
                              <ReservationGridItem
                                itemData={t}
                                showDate={i === 0}
                                bind={eventTimeId}
                                key={t.id}
                              />
                            ))}
                          </React.Fragment>
                        ))}
                      </ReservationGrid>
                    </>
                  ) : !hasReservations ? (
                    <P>{t.noReservationsText}</P>
                  ) : null}
                </FixedSpaceColumn>
              </CalendarModalSection>
              <Gap size="zero" sizeOnMobile="s" />

              {timeAlreadyReserved && (
                <AlertBoxWrapper>
                  <AlertBox
                    title={t.reservationError}
                    message={t.reservationErrorInstruction}
                  />
                </AlertBoxWrapper>
              )}
            </div>
            <CalendarModalButtons>
              <Button
                onClick={returnToSurveyModal}
                data-qa="modal-cancelBtn"
                text={i18n.common.cancel}
              />
              <MutateButton
                primary
                text={i18n.common.confirm}
                disabled={eventTimeId.state === null}
                mutation={addCalendarEventTimeReservationMutation}
                onClick={() => {
                  if (!form.isValid()) {
                    return cancelMutation
                  }
                  return { body: form.value() }
                }}
                onSuccess={returnToSurveyModal}
                data-qa="modal-okBtn"
                onFailure={(failure: Failure<unknown>) => {
                  setTimeAlreadyReserved(
                    failure.errorCode === 'TIME_ALREADY_RESERVED'
                  )
                  eventTimeId.set(null)
                  invalidateEvents()
                }}
              />
            </CalendarModalButtons>
          </BottomFooterContainer>
        </CalendarModalBackground>
        <CalendarModalCloseButton
          onClick={close}
          aria-label={i18n.common.closeModal}
          icon={faTimes}
        />
      </PlainModal>
    </ModalAccessibilityWrapper>
  )
})

interface ReservationGridItemProps {
  itemData: CalendarEventTime
  bind: BoundFormState<string>
  showDate: boolean
}

export const ReservationGridItem = React.memo(function ReservationGridItem({
  itemData,
  bind,
  showDate
}: ReservationGridItemProps) {
  const [lang] = useLang()
  const durationString = useMemo(
    () => `${itemData.startTime.format()} - ${itemData.endTime.format()}`,
    [itemData.startTime, itemData.endTime]
  )

  return (
    <>
      <Label>
        {showDate
          ? capitalizeFirstLetter(itemData.date.format('EEEEEE d.M.', lang))
          : ''}
      </Label>
      <Label data-qa="discussion-time-duration">{durationString}</Label>
      <Radio
        key={itemData.id}
        checked={bind.state === itemData.id}
        onChange={() => bind.set(itemData.id)}
        label=""
        ariaLabel={durationString}
        data-qa={`radio-${itemData.id}`}
      />
    </>
  )
})

export const LineContainer = styled.div`
  padding: 0;
  margin: 0;
`
const ReservationGrid = styled.div`
  display: grid;
  grid-template-columns: 3fr 2fr 1fr;
  gap: ${defaultMargins.xs};
  align-items: center;
`
const AlertBoxWrapper = styled.div`
  margin: 0 20px;
`
export const BackButtonInline = styled(Button)`
  color: ${colors.main.m1};
  margin-top: ${defaultMargins.s};
  margin-left: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
  overflow-x: hidden;
  display: flex;
`
