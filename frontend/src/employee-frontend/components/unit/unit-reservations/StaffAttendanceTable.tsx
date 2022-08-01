// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import groupBy from 'lodash/groupBy'
import isEqual from 'lodash/isEqual'
import partition from 'lodash/partition'
import sortBy from 'lodash/sortBy'
import React, {
  useMemo,
  useState,
  useEffect,
  useCallback,
  MutableRefObject
} from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { OperationalDay } from 'lib-common/api-types/reservations'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  Attendance,
  EmployeeAttendance,
  ExternalAttendance,
  UpsertStaffAndExternalAttendanceRequest,
  UpsertStaffAttendance
} from 'lib-common/generated/api-types/attendance'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { Table, Tbody } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { BaseProps } from 'lib-components/utils'
import { defaultMargins } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import {
  faCheck,
  faCircleEllipsis,
  faClock,
  faExclamationTriangle
} from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { formatName } from '../../../utils'
import EllipsisMenu from '../../common/EllipsisMenu'

import StaffAttendanceDetailsModal from './StaffAttendanceDetailsModal'
import {
  AttendanceTableHeader,
  DayTd,
  DayTr,
  NameTd,
  NameWrapper,
  StyledTd,
  TimeInputWithoutPadding
} from './attendance-elements'

interface Props {
  unitId: UUID
  operationalDays: OperationalDay[]
  staffAttendances: EmployeeAttendance[]
  extraAttendances: ExternalAttendance[]
  saveAttendances: (
    body: UpsertStaffAndExternalAttendanceRequest
  ) => Promise<Result<void>>
  deleteAttendances: (
    staffAttendanceIds: UUID[],
    externalStaffAttendanceIds: UUID[]
  ) => Promise<Result<void>[]>
  reloadStaffAttendances: () => Promise<void>
  groups: Result<DaycareGroup[]>
  groupFilter: (id: UUID) => boolean
  selectedGroup: UUID | null
  weekSavingFns: MutableRefObject<Map<string, () => Promise<void>>>
}

export default React.memo(function StaffAttendanceTable({
  unitId,
  staffAttendances,
  extraAttendances,
  operationalDays,
  saveAttendances,
  deleteAttendances,
  reloadStaffAttendances,
  groups,
  groupFilter,
  selectedGroup,
  weekSavingFns
}: Props) {
  const { i18n } = useTranslation()
  const [detailsModal, setDetailsModal] = useState<{
    employeeId: string
    date: LocalDate
  }>()
  const closeModal = useCallback(() => setDetailsModal(undefined), [])

  const staffRows = useMemo(
    () =>
      sortBy(
        staffAttendances.map(
          ({ firstName, lastName, attendances, ...rest }) => ({
            ...rest,
            name: formatName(firstName.split(/\s/)[0], lastName, i18n, true),
            attendances: sortBy(
              attendances,
              ({ departed }) => departed?.timestamp ?? Infinity
            )
          })
        ),
        (attendance) => attendance.name
      ),
    [i18n, staffAttendances]
  )

  const extraRowsGroupedByName = useMemo(
    () =>
      sortBy(
        Object.entries(groupBy(extraAttendances, (a) => a.name)).map(
          ([name, attendances]) => ({
            name,
            attendances: sortBy(
              attendances,
              ({ departed }) => departed?.timestamp ?? Infinity
            )
          })
        ),
        (attendance) => attendance.name
      ),
    [extraAttendances]
  )

  return (
    <>
      <Table data-qa="staff-attendances-table">
        <AttendanceTableHeader
          operationalDays={operationalDays}
          startTimeHeader={i18n.unit.staffAttendance.startTime}
          endTimeHeader={i18n.unit.staffAttendance.endTime}
        />
        <Tbody>
          {staffRows.map((row, index) => (
            <AttendanceRow
              key={`${row.employeeId}-${index}`}
              rowIndex={index}
              isPositiveOccupancyCoefficient={
                row.currentOccupancyCoefficient > 0
              }
              name={row.name}
              employeeId={row.employeeId}
              operationalDays={operationalDays}
              attendances={row.attendances}
              saveAttendances={saveAttendances}
              deleteAttendances={deleteAttendances}
              reloadStaffAttendances={reloadStaffAttendances}
              openDetails={
                featureFlags.experimental?.staffAttendanceTypes
                  ? setDetailsModal
                  : undefined
              }
              groupFilter={groupFilter}
              selectedGroup={selectedGroup}
              weekSavingFns={weekSavingFns}
              hasFutureAttendances={row.hasFutureAttendances}
            />
          ))}
          {extraRowsGroupedByName.map((row, index) => (
            <AttendanceRow
              key={`${row.name}-${index}`}
              rowIndex={index}
              isPositiveOccupancyCoefficient={
                row.attendances[0].occupancyCoefficient > 0
              }
              name={row.name}
              operationalDays={operationalDays}
              attendances={row.attendances}
              saveAttendances={saveAttendances}
              deleteAttendances={deleteAttendances}
              reloadStaffAttendances={reloadStaffAttendances}
              groupFilter={groupFilter}
              selectedGroup={selectedGroup}
              weekSavingFns={weekSavingFns}
              hasFutureAttendances={row.attendances[0].hasFutureAttendances}
            />
          ))}
        </Tbody>
      </Table>
      {detailsModal && (
        <StaffAttendanceDetailsModal
          unitId={unitId}
          employeeId={detailsModal.employeeId}
          date={detailsModal.date}
          attendances={staffAttendances}
          close={closeModal}
          reloadStaffAttendances={reloadStaffAttendances}
          groups={groups}
        />
      )}
    </>
  )
})

