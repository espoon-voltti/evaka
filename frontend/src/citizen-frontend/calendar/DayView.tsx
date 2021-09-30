// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import {
  faCheck,
  faChevronLeft,
  faChevronRight,
  faPen,
  faUserMinus
} from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import { tabletMin } from 'lib-components/breakpoints'
import { H1, H2, H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import InputField from 'lib-components/atoms/form/InputField'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { useTranslation } from '../localization'
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
  openAbsenceModal: () => void
}

export default React.memo(function DayView({
  date,
  data,
  selectDate,
  reloadData,
  close,
  openAbsenceModal
}: Props) {
  const i18n = useTranslation()

  const dateIndexInData = useMemo(
    () =>
      data.dailyData.findIndex((reservation) => date.isEqual(reservation.date)),
    [date, data]
  )

  const childrenWithReservations = useMemo(() => {
    const reservations = data.dailyData[dateIndexInData]

    return data.children.map((child) => {
      const reservation = reservations?.children.find(
        ({ childId }) => childId === child.id
      )

      return {
        child,
        reservation
      }
    })
  }, [dateIndexInData, data])

  const [previousDate, nextDate] = useMemo(() => {
    return [
      data.dailyData[dateIndexInData - 1]?.date,
      data.dailyData[dateIndexInData + 1]?.date
    ]
  }, [dateIndexInData, data])

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
    <CalendarModal close={navigate(close)}>
      <Content highlight={date.isEqual(LocalDate.today())}>
        <DayPicker>
          <IconButton
            icon={faChevronLeft}
            onClick={navigate(() => selectDate(previousDate))}
            disabled={!previousDate}
          />
          <DayOfWeek>{`${
            i18n.common.datetime.weekdaysShort[date.getIsoDayOfWeek() - 1]
          } ${date.format('d.M.yyyy')}`}</DayOfWeek>
          <IconButton
            icon={faChevronRight}
            onClick={navigate(() => selectDate(nextDate))}
            disabled={!nextDate}
          />
        </DayPicker>
        <Gap size="m" />
        <ReservationTitle>
          <H2 noMargin>Varaukset ja toteuma</H2>
          {editable ? (
            editing ? (
              <InlineButton
                icon={faCheck}
                disabled={saving}
                onClick={save}
                text={i18n.common.save}
                iconRight
              />
            ) : (
              <InlineButton
                icon={faPen}
                onClick={startEditing}
                text={i18n.common.edit}
                iconRight
              />
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
                    <span>{`${reservation.reservation.startTime} – ${reservation.reservation.endTime}`}</span>
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
          onClick={openAbsenceModal}
        />
      </BottomBar>
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
        startTime: reservation?.reservation?.startTime ?? '',
        endTime: reservation?.reservation?.endTime ?? '',
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
      editorState.map(({ child, startTime, endTime }) => ({
        childId: child.id,
        date: date,
        reservation:
          startTime !== '' && endTime !== ''
            ? {
                startTime,
                endTime
              }
            : null
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

const Content = styled.div<{ highlight: boolean }>`
  background: ${(p) => p.theme.colors.greyscale.white};
  padding: ${defaultMargins.L};
  padding-left: calc(${defaultMargins.L} - 4px);
  border-left: 4px solid
    ${(p) => (p.highlight ? p.theme.colors.brand.secondary : 'transparent')};

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

const InputWrapper = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: flex-start;
  align-items: center;
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
