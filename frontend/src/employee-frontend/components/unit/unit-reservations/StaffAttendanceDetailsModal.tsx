// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import initial from 'lodash/initial'
import last from 'lodash/last'
import orderBy from 'lodash/orderBy'
import React, { Fragment, useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { ErrorKey } from 'lib-common/form-validation'
import {
  StaffAttendanceType,
  staffAttendanceTypes,
  StaffAttendanceUpsert
} from 'lib-common/generated/api-types/attendance'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Tooltip from 'lib-components/atoms/Tooltip'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import AsyncIconButton from 'lib-components/atoms/buttons/AsyncIconButton'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import ListGrid from 'lib-components/layout/ListGrid'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DatePickerSpacer } from 'lib-components/molecules/date-picker/DatePicker'
import {
  ModalCloseButton,
  PlainModal
} from 'lib-components/molecules/modals/BaseModal'
import { H1, H2, H3, LabelLike } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import {
  faChevronLeft,
  faChevronRight,
  faExclamationTriangle,
  faPlus,
  faTrash
} from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { errorToInputInfo } from '../../../utils/validation/input-info-helper'

export interface ModalAttendance {
  id: UUID
  groupId: UUID
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  type: StaffAttendanceType
}

export interface ModalPlannedAttendance {
  start: HelsinkiDateTime
  end: HelsinkiDateTime
}

interface Props {
  date: LocalDate
  name: string
  attendances: ModalAttendance[]
  plannedAttendances: ModalPlannedAttendance[]
  isExternal: boolean
  onSave: (body: StaffAttendanceUpsert[]) => void
  onSuccess: () => void
  onClose: () => void
  groups: DaycareGroup[]
  onPreviousDate: () => Promise<Result<unknown>>
  onNextDate: () => Promise<Result<unknown>>
}

interface EditedAttendance {
  id: UUID | null
  groupId: UUID | null
  arrived: string
  departed: string
  type: StaffAttendanceType
}

