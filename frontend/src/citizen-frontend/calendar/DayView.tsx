// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import zip from 'lodash/zip'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AttendingChild,
  CitizenCalendarEvent
} from 'lib-common/generated/api-types/calendarevent'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import {
  OpenTimeRange,
  ReservationChild,
  ReservationsResponse,
  TimeRange
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatPreferredName } from 'lib-common/names'
import { useMutation } from 'lib-common/query'
import {
  reservationsAndAttendancesDiffer,
  validateTimeRange
} from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Button from 'lib-components/atoms/buttons/Button'
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
import TimeRangeInput, { TimeRangeWithErrors } from './TimeRangeInput'
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
  reservations: TimeRange[]
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
    editorStateSetter,
    editorAbsenceSetter,
    addSecondReservation,
    removeSecondReservation,
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
                  {zip(childrenWithReservations, editorState).map(
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
                                  initialLetter={
                                    (child.preferredName ||
                                      child.firstName ||
                                      '?')[0]
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
                                  {`${formatPreferredName(child)} ${
                                    child.lastName
                                  }`}
                                </H2>
                              </FixedSpaceColumn>
                            </FixedSpaceRow>

                            <Gap size="s" />

                            <ColoredH3 noMargin>
                              {i18n.calendar.reservationsAndRealized}
                            </ColoredH3>

                            <Gap size="s" />

                            <ReservationTable>
                              <thead>
                                <tr>
                                  <th>
                                    <LabelLike>
                                      {i18n.calendar.reservation}
                                    </LabelLike>
                                  </th>
                                  <th>
                                    <LabelLike>
                                      {i18n.calendar.realized}
                                    </LabelLike>
                                  </th>
                                </tr>
                              </thead>
                              <tbody>
                                <tr>
                                  <td>
                                    {editing &&
                                    (reservationEditable ||
                                      reservations.length) ? (
                                      <EditReservation
                                        childId={child.id}
                                        canAddSecondReservation={
                                          !reservations[1] &&
                                          child.inShiftCareUnit
                                        }
                                        childState={childState}
                                        editorStateSetter={editorStateSetter}
                                        editorAbsenceSetter={
                                          editorAbsenceSetter
                                        }
                                        addSecondReservation={
                                          addSecondReservation
                                        }
                                        removeSecondReservation={
                                          removeSecondReservation
                                        }
                                      />
                                    ) : absence ? (
                                      <Absence
                                        absence={absence}
                                        markedByEmployee={markedByEmployee}
                                      />
                                    ) : (
                                      <Reservations
                                        reservations={reservations}
                                      />
                                    )}
                                  </td>
                                  <td>
                                    {attendances.length > 0
                                      ? attendances.map(
                                          ({ startTime, endTime }) => (
                                            <div
                                              key={JSON.stringify([
                                                startTime,
                                                endTime
                                              ])}
                                            >
                                              {startTime} – {endTime ?? ''}
                                            </div>
                                          )
                                        )
                                      : '–'}
                                  </td>
                                </tr>
                              </tbody>
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
                    disabled={saving}
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

type EditorState = {
  child: ReservationChild
  reservations: TimeRangeWithErrors[]
  absent: boolean
}

const emptyReservation: TimeRangeWithErrors = {
  startTime: '',
  endTime: '',
  errors: { startTime: undefined, endTime: undefined }
}

function useEditState(
  date: LocalDate,
  childrenWithReservations: ChildWithReservations[],
  reservableDays: Record<string, FiniteDateRange[]>
) {
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

  const [editing, setEditing] = useState(false)
  const startEditing = useCallback(() => setEditing(true), [])
  const stopEditing = useCallback(() => setEditing(false), [])

  const initialEditorState: EditorState[] = useMemo(
    () =>
      childrenWithReservations.map(({ child, reservations, absence }) => ({
        child,
        reservations:
          reservations.length > 0
            ? reservations.map((reservation) => ({
                ...reservation,
                errors: { startTime: undefined, endTime: undefined }
              }))
            : [emptyReservation],
        absent: !!absence
      })),
    [childrenWithReservations]
  )
  const [editorState, setEditorState] = useState(initialEditorState)
  useEffect(() => setEditorState(initialEditorState), [initialEditorState])

  const editorStateSetter = useCallback(
    (childId: UUID, index: number, field: 'startTime' | 'endTime') =>
      (value: string) =>
        setEditorState((state) =>
          state.map((childState) => {
            if (childState.child.id !== childId) return childState

            const reservations = childState.reservations.map((timeRange, i) =>
              index === i
                ? addTimeRangeValidationErrors({
                    ...timeRange,
                    [field]: value
                  })
                : timeRange
            )

            return {
              ...childState,
              reservations,
              absent: childrenWithReservations.find(
                (child) => child.child.id === childId
              )?.absence
                ? !reservations.every(
                    (reservation) =>
                      reservation.errors.startTime === undefined &&
                      reservation.errors.endTime === undefined &&
                      reservation.startTime.length > 0 &&
                      reservation.endTime.length > 0
                  )
                : false
            }
          })
        ),
    [childrenWithReservations]
  )

  const editorAbsenceSetter = useCallback(
    (childId: UUID, value: boolean) =>
      setEditorState((state) =>
        state.map((childState) =>
          childState.child.id === childId
            ? {
                ...childState,
                absent: value
              }
            : childState
        )
      ),
    []
  )

  const addSecondReservation = useCallback(
    (childId: UUID) =>
      setEditorState((state) =>
        state.map(
          (childState): EditorState =>
            childState.child.id === childId
              ? {
                  ...childState,
                  reservations: [childState.reservations[0], emptyReservation]
                }
              : childState
        )
      ),
    []
  )

  const removeSecondReservation = useCallback(
    (childId: UUID) =>
      setEditorState((state) =>
        state.map(
          (childState): EditorState =>
            childState.child.id === childId
              ? {
                  ...childState,
                  reservations: [childState.reservations[0]]
                }
              : childState
        )
      ),
    []
  )

  const stateIsValid = useMemo(
    () =>
      editorState.every(({ reservations }) =>
        reservations.every(
          ({ errors }) =>
            errors.startTime === undefined && errors.endTime === undefined
        )
      ),
    [editorState]
  )

  const { mutateAsync: postReservations, isLoading: saving } = useMutation(
    postReservationsMutation
  )

  const save = useCallback(() => {
    if (!stateIsValid) return Promise.resolve()
    return postReservations(
      editorState.map(({ child, reservations, absent }) => ({
        childId: child.id,
        date,
        reservations:
          reservations.flatMap(({ startTime, endTime }) =>
            startTime !== '' && endTime !== '' ? [{ startTime, endTime }] : []
          ) ?? [],
        absent
      }))
    ).then(() => setEditing(false))
  }, [date, editorState, postReservations, stateIsValid])

  const [confirmationModal, setConfirmationModal] = useState<{
    close: () => void
    resolve: () => void
    reject: () => void
  }>()

  const navigate = useCallback(
    (callback: () => void) => () => {
      const stateHasBeenModified = zip(initialEditorState, editorState).some(
        ([initial, current]) =>
          initial &&
          current &&
          zip(initial.reservations, current.reservations).some(
            ([initialReservation, currentReservation]) =>
              initialReservation &&
              currentReservation &&
              (initialReservation.startTime !== currentReservation.startTime ||
                initialReservation.endTime !== currentReservation.endTime)
          )
      )
      if (!editing || !stateHasBeenModified) return callback()

      setConfirmationModal({
        close: () => setConfirmationModal(undefined),
        resolve: () => {
          setConfirmationModal(undefined)
          void save().then(() => callback())
        },
        reject: () => {
          setEditing(false)
          setConfirmationModal(undefined)
          callback()
        }
      })
    },
    [editing, editorState, initialEditorState, save]
  )

  return {
    editable,
    absenceEditable,
    editing,
    startEditing,
    editorState,
    editorStateSetter,
    editorAbsenceSetter,
    addSecondReservation,
    removeSecondReservation,
    stateIsValid,
    saving,
    save,
    navigate,
    confirmationModal,
    stopEditing
  }
}

const EditReservation = React.memo(function EditReservation({
  childId,
  canAddSecondReservation,
  childState,
  editorStateSetter,
  addSecondReservation,
  removeSecondReservation
}: {
  childId: UUID
  canAddSecondReservation: boolean
  childState: EditorState
  editorStateSetter: (
    childId: UUID,
    index: number,
    field: 'startTime' | 'endTime'
  ) => (value: string) => void
  editorAbsenceSetter: (childId: UUID, value: boolean) => void
  addSecondReservation: (childId: UUID) => void
  removeSecondReservation: (childId: UUID) => void
}) {
  const onChangeFirst = useCallback(
    (field: 'startTime' | 'endTime') => editorStateSetter(childId, 0, field),
    [editorStateSetter, childId]
  )
  const onChangeSecond = useCallback(
    (field: 'startTime' | 'endTime') => editorStateSetter(childId, 1, field),
    [editorStateSetter, childId]
  )

  const t = useTranslation()

  return (
    <FixedSpaceColumn>
      <FixedSpaceRow alignItems="center">
        <TimeRangeInput
          value={childState.reservations[0]}
          onChange={onChangeFirst}
          data-qa="first-reservation"
        />
        {canAddSecondReservation && (
          <IconButton
            icon={faPlus}
            onClick={() => addSecondReservation(childId)}
            aria-label={t.common.add}
          />
        )}
      </FixedSpaceRow>
      {!!childState.reservations[1] && (
        <FixedSpaceRow alignItems="center">
          <TimeRangeInput
            value={childState.reservations[1]}
            onChange={onChangeSecond}
            data-qa="second-reservation"
          />
          <IconButton
            icon={faTrash}
            onClick={() => removeSecondReservation(childId)}
            aria-label={t.common.delete}
          />
        </FixedSpaceRow>
      )}
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
  reservations: TimeRange[]
}) {
  const i18n = useTranslation()

  const hasReservations = useMemo(
    () =>
      reservations.length > 0 &&
      reservations.some(({ startTime }) => !!startTime),
    [reservations]
  )

  return hasReservations ? (
    <span data-qa="reservations">
      {reservations
        .map(({ startTime, endTime }) => `${startTime} – ${endTime}`)
        .join(', ')}
    </span>
  ) : (
    <NoReservation data-qa="no-reservations">
      {i18n.calendar.noReservation}
    </NoReservation>
  )
})

export function addTimeRangeValidationErrors(
  timeRange: TimeRangeWithErrors
): TimeRangeWithErrors {
  return {
    startTime: timeRange.startTime,
    endTime: timeRange.endTime,
    errors: validateTimeRange(timeRange)
  }
}

const DayHeader = styled.div<{ highlight: boolean }>`
  position: sticky;
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

const NoReservation = styled.span`
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
  }
`

const EmptyButtonFooterElement = styled.div`
  @media (max-width: ${tabletMin}) {
    display: none;
  }
`

const ReservationTable = styled.table`
  border-collapse: collapse;
  width: 100%;

  * {
    text-align: left;
  }

  th,
  td {
    width: 50%;
  }
`
