// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import initial from 'lodash/initial'
import last from 'lodash/last'
import orderBy from 'lodash/orderBy'
import reduce from 'lodash/reduce'
import uniq from 'lodash/uniq'
import uniqBy from 'lodash/uniqBy'
import React, {
  Fragment,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { ErrorKey } from 'lib-common/form-validation'
import {
  EmployeeAttendance,
  SingleDayStaffAttendanceUpsert,
  StaffAttendanceType,
  staffAttendanceTypes
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
import {
  faChevronLeft,
  faChevronRight,
  faExclamationTriangle,
  faPlus,
  faTrash
} from 'lib-icons'

import { postSingleDayStaffAttendances } from '../../../api/staff-attendance'
import { useTranslation } from '../../../state/i18n'
import { errorToInputInfo } from '../../../utils/validation/input-info-helper'

interface Props {
  unitId: UUID
  employeeId: UUID
  date: LocalDate
  attendances: EmployeeAttendance[]
  close: () => void
  reloadStaffAttendances: () => void
  groups: Result<DaycareGroup[]>
  onPreviousDate: () => Promise<void>
  onNextDate: () => Promise<void>
}

interface EditedAttendance {
  id: UUID | null
  groupId: UUID | null
  arrived: string
  departed: string
  type: StaffAttendanceType
}

export default React.memo(function StaffAttendanceDetailsModal({
  unitId,
  employeeId,
  date,
  attendances,
  close,
  reloadStaffAttendances,
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
  const employee = useMemo(
    () =>
      reduce(
        attendances.filter(
          (attendance) => attendance.employeeId === employeeId
        ),
        (p, n) => ({
          ...p,
          ...n,
          attendances: uniqBy(
            [...p.attendances, ...n.attendances],
            ({ id }) => id
          ),
          groups: uniq([...p.groups, ...n.groups]),
          plannedAttendances: uniq([
            ...p.plannedAttendances,
            ...n.plannedAttendances
          ])
        })
      ),
    [employeeId, attendances]
  )
  const sortedAttendances = useMemo(
    () =>
      orderBy(
        employee?.attendances?.filter(
          ({ arrived, departed }) =>
            arrived < endOfDay && (departed === null || startOfDay < departed)
        ) ?? [],
        ({ arrived }) => arrived
      ),
    [employee?.attendances, endOfDay, startOfDay]
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

  useEffect(() => {
    if (editStateDate !== date) {
      setEditState({
        editState: initialEditState,
        editStateDate: date
      })
    }
  }, [initialEditState, date, editStateDate, editState, attendances])

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
  const [requestBody, errors] = validateEditState(employee, date, editState)
  const save = useCallback(() => {
    if (!requestBody) return
    return postSingleDayStaffAttendances(unitId, employeeId, date, requestBody)
  }, [date, employeeId, requestBody, unitId])

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

  const plannedAttendances = useMemo(
    () =>
      employee?.plannedAttendances.filter(
        ({ end, start }) =>
          end.toLocalDate().isEqual(date) || start.toLocalDate().isEqual(date)
      ),
    [employee?.plannedAttendances, date]
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
          (plannedAttendances?.reduce(
            (prev, { start, end }) => prev + (end.timestamp - start.timestamp),
            0
          ) ?? 0) /
            1000 /
            60,
    [plannedAttendances, totalMinutes]
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

  if (!employee) return null

  const saveDisabled =
    !requestBody ||
    JSON.stringify(initialEditState) === JSON.stringify(editState)

  return (
    <PlainModal margin="auto" data-qa="staff-attendance-details-modal">
      <Content>
        <FixedSpaceRow alignItems="center">
          <AsyncIconButton icon={faChevronLeft} onClick={onPreviousDate} />
          <H1 noMargin>{date.formatExotic('EEEEEE d.M.yyyy')}</H1>
          <AsyncIconButton icon={faChevronRight} onClick={onNextDate} />
        </FixedSpaceRow>
        <H2>
          {employee.firstName} {employee.lastName}
        </H2>
        <H3>{i18n.unit.staffAttendance.summary}</H3>
        <ListGrid rowGap="s" labelWidth="auto">
          <LabelLike>{i18n.unit.staffAttendance.plan}</LabelLike>
          <FixedSpaceColumn data-qa="staff-attendance-summary-plan">
            {plannedAttendances && plannedAttendances.length > 0
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
              `${Math.floor(totalMinutes / 60)}:${(totalMinutes % 60)
                .toString()
                .padStart(2, '0')}${
                plannedAttendances && plannedAttendances.length > 0
                  ? ` (${Math.sign(diffPlannedTotalMinutes) === 1 ? '+' : ''}${
                      Math.sign(diffPlannedTotalMinutes) *
                      Math.floor(Math.abs(diffPlannedTotalMinutes) / 60)
                    }.${Math.abs(diffPlannedTotalMinutes) % 60})`
                  : ''
              }`
            )}
          </div>
        </ListGrid>

        <HorizontalLine slim />

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
                      items={[
                        null,
                        ...groups
                          .map((gs) => gs.map(({ id }) => id))
                          .getOrElse([])
                      ]}
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
                        groups
                          .map((gs) => gs.find(({ id }) => id === item)?.name)
                          .getOrElse(undefined) ??
                        i18n.unit.staffAttendance.noGroup
                      }
                      data-qa="attendance-group-select"
                    />
                  ) : (
                    <InlineButton
                      text={
                        groups
                          .map((gs) => gs.find((g) => g.id === groupId)?.name)
                          .getOrElse(undefined) ?? '-'
                      }
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
                  getItemLabel={(item) => i18n.unit.staffAttendance.types[item]}
                  data-qa="attendance-type-select"
                />
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
          <Button text={i18n.common.cancel} onClick={close} />
          <AsyncButton
            primary
            text={i18n.unit.staffAttendance.saveChanges}
            onClick={save}
            onSuccess={reloadStaffAttendances}
            disabled={saveDisabled}
            data-qa="save"
          />
        </ModalActions>
      </Content>
      <ModalCloseButton
        close={close}
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

function validateEditState(
  employee: EmployeeAttendance | undefined,
  date: LocalDate,
  state: EditedAttendance[]
): [SingleDayStaffAttendanceUpsert[] | undefined, ValidationError[]] {
  const errors: ValidationError[] = []
  for (let i = 0; i < state.length; i++) {
    const attendance = state[i]
    const attendanceErrors: ValidationError = {}

    if (!attendance.arrived) {
      if (i === 0) {
        const isNotOvernightAttendance =
          employee?.attendances
            .find(({ id }) => id === attendance.id)
            ?.arrived.toLocalDate()
            .isEqual(date) ?? true
        if (isNotOvernightAttendance) {
          attendanceErrors.arrived = 'required'
        }
      } else {
        attendanceErrors.arrived = 'required'
      }
    } else {
      const parsedArrived = LocalTime.tryParse(attendance.arrived, 'HH:mm')
      if (!parsedArrived) {
        attendanceErrors.arrived = 'timeFormat'
      }
    }

    if (!attendance.departed) {
      if (i !== state.length - 1) {
        attendanceErrors.departed = 'required'
      }
    } else {
      const parsedDeparted = LocalTime.tryParse(attendance.departed, 'HH:mm')
      if (!parsedDeparted) {
        attendanceErrors.departed = 'timeFormat'
      }
    }

    if (
      i > 0 &&
      !attendanceErrors.arrived &&
      !errors[i - 1].departed &&
      attendance.arrived < state[i - 1].departed
    ) {
      attendanceErrors.arrived = 'timeFormat'
    }

    errors[i] = attendanceErrors
  }

  if (errors.some((error) => Object.keys(error).length > 0)) {
    return [undefined, errors]
  }

  if (state.some(({ groupId }) => groupId === null)) {
    return [undefined, errors]
  }

  const body = state.map((att) => {
    const arrivedAsHelsinkiDateTime = () =>
      HelsinkiDateTime.fromLocal(date, LocalTime.parse(att.arrived, 'HH:mm'))
    return {
      attendanceId: att.id,
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      groupId: att.groupId!,
      arrived: !att.arrived
        ? employee?.attendances.find(({ id }) => id === att.id)?.arrived ??
          arrivedAsHelsinkiDateTime()
        : arrivedAsHelsinkiDateTime(),
      departed: !att.departed
        ? employee?.attendances.find(({ id }) => id === att.id)?.departed ??
          null
        : HelsinkiDateTime.fromLocal(
            date,
            LocalTime.parse(att.departed, 'HH:mm')
          ),
      type: att.type
    }
  })

  return [body, errors]
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