const OvernightAwareTimeRangeEditor = React.memo(
  function OvernightAwareTimeRangeEditor({
    attendance: { arrivalDate, departureDate, startTime, endTime },
    update,
    date,
    splitOvernight,
    errors,
    departureLocked
  }: {
    attendance: FormAttendance
    update: (v: Partial<FormAttendance>) => void
    date: LocalDate
    splitOvernight: (side: 'arrival' | 'departure') => void
    errors?: {
      startTime: boolean
      endTime: boolean
    }
    departureLocked: boolean
  }) {
    return (
      <TimeEditor data-qa="time-range-editor">
        {departureLocked ? (
          <div data-qa="departure-lock">–</div>
        ) : arrivalDate.isEqual(date) ? (
          <TimeInputWithoutPadding
            value={startTime}
            onChange={(value) => update({ startTime: value })}
            info={
              errors?.startTime ? { status: 'warning', text: '' } : undefined
            }
            data-qa="input-start-time"
          />
        ) : (
          <IconButton
            icon={faClock}
            onClick={() => splitOvernight('arrival')}
          />
        )}
        {!departureDate || departureDate.isEqual(date) ? (
          <TimeInputWithoutPadding
            value={endTime}
            onChange={(value) => update({ endTime: value })}
            info={errors?.endTime ? { status: 'warning', text: '' } : undefined}
            data-qa="input-end-time"
          />
        ) : (
          <IconButton
            icon={faClock}
            onClick={() => splitOvernight('departure')}
          />
        )}
      </TimeEditor>
    )
  }
)

const TimeEditor = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-evenly;
  gap: ${defaultMargins.xs};
  width: 100%;
