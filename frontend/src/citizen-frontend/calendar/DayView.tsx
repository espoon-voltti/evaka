// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faTimes } from 'Icons'
import partition from 'lodash/partition'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { getDuplicateChildInfo } from 'citizen-frontend/utils/duplicated-child-utils'
import { mapScheduleType } from 'lib-common/api-types/placement'
import FiniteDateRange from 'lib-common/finite-date-range'
import { array, mapped, object, value } from 'lib-common/form/form'
import {
  BoundForm,
  useBoolean,
  useForm,
  useFormElems,
  useFormField
} from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import {
  AttendingChild,
  CalendarEventTime,
  CalendarEventType,
  CitizenCalendarEvent
} from 'lib-common/generated/api-types/calendarevent'
import { HolidayPeriodEffect } from 'lib-common/generated/api-types/holidayperiod'
import { ScheduleType } from 'lib-common/generated/api-types/placement'
import {
  AbsenceInfo,
  DailyReservationRequest,
  ReservableTimeRange,
  Reservation,
  ReservationResponse,
  ReservationResponseDay,
  ReservationsResponse,
  UsedServiceResult
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatFirstName } from 'lib-common/names'
import { reservationHasTimes } from 'lib-common/reservations'
import TimeInterval from 'lib-common/time-interval'
import { UUID } from 'lib-common/types'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import {
  LegacyButton,
  StyledButton
} from 'lib-components/atoms/buttons/LegacyButton'
import {
  MutateButton,
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import { tabletMin } from 'lib-components/breakpoints'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { MobileOnly } from 'lib-components/layout/responsive-layout'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import {
  ModalCloseButton,
  ModalHeader,
  PlainModal
} from 'lib-components/molecules/modals/BaseModal'
import { H1, H2, H3, Label, LabelLike, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
import { faChevronLeft, faChevronRight } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { Translations, useLang, useTranslation } from '../localization'

import AttendanceInfo from './AttendanceInfo'
import { BottomFooterContainer } from './BottomFooterContainer'
import { CalendarModalBackground, CalendarModalSection } from './CalendarModal'
import {
  ChildImageData,
  getChildImages,
  RoundChildImage
} from './RoundChildImages'
import { formatReservation } from './calendar-elements'
import { isEventTimeCancellable } from './discussion-reservation-modal/discussion-survey'
import {
  deleteCalendarEventTimeReservationMutation,
  postReservationsMutation
} from './queries'
import { Day } from './reservation-modal/TimeInputs'
import {
  day,
  resetDay,
  toDailyReservationRequest
} from './reservation-modal/form'

interface Props {
  date: LocalDate
  reservationsResponse: ReservationsResponse
  selectDate: (date: LocalDate) => void
  onClose: () => void
  openAbsenceModal: (initialDate: LocalDate) => void
  events: CitizenCalendarEvent[]
}

export default React.memo(function DayView({
  date,
  reservationsResponse,
  selectDate,
  onClose,
  openAbsenceModal,
  events
}: Props) {
  const i18n = useTranslation()

  const childImages = useMemo(
    () => getChildImages(reservationsResponse.children),
    [reservationsResponse.children]
  )

  const modalData = useMemo(
    () =>
      computeModalData(date, reservationsResponse, events, childImages, i18n),
    [childImages, date, events, i18n, reservationsResponse]
  )
  const dateActions = useMemo(
    () =>
      findAdjacentDates(selectDate, reservationsResponse, modalData?.dateIndex),
    [modalData?.dateIndex, reservationsResponse, selectDate]
  )

  const onCreateAbsence = useCallback(
    () => openAbsenceModal(date),
    [openAbsenceModal, date]
  )

  const [editing, edit] = useBoolean(false)

  return modalData === undefined ? (
    <DayModal
      date={date}
      dateActions={dateActions}
      rows={[]}
      onClose={onClose}
      leftButton={undefined}
      rightButton={undefined}
    />
  ) : editing ? (
    <Edit modalData={modalData} onClose={onClose} onCancel={edit.off} />
  ) : (
    <View
      modalData={modalData}
      dateActions={dateActions}
      reservableRange={reservationsResponse.reservableRange}
      onClose={onClose}
      onEditReservations={edit.on}
      onCreateAbsence={onCreateAbsence}
    />
  )
})

function View({
  modalData,
  dateActions,
  reservableRange,
  onClose,
  onEditReservations,
  onCreateAbsence
}: {
  modalData: ModalData
  dateActions: DateActions
  reservableRange: FiniteDateRange
  onClose: () => void
  onEditReservations: () => void
  onCreateAbsence: () => void
}) {
  const i18n = useTranslation()

  const { reservationsEditable, absencesEditable } = useMemo(
    () => isEditable(modalData.response, reservableRange),
    [modalData.response, reservableRange]
  )

  const leftButton = reservationsEditable ? (
    <LegacyButton
      onClick={onEditReservations}
      text={i18n.common.edit}
      data-qa="edit"
    />
  ) : undefined

  const rightButton = absencesEditable ? (
    <LegacyButton
      primary
      text={i18n.calendar.newAbsence}
      onClick={onCreateAbsence}
      data-qa="create-absence"
    />
  ) : undefined

  const isPast = modalData.response.date.isBefore(LocalDate.todayInSystemTz())
  return (
    <DayModal
      date={modalData.response.date}
      dateActions={dateActions}
      rows={modalData.rows}
      onClose={onClose}
      leftButton={leftButton}
      rightButton={rightButton}
    >
      {(childIndex) => {
        const child = modalData.response.children[childIndex]
        return (!featureFlags.automaticFixedScheduleAbsences ||
          isPast ||
          child.reservations.length === 0) &&
          child.absence !== null ? (
          <Absence absence={child.absence} />
        ) : (
          <Reservations
            reservations={child.reservations}
            scheduleType={child.scheduleType}
            reservableTimeRange={child.reservableTimeRange}
            holidayPeriodEffect={child.holidayPeriodEffect}
          />
        )
      }}
    </DayModal>
  )
}

function Edit({
  modalData,
  onClose,
  onCancel
}: {
  modalData: ModalData
  onClose: () => void
  onCancel: () => void
}) {
  const i18n = useTranslation()

  const form = useForm(editorForm, () => initialFormState(modalData), {
    ...i18n.validationErrors,
    ...i18n.calendar.validationErrors
  })
  const formElems = useFormElems(form)

  const [showAllErrors, useShowAllErrors] = useBoolean(false)

  const leftButton = (
    <LegacyButton
      onClick={onCancel}
      text={i18n.common.cancel}
      data-qa="cancel"
    />
  )

  const rightButton = (
    <MutateButton
      primary
      mutation={postReservationsMutation}
      onClick={() => {
        if (!form.isValid()) {
          useShowAllErrors.on()
          return cancelMutation
        } else {
          return { body: form.value()(modalData.response.date) }
        }
      }}
      disabled={!form.isValid()}
      onSuccess={onCancel}
      text={i18n.common.save}
      data-qa="save"
    />
  )

  return (
    <DayModal
      date={modalData.response.date}
      dateActions={undefined}
      rows={modalData.rows}
      onClose={onClose}
      leftButton={leftButton}
      rightButton={rightButton}
    >
      {(childIndex) => {
        const bind = formElems[childIndex]
        return (
          <div>
            <EditReservation showAllErrors={showAllErrors} bind={bind} />
          </div>
        )
      }}
    </DayModal>
  )
}

interface DayModalProps {
  date: LocalDate
  dateActions: DateActions | undefined
  rows: ModalRow[]
  onClose: () => void
  leftButton: React.ReactNode | undefined
  rightButton: React.ReactNode
  children?: ((childIndex: number) => React.ReactNode) | undefined
}

const DayModal = React.memo(function DayModal({
  date,
  dateActions,
  rows,
  onClose,
  leftButton,
  rightButton,
  children: renderReservation = () => undefined
}: DayModalProps) {
  const i18n = useTranslation()
  const [lang] = useLang()

  return (
    <ModalAccessibilityWrapper>
      <PlainModal margin="auto" mobileFullScreen data-qa="calendar-dayview">
        <CalendarModalBackground>
          <BottomFooterContainer>
            <div>
              <DayHeader highlight={date.isEqual(LocalDate.todayInSystemTz())}>
                <ModalCloseButton
                  close={onClose}
                  closeLabel={i18n.common.closeModal}
                  data-qa="day-view-close-button"
                />
                <CalendarModalSection>
                  <DayPicker>
                    <IconOnlyButton
                      icon={faChevronLeft}
                      onClick={dateActions?.navigateToPreviousDate}
                      disabled={!dateActions?.navigateToPreviousDate}
                      aria-label={i18n.calendar.previousDay}
                    />
                    <ModalHeader headingComponent={DayOfWeek}>
                      {date.format('EEEEEE d.M.yyyy', lang)}
                    </ModalHeader>
                    <IconOnlyButton
                      icon={faChevronRight}
                      onClick={dateActions?.navigateToNextDate}
                      disabled={!dateActions?.navigateToNextDate}
                      aria-label={i18n.calendar.nextDay}
                    />
                  </DayPicker>
                </CalendarModalSection>
              </DayHeader>

              <Gap size="m" sizeOnMobile="s" />
              {rows.length === 0 ? (
                <CalendarModalSection data-qa="no-active-placements-msg">
                  {i18n.calendar.noActivePlacements}
                </CalendarModalSection>
              ) : (
                rows.map((row, childIndex) => (
                  <React.Fragment key={row.childId}>
                    {childIndex !== 0 ? (
                      <Gap size="zero" sizeOnMobile="s" />
                    ) : null}
                    <CalendarModalSection data-qa={`child-${row.childId}`}>
                      {childIndex !== 0 ? (
                        <HorizontalLine dashed slim hiddenOnMobile />
                      ) : null}
                      <FixedSpaceRow>
                        <FixedSpaceColumn>
                          <RoundChildImage
                            imageId={row.image.imageId}
                            fallbackText={row.image.initialLetter}
                            colorIndex={row.image.colorIndex}
                            size={48}
                          />
                        </FixedSpaceColumn>
                        <FixedSpaceColumn
                          spacing="zero"
                          justifyContent="center"
                        >
                          <H2 noMargin data-qa="child-name">
                            {`${formatFirstName(row)} ${row.lastName}`}
                          </H2>
                          {row.duplicateInfo !== undefined && (
                            <H3 noMargin> {row.duplicateInfo} </H3>
                          )}
                        </FixedSpaceColumn>
                      </FixedSpaceRow>

                      <Gap size="s" />

                      <ColoredH3 noMargin>
                        {i18n.calendar.reservationsAndRealized}
                      </ColoredH3>

                      {row.shiftCare && (
                        <ExpandingInfo
                          width="auto"
                          info={i18n.calendar.shiftCareInfo()}
                          inlineChildren
                        >
                          <Label>{i18n.calendar.shiftCareInfoLabel}</Label>
                        </ExpandingInfo>
                      )}
                      <Gap size="s" />

                      <ReservationAttendanceInfo>
                        <ReservationAttendanceHeading>
                          {i18n.calendar.reservation}
                        </ReservationAttendanceHeading>
                        {renderReservation(childIndex)}
                        <AttendanceInfo
                          attendances={row.attendances}
                          reservations={row.reservations}
                          usedService={row.usedService}
                        />
                      </ReservationAttendanceInfo>

                      {row.events.length > 0 && (
                        <>
                          <MobileOnly>
                            <HorizontalLine dashed slim />
                          </MobileOnly>
                          <Gap size="m" sizeOnMobile="zero" />
                          <ColoredH3 noMargin>{i18n.calendar.events}</ColoredH3>
                          <Gap size="s" />
                          <FixedSpaceColumn spacing="s">
                            {row.events.map((event) => {
                              if (event.eventType === 'DAYCARE_EVENT') {
                                return (
                                  <FixedSpaceColumn
                                    spacing="xxs"
                                    key={event.id}
                                    data-qa={`event-${event.id}`}
                                  >
                                    <LabelLike data-qa="event-title">
                                      {event.title} /{' '}
                                      {event.currentAttending.type === 'unit'
                                        ? event.currentAttending.unitName
                                        : event.currentAttending.type ===
                                            'group'
                                          ? event.currentAttending.groupName
                                          : row.firstName}
                                    </LabelLike>
                                    <P noMargin data-qa="event-description">
                                      {event.description}
                                    </P>
                                  </FixedSpaceColumn>
                                )
                              } else if (
                                event.eventType === 'DISCUSSION_SURVEY'
                              ) {
                                return (
                                  <SurveyContainer
                                    key={event.id}
                                    data-qa={`event-${event.id}`}
                                  >
                                    <LabelLike>
                                      <WordBreakContainer>
                                        <P noMargin data-qa="title-text">
                                          {event.title}
                                        </P>
                                      </WordBreakContainer>
                                      {event.reservedTimes.map((rt, i) => (
                                        <DiscussionReservationContainer
                                          key={`reservation-${i}`}
                                        >
                                          <div>
                                            <P
                                              noMargin
                                              data-qa={`reservation-time-${rt.id}`}
                                            >{`${i18n.calendar.discussionTimeReservation.timePreDescriptor} ${rt.startTime.format()} - ${rt.endTime.format()}`}</P>
                                          </div>
                                          {rt.date.isEqualOrAfter(
                                            LocalDate.todayInHelsinkiTz()
                                          ) && (
                                            <MutateButton
                                              appearance="inline"
                                              data-qa={`reservation-cancel-button-${rt.id}`}
                                              icon={faTimes}
                                              text={
                                                i18n.calendar
                                                  .discussionTimeReservation
                                                  .cancelTimeButtonText
                                              }
                                              disabled={
                                                !isEventTimeCancellable(
                                                  rt,
                                                  LocalDate.todayInHelsinkiTz()
                                                )
                                              }
                                              mutation={
                                                deleteCalendarEventTimeReservationMutation
                                              }
                                              onClick={() => {
                                                if (rt.childId !== null) {
                                                  return {
                                                    childId: rt.childId,
                                                    calendarEventTimeId: rt.id
                                                  }
                                                } else {
                                                  return cancelMutation
                                                }
                                              }}
                                            />
                                          )}
                                        </DiscussionReservationContainer>
                                      ))}
                                    </LabelLike>
                                    <P noMargin data-qa="event-description">
                                      {event.description}
                                    </P>
                                  </SurveyContainer>
                                )
                              } else return null
                            })}
                          </FixedSpaceColumn>
                        </>
                      )}
                    </CalendarModalSection>
                  </React.Fragment>
                ))
              )}

              <Gap size="L" sizeOnMobile="s" />
            </div>

            {rows.length > 0 && (leftButton || rightButton) ? (
              <ButtonFooter>
                {leftButton ?? <EmptyButtonFooterElement />}
                {rightButton}
              </ButtonFooter>
            ) : null}
          </BottomFooterContainer>
        </CalendarModalBackground>
      </PlainModal>
    </ModalAccessibilityWrapper>
  )
})

interface ModalData {
  response: ReservationResponseDay
  rows: ModalRow[]
  dateIndex: number
}

interface ModalRow {
  childId: string
  firstName: string
  lastName: string
  image: ChildImageData
  duplicateInfo: string | undefined
  shiftCare: boolean
  attendances: TimeInterval[]
  usedService: UsedServiceResult | null
  reservations: ReservationResponse[]
  events: ModalRowEvent[]
}

interface ModalRowEvent extends CitizenCalendarEvent {
  currentAttending: AttendingChild
  reservedTimes: CalendarEventTime[]
  eventType: CalendarEventType
}

function computeModalData(
  date: LocalDate,
  reservationsResponse: ReservationsResponse,
  events: CitizenCalendarEvent[],
  childImages: ChildImageData[],
  i18n: Translations
): ModalData | undefined {
  const index = reservationsResponse.days.findIndex((reservation) =>
    date.isEqual(reservation.date)
  )
  if (index === -1) {
    return undefined
  }

  const response = reservationsResponse.days[index]

  const duplicateChildInfo = getDuplicateChildInfo(
    reservationsResponse.children,
    i18n
  )

  const rows = response.children.flatMap((child) => {
    const person = reservationsResponse.children.find(
      (c) => c.id === child.childId
    )
    const image = childImages.find((i) => i.childId === child.childId)
    if (person === undefined || image === undefined) {
      // Should not happen
      return []
    }

    return {
      childId: child.childId,
      firstName: person.firstName,
      lastName: person.lastName,
      image,
      duplicateInfo: duplicateChildInfo[child.childId],
      shiftCare: child.shiftCare,
      attendances: child.attendances,
      reservations: child.reservations,
      usedService: child.usedService,
      events: events.flatMap((event) => {
        const currentAttending = event.attendingChildren?.[child.childId]?.find(
          ({ periods }) => periods.some((period) => period.includes(date))
        )
        const reservedTimes = (event.timesByChild[child.childId] ?? []).filter(
          (t) => t.childId === child.childId && t.date.isEqual(date)
        )
        if (event.eventType === 'DAYCARE_EVENT') {
          return currentAttending === undefined
            ? []
            : [{ ...event, currentAttending, reservedTimes }]
        } else if (event.eventType === 'DISCUSSION_SURVEY') {
          return reservedTimes.length > 0
            ? currentAttending
              ? [{ ...event, currentAttending, reservedTimes }]
              : []
            : []
        } else return []
      })
    }
  })

  return {
    response,
    rows,
    dateIndex: index
  }
}

interface DateActions {
  navigateToPreviousDate: (() => void) | undefined
  navigateToNextDate: (() => void) | undefined
}

function findAdjacentDates(
  selectDate: (date: LocalDate) => void,
  reservationsResponse: ReservationsResponse,
  todayIndex: number | undefined
): DateActions {
  if (todayIndex === undefined) {
    return { navigateToPreviousDate: undefined, navigateToNextDate: undefined }
  }
  const previousDate =
    todayIndex > 1 ? reservationsResponse.days[todayIndex - 1].date : undefined
  const nextDate =
    todayIndex < reservationsResponse.days.length - 1
      ? reservationsResponse.days[todayIndex + 1].date
      : undefined
  return {
    navigateToPreviousDate: previousDate
      ? () => selectDate(previousDate)
      : undefined,
    navigateToNextDate: nextDate ? () => selectDate(nextDate) : undefined
  }
}

function isEditable(
  responseDay: ReservationResponseDay,
  reservableRange: FiniteDateRange
): { reservationsEditable: boolean; absencesEditable: boolean } {
  const today = LocalDate.todayInSystemTz()

  const allChildrenHaveUneditableAbsence = responseDay.children.every(
    (child) => child.absence && !child.absence.editable
  )
  const reservationsEditable =
    reservableRange.includes(responseDay.date) &&
    !allChildrenHaveUneditableAbsence
  const absencesEditable =
    responseDay.date.isEqualOrAfter(today) && !allChildrenHaveUneditableAbsence

  return { reservationsEditable, absencesEditable }
}

const childForm = object({
  childId: value<UUID>(),
  day
})

function initialChildFormState(
  childId: UUID,
  day: ReservationResponseDay
): StateOf<typeof childForm> {
  return {
    childId,
    day: resetDay([day], [childId])
  }
}

const editorForm = mapped(
  array(childForm),
  // The output value is a function that creates an array DailyReservationRequest given the date
  (output) =>
    (date: LocalDate): DailyReservationRequest[] =>
      output.flatMap(
        ({ childId, day }) =>
          toDailyReservationRequest(childId, date, day) ?? []
      )
)

function initialFormState(day: ModalData): StateOf<typeof editorForm> {
  return day.response.children.map((child) =>
    initialChildFormState(child.childId, day.response)
  )
}

const EditReservation = React.memo(function EditReservation({
  bind,
  showAllErrors
}: {
  bind: BoundForm<typeof childForm>
  showAllErrors: boolean
}) {
  const day = useFormField(bind, 'day')
  return (
    <Day
      bind={day}
      label={undefined}
      showAllErrors={showAllErrors}
      dataQaPrefix="edit-reservation"
    />
  )
})

const Absence = React.memo(function Absence({
  absence
}: {
  absence: AbsenceInfo
}) {
  const i18n = useTranslation()
  const [open, setOpen] = useState(false)
  const onClick = useCallback(() => setOpen((prev) => !prev), [])

  if (absence.editable) {
    return (
      <span data-qa="absence">
        {absence.type === 'SICKLEAVE'
          ? i18n.calendar.absences.SICKLEAVE
          : featureFlags.citizenAttendanceSummary &&
              absence.type === 'PLANNED_ABSENCE'
            ? i18n.calendar.absences.PLANNED_ABSENCE
            : i18n.calendar.absent}
      </span>
    )
  }
  return (
    <>
      <FixedSpaceRow data-qa="absence">
        <FixedSpaceColumn>
          {i18n.calendar.absenceMarkedByEmployee}
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <InfoButton
            onClick={onClick}
            aria-label={i18n.common.openExpandingInfo}
            open={open}
          />
        </FixedSpaceColumn>
      </FixedSpaceRow>
      {open && (
        <ReservationAttendanceSection>
          <ExpandingInfoBox
            width="auto"
            info={i18n.calendar.contactStaffToEditAbsence}
            close={onClick}
          />
        </ReservationAttendanceSection>
      )}
    </>
  )
})

const Reservations = React.memo(function Reservations({
  reservations,
  scheduleType,
  reservableTimeRange,
  holidayPeriodEffect
}: {
  reservations: Reservation[]
  scheduleType: ScheduleType
  reservableTimeRange: ReservableTimeRange
  holidayPeriodEffect: HolidayPeriodEffect | null
}) {
  const i18n = useTranslation()

  const [withTimes, withoutTimes] = useMemo(
    () => partition(reservations, reservationHasTimes),

    [reservations]
  )

  return holidayPeriodEffect?.type === 'NotYetReservable' ? (
    <ExpandingInfo
      width="full"
      info={i18n.calendar.notYetReservableInfo(
        holidayPeriodEffect.period,
        holidayPeriodEffect.reservationsOpenOn
      )}
    >
      {i18n.calendar.notYetReservable}
    </ExpandingInfo>
  ) : withoutTimes.length > 0 ? (
    // In theory, we could have reservations with and without times, but this shouldn't happen in practice
    <ReservationStatus data-qa="reservations-no-times">
      {i18n.calendar.present}
    </ReservationStatus>
  ) : withTimes.length > 0 ? (
    <div data-qa="reservations">
      {withTimes.map((reservation, i, array) => (
        <React.Fragment key={`res-${i}`}>
          {formatReservation(reservation, reservableTimeRange, i18n)}
          {i < array.length - 1 && ', '}
        </React.Fragment>
      ))}
    </div>
  ) : (
    mapScheduleType(scheduleType, {
      RESERVATION_REQUIRED: () => (
        <ReservationStatus data-qa="no-reservations">
          {i18n.calendar.missingReservation}
        </ReservationStatus>
      ),
      FIXED_SCHEDULE: () => (
        <span data-qa="reservation-not-required">
          {i18n.calendar.reservationNotRequired}
        </span>
      ),
      TERM_BREAK: () => (
        <span data-qa="term-break">{i18n.calendar.termBreak}</span>
      )
    })
  )
})

const DayHeader = styled.div<{ highlight: boolean }>`
  position: sticky;
  z-index: 100;
  border-bottom: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  text-transform: capitalize;
  padding: ${defaultMargins.m} 0;
  top: 0;
  background-color: ${(p) => p.theme.colors.grayscale.g0};

  @media (max-width: ${tabletMin}) {
    padding: 0;
    border-bottom: none;
  }

  &::after {
    content: '';
    background-color: ${(p) =>
      p.highlight ? p.theme.colors.status.success : 'transparent'};
    width: 4px;
    left: 0;
    top: 0;
    bottom: 0;
    position: absolute;
  }
`

const ColoredH3 = styled(H3)`
  @media (max-width: ${tabletMin}) {
    color: ${(p) => p.theme.colors.main.m1};
  }
`

const DayPicker = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
`

const DayOfWeek = styled(H1)`
  margin: 0 ${defaultMargins.s};
  text-align: center;
`

const ReservationStatus = styled.span`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
`

const ButtonFooter = styled.div`
  display: flex;
  justify-content: space-between;
  padding: 0 ${defaultMargins.L} ${defaultMargins.L} ${defaultMargins.L};

  @media (max-width: ${tabletMin}) {
    margin-top: 0;
    padding: ${defaultMargins.s};
    display: grid;
    grid-auto-columns: 1fr 1fr;
    gap: ${defaultMargins.s};
    bottom: 0;
    left: 0;
    right: 0;
    box-shadow: 0px -2px 4px rgba(0, 0, 0, 0.15);
    background-color: ${(p) => p.theme.colors.grayscale.g0};

    & > * {
      grid-row: 1;
    }

    // Fit more text to the buttons
    & > ${StyledButton} {
      padding: 0;
      width: 100%;
    }
  }
`

const EmptyButtonFooterElement = styled.div`
  @media (max-width: ${tabletMin}) {
    display: none;
  }
`

const ReservationAttendanceInfo = styled.div`
  @media not all and (max-width: ${tabletMin}) {
    display: grid;
    grid-template-columns: 45% 55%;
    grid-auto-rows: minmax(38px, max-content);
    row-gap: ${defaultMargins.xxs};
  }

  @media (max-width: ${tabletMin}) {
    margin-left: ${defaultMargins.xs};
  }
`

export const ReservationAttendanceSection = styled.div`
  grid-column: 1 / span 2;
`

export const ReservationAttendanceHeading = styled(LabelLike)`
  @media (max-width: ${tabletMin}) {
    margin: ${defaultMargins.xs} 0px ${defaultMargins.xs} 0px;
  }
`
const SurveyContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: start;
  margin: 10px 0;
`

const DiscussionReservationContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: start;
  margin: 10px 0;
`

export const WordBreakContainer = styled.div`
  word-break: break-word;
`