export default React.memo(function StaffAttendanceDetailsModal({
  date,
  name,
  attendances,
  plannedAttendances,
  isExternal,
  onSave,
  onSuccess,
  onClose,
  groups,
  onPreviousDate,
  onNextDate
}: Props) {
  const { i18n } = useTranslation()

  const [startOfDay, endOfDay] = useMemo(
    () => [
      HelsinkiDateTime.fromLocal(date, LocalTime.of(0, 0)),
      HelsinkiDateTime.fromLocal(date.addDays(1), LocalTime.of(0, 0))
    ],
    [date]
  )

  const sortedAttendances = useMemo(
    () =>
      orderBy(
        attendances.filter(
          ({ arrived, departed }) =>
            arrived < endOfDay && (departed === null || startOfDay < departed)
        ) ?? [],
        ({ arrived }) => arrived
      ),
    [attendances, endOfDay, startOfDay]
  )

  const initialEditState = useMemo(
    () =>
      sortedAttendances.map(({ id, groupId, arrived, departed, type }) => ({
        id,
        groupId,
        arrived: date.isEqual(arrived.toLocalDate())
          ? arrived.toLocalTime().format('HH:mm')
          : '',
        departed:
          departed && date.isEqual(departed.toLocalDate())
            ? departed.toLocalTime().format('HH:mm')
            : '',
        type
      })),
    [date, sortedAttendances]
  )
  const [{ editState, editStateDate }, setEditState] = useState<{
    editState: EditedAttendance[]
    editStateDate: LocalDate
  }>({ editState: initialEditState, editStateDate: date })

  if (editStateDate !== date) {
    setEditState({
      editState: initialEditState,
      editStateDate: date
    })
  }

  const updateAttendance = useCallback(
    (index: number, data: Omit<EditedAttendance, 'id'>) =>
      setEditState(({ editState, editStateDate }) => ({
        editStateDate,
        editState: editState.map((row, i) =>
          index === i ? { ...row, ...data } : row
        )
      })),
    []
  )
  const removeAttendance = useCallback(
    (index: number) =>
      setEditState(({ editState, editStateDate }) => ({
        editStateDate,
        editState: editState.filter((att, i) => index !== i)
      })),
    []
  )
  const addNewAttendance = useCallback(
    () =>
      setEditState(({ editState, editStateDate }) => ({
        editStateDate,
        editState: [
          ...editState,
          {
            id: null,
            groupId: null,
            arrived: '',
            departed: '',
            type: 'PRESENT'
          }
        ]
      })),
    []
  )
  const [requestBody, errors] = validateEditState(attendances, date, editState)
  const save = useCallback(() => {
    if (!requestBody) return
    return onSave(requestBody)
  }, [onSave, requestBody])

  const gaplessAttendances = useMemo(
    () =>
      sortedAttendances
        .reduce<
          {
            arrived: HelsinkiDateTime
            departed: HelsinkiDateTime | null
          }[][]
        >(
          (prev, { arrived, departed }) =>
            prev.length === 0 || !last(last(prev))?.departed?.isEqual(arrived)
              ? [
                  ...prev,
                  [
                    {
                      arrived,
                      departed
                    }
                  ]
                ]
              : [
                  ...initial(prev),
                  [...(last(prev) ?? []), { arrived, departed }]
                ],
          []
        )
        .map((gaplessPeriod) => ({
          arrived: gaplessPeriod[0].arrived,
          departed: last(gaplessPeriod)?.departed ?? null
        })),
    [sortedAttendances]
  )

  const plannedAttendancesToday = useMemo(
    () =>
      plannedAttendances.filter(
        ({ end, start }) =>
          end.toLocalDate().isEqual(date) || start.toLocalDate().isEqual(date)
      ),
    [plannedAttendances, date]
  )

  const totalMinutes = useMemo(
    () =>
      gaplessAttendances.some(
        ({ arrived, departed }) =>
          !arrived.toLocalDate().isEqual(LocalDate.todayInHelsinkiTz()) &&
          !departed
      )
        ? 'incalculable'
        : gaplessAttendances.reduce(
            (prev, { arrived, departed }) =>
              prev +
              ((departed?.timestamp ?? HelsinkiDateTime.now().timestamp) -
                arrived.timestamp),
            0
          ) /
          1000 /
          60,
    [gaplessAttendances]
  )

  const diffPlannedTotalMinutes = useMemo(
    () =>
      totalMinutes === 'incalculable'
        ? 0
        : totalMinutes -
          (plannedAttendancesToday?.reduce(
            (prev, { start, end }) => prev + (end.timestamp - start.timestamp),
            0
          ) ?? 0) /
            1000 /
            60,
    [plannedAttendancesToday, totalMinutes]
  )

  const getGapBefore = useCallback(
    (currentIndex: number) => {
      if (!requestBody) return undefined

      const previousEntry = requestBody[currentIndex - 1]
      const currentEntry = requestBody[currentIndex]

      if (!previousEntry || !currentEntry) return undefined

      if (
        !previousEntry.departed ||
        !currentEntry.arrived.isEqual(previousEntry.departed)
      ) {
        return `${
          previousEntry.departed?.toLocalTime().format('HH:mm') ?? ''
        } – ${currentEntry.arrived.toLocalTime().format('HH:mm')}`
      }

      return undefined
    },
    [requestBody]
  )

  const openGroups = useMemo(() => groups.filter(isGroupOpen), [groups])

  const saveDisabled = useMemo(
    () =>
      !requestBody ||
      JSON.stringify(initialEditState) === JSON.stringify(editState),
    [editState, initialEditState, requestBody]
  )

  return (
    <PlainModal margin="auto" data-qa="staff-attendance-details-modal">
      <Content>
        <FixedSpaceRow alignItems="center">
          <AsyncIconButton
            icon={faChevronLeft}
            onClick={onPreviousDate}
            onSuccess={() => undefined}
            aria-label={i18n.unit.staffAttendance.previousDay}
          />
          <H1 noMargin>{date.formatExotic('EEEEEE d.M.yyyy')}</H1>
          <AsyncIconButton
            icon={faChevronRight}
            onClick={onNextDate}
            onSuccess={() => undefined}
            aria-label={i18n.unit.staffAttendance.nextDay}
          />
        </FixedSpaceRow>
        <H2>{name}</H2>
        {!isExternal ? (
          <>
            <H3>{i18n.unit.staffAttendance.summary}</H3>
            <ListGrid rowGap="s" labelWidth="auto">
              <LabelLike>{i18n.unit.staffAttendance.plan}</LabelLike>
              <FixedSpaceColumn data-qa="staff-attendance-summary-plan">
                {plannedAttendances.length > 0
                  ? plannedAttendances.map(({ end, start }, i) => (
                      <div key={i}>
                        {formatDate(start, date)} –{' '}
                        {end ? formatDate(end, date) : ''}
                      </div>
                    ))
                  : '–'}
              </FixedSpaceColumn>
              <LabelLike>{i18n.unit.staffAttendance.realized}</LabelLike>
              <FixedSpaceColumn data-qa="staff-attendance-summary-realized">
                {gaplessAttendances.length > 0
                  ? gaplessAttendances.map(({ arrived, departed }, i) => (
                      <div key={i}>
                        {arrived ? formatDate(arrived, date) : ''} –{' '}
                        {departed ? formatDate(departed, date) : ''}
                      </div>
                    ))
                  : '–'}
              </FixedSpaceColumn>
              <LabelLike>{i18n.unit.staffAttendance.hours}</LabelLike>
              <div data-qa="staff-attendance-summary-hours">
                {totalMinutes === 'incalculable' ? (
                  <Tooltip tooltip={i18n.unit.staffAttendance.incalculableSum}>
                    <FontAwesomeIcon
                      icon={faExclamationTriangle}
                      color={colors.status.warning}
                    />
                  </Tooltip>
                ) : totalMinutes === 0 ? (
                  '-'
                ) : (
                  `${formatMinutes(totalMinutes)}${
                    plannedAttendances.length > 0
                      ? ` (${
                          Math.sign(diffPlannedTotalMinutes) === 1 ? '+' : '-'
                        }${formatMinutes(Math.abs(diffPlannedTotalMinutes))})`
                      : ''
                  }`
                )}
              </div>
            </ListGrid>

            <HorizontalLine slim />
          </>
        ) : null}

        <H3>{i18n.unit.staffAttendance.dailyAttendances}</H3>
        <ListGrid rowGap="s" labelWidth="auto">
          {editState.map(({ arrived, departed, type, groupId }, index) => {
            const gap = getGapBefore(index)

            return (
              <Fragment key={index}>
                {!!gap && (
                  <FullGridWidth>
                    <FixedSpaceRow
                      justifyContent="center"
                      alignItems="center"
                      spacing="s"
                    >
                      <FontAwesomeIcon
                        icon={faExclamationTriangle}
                        color={colors.status.warning}
                      />
                      <div data-qa={`attendance-gap-warning-${index}`}>
                        {i18n.unit.staffAttendance.gapWarning(gap)}
                      </div>
                    </FixedSpaceRow>
                  </FullGridWidth>
                )}
                <GroupIndicator data-qa="group-indicator">
                  {groupId === null ? (
                    <Select
                      items={[null, ...openGroups.map(({ id }) => id)]}
                      selectedItem={groupId}
                      onChange={(value) =>
                        updateAttendance(index, {
                          arrived,
                          departed,
                          type,
                          groupId: value
                        })
                      }
                      getItemLabel={(item) =>
                        groups.find(({ id }) => id === item)?.name ??
                        i18n.unit.staffAttendance.noGroup
                      }
                      data-qa="attendance-group-select"
                    />
                  ) : (
                    <InlineButton
                      text={groups.find((g) => g.id === groupId)?.name ?? '-'}
                      onClick={() =>
                        updateAttendance(index, {
                          arrived,
                          departed,
                          type,
                          groupId: null
                        })
                      }
                    />
                  )}
                </GroupIndicator>
                {featureFlags.experimental?.staffAttendanceTypes &&
                !isExternal ? (
                  <Select
                    items={[...staffAttendanceTypes]}
                    selectedItem={type}
                    onChange={(value) =>
                      value &&
                      updateAttendance(index, {
                        arrived,
                        departed,
                        type: value,
                        groupId
                      })
                    }
                    getItemLabel={(item) =>
                      i18n.unit.staffAttendance.types[item]
                    }
                    data-qa="attendance-type-select"
                  />
                ) : null}
                <InputRow>
                  <TimeInput
                    value={arrived}
                    onChange={(value) =>
                      updateAttendance(index, {
                        arrived: value,
                        departed,
                        type,
                        groupId
                      })
                    }
                    info={errorToInputInfo(
                      errors[index].arrived,
                      i18n.validationErrors
                    )}
                    data-qa="arrival-time-input"
                  />
                  <Gap size="xs" horizontal />
                  <DatePickerSpacer />
                  <Gap size="xs" horizontal />
                  <TimeInput
                    value={departed}
                    onChange={(value) =>
                      updateAttendance(index, {
                        arrived,
                        departed: value,
                        type,
                        groupId
                      })
                    }
                    info={errorToInputInfo(
                      errors[index].departed,
                      i18n.validationErrors
                    )}
                    data-qa="departure-time-input"
                  />
                  <Gap size="xs" horizontal />
                  <IconButton
                    icon={faTrash}
                    onClick={() => removeAttendance(index)}
                    aria-label={i18n.common.remove}
                  />
                </InputRow>
              </Fragment>
            )
          })}
          <NewAttendance>
            <InlineButton
              icon={faPlus}
              text={i18n.unit.staffAttendance.addNewAttendance}
              onClick={addNewAttendance}
              data-qa="new-attendance"
            />
          </NewAttendance>
        </ListGrid>
        <Gap size="L" />
        <ModalActions>
          <Button text={i18n.common.cancel} onClick={onClose} />
          <AsyncButton
            primary
            text={i18n.unit.staffAttendance.saveChanges}
            onClick={save}
            onSuccess={onSuccess}
            disabled={saveDisabled}
            data-qa="save"
          />
        </ModalActions>
      </Content>
      <ModalCloseButton
        close={onClose}
        closeLabel={i18n.common.closeModal}
        data-qa="close"
      />
    </PlainModal>
  )
})

