// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import groupBy from 'lodash/groupBy'
import mapValues from 'lodash/mapValues'
import maxBy from 'lodash/maxBy'
import minBy from 'lodash/minBy'
import sortBy from 'lodash/sortBy'
import uniq from 'lodash/uniq'
import uniqBy from 'lodash/uniqBy'
import React, { useMemo, useState, useCallback } from 'react'
import styled from 'styled-components'

import {
  getStaffAttendances,
  upsertExternalAttendances,
  upsertStaffAttendances
} from 'employee-frontend/api/staff-attendance'
import { getUnitAttendanceReservations } from 'employee-frontend/api/unit'
import { Loading, Result, Success } from 'lib-common/api'
import {
  OperationalDay,
  UnitAttendanceReservations
} from 'lib-common/api-types/reservations'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  Attendance,
  EmployeeAttendance,
  ExternalAttendance,
  StaffAttendanceResponse,
  PlannedStaffAttendance,
  StaffAttendanceUpsert
} from 'lib-common/generated/api-types/attendance'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
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

import { Translations, useTranslation } from '../../../state/i18n'
import { formatName } from '../../../utils'

import StaffAttendanceDetailsModal, {
  ModalAttendance,
  ModalPlannedAttendance
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
  groupFilter: (id: UUID) => boolean
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

  const [modalStaffAttendance, setModalStaffAttendance] = useState<
    Result<StaffAttendanceResponse>
  >(Loading.of())
  const [modalAttendance, setModalAttendance] = useState<
    Result<UnitAttendanceReservations>
  >(Loading.of())
  const loadModalStaffAttendance = useRestApi(
    getStaffAttendances,
    setModalStaffAttendance
  )
  const loadModalAttendance = useRestApi(
    getUnitAttendanceReservations,
    setModalAttendance
  )

  const modalOperationalDays = useMemo(
    () => modalAttendance.getOrElse(undefined)?.operationalDays,
    [modalAttendance]
  )

  const changeDate = useCallback(
    async (getNearestDay: GetNearestDayFn, newStartOfWeek: LocalDate) => {
      if (!detailsModalConfig) return Loading.of()

      const nearestNextDate =
        (modalOperationalDays &&
          getNearestDay(modalOperationalDays, detailsModalConfig.date)) ??
        getNearestDay(operationalDays, detailsModalConfig.date)

      if (!nearestNextDate) {
        const nextWeekRange = new FiniteDateRange(
          newStartOfWeek,
          newStartOfWeek.addDays(6)
        )

        const [, attendance] = await Promise.all([
          loadModalStaffAttendance(unitId, nextWeekRange),
          loadModalAttendance(unitId, nextWeekRange)
        ])

        const newOperationalDays =
          attendance.getOrElse(undefined)?.operationalDays

        if (!newOperationalDays) return Loading.of()

        const newPreviousDay = getNearestDay(
          newOperationalDays,
          detailsModalConfig.date
        )

        if (!newPreviousDay) return Loading.of()

        setDetailsModalConfig({
          ...detailsModalConfig,
          date: newPreviousDay.date
        })
        return Success.of()
      }

      setDetailsModalConfig({
        ...detailsModalConfig,
        date: nearestNextDate.date
      })
      return Success.of()
    },
    [
      detailsModalConfig,
      loadModalAttendance,
      loadModalStaffAttendance,
      modalOperationalDays,
      operationalDays,
      unitId
    ]
  )

  const staffRows: StaffRow[] = useMemo(
    () => getStaffRows(staffAttendances, i18n),
    [i18n, staffAttendances]
  )

  const externalRowsGroupedByName = useMemo(
    () => getExternalRows(externalAttendances),
    [externalAttendances]
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
            externalAttendances,
            modalStaffAttendance
          )
        : undefined,
    [
      detailsModalConfig,
      externalAttendances,
      staffAttendances,
      modalStaffAttendance
    ]
  )

  const handleModalSave = useCallback(
    (entries: StaffAttendanceUpsert[]) => {
      if (!detailsModalConfig) return Promise.reject()
      if (detailsModalConfig.target.type === 'employee') {
        return upsertStaffAttendances({
          unitId,
          employeeId: detailsModalConfig.target.employeeId,
          date: detailsModalConfig.date,
          entries
        })
      } else {
        return upsertExternalAttendances({
          unitId,
          name: detailsModalConfig.target.name,
          date: detailsModalConfig.date,
          entries
        })
      }
    },
    [detailsModalConfig, unitId]
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
        <StaffAttendanceDetailsModal
          name={detailsModalConfig.name}
          date={detailsModalConfig.date}
          attendances={modalData.attendances}
          plannedAttendances={modalData.plannedAttendances}
          isExternal={detailsModalConfig.target.type === 'external'}
          onSave={handleModalSave}
          onClose={closeDetailsModal}
          onSuccess={() => {
            void reloadStaffAttendances()

            if (
              !operationalDays.some(({ date }) =>
                date.isEqual(detailsModalConfig.date)
              )
            ) {
              const startOfWeek = detailsModalConfig.date.startOfWeek()
              void loadModalStaffAttendance(
                unitId,
                new FiniteDateRange(startOfWeek, startOfWeek.addDays(6))
              )
            }
          }}
          groups={groups}
          onPreviousDate={() =>
            changeDate(
              getNearestPreviousDay,
              detailsModalConfig.date.startOfWeek().subWeeks(1)
            )
          }
          onNextDate={() =>
            changeDate(
              getNearestNextDay,
              detailsModalConfig.date.startOfWeek().addWeeks(1)
            )
          }
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

interface StaffRow {
  employeeId: UUID
  name: string
  attendances: Attendance[]
  plannedAttendances: PlannedStaffAttendance[]
  currentOccupancyCoefficient: number
}

function getStaffRows(
  staffAttendances: EmployeeAttendance[],
  i18n: Translations
): StaffRow[] {
  return sortBy(
    staffAttendances.map(
      (attendance): StaffRow => ({
        employeeId: attendance.employeeId,
        name: formatName(
          attendance.firstName.split(/\s/)[0],
          attendance.lastName,
          i18n,
          true
        ),
        attendances: sortBy(
          attendance.attendances,
          ({ departed }) => departed?.timestamp ?? Infinity
        ),
        plannedAttendances: attendance.plannedAttendances,
        currentOccupancyCoefficient: attendance.currentOccupancyCoefficient
      })
    ),
    (attendance) => attendance.name
  )
}

interface ExternalRow {
  name: string
  attendances: ExternalAttendance[]
}

function getExternalRows(
  externalAttendances: ExternalAttendance[]
): ExternalRow[] {
  return sortBy(
    Object.entries(groupBy(externalAttendances, (a) => a.name)).map(
      ([name, attendances]): ExternalRow => ({
        name,
        attendances: sortBy(
          attendances,
          ({ departed }) => departed?.timestamp ?? Infinity
        )
      })
    ),
    (attendance) => attendance.name
  )
}

function computePersonCountSums(
  staffRows: StaffRow[],
  externalRows: ExternalRow[],
  groupFilter: (groupId: UUID) => boolean
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
  groupFilter: (groupId: UUID) => boolean
): LocalDate[] {
  return uniqBy(
    attendances
      .filter(
        ({ type, groupId }) =>
          type !== 'OTHER_WORK' && type !== 'TRAINING' && groupFilter(groupId)
      )
      // TODO: What if arrived and departed are > 1 day apart?
      .flatMap(({ departed, arrived }) =>
        [arrived.toLocalDate()].concat(departed?.toLocalDate() ?? [])
      ),
    (localDate) => localDate.date
  )
}

type GetNearestDayFn = (
  days: OperationalDay[],
  targetDate: LocalDate
) => OperationalDay | undefined

const getNearestPreviousDay: GetNearestDayFn = (days, targetDate) =>
  maxBy(
    days.filter(({ date }) => date.isBefore(targetDate)),
    ({ date }) => date.differenceInDays(targetDate)
  )

const getNearestNextDay: GetNearestDayFn = (days, targetDate) =>
  minBy(
    days.filter(({ date }) => date.isAfter(targetDate)),
    ({ date }) => date.differenceInDays(targetDate)
  )

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
  groupFilter: (id: UUID) => boolean
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
            hasHiddenAttendances
          }
        })
        .map(
          ({
            date,
            attendancesForToday,
            hasHiddenAttendances,
            plannedAttendancesForToday
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
                    attendancesForToday.map((attendance, i) => (
                      <AttendanceCell key={i}>
                        <AttendanceTime data-qa="arrival-time">
                          {renderTime(attendance.arrived, date)}
                        </AttendanceTime>
                        <AttendanceTime data-qa="departure-time">
                          {renderTime(attendance.departed, date)}
                        </AttendanceTime>
                      </AttendanceCell>
                    ))
                  ) : (
                    <AttendanceCell>
                      <AttendanceTime>{renderTime(null, date)}</AttendanceTime>
                      <AttendanceTime>{renderTime(null, date)}</AttendanceTime>
                    </AttendanceCell>
                  )}
                </AttendanceTimes>
                <DetailsToggle showAlways={hasHiddenAttendances}>
                  <IconButton
                    icon={faCircleEllipsis}
                    onClick={() => openDetails(date)}
                    data-qa="open-details"
                    aria-label={i18n.common.open}
                  />
                </DetailsToggle>
              </DayCell>
            </DayTd>
          )
        )}
    </DayTr>
  )
})

