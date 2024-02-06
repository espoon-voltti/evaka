// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import groupBy from 'lodash/groupBy'
import mapValues from 'lodash/mapValues'
import sortBy from 'lodash/sortBy'
import uniq from 'lodash/uniq'
import uniqBy from 'lodash/uniqBy'
import React, { useMemo, useState, useCallback } from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { ErrorKey } from 'lib-common/form-validation'
import {
  Attendance,
  EmployeeAttendance,
  ExternalAttendance,
  PlannedStaffAttendance,
  StaffAttendanceUpsert,
  ExternalAttendanceUpsert
} from 'lib-common/generated/api-types/attendance'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import { OperationalDay } from 'lib-common/generated/api-types/reservations'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { presentInGroup } from 'lib-common/staff-attendance'
import { UUID } from 'lib-common/types'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Table, Tbody } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { BaseProps } from 'lib-components/utils'
import { defaultMargins } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'
import { faCircleEllipsis } from 'lib-icons'

import {
  upsertExternalAttendances,
  upsertStaffAttendances
} from '../../../api/staff-attendance'
import { Translations, useTranslation } from '../../../state/i18n'
import { formatName } from '../../../utils'

import StaffAttendanceDetailsModal, {
  EditedAttendance,
  ModalAttendance,
  ModalPlannedAttendance,
  ValidationError
} from './StaffAttendanceDetailsModal'
import StaffAttendanceExternalPersonModal from './StaffAttendanceExternalPersonModal'
import {
  AttendanceTableHeader,
  DayTd,
  DayTr,
  NameTd,
  NameWrapper
} from './attendance-elements'

interface Props {
  unitId: UUID
  operationalDays: OperationalDay[]
  staffAttendances: EmployeeAttendance[]
  externalAttendances: ExternalAttendance[]
  reloadStaffAttendances: () => Promise<Result<unknown>>
  groups: DaycareGroup[]
  groupFilter: (ids: UUID[]) => boolean
  defaultGroup: UUID | null
}

type DetailsModalTarget =
  | { type: 'employee'; employeeId: UUID }
  | { type: 'external'; name: string }

interface DetailsModalConfig {
  date: LocalDate
  name: string
  attendances: ModalAttendance[]
  plannedAttendances?: ModalPlannedAttendance[]
  target: DetailsModalTarget
}

