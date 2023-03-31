// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import partition from 'lodash/partition'
import zip from 'lodash/zip'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import { getDuplicateChildInfo } from 'citizen-frontend/utils/duplicated-child-utils'
import FiniteDateRange from 'lib-common/finite-date-range'
import { boolean, localTimeRange } from 'lib-common/form/fields'
import { array, mapped, object, value } from 'lib-common/form/form'
import {
  BoundForm,
  useBoolean,
  useForm,
  useFormElem,
  useFormElems,
  useFormField
} from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import {
  AttendingChild,
  CitizenCalendarEvent
} from 'lib-common/generated/api-types/calendarevent'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import {
  DailyReservationRequest,
  OpenTimeRange,
  Reservation,
  ReservationChild,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatFirstName } from 'lib-common/names'
import { useMutation } from 'lib-common/query'
import {
  reservationHasTimes,
  reservationsAndAttendancesDiffer
} from 'lib-common/reservations'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Button, { StyledButton } from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
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
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import {
  ModalCloseButton,
  ModalHeader,
  PlainModal
} from 'lib-components/molecules/modals/BaseModal'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H1, H2, H3, LabelLike, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faChevronLeft, faChevronRight, faPlus, faTrash } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { useLang, useTranslation } from '../localization'

import { BottomFooterContainer } from './BottomFooterContainer'
import { CalendarModalBackground, CalendarModalSection } from './CalendarModal'
import { RoundChildImage } from './RoundChildImages'
import TimeRangeInput from './TimeRangeInput'
import { postReservationsMutation } from './queries'
import { isDayReservableForSomeone } from './utils'

interface Props {
  date: LocalDate
  reservationsResponse: ReservationsResponse
  selectDate: (date: LocalDate) => void
  close: () => void
  openAbsenceModal: (initialDate: LocalDate) => void
  events: CitizenCalendarEvent[]
}

interface ChildWithReservations {
  child: ReservationChild
  absence: AbsenceType | undefined
  reservations: Reservation[]
  attendances: OpenTimeRange[]
  reservationEditable: boolean
  markedByEmployee: boolean
}

function getChildrenWithReservations(
  date: LocalDate,
  reservationsResponse: ReservationsResponse
): ChildWithReservations[] {
  const dailyData = reservationsResponse.dailyData.find((day) =>
    date.isEqual(day.date)
  )
  if (!dailyData) return []

  return reservationsResponse.children
    .filter((child) =>
      child.placements.some((pl) => date.isBetween(pl.start, pl.end))
    )
    .filter((child) =>
      child.maxOperationalDays.includes(date.getIsoDayOfWeek())
    )
    .filter(
      (child) => !dailyData.isHoliday || child.maxOperationalDays.length == 7
    )
    .map((child) => {
      const childReservations = dailyData?.children.find(
        ({ childId }) => childId === child.id
      )

      const markedByEmployee = childReservations?.markedByEmployee ?? false

      const reservationEditable =
        !markedByEmployee &&
        (!dailyData.isHoliday || child.inShiftCareUnit) &&
        child.maxOperationalDays.includes(date.getIsoDayOfWeek())

      return {
        child,
        absence: childReservations?.absence ?? undefined,
        reservations: childReservations?.reservations ?? [],
        attendances: childReservations?.attendances ?? [],
        reservationEditable,
        markedByEmployee
      }
    })
}

function getSurroundingDates(
  date: LocalDate,
  reservationsResponse: ReservationsResponse
) {
  const dateIndexInData = reservationsResponse.dailyData.findIndex(
    (reservation) => date.isEqual(reservation.date)
  )
  return [
    reservationsResponse.dailyData[dateIndexInData - 1]?.date,
    reservationsResponse.dailyData[dateIndexInData + 1]?.date
  ]
}