function getAttendancesForGroupAndDate(
  attendances: Attendance[],
  groupFilter: (id: UUID) => boolean,
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
  const matchingAttendances = attendancesForDate.filter(
    ({ groupId, type }) =>
      (groupId === undefined || groupFilter(groupId)) &&
      (type === undefined || type === 'PRESENT')
  )
  return {
    matchingAttendances,
    hasHiddenAttendances: matchingAttendances.length < attendancesForDate.length
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
  externalAttendances: ExternalAttendance[],
  modalStaffAttendance: Result<StaffAttendanceResponse>
): {
  attendances: ModalAttendance[]
  plannedAttendances: ModalPlannedAttendance[]
} {
  if (detailsModalConfig.target.type === 'employee') {
    const employeeId = detailsModalConfig.target.employeeId
    const combined: EmployeeAttendance[] = staffAttendances
      .concat(modalStaffAttendance.getOrElse(undefined)?.staff ?? [])
      .filter((attendance) => attendance.employeeId === employeeId)
    const attendances: ModalAttendance[] = uniqBy(
      combined.flatMap((employee) => employee.attendances),
      (attendance) => attendance.id
    )
    const plannedAttendances: ModalPlannedAttendance[] = uniq(
      combined.flatMap((employee) => employee.plannedAttendances)
    ).map((attendance) => ({
      start: attendance.start,
      end: attendance.end
    }))
    return { attendances, plannedAttendances }
  } else {
    const name = detailsModalConfig.target.name
    const attendances: ModalAttendance[] = externalAttendances
      .concat(modalStaffAttendance.getOrElse(undefined)?.extraAttendances ?? [])
      .filter((attendance) => attendance.name === name)
    return { attendances, plannedAttendances: [] }
  }
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
