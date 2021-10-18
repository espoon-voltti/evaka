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
import InputField from 'lib-components/atoms/form/InputField'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { useTranslation } from '../localization'
import { TIME_REGEXP, regexp } from 'lib-common/form-validation'
import {
  ChildDailyData,
  ReservationChild,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'
import { postReservations } from './api'
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
    const dailyData = data.dailyData[dateIndexInData]

    return data.children
      .filter(
        (child) =>
          child.placementMinStart.isEqualOrBefore(date) &&
          child.placementMaxEnd.isEqualOrAfter(date)
      )
      .map((child) => {
        const childReservations = dailyData?.children.find(
          ({ childId }) => childId === child.id
        )

        return {
          child,
          data: {
            absence: childReservations?.absence ?? null,
            reservations: childReservations?.reservations ?? [
              { startTime: '', endTime: '' }
            ]
          }
        }
      })
  }, [dateIndexInData, data, date])

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
    addSecondReservation,
    removeSecondReservation,
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
          ? editorState.map(({ child, data }, childIndex) => (
              <Fragment key={child.id}>
                {childIndex !== 0 ? <Separator /> : null}
                <H3 noMargin>
                  {child.preferredName || child.firstName.split(' ')[0]}
                </H3>
                <Gap size="s" />
                <Grid>
                  <Label>Varaus</Label>
                  <FixedSpaceColumn>
                    <FixedSpaceRow alignItems="center">
                      <InputField
                        type="time"
                        onChange={editorStateSetter(child.id, 0, 'startTime')}
                        value={data[0].startTime}
                        info={errorToInputInfo(
                          data[0].errors.startTime,
                          i18n.validationErrors
                        )}
                        readonly={saving}
                      />
                      <span>–</span>
                      <InputField
                        type="time"
                        onChange={editorStateSetter(child.id, 0, 'endTime')}
                        value={data[0].endTime}
                        info={errorToInputInfo(
                          data[0].errors.endTime,
                          i18n.validationErrors
                        )}
                        readonly={saving}
                      />
                      {data[1] || !child.inShiftCareUnit ? null : (
                        <IconButton
                          icon={faPlus}
                          onClick={() => addSecondReservation(child.id)}
                        />
                      )}
                    </FixedSpaceRow>
                    {data[1] ? (
                      <FixedSpaceRow alignItems="center">
                        <InputField
                          type="time"
                          onChange={editorStateSetter(child.id, 1, 'startTime')}
                          value={data[1].startTime}
                          info={errorToInputInfo(
                            data[1].errors.startTime,
                            i18n.validationErrors
                          )}
                          readonly={saving}
                        />
                        <span>–</span>
                        <InputField
                          type="time"
                          onChange={editorStateSetter(child.id, 1, 'endTime')}
                          value={data[1].endTime}
                          info={errorToInputInfo(
                            data[1].errors.endTime,
                            i18n.validationErrors
                          )}
                          readonly={saving}
                        />
                        <IconButton
                          icon={faTrash}
                          onClick={() => removeSecondReservation(child.id)}
                        />
                      </FixedSpaceRow>
                    ) : null}
                  </FixedSpaceColumn>
                  <Label>Toteuma</Label>
                  <span>–</span>
                </Grid>
              </Fragment>
            ))
          : childrenWithReservations.map(({ child, data }, childIndex) => (
              <Fragment key={child.id}>
                {childIndex !== 0 ? <Separator /> : null}
                <H3 noMargin>
                  {child.preferredName || child.firstName.split(' ')[0]}
                </H3>
                <Gap size="s" />
                <Grid>
                  <Label>Varaus</Label>
                  {data.absence ? (
                    <span>
                      {i18n.calendar.absences[data.absence] ??
                        i18n.calendar.absent}
                    </span>
                  ) : data.reservations.length > 0 &&
                    data.reservations.some(({ startTime }) => !!startTime) ? (
                    <span>
                      {data.reservations
                        .map(
                          ({ startTime, endTime }) =>
                            `${startTime} – ${endTime}`
                        )
                        .filter((res) => res)
                        .join(', ')}
                    </span>
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
    data: Omit<ChildDailyData, 'childId'>
  }[]
) {
  const editable = data.reservableDays.includes(date)

  const [editing, setEditing] = useState(false)
  const startEditing = () => setEditing(true)

  const editorStateFromProps = useMemo(
    () =>
      childrenWithReservations.map(({ child, data }) => ({
        child,
        data: data?.reservations.map(({ startTime, endTime }) => ({
          startTime,
          endTime,
          errors: {
            startTime: undefined,
            endTime: undefined
          }
        }))
      })),
    [childrenWithReservations]
  )
  const [editorState, setEditorState] = useState(editorStateFromProps)
  useEffect(() => setEditorState(editorStateFromProps), [editorStateFromProps])

  const editorStateSetter =
    (childId: string, index: number, field: 'startTime' | 'endTime') =>
    (value: string) =>
      setEditorState((state) =>
        state.map(({ child, data }) =>
          child.id === childId
            ? {
                child,
                data: data.map((d, i) =>
                  index === i
                    ? {
                        ...d,
                        [field]: value,
                        errors: {
                          ...d.errors,
                          [field]:
                            value === '' &&
                            child[
                              field === 'startTime' ? 'endTime' : 'startTime'
                            ] !== ''
                              ? 'required'
                              : regexp(value, TIME_REGEXP, 'timeFormat')
                        }
                      }
                    : d
                )
              }
            : { child, data }
        )
      )

  const addSecondReservation = (childId: string) =>
    setEditorState((state) =>
      state.map((childState) =>
        childState.child.id === childId
          ? {
              ...childState,
              data: [
                childState.data[0],
                {
                  startTime: '',
                  endTime: '',
                  errors: { startTime: undefined, endTime: undefined }
                }
              ]
            }
          : childState
      )
    )

  const removeSecondReservation = (childId: string) =>
    setEditorState((state) =>
      state.map((childState) =>
        childState.child.id === childId
          ? {
              ...childState,
              data: [childState.data[0]]
            }
          : childState
      )
    )

  const stateIsValid = editorState.every(({ data }) =>
    data?.every(
      ({ errors }) =>
        errors.startTime === undefined && errors.endTime === undefined
    )
  )

  const [saving, setSaving] = useState(false)
  const save = () => {
    if (!stateIsValid) return Promise.resolve()

    setSaving(true)
    return postReservations(
      editorState.map(({ child, data }) => ({
        childId: child.id,
        date,
        reservations:
          data?.flatMap(({ startTime, endTime }) =>
            startTime !== '' && endTime !== '' ? [{ startTime, endTime }] : []
          ) ?? []
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
      const childEditorState = editorState.find(
        (child) => child.child.id === childFromProps.child.id
      )

      return (
        !childEditorState ||
        childFromProps.data.some(
          ({ startTime, endTime }, index) =>
            startTime !== childEditorState.data[index]?.startTime ||
            endTime !== childEditorState.data[index]?.endTime
        )
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
    addSecondReservation,
    removeSecondReservation,
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