const formatDate = (time: HelsinkiDateTime, currentDate: LocalDate) =>
  time.toLocalDate().isEqual(currentDate)
    ? time.toLocalTime().format('HH:mm')
    : '→'

interface ValidationError {
  arrived?: ErrorKey
  departed?: ErrorKey
}

export function validateEditState(
  attendances: ModalAttendance[],
  date: LocalDate,
  state: EditedAttendance[]
): [StaffAttendanceUpsert[] | undefined, ValidationError[]] {
  const body: StaffAttendanceUpsert[] = []
  const errors: ValidationError[] = []
  let hasErrors = false

  for (let i = 0; i < state.length; i++) {
    const item = state[i]
    const existing = attendances.find((a) => a.id === item.id)

    const itemErrors: ValidationError = {}

    let parsedArrived: LocalTime | undefined = undefined
    if (!item.arrived) {
      if (i === 0) {
        const isOvernightAttendance =
          existing && existing.arrived.toLocalDate().isBefore(date)
        if (!isOvernightAttendance) {
          itemErrors.arrived = 'required'
        }
      } else {
        itemErrors.arrived = 'required'
      }
    } else {
      parsedArrived = LocalTime.tryParse(item.arrived, 'HH:mm')
      if (!parsedArrived) {
        itemErrors.arrived = 'timeFormat'
      }
    }

    let parsedDeparted: LocalTime | undefined = undefined
    if (!item.departed) {
      if (i !== state.length - 1) {
        itemErrors.departed = 'required'
      }
    } else {
      parsedDeparted = LocalTime.tryParse(item.departed, 'HH:mm')
      if (!parsedDeparted) {
        itemErrors.departed = 'timeFormat'
      }
    }

    if (
      i !== state.length - 1 &&
      parsedArrived &&
      parsedDeparted &&
      parsedDeparted.isBefore(parsedArrived)
    ) {
      itemErrors.departed = 'timeFormat'
    }

    if (
      i > 0 &&
      !itemErrors.arrived &&
      !errors[i - 1].departed &&
      item.arrived < state[i - 1].departed
    ) {
      itemErrors.arrived = 'timeFormat'
    }

    const arrived = parsedArrived
      ? HelsinkiDateTime.fromLocal(date, parsedArrived)
      : existing?.arrived ?? null
    if (
      arrived !== null &&
      item.groupId !== null &&
      Object.keys(itemErrors).length === 0
    ) {
      const departed = parsedDeparted
        ? HelsinkiDateTime.fromLocal(
            // If departed is before arrived, it means that the attendance
            // is overnight and departed is on the next day
            parsedArrived && parsedDeparted.isBefore(parsedArrived)
              ? date.addDays(1)
              : date,
            parsedDeparted
          )
        : existing?.departed ?? null
      body.push({
        id: item.id,
        type: item.type,
        groupId: item.groupId,
        arrived,
        departed
      })
    } else {
      hasErrors = true
    }
    errors[i] = itemErrors
  }

  if (hasErrors) {
    return [undefined, errors]
  }

  return [body, errors]
}

function formatMinutes(minutes: number) {
  return `${Math.floor(minutes / 60)}:${Math.floor(minutes % 60)
    .toString()
    .padStart(2, '0')}`
}

function isGroupOpen(group: DaycareGroup) {
  return new DateRange(group.startDate, group.endDate).includes(
    LocalDate.todayInHelsinkiTz()
  )
}

const Content = styled.div`
  padding: ${defaultMargins.L};
`

const GroupIndicator = styled.div`
  grid-column: 1 / 3;
  margin-bottom: -${defaultMargins.xs};
`

const InputRow = styled.div`
  display: flex;
`

const NewAttendance = styled.div`
  grid-column: 1 / 3;
`

const ModalActions = styled.div`
  display: flex;
  justify-content: space-between;
`

const FullGridWidth = styled.div`
  grid-column: 1 / -1;
`