export default React.memo(function StaffAttendanceTable({
  unitId,
  staffAttendances,
  externalAttendances,
  operationalDays,
  reloadStaffAttendances,
  groups,
  groupFilter,
  defaultGroup
}: Props) {
  const { i18n } = useTranslation()
  const [detailsModalConfig, setDetailsModalConfig] =
    useState<DetailsModalConfig>()
  const closeDetailsModal = useCallback(
    () => setDetailsModalConfig(undefined),
    []
  )

  const [showExternalPersonModal, setShowExternalPersonModal] =
    useState<boolean>(false)
  const toggleAddPersonModal = useCallback(
    () => setShowExternalPersonModal((prev) => !prev),
    []
  )

  const staffRows: StaffRow[] = useMemo(
    () => getStaffRows(staffAttendances, groupFilter, i18n),
    [staffAttendances, groupFilter, i18n]
  )

  const externalRowsGroupedByName = useMemo(
    () => getExternalRows(externalAttendances, groupFilter),
    [externalAttendances, groupFilter]
  )

  const personCountSumsPerDate = useMemo(
    () =>
      computePersonCountSums(staffRows, externalRowsGroupedByName, groupFilter),
    [staffRows, externalRowsGroupedByName, groupFilter]
  )

  const modalData = useMemo(
    () =>
      detailsModalConfig
        ? computeModalAttendances(
            detailsModalConfig,
            staffAttendances,
            externalAttendances
          )
        : undefined,
    [detailsModalConfig, externalAttendances, staffAttendances]
  )

  return (
    <>
      <Table data-qa="staff-attendances-table">
        <AttendanceTableHeader
          operationalDays={operationalDays}
          nameColumnLabel={i18n.unit.staffAttendance.staffName}
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
              plannedAttendances={row.plannedAttendances}
              groupFilter={groupFilter}
              openDetails={(date: LocalDate) => {
                setDetailsModalConfig({
                  date,
                  name: row.name,
                  attendances: row.attendances,
                  plannedAttendances: row.plannedAttendances,
                  target: { type: 'employee', employeeId: row.employeeId }
                })
              }}
            />
          ))}
          {externalRowsGroupedByName.map((row, index) => (
            <AttendanceRow
              key={`${row.name}-${index}`}
              rowIndex={staffRows.length + index}
              isPositiveOccupancyCoefficient={
                row.attendances[0].occupancyCoefficient > 0
              }
              name={row.name}
              operationalDays={operationalDays}
              attendances={row.attendances}
              groupFilter={groupFilter}
              openDetails={(date: LocalDate) => {
                setDetailsModalConfig({
                  date,
                  name: row.name,
                  attendances: row.attendances,
                  target: { type: 'external', name: row.name }
                })
              }}
            />
          ))}
        </Tbody>
        <tfoot>
          <BottomSumTr>
            <BottomSumTd>{i18n.unit.staffAttendance.personCount}</BottomSumTd>
            {operationalDays.map(({ date }) => (
              <BottomSumTd
                centered
                key={date.toString()}
                data-qa="person-count-sum"
              >
                {personCountSumsPerDate[date.toString()] ?? '–'}{' '}
                {i18n.unit.staffAttendance.personCountAbbr}
              </BottomSumTd>
            ))}
            <BottomSumTd />
          </BottomSumTr>
        </tfoot>
      </Table>
      {groups.length > 0 ? (
        <AddButton
          text={i18n.unit.staffAttendance.addPerson}
          aria-label={i18n.unit.staffAttendance.addPerson}
          data-qa="add-person-button"
          onClick={toggleAddPersonModal}
        />
      ) : null}
      {detailsModalConfig && modalData ? (
        <StaffAttendanceModal
          detailsModalConfig={detailsModalConfig}
          modalData={modalData}
          unitId={unitId}
          groups={groups}
          defaultGroupId={defaultGroup}
          reloadStaffAttendances={reloadStaffAttendances}
          onClose={closeDetailsModal}
        />
      ) : null}
      {showExternalPersonModal && (
        <StaffAttendanceExternalPersonModal
          onClose={toggleAddPersonModal}
          onSave={async () => {
            await reloadStaffAttendances()
            toggleAddPersonModal()
          }}
          unitId={unitId}
          groups={groups}
          defaultGroupId={defaultGroup ?? groups[0].id}
        />
      )}
    </>
  )
})

const StaffAttendanceModal = React.memo(function StaffAttendanceModal({
  detailsModalConfig,
  modalData,
  unitId,
  groups,
  defaultGroupId,
  reloadStaffAttendances,
  onClose
}: {
  detailsModalConfig: DetailsModalConfig
  modalData: {
    attendances: ModalAttendance[]
    plannedAttendances: ModalPlannedAttendance[]
  }
  unitId: string
  groups: DaycareGroup[]
  defaultGroupId: string | null
  reloadStaffAttendances: () => Promise<Result<unknown>>
  onClose: () => void
}) {
  const onSuccess = useCallback(() => {
    void reloadStaffAttendances()
    onClose()
  }, [onClose, reloadStaffAttendances])
  const { target, date } = detailsModalConfig
  const onSaveStaff = useCallback(
    (entries: StaffAttendanceUpsert[]) =>
      target.type === 'employee'
        ? upsertStaffAttendances({
            unitId,
            employeeId: target.employeeId,
            date,
            entries
          })
        : undefined,
    [date, target, unitId]
  )
  const onSaveExternal = useCallback(
    (entries: ExternalAttendanceUpsert[]) =>
      target.type === 'external'
        ? upsertExternalAttendances({
            unitId,
            name: target.name,
            date,
            entries
          })
        : undefined,
    [date, target, unitId]
  )

  return target.type === 'employee' ? (
    <StaffAttendanceDetailsModal
      isExternal={false}
      onSave={onSaveStaff}
      validate={staffAttendanceValidator(detailsModalConfig)}
      name={detailsModalConfig.name}
      date={detailsModalConfig.date}
      attendances={modalData.attendances}
      plannedAttendances={modalData.plannedAttendances}
      groups={groups}
      defaultGroupId={defaultGroupId}
      onClose={onClose}
      onSuccess={onSuccess}
    />
  ) : (
    <StaffAttendanceDetailsModal
      isExternal={true}
      onSave={onSaveExternal}
      previousStaffOccupancyEffect={
        detailsModalConfig.attendances[0] === undefined ||
        detailsModalConfig.attendances[0].occupancyCoefficient > 0
      }
      validate={externalAttendanceValidator(detailsModalConfig)}
      name={detailsModalConfig.name}
      date={detailsModalConfig.date}
      attendances={modalData.attendances}
      plannedAttendances={modalData.plannedAttendances}
      groups={groups}
      defaultGroupId={defaultGroupId}
      onClose={onClose}
      onSuccess={onSuccess}
    />
  )
})