export default React.memo(function DayView({
  date,
  reservationsResponse,
  selectDate,
  close,
  openAbsenceModal,
  events
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const childrenWithReservations = useMemo(
    () => getChildrenWithReservations(date, reservationsResponse),
    [date, reservationsResponse]
  )

  const [previousDate, nextDate] = useMemo(
    () => getSurroundingDates(date, reservationsResponse),
    [date, reservationsResponse]
  )

  const {
    editable,
    absenceEditable,
    editing,
    startEditing,
    editorState,
    saving,
    save,
    navigate,
    confirmationModal,
    stopEditing
  } = useEditState(
    date,
    childrenWithReservations,
    reservationsResponse.reservableDays
  )

  const navigateToPrevDate = useCallback(
    () => selectDate(previousDate),
    [selectDate, previousDate]
  )
  const navigateToNextDate = useCallback(
    () => selectDate(nextDate),
    [selectDate, nextDate]
  )
  const onCreateAbsence = useCallback(
    () => openAbsenceModal(date),
    [openAbsenceModal, date]
  )

  const childStates = useFormElems(editorState)

  const duplicateChildInfo = getDuplicateChildInfo(
    childrenWithReservations.map((reservation) => reservation.child),
    i18n
  )

  return (
    <ModalAccessibilityWrapper>
      <PlainModal margin="auto" mobileFullScreen data-qa="calendar-dayview">
        <CalendarModalBackground>
          <BottomFooterContainer>
            <div>
              <DayHeader highlight={date.isEqual(LocalDate.todayInSystemTz())}>
                <ModalCloseButton
                  close={navigate(close)}
                  closeLabel={i18n.common.closeModal}
                  data-qa="day-view-close-button"
                />
                <CalendarModalSection>
                  <DayPicker>
                    <IconButton
                      icon={faChevronLeft}
                      onClick={navigateToPrevDate}
                      disabled={!previousDate}
                      aria-label={i18n.calendar.previousDay}
                    />
                    <ModalHeader headingComponent={DayOfWeek}>
                      {date.format('EEEEEE d.M.yyyy', lang)}
                    </ModalHeader>
                    <IconButton
                      icon={faChevronRight}
                      onClick={navigateToNextDate}
                      disabled={!nextDate}
                      aria-label={i18n.calendar.nextDay}
                    />
                  </DayPicker>
                </CalendarModalSection>
              </DayHeader>

              <Gap size="m" sizeOnMobile="s" />

              {childrenWithReservations.length === 0 ? (
                <CalendarModalSection data-qa="no-active-placements-msg">
                  {i18n.calendar.noActivePlacements}
                </CalendarModalSection>
              ) : (
                <>
                  {zip(childrenWithReservations, childStates).map(
                    ([childWithReservation, childState], childIndex) => {
                      if (!childWithReservation || !childState) return null

                      const {
                        child,
                        absence,
                        reservations,
                        attendances,
                        reservationEditable,
                        markedByEmployee
                      } = childWithReservation

                      const showAttendanceWarning =
                        !editing &&
                        reservationsAndAttendancesDiffer(
                          reservations,
                          attendances
                        )

                      const childEvents = events
                        .map((event) => ({
                          ...event,
                          currentAttending: event.attendingChildren?.[
                            child.id
                          ]?.find(({ periods }) =>
                            periods.some((period) => period.includes(date))
                          )
                        }))
                        .filter(
                          (
                            event
                          ): event is CitizenCalendarEvent & {
                            currentAttending: AttendingChild
                          } => !!event.currentAttending
                        )

                      return (
                        <React.Fragment key={child.id}>
                          {childIndex !== 0 ? (
                            <Gap size="zero" sizeOnMobile="s" />
                          ) : null}
                          <CalendarModalSection data-qa={`child-${child.id}`}>
                            {childIndex !== 0 ? (
                              <HorizontalLine dashed slim hiddenOnMobile />
                            ) : null}
                            <FixedSpaceRow>
                              <FixedSpaceColumn>
                                <RoundChildImage
                                  imageId={child.imageId}
                                  fallbackText={
                                    (formatFirstName(child) || '?')[0]
                                  }
                                  colorIndex={childIndex}
                                  size={48}
                                />
                              </FixedSpaceColumn>
                              <FixedSpaceColumn
                                spacing="zero"
                                justifyContent="center"
                              >
                                <H2 noMargin data-qa="child-name">
                                  {`${formatFirstName(child)} ${
                                    child.lastName
                                  }`}
                                </H2>
                                {duplicateChildInfo[child.id] !== undefined && (
                                  <H3 noMargin>
                                    {duplicateChildInfo[child.id]}
                                  </H3>
                                )}
                              </FixedSpaceColumn>
                            </FixedSpaceRow>

                            <Gap size="s" />

                            <ColoredH3 noMargin>
                              {i18n.calendar.reservationsAndRealized}
                            </ColoredH3>

                            <Gap size="s" />

                            <ReservationTable>
                              <LabelLike>{i18n.calendar.reservation}</LabelLike>
                              <div>
                                {editing &&
                                (reservationEditable || reservations.length) ? (
                                  <EditReservation
                                    canAddSecondReservation={
                                      !reservations[1] && child.inShiftCareUnit
                                    }
                                    childState={childState}
                                  />
                                ) : absence ? (
                                  <Absence
                                    absence={absence}
                                    markedByEmployee={markedByEmployee}
                                  />
                                ) : (
                                  <Reservations reservations={reservations} />
                                )}
                              </div>
                              <LabelLike>{i18n.calendar.realized}</LabelLike>
                              <div>
                                {attendances.length > 0
                                  ? attendances.map(
                                      ({ startTime, endTime }) => {
                                        const start = startTime.format()
                                        const end = endTime?.format() ?? ''
                                        return (
                                          <div key={`${start}-${end}`}>
                                            {start} – {end}
                                          </div>
                                        )
                                      }
                                    )
                                  : '–'}
                              </div>
                            </ReservationTable>
                            {showAttendanceWarning && (
                              <AlertBox
                                message={i18n.calendar.attendanceWarning}
                                wide
                              />
                            )}

                            {childEvents.length > 0 && (
                              <>
                                <MobileOnly>
                                  <HorizontalLine dashed slim />
                                </MobileOnly>
                                <Gap size="m" sizeOnMobile="zero" />
                                <ColoredH3 noMargin>
                                  {i18n.calendar.events}
                                </ColoredH3>
                                <Gap size="s" />
                                <FixedSpaceColumn spacing="s">
                                  {childEvents.map((event) => (
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
                                          : child.firstName}
                                      </LabelLike>
                                      <P noMargin data-qa="event-description">
                                        {event.description}
                                      </P>
                                    </FixedSpaceColumn>
                                  ))}
                                </FixedSpaceColumn>
                              </>
                            )}
                          </CalendarModalSection>
                        </React.Fragment>
                      )
                    }
                  )}
                </>
              )}

              {confirmationModal ? (
                <InfoModal
                  title={i18n.common.saveConfirmation}
                  close={confirmationModal.close}
                  resolve={{
                    action: confirmationModal.resolve,
                    label: i18n.common.save
                  }}
                  reject={{
                    action: confirmationModal.reject,
                    label: i18n.common.discard
                  }}
                />
              ) : null}

              <Gap size="L" sizeOnMobile="s" />
            </div>

            {childrenWithReservations.length > 0 && (
              <ButtonFooter>
                {editable ? (
                  editing ? (
                    <Button
                      disabled={saving}
                      onClick={stopEditing}
                      text={i18n.common.cancel}
                      data-qa="cancel"
                    />
                  ) : (
                    <Button
                      onClick={startEditing}
                      text={i18n.common.edit}
                      data-qa="edit"
                    />
                  )
                ) : (
                  <EmptyButtonFooterElement />
                )}
                {editing ? (
                  <Button
                    primary
                    disabled={saving || !editorState.isValid()}
                    onClick={save}
                    text={i18n.common.save}
                    data-qa="save"
                  />
                ) : (
                  <Button
                    primary
                    text={i18n.calendar.newAbsence}
                    onClick={onCreateAbsence}
                    disabled={!absenceEditable}
                    data-qa="create-absence"
                  />
                )}
              </ButtonFooter>
            )}
          </BottomFooterContainer>
        </CalendarModalBackground>
      </PlainModal>
    </ModalAccessibilityWrapper>
  )
})

