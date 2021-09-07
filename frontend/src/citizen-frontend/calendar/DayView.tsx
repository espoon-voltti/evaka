// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { faCheck, faChevronLeft, faChevronRight, faPen } from 'lib-icons'
import { DATE_FORMAT_TIME_ONLY, formatDate } from 'lib-common/date'
import LocalDate from 'lib-common/local-date'
import { H1, H2, H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InputField from 'lib-components/atoms/form/InputField'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { useTranslation } from 'citizen-frontend/localization'
import { TIME_REGEXP, regexp } from 'lib-common/form-validation'
import {
  postReservations,
  ChildDailyData,
  ReservationChild,
  ReservationsResponse
} from './api'
import CalendarModal from './CalendarModal'
import { errorToInputInfo } from '../input-info-helper'

interface Props {
  date: LocalDate
  data: ReservationsResponse
  selectDate: (date: LocalDate) => void
  reloadData: () => void
  close: () => void
}

export default React.memo(function DayView({
  date,
  data,
  selectDate,
  reloadData,
  close
}: Props) {
  const i18n = useTranslation()

  const childrenWithReservations = useMemo(() => {
    const reservations = data.dailyData.find((reservation) =>
      date.isEqual(reservation.date)
    )

    return data.children.map((child) => {
      const reservation = reservations?.children.find(
        ({ childId }) => childId === child.id
      )

      return {
        child,
        reservation
      }
    })
  }, [date, data])

  const {
    editable,
    editing,
    startEditing,
    editorState,
    editorStateSetter,
    saving,
    save,
    navigate,
    confirmationModal
  } = useEditState(date, data, reloadData, childrenWithReservations)

  return (
    <CalendarModal
      highlight={date.isEqual(LocalDate.today())}
      close={navigate(close)}
    >
      <DayPicker>
        <IconButton
          icon={faChevronLeft}
          onClick={navigate(() => selectDate(date.subDays(1)))}
        />
        <DayOfWeek>{`${
          i18n.common.datetime.weekdays[date.getIsoDayOfWeek() - 1]
        } ${date.format()}`}</DayOfWeek>
        <IconButton
          icon={faChevronRight}
          onClick={navigate(() => selectDate(date.addDays(1)))}
        />
      </DayPicker>
      <Gap size="m" />
      <ReservationTitle>
        <H2 noMargin>Varaukset ja toteuma</H2>
        {editable ? (
          editing ? (
            <IconButton icon={faCheck} disabled={saving} onClick={save} />
          ) : (
            <IconButton icon={faPen} onClick={startEditing} />
          )
        ) : null}
      </ReservationTitle>
      <Gap size="s" />
      {editing
        ? editorState.map(({ child, startTime, endTime, errors }, index) => (
            <Fragment key={child.id}>
              {index !== 0 ? <Separator /> : null}
              <H3 noMargin>
                {child.preferredName || child.firstName.split(' ')[0]}
              </H3>
              <Gap size="s" />
              <Grid>
                <Label>Varaus</Label>
                <InputWrapper>
                  <InputField
                    type="time"
                    onChange={editorStateSetter(index, 'startTime')}
                    value={startTime}
                    info={errorToInputInfo(
                      errors.startTime,
                      i18n.validationErrors
                    )}
                    readonly={saving}
                  />
                  –
                  <InputField
                    type="time"
                    onChange={editorStateSetter(index, 'endTime')}
                    value={endTime}
                    info={errorToInputInfo(
                      errors.endTime,
                      i18n.validationErrors
                    )}
                    readonly={saving}
                  />
                </InputWrapper>
                <Label>Toteuma</Label>
                <span>–</span>
              </Grid>
            </Fragment>
          ))
        : childrenWithReservations.map(({ child, reservation }, index) => (
            <Fragment key={child.id}>
              {index !== 0 ? <Separator /> : null}
              <H3 noMargin>
                {child.preferredName || child.firstName.split(' ')[0]}
              </H3>
              <Gap size="s" />
              <Grid>
                <Label>Varaus</Label>
                {reservation?.absence ? (
                  <span>
                    {i18n.calendar.absences[reservation.absence] ??
                      i18n.calendar.absent}
                  </span>
                ) : reservation?.reservation ? (
                  <span>{`${formatDate(
                    reservation.reservation.startTime,
                    DATE_FORMAT_TIME_ONLY
                  )} – ${formatDate(
                    reservation.reservation.endTime,
                    DATE_FORMAT_TIME_ONLY
                  )}`}</span>
                ) : (
                  <NoReservation>Ei varausta</NoReservation>
                )}
                <Label>Toteuma</Label>
                <span>–</span>
              </Grid>
            </Fragment>
          ))}
      {confirmationModal ? (
        <InfoModal
          title="Haluatko tallentaa muutokset?"
          close={confirmationModal.close}
          resolve={{
            action: confirmationModal.resolve,
            label: 'Tallenna'
          }}
          reject={{
            action: confirmationModal.reject,
            label: 'Älä tallenna'
          }}
        />
      ) : null}
    </CalendarModal>
  )
})

function useEditState(
  date: LocalDate,
  data: ReservationsResponse,
  reloadData: () => void,
  childrenWithReservations: {
    child: ReservationChild
    reservation: ChildDailyData | undefined
  }[]
) {
  const editable = data.reservableDays.includes(date)

  const [editing, setEditing] = useState(false)
  const startEditing = () => setEditing(true)

  const editorStateFromProps = useMemo(
    () =>
      childrenWithReservations.map(({ child, reservation }) => ({
        child,
        startTime: reservation?.reservation
          ? formatDate(reservation.reservation.startTime, DATE_FORMAT_TIME_ONLY)
          : '',
        endTime: reservation?.reservation
          ? formatDate(reservation.reservation.endTime, DATE_FORMAT_TIME_ONLY)
          : '',
        errors: {
          startTime: undefined,
          endTime: undefined
        }
      })),
    [childrenWithReservations]
  )
  const [editorState, setEditorState] = useState(editorStateFromProps)
  useEffect(() => setEditorState(editorStateFromProps), [editorStateFromProps])

  const editorStateSetter =
    (index: number, field: 'startTime' | 'endTime') => (value: string) =>
      setEditorState((state) =>
        state.map((child, i) =>
          i === index
            ? {
                ...child,
                [field]: value,
                errors: {
                  ...child.errors,
                  [field]:
                    value === '' &&
                    child[field === 'startTime' ? 'endTime' : 'startTime'] !==
                      ''
                      ? 'required'
                      : regexp(value, TIME_REGEXP, 'timeFormat')
                }
              }
            : child
        )
      )

  const stateIsValid = editorState.every(
    ({ errors }) =>
      errors.startTime === undefined && errors.endTime === undefined
  )

  const [saving, setSaving] = useState(false)
  const save = () => {
    if (!stateIsValid) return Promise.resolve()

    setSaving(true)
    return postReservations(
      editorState
        .filter(({ startTime, endTime }) => startTime !== '' && endTime !== '')
        .map(({ child, startTime, endTime }) => ({
          childId: child.id,
          date: date,
          reservation: {
            startTime,
            endTime
          }
        }))
    )
      .then(() => setEditing(false))
      .then(() => reloadData())
      .finally(() => setSaving(false))
  }

  const [confirmationModal, setConfirmationModal] =
    useState<{ close: () => void; resolve: () => void; reject: () => void }>()

  const navigate = (callback: () => void) => () => {
    const stateHasBeenModified = editorStateFromProps.some((childFromProps) => {
      const editorData = editorState.find(
        (child) => child.child.id === childFromProps.child.id
      )

      return (
        !editorData ||
        childFromProps.startTime !== editorData.startTime ||
        childFromProps.endTime !== editorData.endTime
      )
    })

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
  }

  return {
    editable,
    editing,
    startEditing,
    editorState,
    editorStateSetter,
    stateIsValid,
    saving,
    save,
    navigate,
    confirmationModal
  }
}

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

const InputWrapper = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: flex-start;
  align-items: center;
`