interface StaffRow {
  employeeId: UUID
  name: string
  attendances: Attendance[]
  plannedAttendances: PlannedStaffAttendance[]
  employeeGroups: UUID[]
  currentOccupancyCoefficient: number
}

function getStaffRows(
  staffAttendances: EmployeeAttendance[],
  groupFilter: (groupIds: UUID[]) => boolean,
  i18n: Translations
): StaffRow[] {
  return sortBy(
    staffAttendances
      .map(
        (entry): StaffRow => ({
          employeeId: entry.employeeId,
          name: formatName(
            entry.firstName.split(/\s/)[0],
            entry.lastName,
            i18n,
            true
          ),
          attendances: sortBy(
            entry.attendances.filter(
              (a) => a.groupId && groupFilter([a.groupId])
            ),
            ({ departed }) => departed?.timestamp ?? Infinity
          ),
          plannedAttendances: entry.plannedAttendances,
          employeeGroups: entry.groups,
          currentOccupancyCoefficient: entry.currentOccupancyCoefficient
        })
      )
      .filter(
        (row) => groupFilter(row.employeeGroups) || row.attendances.length > 0
      ),
    (attendance) => attendance.name
  )
}

interface ExternalRow {
  name: string
  attendances: ExternalAttendance[]
}

function getExternalRows(
  externalAttendances: ExternalAttendance[],
  groupFilter: (groupIds: UUID[]) => boolean
): ExternalRow[] {
  return sortBy(
    Object.entries(groupBy(externalAttendances, (a) => a.name))
      .map(
        ([name, attendances]): ExternalRow => ({
          name,
          attendances: sortBy(
            attendances.filter((a) => groupFilter([a.groupId])),
            ({ departed }) => departed?.timestamp ?? Infinity
          )
        })
      )
      .filter((row) => row.attendances.length > 0),
    (attendance) => attendance.name
  )
}

function computePersonCountSums(
  staffRows: StaffRow[],
  externalRows: ExternalRow[],
  groupFilter: (groupIds: UUID[]) => boolean
): Record<string, number | undefined> {
  const employeeAttendanceDates = staffRows
    .flatMap(({ attendances, employeeId }) =>
      getUniqueAttendanceDates(attendances, groupFilter).map((date) => ({
        date,
        employeeId
      }))
    )
    .concat(
      externalRows.flatMap((row) =>
        getUniqueAttendanceDates(row.attendances, groupFilter).map((date) => ({
          date,
          employeeId: `external-${row.name}`
        }))
      )
    )
  return mapValues(
    groupBy(employeeAttendanceDates, ({ date }) => date.toString()),
    (rows) => uniqBy(rows, ({ employeeId }) => employeeId).length
  )
}

function getUniqueAttendanceDates(
  attendances: Attendance[] | ExternalAttendance[],
  groupFilter: (groupIds: UUID[]) => boolean
): LocalDate[] {
  return uniqBy(
    attendances
      .filter(
        ({ type, groupId }) =>
          groupId && groupFilter([groupId]) && presentInGroup(type)
      )
      // TODO: What if arrived and departed are > 1 day apart?
      .flatMap(({ departed, arrived }) =>
        [arrived.toLocalDate()].concat(departed?.toLocalDate() ?? [])
      ),
    (localDate) => localDate.date
  )
}

const BottomSumTr = styled.tr`
  background-color: ${(p) => p.theme.colors.grayscale.g4};
  font-weight: ${fontWeights.semibold};
`

const BottomSumTd = styled.td<{ centered?: boolean }>`
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  text-align: ${(p) => (p.centered ? 'center' : 'left')};
`

interface AttendanceRowProps extends BaseProps {
  rowIndex: number
  isPositiveOccupancyCoefficient: boolean
  name: string
  employeeId?: string
  operationalDays: OperationalDay[]
  attendances: Attendance[]
  plannedAttendances?: PlannedStaffAttendance[]
  groupFilter: (ids: UUID[]) => boolean
  openDetails: (date: LocalDate) => void
}