const childForm = object({
  child: value<ReservationChild>(),
  reservations: array(localTimeRange),
  absent: boolean()
})

function childFormState({
  child,
  reservations,
  absence
}: ChildWithReservations): StateOf<typeof childForm> {
  const withTimes = reservations.filter(reservationHasTimes)
  return {
    child,
    reservations:
      withTimes.length > 0
        ? withTimes.map((reservation) => ({
            startTime: reservation.startTime.format(),
            endTime: reservation.endTime.format()
          }))
        : [{ startTime: '', endTime: '' }],
    absent: absence !== undefined
  }
}

const editorForm = mapped(
  array(childForm),
  // The output value is a function that creates an array DailyReservationRequest given the date
  (output) =>
    (date: LocalDate): DailyReservationRequest[] =>
      output.map(({ child, reservations, absent }) => ({
        childId: child.id,
        date,
        reservations: reservations.flatMap((reservation) =>
          reservation
            ? [
                {
                  type: 'TIMES',
                  startTime: reservation.startTime,
                  endTime: reservation.endTime
                }
              ]
            : []
        ),
        absent
      }))
)

function editorFormState(
  childrenWithReservations: ChildWithReservations[]
): StateOf<typeof editorForm> {
  return childrenWithReservations.map(childFormState)
}