`

interface AttendanceRowProps extends BaseProps {
  rowIndex: number
  isPositiveOccupancyCoefficient: boolean
  name: string
  employeeId?: string
  operationalDays: OperationalDay[]
  attendances: Attendance[]
  saveAttendances: (
    body: UpsertStaffAndExternalAttendanceRequest
  ) => Promise<Result<void>>
  deleteAttendances: (
    staffAttendanceIds: UUID[],
    externalStaffAttendanceIds: UUID[]
  ) => Promise<Result<void>[]>
  reloadStaffAttendances: () => Promise<void>
  openDetails?: (v: { employeeId: string; date: LocalDate }) => void
  groupFilter: (id: UUID) => boolean
  selectedGroup: UUID | null // for new attendances
  weekSavingFns: MutableRefObject<Map<string, () => void>>
  hasFutureAttendances: boolean
}

interface FormAttendance {
  groupId: UUID | null
  startTime: string
  endTime: string
  attendanceId: UUID | null
  /**
   * The tracking ID is used internally in this component for referencing
   * attendances not yet in the database (e.g., empty placeholders).
   * If the attendance has been added to the database, the tracking ID will
   * equal the real attendance ID.
   */
  trackingId: string
  arrivalDate: LocalDate
  departureDate: LocalDate | null
  wasDeleted?: boolean
}

let trackingIdCounter = 1
const emptyAttendanceOn = (date: LocalDate): FormAttendance => ({
  groupId: null,
  startTime: '',
  endTime: '',
  attendanceId: null,
  trackingId: (trackingIdCounter++).toString(),
  arrivalDate: date,
  departureDate: date
})

const mergeExistingFormAttendance = (
  existing: FormAttendance,
  current: FormAttendance
): FormAttendance => ({
  startTime:
    current.startTime.length > 0 ? current.startTime : existing.startTime,
  endTime: current.endTime.length > 0 ? current.endTime : existing.endTime,
  departureDate: existing.departureDate ?? current.departureDate,
  arrivalDate: existing.arrivalDate ?? current.arrivalDate,
  groupId: existing.groupId ?? current.groupId,
  trackingId: existing.trackingId ?? current.trackingId,
  attendanceId: existing.attendanceId ?? current.attendanceId
})

type QueueExecutionFn = () => Promise<unknown>

const AttendanceRow = React.memo(function AttendanceRow({
  rowIndex,
  isPositiveOccupancyCoefficient,
  name,
  employeeId,
  operationalDays,
  attendances,
  saveAttendances,
  deleteAttendances,
  reloadStaffAttendances,
  openDetails,
  groupFilter,
  selectedGroup,
  weekSavingFns,
  hasFutureAttendances: _hasFutureAttendances
}: AttendanceRowProps) {
  const { i18n } = useTranslation()

  const [editing, setEditing] = useState<boolean>(false)

  const [savingQueue, setSavingQueue] = useState<QueueExecutionFn[]>([])
  const [saveStatus, setSaveStatus] = useState<'loading' | 'idle'>('idle')

  const processQueue = useCallback(() => {
    setSavingQueue((queue) => {
      if (queue[0]) {
        setSaveStatus('loading')
        void queue[0]().then(() => processQueue())
        return queue.slice(1)
      } else {
        setSaveStatus('idle')
        return []
      }
    })
  }, [])

  const addToQueue = useCallback(
    (fn: QueueExecutionFn) => {
      setSavingQueue([...savingQueue, fn])
      if (savingQueue.length === 0) {
        processQueue()
      }
    },
    [processQueue, savingQueue]
  )

  const [form, setForm] = useState<FormAttendance[]>()
  const [hasFutureAttendances, setHasFutureAttendances] = useState(false)

  const [hasHiddenDailyAttendances, setHasHiddenDailyAttendances] =
    useState(false)

  useEffect(() => {
    // this effect converts existing attendances into editable form
    // attendances, and creates empty attendances for dates without
    // existing attendances

    const [shownDailyAttendances, hiddenDailyAttendances] = partition(
      attendances,
      ({ groupId, type }) =>
        (groupId === undefined || groupFilter(groupId)) &&
        (type === undefined || type === 'PRESENT')
    )

    setHasHiddenDailyAttendances(hiddenDailyAttendances.length > 0)

    const formAttendances = shownDailyAttendances.map(
      ({ groupId, arrived, departed, id }) => ({
        groupId,
        startTime: arrived.toLocalTime().format('HH:mm'),
        endTime: departed?.toLocalTime().format('HH:mm') ?? '',
        attendanceId: id,
        trackingId: id,
        arrivalDate: arrived.toLocalDate(),
        departureDate: departed?.toLocalDate() ?? arrived.toLocalDate()
      })
    )

    if (!editing) {
      const ranges = formAttendances.map(
        ({ arrivalDate, departureDate }) =>
          new FiniteDateRange(arrivalDate, departureDate)
      )

      const newEmptyAttendances = operationalDays
        .filter(({ date }) => !ranges.some((range) => range.includes(date)))
        .map(({ date }) => emptyAttendanceOn(date))

      setForm([...formAttendances, ...newEmptyAttendances])
      setHasFutureAttendances(_hasFutureAttendances)
      return
    }

    // the parent component may update data even after the initial load
    // and after the user may have changed values, so the existing data
    // needs to be combined with the new data (but not when view-only)
    setForm((formValue) => {
      const restoredAttendances = formAttendances.map((newAttendance) => {
        const existingFormValue =
          newAttendance.attendanceId &&
          formValue?.find(
            (value) =>
              value.attendanceId &&
              value.attendanceId === newAttendance.attendanceId
          )

        return existingFormValue && !existingFormValue.wasDeleted
          ? mergeExistingFormAttendance(existingFormValue, newAttendance)
          : newAttendance
      })

      const ranges = restoredAttendances
        .map(
          ({ arrivalDate, departureDate }) =>
            departureDate && new FiniteDateRange(arrivalDate, departureDate)
        )
        .filter((range): range is FiniteDateRange => !!range)

      const newEmptyAttendances = operationalDays
        .filter(({ date }) => !ranges.some((range) => range.includes(date)))
        .map(({ date }) => emptyAttendanceOn(date))
        .map((newAttendance) => {
          const existingFormValue = formValue?.find(
            (value) =>
              value.arrivalDate.isEqual(newAttendance.arrivalDate) &&
              !value.attendanceId
          )

          return existingFormValue && !existingFormValue.wasDeleted
            ? mergeExistingFormAttendance(existingFormValue, newAttendance)
            : newAttendance
        })

      return [...restoredAttendances, ...newEmptyAttendances]
    })
    setHasFutureAttendances(_hasFutureAttendances)
  }, [
    groupFilter,
    attendances,
    operationalDays,
    editing,
    _hasFutureAttendances
  ])

  const createAttendance = useCallback(
    (date: LocalDate, data: Partial<FormAttendance>) =>
      setForm((formValue) => [
        ...(formValue ?? []),
        {
          ...emptyAttendanceOn(date),
          ...data
        }
      ]),
    []
  )

  const renderTime = useCallback((time: string, sameDay: boolean) => {
    if (!sameDay) return '→'
    if (time === '') return '–'
    return time
  }, [])

  const updateAttendance = useCallback(
    (trackingId: string, updatedValue: Partial<FormAttendance>) =>
      setForm((formValue) =>
        formValue?.map((value) =>
          value.trackingId === trackingId
            ? {
                ...value,
                ...updatedValue
              }
            : value
        )
      ),
    []
  )

  const removeAttendances = useCallback(
    (trackingIds: string[]) => {
      setForm((formValue) => {
        const [filteredValues, removedValues] = partition(
          formValue,
          (value) => !trackingIds.includes(value.trackingId)
        )

        const deletedAttendances = removedValues.filter(
          (val): val is FormAttendance & { attendanceId: string } =>
            !!val.attendanceId
        )

        if (deletedAttendances.length > 0) {
          addToQueue(() =>
            deleteAttendances(
              deletedAttendances.map(({ attendanceId }) => attendanceId),
              []
            )
          )
        }

        return filteredValues
      })
    },
    [deleteAttendances, addToQueue]
  )

  // if there is an attendance with a missing departure time, a departure must
  // be filled in before another attendance arrival can be made
  const departureLock = useMemo(() => {
    const lockingAttendance = sortBy(form, (value) => value.arrivalDate).find(
      ({ departureDate, endTime, startTime }) =>
        !departureDate || (endTime.length === 0 && startTime.length > 0)
    )

    return (
      lockingAttendance && {
        attendance: lockingAttendance,
        since: lockingAttendance.arrivalDate.addDays(1)
      }
    )
  }, [form])

  // when there is a departure lock, there is a technically viable ending
  // departure time in the future for an open attendance but that (other)
  // date has an arrival time too, so it is not logical to use this
  // departure time; this should only happen mid-editing
  const departureLockError = useMemo(() => {
    // this memo also combines overnight attendances into one, or
    // breaks them apart if the attendance's departure time was emptied

    if (departureLock) {
      const sorted = sortBy(form, (value) => value.arrivalDate)
      const departureLockIndex = sorted.findIndex(
        ({ trackingId }) => trackingId === departureLock.attendance.trackingId
      )

      if (departureLockIndex === -1) return false

      const firstViableDepartureDay = sorted
        .slice(departureLockIndex)
        .find(({ endTime }) => endTime.length > 0)

      if (
        firstViableDepartureDay?.startTime &&
        firstViableDepartureDay.trackingId !==
          departureLock.attendance.trackingId
      ) {
        return true
      }

      if (firstViableDepartureDay?.departureDate) {
        updateAttendance(departureLock.attendance.trackingId, {
          endTime: firstViableDepartureDay.endTime,
          departureDate: firstViableDepartureDay.arrivalDate
        })

        if (form) {
          for (const overlappingDate of new FiniteDateRange(
            departureLock.attendance.arrivalDate.addDays(1),
            firstViableDepartureDay.departureDate
          ).dates()) {
            removeAttendances(
              form
                .filter(({ arrivalDate }) =>
                  arrivalDate.isEqual(overlappingDate)
                )
                .map(({ trackingId }) => trackingId)
            )
          }
        }
      } else if (
        departureLock.attendance.departureDate &&
        !departureLock.attendance.departureDate.isEqual(
          departureLock.attendance.arrivalDate
        )
      ) {
        createAttendance(departureLock.attendance.arrivalDate, {
          startTime: departureLock.attendance.startTime
        })

        if (
          departureLock.attendance.departureDate
            .subDays(1)
            .isAfter(departureLock.attendance.arrivalDate.addDays(1))
        ) {
          for (const fillerDate of new FiniteDateRange(
            departureLock.attendance.arrivalDate.addDays(1),
            departureLock.attendance.departureDate.subDays(1)
          ).dates()) {
            createAttendance(fillerDate, {})
          }
        }

        updateAttendance(departureLock.attendance.trackingId, {
          arrivalDate: departureLock.attendance.departureDate,
          startTime: ''
        })
      }
    }

    return hasFutureAttendances
  }, [
    form,
    departureLock,
    updateAttendance,
    removeAttendances,
    createAttendance,
    hasFutureAttendances
  ])

  const { formErrors, validatedForm } = useMemo(() => {
    if (!form) {
      return {
        formErrors: {} as Record<string, undefined>,
        validatedForm: undefined
      }
    }

    const hasLockError = (trackingId: string) =>
      departureLockError && departureLock?.attendance.trackingId === trackingId

    const [invalidAttendances, validatedAttendances] = partition(
      form.map((attendance) => ({
        attendance,
        parsedTimes: {
          startTime: LocalTime.tryParse(attendance.startTime, 'HH:mm'),
          endTime: LocalTime.tryParse(attendance.endTime, 'HH:mm')
        }
      })) ?? [],
      ({ parsedTimes, attendance }) =>
        // likely mid-edit with other existing future attendances
        hasLockError(attendance.trackingId) ||
        // must have a start time, multi-day attendances are collapsed onto one
        // so there are no end-time–only attendances
        !parsedTimes.startTime ||
        // in some cases, the departing time may be empty (e.g., when the day
        // isn't over yet); departure lock above handles other edge cases,
        // so only the format is validated
        (!parsedTimes.endTime && attendance.endTime.length > 0) ||
        // arrival and departure time must be linear, unless they are on different dates
        ((!attendance.departureDate ||
          attendance.arrivalDate.isEqual(attendance.departureDate)) &&
          parsedTimes.endTime &&
          parsedTimes.startTime.isAfter(parsedTimes.endTime))
    )

    // attendances that are (no longer) valid need to be deleted from
    // the database and marked as such internally
    const deletableAttendances = invalidAttendances
      .map(({ attendance }) => attendance)
      .filter(
        (attendance): attendance is FormAttendance & { attendanceId: UUID } =>
          !!attendance.attendanceId
      )

    if (deletableAttendances.length > 0) {
      addToQueue(() =>
        deleteAttendances(
          deletableAttendances.map(({ attendanceId }) => attendanceId),
          []
        )
      )

      for (const invalidAttendance of deletableAttendances) {
        updateAttendance(invalidAttendance.trackingId, {
          attendanceId: null,
          wasDeleted: true
        })
      }
    }

    return {
      validatedForm: validatedAttendances as {
        attendance: FormAttendance
        parsedTimes: {
          startTime: LocalTime
          endTime: LocalTime | null
        }
      }[],
      formErrors: Object.fromEntries(
        invalidAttendances
          .filter(
            ({ attendance }) =>
              hasLockError(attendance.trackingId) ||
              attendance.startTime.length > 0 ||
              attendance.endTime.length > 0
          )
          .map(({ attendance, parsedTimes }) => [
            attendance.trackingId,
            {
              startTime:
                (!parsedTimes.startTime ||
                  (parsedTimes.endTime &&
                    parsedTimes.startTime.isAfter(parsedTimes.endTime))) ??
                false,
              endTime:
                hasLockError(attendance.trackingId) ||
                ((!parsedTimes.endTime ||
                  (parsedTimes.startTime &&
                    parsedTimes.startTime.isAfter(parsedTimes.endTime))) ??
                  false)
            }
          ])
      )
    }
  }, [
    deleteAttendances,
    departureLock?.attendance.trackingId,
    departureLockError,
    addToQueue,
    form,
    updateAttendance
  ])

  const baseAttendances = useMemo(
    () =>
      validatedForm
        ?.map((row) => ({
          attendanceId: row.attendance.attendanceId,
          type: 'PRESENT',
          groupId: row.attendance.groupId ?? selectedGroup,
          arrived: HelsinkiDateTime.fromLocal(
            row.attendance.arrivalDate,
            row.parsedTimes.startTime
          ),
          departed:
            row.parsedTimes.endTime &&
            row.attendance.departureDate &&
            HelsinkiDateTime.fromLocal(
              row.attendance.departureDate,
              row.parsedTimes.endTime
            )
        }))
        .filter(
          (row): row is Omit<UpsertStaffAttendance, 'employeeId'> =>
            !!row.groupId
        ),
    [selectedGroup, validatedForm]
  )

  const [lastSavedForm, setLastSavedForm] = useState<{
    form: Omit<UpsertStaffAttendance, 'employeeId'>[]
    employeeId?: UUID
    name?: string
  }>()

  useEffect(() => {
    setLastSavedForm((lsf) => {
      if (
        baseAttendances &&
        (!lsf ||
          (employeeId && lsf.employeeId !== employeeId) ||
          (name && lsf.name !== name))
      ) {
        return {
          form: baseAttendances,
          employeeId,
          name
        }
      }

      return lsf
    })
  }, [employeeId, name, baseAttendances])

  const saveForm = useCallback(
    async (savePartials: boolean, isClosingEditor: boolean) => {
      if (!baseAttendances) return

      const updatedBaseAttendances = lastSavedForm
        ? baseAttendances.filter(
            (attendance) =>
              // if the user is mid-edit, partial attendances shouldn't be saved
              // because it is possible it is rejected (due to conflicts) in the
              // future, but if the user is exiting the editor/etc., we should try
              (savePartials || !!attendance.departed) &&
              // only new attendances (i.e., without an attendanceId) or changed ones
              // should be saved
              (!attendance.attendanceId ||
                !isEqual(
                  lastSavedForm.form.find(
                    (formRow) =>
                      formRow.attendanceId === attendance.attendanceId
                  ),
                  attendance
                ))
          )
        : baseAttendances

      if (updatedBaseAttendances.length === 0) {
        if (isClosingEditor) {
          await reloadStaffAttendances()
        }

        return
      }

      setLastSavedForm({ form: baseAttendances, employeeId, name })

      if (employeeId) {
        await saveAttendances({
          staffAttendances: updatedBaseAttendances.map((base) => ({
            ...base,
            employeeId
          })),
          externalAttendances: []
        })
      } else if (name) {
        await saveAttendances({
          staffAttendances: [],
          externalAttendances: updatedBaseAttendances.map((base) => ({
            ...base,
            name
          }))
        })
      }

      // reload only if closing the editor, or if new attendances were inserted
      if (
        isClosingEditor ||
        updatedBaseAttendances.some(({ attendanceId }) => !attendanceId)
      ) {
        await reloadStaffAttendances()
      }
    },
    [
      lastSavedForm,
      baseAttendances,
      employeeId,
      name,
      reloadStaffAttendances,
      saveAttendances
    ]
  )

  useEffect(() => {
    const fnMap = weekSavingFns.current

    const key = JSON.stringify([employeeId, name])

    fnMap.delete(key)

    if (editing) {
      fnMap.set(key, () => saveForm(true, false))
    }

    return () => {
      fnMap.delete(key)
    }
  }, [editing, employeeId, name, saveForm, weekSavingFns])

  const [ignoreFormError, setIgnoreFormError] = useState(false)

  useEffect(() => {
    setIgnoreFormError(false)
  }, [validatedForm])

  return (
    <DayTr data-qa={`attendance-row-${rowIndex}`}>
      <NameTd partialRow={false} rowIndex={rowIndex}>
        <FixedSpaceRow spacing="xs">
          <Tooltip
            tooltip={
              isPositiveOccupancyCoefficient
                ? i18n.unit.attendanceReservations.affectsOccupancy
                : i18n.unit.attendanceReservations.doesNotAffectOccupancy
            }
            position="bottom"
            width="large"
          >
            <RoundIcon
              content="K"
              active={isPositiveOccupancyCoefficient}
              color={colors.accents.a3emerald}
              size="s"
              data-qa={
                isPositiveOccupancyCoefficient
                  ? 'icon-occupancy-coefficient-pos'
                  : 'icon-occupancy-coefficient'
              }
            />
          </Tooltip>
          <NameWrapper data-qa="staff-attendance-name">{name}</NameWrapper>
        </FixedSpaceRow>
      </NameTd>
      {form &&
        operationalDays
          .map(({ date }) => ({
            date,
            attendances: form.filter(
              ({ arrivalDate, departureDate }) =>
                (departureDate &&
                  new DateRange(arrivalDate, departureDate).includes(date)) ||
                arrivalDate.isEqual(date)
            )
          }))
          .map(({ date, attendances }) => (
            <DayTd
              key={date.formatIso()}
              className={classNames({ 'is-today': date.isToday() })}
              partialRow={false}
              rowIndex={rowIndex}
              data-qa={`day-cell-${employeeId ?? ''}-${date.formatIso()}`}
            >
              <DayCell>
                <AttendanceTimes data-qa="attendance-day">
                  {attendances.map((attendance, i) => (
                    <AttendanceCell key={i}>
                      {editing && (attendance.groupId || selectedGroup) ? (
                        <OvernightAwareTimeRangeEditor
                          attendance={attendance}
                          date={date}
                          update={(updatedValue) => {
                            updateAttendance(
                              attendance.trackingId,
                              updatedValue
                            )
                          }}
                          splitOvernight={(side) => {
                            if (!attendance.departureDate) return

                            if (side === 'arrival') {
                              createAttendance(date, {
                                startTime: '00:00',
                                endTime: attendance.endTime,
                                arrivalDate: date,
                                departureDate: attendance.departureDate
                              })
                              updateAttendance(attendance.trackingId, {
                                departureDate: date.subDays(1),
                                endTime: '23:59'
                              })
                            } else if (side === 'departure') {
                              createAttendance(date, {
                                endTime: '23:59',
                                startTime: attendance.startTime,
                                departureDate: date,
                                arrivalDate: attendance.arrivalDate
                              })
                              updateAttendance(attendance.trackingId, {
                                arrivalDate: date.addDays(1),
                                startTime: '00:00'
                              })
                            }
                          }}
                          errors={formErrors[attendance.trackingId]}
                          departureLocked={
                            departureLock && !departureLockError
                              ? date.isEqualOrAfter(departureLock.since)
                              : false
                          }
                        />
                      ) : (
                        <>
                          <AttendanceTime data-qa="arrival-time">
                            {renderTime(
                              attendance.startTime,
                              attendance.arrivalDate.isEqual(date)
                            )}
                          </AttendanceTime>
                          <AttendanceTime data-qa="departure-time">
                            {renderTime(
                              attendance.endTime,
                              attendance.departureDate?.isEqual(date) ?? true
                            )}
                          </AttendanceTime>
                        </>
                      )}
                    </AttendanceCell>
                  ))}
                </AttendanceTimes>
                {!!employeeId && openDetails && !editing && (
                  <DetailsToggle showAlways={hasHiddenDailyAttendances}>
                    <IconButton
                      icon={faCircleEllipsis}
                      onClick={() => openDetails({ employeeId, date })}
                      data-qa={`open-details-${employeeId}-${date.formatIso()}`}
                    />
                  </DetailsToggle>
                )}
              </DayCell>
            </DayTd>
          ))}
      <StyledTd partialRow={false} rowIndex={rowIndex} rowSpan={1}>
        {editing ? (
          !ignoreFormError &&
          Object.values(formErrors).some(
            (error) => error?.endTime || error?.startTime
          ) ? (
            <FormErrorWarning onIgnore={() => setIgnoreFormError(true)} />
          ) : (
            <SaveRowButton
              loading={saveStatus === 'loading'}
              save={() =>
                addToQueue(() =>
                  saveForm(true, true).then(() => setEditing(false))
                )
              }
            />
          )
        ) : (
          <RowMenu onEdit={() => setEditing(true)} />
        )}
      </StyledTd>
    </DayTr>
  )
})

export const SaveRowButton = React.memo(function SaveRowButton({
  loading,
  save,
  'data-qa': dataQa
}: {
  loading: boolean
  save: () => void
  'data-qa'?: string
}) {
  if (loading) {
    return <SpinnerSegment size="m" margin="zero" data-qa={dataQa} />
  }

  return (
    <IconButton
      icon={faCheck}
      onClick={save}
      disabled={loading}
      data-qa="inline-editor-state-button"
    />
  )
})

export const FormErrorWarning = React.memo(function FormErrorWarning({
  onIgnore
}: {
  onIgnore: () => void
}) {
  const { i18n } = useTranslation()

  return (
    <Tooltip tooltip={i18n.unit.staffAttendance.formErrorWarning}>
      <IconButton
        icon={faExclamationTriangle}
        color={colors.status.warning}
        onClick={onIgnore}
        data-qa="form-error-warning"
      />
    </Tooltip>
  )
})

const DetailsToggle = styled.div<{ showAlways: boolean }>`
  display: flex;
  align-items: center;
  padding: ${defaultMargins.xxs};
  margin-left: -${defaultMargins.s};
  visibility: ${({ showAlways }) => (showAlways ? 'visible' : 'hidden')};
`

const DayCell = styled.div`
  display: flex;

  &:hover {
    ${DetailsToggle} {
      visibility: visible;
    }
  }
`

const AttendanceTimes = styled.div`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
`

const AttendanceCell = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-evenly;
  padding: ${defaultMargins.xs};
  gap: ${defaultMargins.xs};
`

const AttendanceTime = styled.span`
  font-weight: ${fontWeights.semibold};
  flex: 1 0 54px;
  text-align: center;
  white-space: nowrap;
`

type RowMenuProps = {
  onEdit: () => void
}
const RowMenu = React.memo(function RowMenu({ onEdit }: RowMenuProps) {
  const { i18n } = useTranslation()
  return (
    <EllipsisMenu
      items={[
        {
          id: 'edit-row',
          label: i18n.unit.attendanceReservations.editRow,
          onClick: onEdit
        }
      ]}
      data-qa="row-menu"
    />
  )
})