const AttendanceRow = React.memo(function AttendanceRow({
  rowIndex,
  isPositiveOccupancyCoefficient,
  name,
  employeeId,
  operationalDays,
  attendances,
  plannedAttendances,
  groupFilter,
  openDetails
}: AttendanceRowProps) {
  const { i18n } = useTranslation()
  const today = LocalDate.todayInHelsinkiTz()

  const attendanceTooltipText = (attendance: Attendance) =>
    attendance.departedAutomatically ? (
      <div>
        <div>{i18n.unit.staffAttendance.departedAutomatically}</div>
      </div>
    ) : undefined

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
      {operationalDays
        .map(({ date }) => {
          const { matchingAttendances, hasHiddenAttendances } =
            getAttendancesForGroupAndDate(attendances, groupFilter, date)
          const plannedAttendancesForToday = getPlannedAttendancesForDate(
            plannedAttendances ?? [],
            date
          )
          return {
            date,
            plannedAttendancesForToday,
            attendancesForToday: matchingAttendances,
            hasHiddenAttendances,
            allowDetailsModal: date.isEqualOrBefore(today)
          }
        })
        .map(
          ({
            date,
            attendancesForToday,
            hasHiddenAttendances,
            plannedAttendancesForToday,
            allowDetailsModal
          }) => (
            <DayTd
              key={date.formatIso()}
              className={classNames({ 'is-today': date.isToday() })}
              partialRow={false}
              rowIndex={rowIndex}
              data-qa={`day-cell-${employeeId ?? ''}-${date.formatIso()}`}
            >
              <DayCell data-qa={`attendance-${date.formatIso()}-${rowIndex}`}>
                <PlannedAttendanceTimes data-qa="planned-attendance-day">
                  {plannedAttendancesForToday.length > 0 ? (
                    plannedAttendancesForToday.map((plannedAttendance, i) => (
                      <AttendanceCell key={i}>
                        <AttendanceTime data-qa="planned-attendance-start">
                          {renderTime(plannedAttendance.start, date)}
                        </AttendanceTime>
                        <AttendanceTime data-qa="planned-attendance-end">
                          {renderTime(plannedAttendance.end, date)}
                        </AttendanceTime>
                      </AttendanceCell>
                    ))
                  ) : (
                    <AttendanceCell>
                      <AttendanceTime data-qa="planned-arrival-time">
                        {renderTime(null, date)}
                      </AttendanceTime>
                      <AttendanceTime data-qa="planned-departure-time">
                        {renderTime(null, date)}
                      </AttendanceTime>
                    </AttendanceCell>
                  )}
                </PlannedAttendanceTimes>
                <AttendanceTimes data-qa="attendance-day">
                  {attendancesForToday.length > 0 ? (
                    attendancesForToday.map((attendance, i) => {
                      const tooltip = attendanceTooltipText(attendance)
                      return (
                        <Tooltip
                          key={i}
                          tooltip={tooltip}
                          data-qa="attendance-tooltip"
                        >
                          <AttendanceCell>
                            <AttendanceTime data-qa="arrival-time">
                              {renderTime(attendance.arrived, date)}
                            </AttendanceTime>
                            <AttendanceTime data-qa="departure-time">
                              {renderTime(attendance.departed, date)}
                              {tooltip ? `(*)` : ''}
                            </AttendanceTime>
                          </AttendanceCell>
                        </Tooltip>
                      )
                    })
                  ) : (
                    <AttendanceCell>
                      <AttendanceTime>{renderTime(null, date)}</AttendanceTime>
                      <AttendanceTime>{renderTime(null, date)}</AttendanceTime>
                    </AttendanceCell>
                  )}
                </AttendanceTimes>
                {allowDetailsModal && (
                  <DetailsToggle showAlways={hasHiddenAttendances}>
                    <IconButton
                      icon={faCircleEllipsis}
                      onClick={() => openDetails(date)}
                      data-qa="open-details"
                      aria-label={i18n.common.open}
                    />
                  </DetailsToggle>
                )}
              </DayCell>
            </DayTd>
          )
        )}
    </DayTr>
  )
})

function getAttendancesForGroupAndDate(
  attendances: Attendance[],
  groupFilter: (ids: UUID[]) => boolean,
  date: LocalDate
): { matchingAttendances: Attendance[]; hasHiddenAttendances: boolean } {
  const attendancesForDate = attendances.filter(
    (a) =>
      a.arrived.toLocalDate().isEqual(date) ||
      (a.departed &&
        new DateRange(
          a.arrived.toLocalDate(),
          a.departed.toLocalDate()
        ).includes(date))
  )

  const presentAttendances = attendancesForDate.filter(
    ({ groupId, type }) =>
      groupId && groupFilter([groupId]) && presentInGroup(type)
  )
  return {
    matchingAttendances: presentAttendances,
    hasHiddenAttendances: presentAttendances.length < attendancesForDate.length
  }
}