function useEditState(
  date: LocalDate,
  childrenWithReservations: ChildWithReservations[],
  reservableDays: Record<string, FiniteDateRange[]>
) {
  const t = useTranslation()
  const today = LocalDate.todayInSystemTz()

  const anyChildReservable = useMemo(
    () =>
      childrenWithReservations.some(
        ({ reservationEditable }) => reservationEditable
      ),
    [childrenWithReservations]
  )

  const editable = useMemo(
    () => anyChildReservable && isDayReservableForSomeone(date, reservableDays),
    [anyChildReservable, reservableDays, date]
  )

  const absenceEditable = useMemo(
    () => anyChildReservable && date.isEqualOrAfter(today),
    [anyChildReservable, date, today]
  )

  const [editing, { on: startEditing, off: stopEditing }] = useBoolean(false)

  const initialEditorState = useMemo(
    () => editorFormState(childrenWithReservations),
    [childrenWithReservations]
  )

  const editorState = useForm(
    editorForm,
    () => initialEditorState,
    t.validationErrors
  )

  // TODO: Reset component state properly by unmount+mount
  const set = editorState.set
  useEffect(() => set(initialEditorState), [initialEditorState, set])

  const stateIsValid = editorState.isValid()

  const { mutateAsync: postReservations, isLoading: saving } = useMutation(
    postReservationsMutation
  )

  const save = useCallback(() => {
    const request: DailyReservationRequest[] = editorState.value()(date)
    return postReservations(request).then(() => stopEditing())
  }, [date, editorState, postReservations, stopEditing])

  const [confirmationModal, setConfirmationModal] = useState<{
    close: () => void
    resolve: () => void
    reject: () => void
  }>()

  const navigate = useCallback(
    (callback: () => void) => () => {
      if (!editing) return callback()

      const stateHasBeenModified = !zip(
        initialEditorState,
        editorState.state
      ).every(
        ([initial, current]) =>
          initial &&
          current &&
          reservationsEqual(initial.reservations, current.reservations)
      )
      if (!stateHasBeenModified) return callback()

      setConfirmationModal({
        close: () => setConfirmationModal(undefined),
        resolve: () => {
          setConfirmationModal(undefined)
          void save().then(() => callback())
        },
        reject: () => {
          stopEditing()
          setConfirmationModal(undefined)
          callback()
        }
      })
    },
    [editing, editorState.state, initialEditorState, save, stopEditing]
  )

  return {
    editable,
    absenceEditable,
    editing,
    startEditing,
    editorState,
    stateIsValid,
    saving,
    save,
    navigate,
    confirmationModal,
    stopEditing
  }
}

