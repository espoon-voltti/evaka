// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import zip from 'lodash/zip'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import {
  OpenTimeRange,
  ReservationChild,
  ReservationsResponse,
  TimeRange
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatPreferredName } from 'lib-common/names'
import {
  reservationsAndAttendancesDiffer,
  validateTimeRange
} from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { tabletMin } from 'lib-components/breakpoints'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import {
  ModalCloseButton,
  PlainModal
} from 'lib-components/molecules/modals/BaseModal'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { fontWeights, H1, H2, H3, LabelLike } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import {
  faChevronLeft,
  faChevronRight,
  faPlus,
  fasUserMinus,
  faTrash,
  faUserMinus
} from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { useLang, useTranslation } from '../localization'

import { BottomFooterContainer } from './BottomFooterContainer'
import { RoundChildImage } from './RoundChildImages'
import TimeRangeInput, { TimeRangeWithErrors } from './TimeRangeInput'
import { postReservations } from './api'

interface Props {
  date: LocalDate
  reservationsResponse: ReservationsResponse
  selectDate: (date: LocalDate) => void
  reloadData: () => void
  close: () => void
  openAbsenceModal: (initialDate: LocalDate) => void
}

interface ChildWithReservations {
  child: ReservationChild
  absence: AbsenceType | undefined
  reservations: TimeRange[]
  attendances: OpenTimeRange[]
  dayOff: boolean
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
    .filter(
      (child) =>
        child.placementMinStart.isEqualOrBefore(date) &&
        child.placementMaxEnd.isEqualOrAfter(date)
    )
    .map((child) => {
      const childReservations = dailyData?.children.find(
        ({ childId }) => childId === child.id
      )

      const markedByEmployee = childReservations?.markedByEmployee ?? false

      const reservationEditable =
        !markedByEmployee &&
        (!dailyData.isHoliday || child.inShiftCareUnit) &&
        child.maxOperationalDays.includes(date.getIsoDayOfWeek()) &&
        !childReservations?.dayOff

      return {
        child,
        absence: childReservations?.absence ?? undefined,
        reservations: childReservations?.reservations ?? [],
        attendances: childReservations?.attendances ?? [],
        dayOff: childReservations?.dayOff ?? false,
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
  reloadData,
  close,
  openAbsenceModal
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
    confirmationModal
  } = useEditState(
    date,
    childrenWithReservations,
    reservationsResponse.reservableDays,
    reloadData
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
        <HighlightBorder highlight={date.isEqual(LocalDate.todayInSystemTz())}>
          <BottomFooterContainer>
            <Content>
              <DayPicker>
                <IconButton
                  icon={faChevronLeft}
                  onClick={navigateToPrevDate}
                  disabled={!previousDate}
                  altText={i18n.calendar.previousDay}
                />
                <DayOfWeek>{date.format('EEEEEE d.M.yyyy', lang)}</DayOfWeek>
                <IconButton
                  icon={faChevronRight}
                  onClick={navigateToNextDate}
                  disabled={!nextDate}
                  altText={i18n.calendar.nextDay}
                />
              </DayPicker>
              <Gap size="m" />
              <ReservationTitle>
                <H2 noMargin>{i18n.calendar.reservationsAndRealized}</H2>
              </ReservationTitle>
              <Gap size="s" />
              {zip(childrenWithReservations, editorState).map(
                ([childWithReservation, childState], childIndex) => {
                  if (!childWithReservation || !childState) return null

                  const {
                    child,
                    absence,
                    reservations,
                    attendances,
                    dayOff,
                    reservationEditable,
                    markedByEmployee
                  } = childWithReservation

                  const showAttendanceWarning =
                    !editing &&
                    reservationsAndAttendancesDiffer(reservations, attendances)

                  return (
                    <div key={child.id} data-qa={`reservations-of-${child.id}`}>
                      {childIndex !== 0 ? <Separator /> : null}
                      <FixedSpaceRow>
                        <FixedSpaceColumn>
                          <RoundChildImage
                            imageId={child.imageId}
                            initialLetter={
                              (child.preferredName || child.firstName || '?')[0]
                            }
                            colorIndex={childIndex}
                            size={48}
                          />
                        </FixedSpaceColumn>
                        <FixedSpaceColumn
                          spacing="zero"
                          justifyContent="center"
                        >
                          <ChildNameHeading noMargin data-qa="child-name">
                            {formatPreferredName(child)}
                          </ChildNameHeading>
                        </FixedSpaceColumn>
                      </FixedSpaceRow>
                      <Gap size="s" />
                      <Grid>
                        <LabelLike>{i18n.calendar.reservation}</LabelLike>
                        {editing &&
                        (reservationEditable || reservations.length) ? (
                          <EditReservation
                            childId={child.id}
                            canAddSecondReservation={
                              !reservations[1] && child.inShiftCareUnit
                            }
                            childState={childState}
                            editorStateSetter={editorStateSetter}
                            editorAbsenceSetter={editorAbsenceSetter}
                            addSecondReservation={addSecondReservation}
                            removeSecondReservation={removeSecondReservation}
                          />
                        ) : absence ? (
                          <Absence
                            absence={absence}
                            markedByEmployee={markedByEmployee}
                          />
                        ) : reservations.length === 0 && dayOff ? (
                          <DayOff />
                        ) : (
                          <Reservations reservations={reservations} />
                        )}
                        <LabelLike>{i18n.calendar.realized}</LabelLike>
                        <span>
                          {attendances.length > 0
                            ? attendances
                                .map(
                                  ({ startTime, endTime }) =>
                                    `${startTime} – ${endTime ?? ''}`
                                )
                                .join(', ')
                            : '–'}
                        </span>
                        {showAttendanceWarning && (
                          <Warning>
                            {i18n.calendar.attendanceWarning}
                            <StatusIcon status="warning" />
                          </Warning>
                        )}
                      </Grid>
                    </div>
                  )
                }
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
            </Content>
            <ButtonFooter>
              {editable ? (
                editing ? (
                  <Button
                    disabled={saving}
                    onClick={save}
                    text={i18n.common.save}
                    data-qa="save"
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
              <Button
                primary
                text={i18n.calendar.newAbsence}
                onClick={onCreateAbsence}
                disabled={!absenceEditable}
                data-qa="create-absence"
              />
            </ButtonFooter>
          </BottomFooterContainer>
        </HighlightBorder>
        <ModalCloseButton
          close={navigate(close)}
          closeLabel={i18n.common.closeModal}
          data-qa="day-view-close-button"
        />
      </PlainModal>
    </ModalAccessibilityWrapper>
  )
})

const ChildNameHeading = styled(H3)`
  font-weight: ${fontWeights.medium};
`

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
  reservableDays: FiniteDateRange[],
  reloadData: () => void
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
    () => anyChildReservable && reservableDays.some((r) => r.includes(date)),
    [anyChildReservable, reservableDays, date]
  )

  const absenceEditable = useMemo(
    () => anyChildReservable && date.isEqualOrAfter(today),
    [anyChildReservable, date, today]
  )

  const [editing, setEditing] = useState(false)
  const startEditing = useCallback(() => setEditing(true), [])

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
          state.map((childState) =>
            childState.child.id === childId
              ? {
                  ...childState,
                  reservations: childState.reservations.map((timeRange, i) =>
                    index === i
                      ? addTimeRangeValidationErrors({
                          ...timeRange,
                          [field]: value
                        })
                      : timeRange
                  )
                }
              : childState
          )
        ),
    []
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

  const [saving, setSaving] = useState(false)
  const save = useCallback(() => {
    if (!stateIsValid) return Promise.resolve()

    setSaving(true)
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
    )
      .then(() => setEditing(false))
      .then(() => reloadData())
      .finally(() => setSaving(false))
  }, [date, editorState, reloadData, stateIsValid])

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
    confirmationModal
  }
}

const EditReservation = React.memo(function EditReservation({
  childId,
  canAddSecondReservation,
  childState,
  editorStateSetter,
  editorAbsenceSetter,
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
  const i18n = useTranslation()

  const onChangeFirst = useCallback(
    (field: 'startTime' | 'endTime') => editorStateSetter(childId, 0, field),
    [editorStateSetter, childId]
  )
  const onChangeSecond = useCallback(
    (field: 'startTime' | 'endTime') => editorStateSetter(childId, 1, field),
    [editorStateSetter, childId]
  )

  return (
    <FixedSpaceColumn>
      <FixedSpaceRow alignItems="center">
        {childState.absent ? (
          <>
            {i18n.calendar.absent}
            <Gap horizontal size="s" />
          </>
        ) : (
          <TimeRangeInput
            value={childState.reservations[0]}
            onChange={onChangeFirst}
            data-qa="first-reservation"
          />
        )}
        <IconButton
          icon={childState.absent ? fasUserMinus : faUserMinus}
          onClick={() => editorAbsenceSetter(childId, !childState.absent)}
        />
        {canAddSecondReservation && (
          <IconButton
            icon={faPlus}
            onClick={() => addSecondReservation(childId)}
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
          />
        </FixedSpaceColumn>
      </FixedSpaceRow>
      {open && (
        <Colspan2>
          <ExpandingInfoBox
            width="auto"
            info={i18n.calendar.contactStaffToEditAbsence}
            close={onClick}
          />
        </Colspan2>
      )}
    </>
  )
})

const DayOff = React.memo(function DayOff() {
  const i18n = useTranslation()
  const [open, setOpen] = useState(false)
  const onClick = useCallback(() => setOpen((prev) => !prev), [])

  return (
    <>
      <FixedSpaceRow data-qa="day-off">
        <FixedSpaceColumn>{i18n.calendar.dayOff}</FixedSpaceColumn>
        <FixedSpaceColumn>
          <InfoButton
            onClick={onClick}
            aria-label={i18n.common.openExpandingInfo}
          />
        </FixedSpaceColumn>
      </FixedSpaceRow>
      {open && (
        <Colspan2>
          <ExpandingInfoBox
            width="auto"
            info={i18n.calendar.contactStaffToEditAbsence}
            close={onClick}
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

const HighlightBorder = styled.div<{ highlight: boolean }>`
  height: 100%;
  position: relative;

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

const Content = styled.div`
  background: ${(p) => p.theme.colors.grayscale.g0};
  padding: ${defaultMargins.L};

  @media (max-width: ${tabletMin}) {
    padding: ${defaultMargins.s};
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

const ReservationTitle = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-between;
  align-items: center;
`

const Grid = styled.div`
  display: grid;
  grid-template-columns: max-content auto;
  row-gap: ${defaultMargins.xs};
  column-gap: ${defaultMargins.s};
  max-width: 100%;
`

const Colspan2 = styled.div`
  grid-column: 1 / span 2;
`

const NoReservation = styled.span`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
`

const Separator = styled.div`
  border-top: 2px dotted ${(p) => p.theme.colors.grayscale.g15};
  margin: ${defaultMargins.s} 0;
`

const Warning = styled.div`
  grid-column: 1 / 3;
  color: ${({ theme }) => theme.colors.accents.a2orangeDark};
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