function getPlannedAttendancesForDate(
  plannedAttendances: PlannedStaffAttendance[],
  date: LocalDate
): PlannedStaffAttendance[] {
  return (plannedAttendances ?? []).filter(
    (plannedAttendance) =>
      plannedAttendance.start.toLocalDate().isEqual(date) ||
      plannedAttendance.end.toLocalDate().isEqual(date)
  )
}

function renderTime(
  timestamp: HelsinkiDateTime | null,
  today: LocalDate
): string {
  if (timestamp && !timestamp.toLocalDate().isEqual(today)) return '→'
  if (timestamp === null) return '–'
  return timestamp.toLocalTime().format()
}

function computeModalAttendances(
  detailsModalConfig: DetailsModalConfig,
  staffAttendances: EmployeeAttendance[],
  externalAttendances: ExternalAttendance[]
): {
  attendances: ModalAttendance[]
  plannedAttendances: ModalPlannedAttendance[]
} {
  const startOfDay = HelsinkiDateTime.fromLocal(
    detailsModalConfig.date,
    LocalTime.of(0, 0)
  )
  const endOfDay = HelsinkiDateTime.fromLocal(
    detailsModalConfig.date.addDays(1),
    LocalTime.of(0, 0)
  )
  if (detailsModalConfig.target.type === 'employee') {
    const employeeId = detailsModalConfig.target.employeeId
    const combined: EmployeeAttendance[] = staffAttendances.filter(
      (attendance) => attendance.employeeId === employeeId
    )
    const attendances: ModalAttendance[] = uniqBy(
      combined
        .flatMap((employee) => employee.attendances)
        .filter(
          (attendance) =>
            attendance.arrived <= endOfDay &&
            (attendance.departed === null || startOfDay <= attendance.departed)
        ),
      (attendance) => attendance.id
    )
    const plannedAttendances: ModalPlannedAttendance[] = uniq(
      combined.flatMap((employee) => employee.plannedAttendances)
    )
      .filter(({ start, end }) => start <= endOfDay && startOfDay <= end)
      .map(({ start, end }) => ({ start, end }))
    return { attendances, plannedAttendances }
  } else {
    const name = detailsModalConfig.target.name
    const attendances: ModalAttendance[] = externalAttendances.filter(
      (attendance) =>
        attendance.name === name &&
        attendance.arrived <= endOfDay &&
        (attendance.departed === null || startOfDay <= attendance.departed)
    )
    return { attendances, plannedAttendances: [] }
  }
}

type ValidatorConfig = Pick<DetailsModalConfig, 'date' | 'attendances'>

export const staffAttendanceValidator =
  (config: ValidatorConfig) =>
  (
    state: EditedAttendance[]
  ): [undefined | StaffAttendanceUpsert[], ValidationError[]] => {
    const body: (StaffAttendanceUpsert | undefined)[] = []
    const errors: ValidationError[] = []
    for (let i = 0; i < state.length; i++) {
      const item = state[i]
      const existing = config.attendances.find((a) => a.id === item.id)
      const isFirstAttendance = i === 0
      const isLastAttendance = i === state.length - 1

      const [arrived, arrivedError] = validateArrived(
        config,
        item,
        existing,
        isFirstAttendance,
        body[i - 1]?.departed
      )
      const [departed, departedError] = validateDeparted(
        config,
        item,
        isLastAttendance,
        arrived
      )
      const [groupId, groupIdError] = validateGroupId(item)

      if (
        arrived !== undefined &&
        departed !== undefined &&
        groupId !== undefined
      ) {
        body[i] = {
          id: item.id,
          type: item.type,
          arrived,
          departed,
          groupId
        }
      }

      errors[i] = {
        arrived: arrivedError,
        departed: departedError,
        groupId: groupIdError
      }
    }

    const a = body.filter((item): item is StaffAttendanceUpsert => !!item)
    if (a.length === state.length) {
      return [a, errors]
    }

    return [undefined, errors]
  }