function reservationsEqual(
  values1: { startTime: string; endTime: string }[],
  values2: { startTime: string; endTime: string }[]
) {
  return zip(values1, values2).every(([value1, value2]) => {
    if (!value1 || !value2) return false
    return value1.startTime === value2.startTime
  })
}

const EditReservation = React.memo(function EditReservation({
  canAddSecondReservation,
  childState
}: {
  canAddSecondReservation: boolean
  childState: BoundForm<typeof childForm>
}) {
  const t = useTranslation()

  const reservations = useFormField(childState, 'reservations')
  const updateReservations = reservations.update

  const addSecondReservation = useCallback(() => {
    updateReservations((prev) => [...prev, { startTime: '', endTime: '' }])
  }, [updateReservations])

  const removeSecondReservation = useCallback(() => {
    updateReservations((prev) => prev.slice(0, 1))
  }, [updateReservations])

  const firstReservation = useFormElem(reservations, 0)
  const secondReservation = useFormElem(reservations, 1)

  if (firstReservation === undefined) {
    throw new Error('BUG: At least one reservation expected')
  }

  return (
    <FixedSpaceColumn>
      <FixedSpaceRow alignItems="center">
        <TimeRangeInput bind={firstReservation} data-qa="first-reservation" />
        {canAddSecondReservation && (
          <IconButton
            icon={faPlus}
            onClick={addSecondReservation}
            aria-label={t.common.add}
          />
        )}
      </FixedSpaceRow>
      {secondReservation !== undefined ? (
        <FixedSpaceRow alignItems="center">
          <TimeRangeInput
            bind={secondReservation}
            data-qa="second-reservation"
          />
          <IconButton
            icon={faTrash}
            onClick={removeSecondReservation}
            aria-label={t.common.delete}
          />
        </FixedSpaceRow>
      ) : null}
    </FixedSpaceColumn>
  )
})

const Absence = React.memo(function Absence({
  absence,
  markedByEmployee
}: {
  absence: AbsenceType
  markedByEmployee: boolean
}) {
  const i18n = useTranslation()
  const [open, setOpen] = useState(false)
  const onClick = useCallback(() => setOpen((prev) => !prev), [])

  if (!markedByEmployee) {
    return (
      <span data-qa="absence">
        {absence === 'SICKLEAVE'
          ? i18n.calendar.absences.SICKLEAVE
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
        <Colspan2>
          <ExpandingInfoBox
            width="auto"
            info={i18n.calendar.contactStaffToEditAbsence}
            close={onClick}
            closeLabel={i18n.common.close}
          />
        </Colspan2>
      )}
    </>
  )
})

const Reservations = React.memo(function Reservations({
  reservations
}: {
  reservations: Reservation[]
}) {
  const i18n = useTranslation()

  const [withTimes, withoutTimes] = useMemo(
    () => partition(reservations, reservationHasTimes),

    [reservations]
  )

  return withoutTimes.length > 0 ? (
    // In theory, we could have reservations with and without times, but this shouldn't happen in practice
    <ReservationStatus data-qa="reservations-no-times">
      {i18n.calendar.reservationNoTimes}
    </ReservationStatus>
  ) : withTimes.length > 0 ? (
    <span data-qa="reservations">
      {withTimes
        .map(
          ({ startTime, endTime }) =>
            `${startTime.format()} – ${endTime.format()}`
        )
        .join(', ')}
    </span>
  ) : (
    <ReservationStatus data-qa="no-reservations">
      {i18n.calendar.noReservation}
    </ReservationStatus>
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

const Colspan2 = styled.div`
  grid-column: 1 / span 2;
`

const ReservationStatus = styled.span`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
`

const ButtonFooter = styled.div`
  display: flex;
  justify-content: space-between;
  padding: ${defaultMargins.L};
  padding-top: 0;

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
    }
  }
`

const EmptyButtonFooterElement = styled.div`
  @media (max-width: ${tabletMin}) {
    display: none;
  }
`

const ReservationTable = styled.div`
  display: grid;
  grid-template-rows: minmax(38px, max-content) minmax(38px, max-content);
  grid-template-columns: 42% 58%;
  row-gap: ${defaultMargins.xxs};
`
