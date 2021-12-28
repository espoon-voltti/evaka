// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import {
  faCheck,
  faChevronLeft,
  faChevronRight,
  faPen,
  faPlus,
  faTrash,
  faUserMinus
} from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import { tabletMin } from 'lib-components/breakpoints'
import { H1, H2, H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { useTranslation } from '../localization'
import {
  Reservation,
  ReservationChild,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'
import { AbsenceType } from 'lib-common/generated/enums'
import { postReservations } from './api'
import CalendarModal from './CalendarModal'
import { zip } from 'lodash'
import FiniteDateRange from 'lib-common/finite-date-range'
import { UUID } from 'lib-common/types'
import TimeRangeInput, { TimeRange, validateTimeRange } from './TimeRangeInput'

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
  reservations: Reservation[]
  reservationEditable: boolean
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

      const reservationEditable =
        child.maxOperationalDays.includes(date.getIsoDayOfWeek()) &&
        (!dailyData.isHoliday || child.inShiftCareUnit)

      return {
        child,
        reservationEditable,
        absence: childReservations?.absence ?? undefined,
        reservations: childReservations?.reservations ?? []
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
    editing,
    startEditing,
    editorState,
    editorStateSetter,
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
    <CalendarModal close={navigate(close)} data-qa="calendar-dayview">
      <Content highlight={date.isEqual(LocalDate.today())}>
        <DayPicker>
          <IconButton
            icon={faChevronLeft}
            onClick={navigateToPrevDate}
            disabled={!previousDate}
          />
          <DayOfWeek>{`${
            i18n.common.datetime.weekdaysShort[date.getIsoDayOfWeek() - 1]
          } ${date.format('d.M.yyyy')}`}</DayOfWeek>
          <IconButton
            icon={faChevronRight}
            onClick={navigateToNextDate}
            disabled={!nextDate}
          />
        </DayPicker>
        <Gap size="m" />
        <ReservationTitle>
          <H2 noMargin>{i18n.calendar.reservationsAndRealized}</H2>
          {editable &&
            (editing ? (
              <InlineButton
                icon={faCheck}
                disabled={saving}
                onClick={save}
                text={i18n.common.save}
                iconRight
                data-qa="save"
              />
            ) : (
              <InlineButton
                icon={faPen}
                onClick={startEditing}
                text={i18n.common.edit}
                iconRight
                data-qa="edit"
              />
            ))}
        </ReservationTitle>
        <Gap size="s" />
        {zip(childrenWithReservations, editorState).map(
          ([childWithReservation, childState], childIndex) => {
            if (!childWithReservation || !childState) return null

            const { child, absence, reservations, reservationEditable } =
              childWithReservation

            return (
              <div key={child.id} data-qa={`reservations-of-${child.id}`}>
                {childIndex !== 0 ? <Separator /> : null}
                <H3 noMargin data-qa="child-name">
                  {child.preferredName || child.firstName.split(' ')[0]}
                </H3>
                <Gap size="s" />
                <Grid>
                  <Label>{i18n.calendar.reservation}</Label>
                  {editing && (reservationEditable || reservations.length) ? (
                    <EditReservation
                      childId={child.id}
                      canAddSecondReservation={
                        !reservations[1] && child.inShiftCareUnit
                      }
                      childState={childState}
                      editorStateSetter={editorStateSetter}
                      addSecondReservation={addSecondReservation}
                      removeSecondReservation={removeSecondReservation}
                    />
                  ) : absence ? (
                    <Absence absence={absence} />
                  ) : (
                    <Reservations reservations={reservations} />
                  )}
                  <Label>{i18n.calendar.realized}</Label>
                  <span>–</span>
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
      <BottomBar>
        <AbsenceButton
          text={i18n.calendar.newAbsence}
          icon={faUserMinus}
          onClick={onCreateAbsence}
          data-qa="create-absence"
        />
      </BottomBar>
    </CalendarModal>
  )
})

type EditorState = {
  child: ReservationChild
  reservations: TimeRange[]
}

const emptyReservation: TimeRange = {
  startTime: '',
  endTime: '',
  errors: { startTime: undefined, endTime: undefined }
}

function reservationToTimeRange({
  startTime,
  endTime
}: Reservation): TimeRange {
  return { ...emptyReservation, startTime, endTime }
}

function useEditState(
  date: LocalDate,
  childrenWithReservations: ChildWithReservations[],
  reservableDays: FiniteDateRange | null,
  reloadData: () => void
) {
  const editable = useMemo(
    () =>
      (reservableDays?.includes(date) ?? false) &&
      childrenWithReservations.some(
        ({ reservationEditable }) => reservationEditable
      ),
    [date, reservableDays, childrenWithReservations]
  )

  const [editing, setEditing] = useState(false)
  const startEditing = useCallback(() => setEditing(true), [])

  const initialEditorState: EditorState[] = useMemo(
    () =>
      childrenWithReservations.map(({ child, reservations }) => ({
        child,
        reservations:
          reservations.length > 0
            ? reservations.map(reservationToTimeRange)
            : [emptyReservation]
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
                      ? validateTimeRange({
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
      editorState.map(({ child, reservations }) => ({
        childId: child.id,
        date,
        reservations:
          reservations.flatMap(({ startTime, endTime }) =>
            startTime !== '' && endTime !== '' ? [{ startTime, endTime }] : []
          ) ?? []
      }))
    )
      .then(() => setEditing(false))
      .then(() => reloadData())
      .finally(() => setSaving(false))
  }, [date, editorState, reloadData, stateIsValid])

  const [confirmationModal, setConfirmationModal] =
    useState<{ close: () => void; resolve: () => void; reject: () => void }>()

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
    editing,
    startEditing,
    editorState,
    editorStateSetter,
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
  absence
}: {
  absence: AbsenceType
}) {
  const i18n = useTranslation()
  return <span>{i18n.calendar.absences[absence] ?? i18n.calendar.absent}</span>
})

const Reservations = React.memo(function Reservations({
  reservations
}: {
  reservations: Reservation[]
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
        .filter((res) => res)
        .join(', ')}
    </span>
  ) : (
    <NoReservation data-qa="no-reservations">
      {i18n.calendar.noReservation}
    </NoReservation>
  )
})

const Content = styled.div<{ highlight: boolean }>`
  background: ${(p) => p.theme.colors.greyscale.white};
  padding: ${defaultMargins.L};
  padding-left: calc(${defaultMargins.L} - 4px);
  border-left: 4px solid
    ${(p) =>
      p.highlight ? p.theme.colors.accents.successGreen : 'transparent'};

  @media (max-width: ${tabletMin}) {
    padding: ${defaultMargins.s};
    padding-left: calc(${defaultMargins.s} - 4px);
  }
`

const DayPicker = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-between;
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
  grid-template-columns: min-content auto;
  row-gap: ${defaultMargins.xs};
  column-gap: ${defaultMargins.s};
`

const NoReservation = styled.span`
  color: ${(p) => p.theme.colors.accents.orangeDark};
`

const Separator = styled.div`
  border-top: 2px dotted ${(p) => p.theme.colors.greyscale.lighter};
  margin: ${defaultMargins.s} 0;
`

const BottomBar = styled.div`
  background: ${(p) => p.theme.colors.greyscale.white};
  border-top: 2px solid ${({ theme }) => theme.colors.greyscale.lighter};
`

const AbsenceButton = styled(InlineButton)`
  padding: ${defaultMargins.s} ${defaultMargins.L};
  text-align: left;
  width: 100%;

  @media (max-width: ${tabletMin}) {
    padding: ${defaultMargins.s};
  }
`