export const externalAttendanceValidator =
  (config: ValidatorConfig) =>
  (
    state: EditedAttendance[]
  ): [undefined | ExternalAttendanceUpsert[], ValidationError[]] => {
    const body: (ExternalAttendanceUpsert | undefined)[] = []
    const errors: ValidationError[] = []

    for (let i = 0; i < state.length; i++) {
      const item = state[i]
      const existing = config.attendances.find((a) => a.id === item.id)
      const isFirstAttendance = i === 0
      const isLastAttendance = i === state.length - 1

      const [arrived, arrivedError] = validateArrived(
        config,
        item,
        existing,
        isFirstAttendance,
        body[i - 1]?.departed
      )
      const [departed, departedError] = validateDeparted(
        config,
        item,
        isLastAttendance,
        arrived
      )
      const [groupId, groupIdError] = item.groupId
        ? [item.groupId, undefined]
        : [undefined, 'required' as const]

      if (
        arrived !== undefined &&
        departed !== undefined &&
        groupId !== undefined
      ) {
        body[i] = {
          ...item,
          arrived,
          departed,
          groupId
        }
      }

      errors[i] = {
        arrived: arrivedError,
        departed: departedError,
        groupId: groupIdError
      }
    }

    const a = body.filter((item): item is ExternalAttendanceUpsert => !!item)
    if (a.length === state.length) {
      return [a, errors]
    }

    return [undefined, errors]
  }
const validateArrived = (
  config: ValidatorConfig,
  item: EditedAttendance,
  existing: ModalAttendance | undefined,
  isFirstAttendance: boolean,
  previousDeparted: HelsinkiDateTime | null | undefined
): [undefined, ErrorKey] | [HelsinkiDateTime, undefined] => {
  const parsedArrived = item.arrived
    ? LocalTime.tryParse(item.arrived)
    : undefined

  const isOvernightAttendance =
    existing && existing.arrived.toLocalDate().isBefore(config.date)

  // If there is an open attendance (arrival without departure), check that only departure can be set
  if (
    isFirstAttendance &&
    isOvernightAttendance &&
    existing.departed === null
  ) {
    if (!item.departed || item.arrived) return [undefined, 'openAttendance']
  }

  if (!item.arrived && isFirstAttendance && isOvernightAttendance) {
    return [existing.arrived, undefined]
  }

  if (!item.arrived) {
    return [undefined, 'required']
  }

  if (!parsedArrived) {
    return [undefined, 'timeFormat']
  }

  const arrived = HelsinkiDateTime.fromLocal(config.date, parsedArrived)
  if (previousDeparted && arrived.isBefore(previousDeparted)) {
    return [undefined, 'timeFormat']
  }

  return [arrived, undefined]
}
const validateDeparted = (
  config: ValidatorConfig,
  item: EditedAttendance,
  isLastAttendance: boolean,
  arrived: HelsinkiDateTime | undefined
): [undefined, ErrorKey] | [HelsinkiDateTime | null, undefined] => {
  const parsedDeparted = item.departed
    ? LocalTime.tryParse(item.departed)
    : undefined

  if (!item.departed && isLastAttendance && config.date.isToday()) {
    return [null, undefined]
  }

  if (!item.departed) {
    return [undefined, 'required']
  }

  if (!parsedDeparted) {
    return [undefined, 'timeFormat']
  }

  const departed = HelsinkiDateTime.fromLocal(config.date, parsedDeparted)
  if (arrived && arrived.isEqual(departed)) {
    return [undefined, 'timeFormat']
  }

  if (arrived && departed.isBefore(arrived)) {
    if (isLastAttendance) {
      return [
        HelsinkiDateTime.fromLocal(
          departed.toLocalDate().addDays(1),
          departed.toLocalTime()
        ),
        undefined
      ]
    }
    return [undefined, 'timeFormat']
  }

  return [departed, undefined]
}

const validateGroupId = (
  item: EditedAttendance
): [undefined, ErrorKey] | [string | null, undefined] => {
  if (presentInGroup(item.type) && item.groupId === null) {
    return [undefined, 'required']
  }

  return [item.groupId, undefined]
}

const DetailsToggle = styled.div<{ showAlways: boolean }>`
  display: flex;
  align-items: center;
  padding: ${defaultMargins.xxs};
  margin-left: -${defaultMargins.s};
  visibility: ${({ showAlways }) => (showAlways ? 'visible' : 'hidden')};
  position: absolute;
  bottom: 0;
  right: 0;
  margin-bottom: 3px;
`

const DayCell = styled.div`
  display: flex;
  flex-direction: column;
  position: relative;

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
  background-color: ${colors.grayscale.g4};
  width: 100%;
  padding: 0 23px 0 0;
`

const PlannedAttendanceTimes = styled.div`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  padding: 0 23px 0 0;
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
