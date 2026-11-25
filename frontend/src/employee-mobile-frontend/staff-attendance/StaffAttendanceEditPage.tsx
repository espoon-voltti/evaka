// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import sortBy from 'lodash/sortBy'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'
import { useLocation, useSearchParams } from 'wouter'

import { combine, Success } from 'lib-common/api'
import DateTimeRange from 'lib-common/date-time-range'
import { boolean, localDate, localTime, string } from 'lib-common/form/fields'
import {
  array,
  mapped,
  object,
  oneOf,
  required,
  validated,
  value
} from 'lib-common/form/form'
import type { BoundForm } from 'lib-common/form/hooks'
import { useForm, useFormElems, useFormFields } from 'lib-common/form/hooks'
import type { StateOf } from 'lib-common/form/types'
import type {
  GroupInfo,
  OtherUnitStaffAttendance,
  StaffAttendanceType,
  StaffAttendanceUpsert,
  StaffMember,
  StaffMemberAttendance
} from 'lib-common/generated/api-types/attendance'
import type {
  EmployeeId,
  GroupId,
  StaffAttendanceRealtimeId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import {
  attendancesOverlap,
  isDuringAttendance,
  presentInGroup
} from 'lib-common/staff-attendance'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import { EMPTY_PIN, PinInputF } from 'lib-components/molecules/PinInput'
import { H2, H3, H4, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import {
  isStaffAttendanceTypesEnabled,
  staffAttendanceTypes
} from 'lib-customizations/employeeMobile'
import { faLockAlt, faTrash, faArrowLeft } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { FlexColumn } from '../common/components'
import type { Translations } from '../common/i18n'
import { useTranslation } from '../common/i18n'
import type { UnitOrGroup } from '../common/unit-or-group'
import { unitInfoQuery } from '../units/queries'

import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import {
  staffAttendanceMutation,
  staffAttendanceQuery,
  staffAttendancesByEmployeeQuery
} from './queries'
import { toStaff } from './utils'

const typesWithoutGroup = new Set(
  staffAttendanceTypes.filter((type) => !presentInGroup(type))
)
const emptyGroupIdDomValue = ''

const getDeparted = (
  date: LocalDate,
  arrivedTime: LocalTime,
  departedTime: LocalTime | null | undefined
): HelsinkiDateTime | null => {
  if (!departedTime) return null

  const departedDate = departedTime.isBefore(arrivedTime)
    ? date.addDays(1)
    : date
  return HelsinkiDateTime.fromLocal(departedDate, departedTime)
}

const staffAttendanceForm = mapped(
  validated(
    object({
      id: value<StaffAttendanceRealtimeId | null>(),
      type: required(oneOf<StaffAttendanceType>()),
      groupEditMode: required(boolean()),
      groupId: required(oneOf<GroupId | null>()),
      arrivedDate: required(localDate()),
      arrivedTime: required(localTime()),
      departedTime: localTime(),
      occupancyEffect: required(boolean())
    }),
    (output) => {
      if (!typesWithoutGroup.has(output.type) && output.groupId === null) {
        return { groupId: 'required' }
      }
      const arrived = HelsinkiDateTime.fromLocal(
        output.arrivedDate,
        output.arrivedTime
      )
      const departed = getDeparted(
        output.arrivedDate,
        output.arrivedTime,
        output.departedTime
      )
      if (departed && !departed.isAfter(arrived)) {
        return { departedTime: 'timeFormat' }
      }
      if (departed && departed.isAfter(HelsinkiDateTime.now())) {
        return { departedTime: 'future' }
      }
      return undefined
    }
  ),
  (output): StaffAttendanceUpsert => ({
    id: output.id,
    type: output.type,
    groupId: output.groupId,
    period: new DateTimeRange(
      HelsinkiDateTime.fromLocal(output.arrivedDate, output.arrivedTime),
      getDeparted(output.arrivedDate, output.arrivedTime, output.departedTime)
    ),
    hasStaffOccupancyEffect: typesWithoutGroup.has(output.type)
      ? false
      : output.occupancyEffect
  })
)

interface OtherAttendance {
  unitName: string | null
  groupId: GroupId | null
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
}

const attendanceTimes = ({ period }: StaffAttendanceUpsert) => ({
  arrived: period.start,
  departed: period.end
})

const staffAttendancesForm = (otherAttendances: OtherAttendance[]) =>
  object({
    rows: validated(array(staffAttendanceForm), (output) =>
      output.some((row, index, array) =>
        array.some(
          (other, otherIndex) =>
            index !== otherIndex &&
            attendancesOverlap(attendanceTimes(row), attendanceTimes(other))
        )
      )
        ? 'overlap'
        : output.some((row) =>
              otherAttendances.some((other) =>
                attendancesOverlap(attendanceTimes(row), other)
              )
            )
          ? 'overlapWithOtherAttendance'
          : undefined
    )
  })

type StaffAttendancesForm = ReturnType<typeof staffAttendancesForm>

const getOtherAttendances = (
  date: LocalDate,
  attendances: StaffMemberAttendance[],
  otherUnitAttendances: OtherUnitStaffAttendance[]
): OtherAttendance[] => {
  const dayStart = date.startOfDay()
  const nextDayStart = date.addDays(1).startOfDay()
  return [
    ...otherUnitAttendances.map(({ unitName, arrived, departed }) => ({
      unitName,
      groupId: null,
      arrived,
      departed
    })),
    ...attendances
      .filter(
        ({ arrived }) =>
          arrived.isBefore(dayStart) || !arrived.isBefore(nextDayStart)
      )
      .map(({ groupId, arrived, departed }) => ({
        unitName: null,
        groupId,
        arrived,
        departed
      }))
  ]
}

const pinForm = object({
  pinCode: validated(array(string()), (output) =>
    output.join('').trim().length < 4 ? 'required' : undefined
  )
})

const initialFormState = (
  i18n: Translations,
  date: LocalDate,
  groups: GroupInfo[],
  attendances: StaffMemberAttendance[]
): StateOf<StaffAttendancesForm> => ({
  rows: sortBy(
    attendances.filter((a) => a.arrived.toLocalDate().isEqual(date)),
    (attendance) => attendance.arrived
  ).map((attendance) => initialRowState(i18n, groups, attendance))
})

function isUpsert(
  attendance: StaffMemberAttendance | StaffAttendanceUpsert
): attendance is StaffAttendanceUpsert {
  return 'hasStaffOccupancyEffect' in attendance
}

const initialRowState = (
  i18n: Translations,
  groups: GroupInfo[],
  attendance: StaffMemberAttendance | StaffAttendanceUpsert
): StateOf<typeof staffAttendanceForm> => {
  const { arrived, departed } = isUpsert(attendance)
    ? attendanceTimes(attendance)
    : attendance
  return {
    id: attendance.id,
    type: {
      domValue: attendance.type,
      options: staffAttendanceTypes.map((type) => ({
        domValue: type,
        value: type,
        label: i18n.attendances.staffTypes[type]
      }))
    },
    groupEditMode: false,
    groupId: {
      domValue: attendance.groupId || emptyGroupIdDomValue,
      options: [
        {
          domValue: emptyGroupIdDomValue,
          value: null,
          label: i18n.attendances.noGroup
        },
        ...groups.map((group) => ({
          domValue: group.id,
          value: group.id,
          label: group.name
        }))
      ]
    },
    arrivedDate: localDate.fromDate(arrived.toLocalDate()),
    arrivedTime: arrived.toLocalTime().format(),
    departedTime: departed?.toLocalTime().format() ?? '',
    occupancyEffect: isUpsert(attendance)
      ? attendance.hasStaffOccupancyEffect
      : attendance.occupancyCoefficient > 0
  }
}

const initialPinCodeForm = (): StateOf<typeof pinForm> => ({
  pinCode: EMPTY_PIN
})

export default React.memo(function StaffAttendanceEditPage({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const employeeId = useIdRouteParam<EmployeeId>('employeeId')
  const { i18n } = useTranslation()

  const [searchParams] = useSearchParams()
  const date = useMemo(
    () =>
      LocalDate.tryParseIso(searchParams.get('date') ?? '') ??
      LocalDate.todayInHelsinkiTz(),
    [searchParams]
  )

  const unitId = unitOrGroup.unitId
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))
  const staffAttendanceResponse = useQueryResult(
    staffAttendanceQuery({ unitId, startDate: date, endDate: date })
  )
  const employeeAttendancesResponse = useQueryResult(
    staffAttendancesByEmployeeQuery({
      unitId,
      employeeId,
      from: date,
      to: date.addDays(1)
    })
  )
  const combinedResult = useMemo(
    () =>
      combine(
        unitInfoResponse,
        staffAttendanceResponse,
        employeeAttendancesResponse.isFailure &&
          employeeAttendancesResponse.statusCode === 404
          ? Success.of(null)
          : employeeAttendancesResponse
      ).map(([{ groups, staff }, { staff: staffMember }, withNextDay]) => ({
        groups: sortBy(groups, (group) => group.name),
        staff: staff.find((s) => s.id === employeeId),
        staffMember: staffMember.find((s) => s.employeeId === employeeId),
        otherAttendances: withNextDay
          ? getOtherAttendances(
              date,
              withNextDay.staffMember.attendances,
              withNextDay.otherUnitAttendances
            )
          : []
      })),
    [
      date,
      employeeId,
      unitInfoResponse,
      staffAttendanceResponse,
      employeeAttendancesResponse
    ]
  )

  return renderResult(
    combinedResult,
    ({ groups, staff, staffMember, otherAttendances }, isReloading) => {
      if (isReloading) return null

      return (
        <StaffMemberPageContainer>
          {staffMember === undefined || staff === undefined ? (
            <ErrorSegment
              title={i18n.attendances.staff.errors.employeeNotFound}
            />
          ) : !staff.pinSet ? (
            <ErrorSegment title={i18n.attendances.staff.pinNotSet} />
          ) : staff.pinLocked ? (
            <ErrorSegment title={i18n.attendances.staff.pinLocked} />
          ) : (
            <StaffAttendancesEditor
              date={date}
              unitOrGroup={unitOrGroup}
              employeeId={employeeId}
              groups={groups}
              staffMember={staffMember}
              otherAttendances={otherAttendances}
            />
          )}
        </StaffMemberPageContainer>
      )
    }
  )
})

const StaffAttendancesEditor = ({
  date,
  unitOrGroup,
  employeeId,
  groups,
  staffMember,
  otherAttendances
}: {
  date: LocalDate
  unitOrGroup: UnitOrGroup
  employeeId: EmployeeId
  groups: GroupInfo[]
  staffMember: StaffMember
  otherAttendances: OtherAttendance[]
}) => {
  const unitId = unitOrGroup.unitId
  const { i18n, lang } = useTranslation()
  const [, navigate] = useLocation()

  const [mode, setMode] = useState<'editor' | 'pin'>('editor')
  const attendancesForm = useMemo(
    () => staffAttendancesForm(otherAttendances),
    [otherAttendances]
  )
  const form = useForm(
    attendancesForm,
    () => initialFormState(i18n, date, groups, staffMember.attendances),
    i18n.attendances.staff.validationErrors
  )
  const { rows } = useFormFields(form)
  const boundRows = useFormElems(rows)
  const pinCodeForm = useForm(
    pinForm,
    initialPinCodeForm,
    i18n.attendances.staff.validationErrors
  )
  const { pinCode } = useFormFields(pinCodeForm)
  const [errorCode, setErrorCode] = useState<string>()

  const rowsInputInfo = rows.inputInfo()

  const otherUnitAttendancesOnDate = useMemo(() => {
    const startOfDay = HelsinkiDateTime.fromLocal(date, LocalTime.MIN)
    const endOfDay = HelsinkiDateTime.fromLocal(date.addDays(1), LocalTime.MIN)
    return otherAttendances.filter(
      (a) =>
        a.unitName !== null &&
        a.arrived.isBefore(endOfDay) &&
        (a.departed === null || a.departed.isAfter(startOfDay))
    )
  }, [otherAttendances, date])

  const rowValues = boundRows.flatMap((row) =>
    row.isValid() ? [row.value()] : []
  )
  const nextDayStart = date.addDays(1).startOfDay()
  const overlappingAttendancesOnNextDay = otherAttendances.filter(
    (other) =>
      !other.arrived.isBefore(nextDayStart) &&
      rowValues.some((row) => attendancesOverlap(attendanceTimes(row), other))
  )

  const continuationAttendance = useMemo(
    () =>
      staffMember.attendances.find(
        (a) =>
          a.arrived.toLocalDate().isEqual(date.subDays(1)) &&
          (a.departed === null || a.departed.toLocalDate().isEqual(date))
      ),
    [staffMember, date]
  )

  if (mode === 'pin') {
    return (
      <>
        <ContentArea $opaque $paddingHorizontal="s">
          <Title centered noMargin>
            {i18n.pin.pinCode}
          </Title>
          <PinInputF bind={pinCode} invalid={errorCode === 'WRONG_PIN'} />
          {errorCode === 'overlappingAttendance' && (
            <AlertBox
              wide
              data-qa="overlapping-attendance-save-error"
              message={i18n.attendances.staff.overlappingAttendanceSaveError}
            />
          )}
        </ContentArea>
        <ContentArea $opaque $paddingHorizontal="s">
          <FixedSpaceRow $justifyContent="space-between">
            <Button
              data-qa="cancel"
              onClick={() => {
                pinCode.update(() => EMPTY_PIN)
                setErrorCode(undefined)
                setMode('editor')
              }}
              text={i18n.common.cancel}
            />
            <MutateButton
              primary
              data-qa="confirm"
              text={i18n.common.confirm}
              mutation={staffAttendanceMutation}
              onClick={() => ({
                unitId,
                body: {
                  employeeId,
                  pinCode: pinCode.value().join(''),
                  date,
                  rows: rows.value()
                }
              })}
              onSuccess={() =>
                navigate(
                  routes.staffAttendance(unitOrGroup, staffMember.employeeId)
                    .value
                )
              }
              onFailure={({ errorCode }) => {
                setErrorCode(errorCode)
                if (errorCode === 'WRONG_PIN') {
                  pinCode.update(() => EMPTY_PIN)
                }
              }}
              disabled={!pinCode.isValid()}
            />
          </FixedSpaceRow>
        </ContentArea>
      </>
    )
  }

  return (
    <>
      <ContentArea $opaque $paddingHorizontal="s">
        <H4 $primary>{toStaff(staffMember).name}</H4>
        <H2 $primary>{date.format('EEEEEE d.M.yyyy', lang)}</H2>
        <H3>{i18n.attendances.staff.summary}</H3>
        {staffMember.spanningPlans.length > 0 && (
          <div>
            <Label>{i18n.attendances.staff.plan}</Label>{' '}
            <div>
              {staffMember.spanningPlans.map((range, i) => {
                const start = range.start.toLocalDate().isEqual(date)
                  ? range.start.toLocalTime().format()
                  : range.start.format('d.M. HH:mm')
                const end = range.end.toLocalDate().isEqual(date)
                  ? range.end.toLocalTime().format()
                  : range.end.format('d.M. HH:mm')
                return <div key={`plan-${i}`}>{`${start} – ${end}`}</div>
              })}
            </div>
          </div>
        )}
        <div>
          <Label>{i18n.attendances.staff.realization}</Label>
          <div>
            {staffMember.attendances
              .map(
                ({ arrived, departed }) =>
                  `${arrived.toLocalTime().format()}–${
                    departed !== null ? departed.toLocalTime().format() : ''
                  }`
              )
              .join(', ')}
          </div>
        </div>
        <H3>{i18n.attendances.staff.rows}</H3>
        {otherUnitAttendancesOnDate.length > 0 && (
          <>
            <InfoBox
              wide
              noMargin
              data-qa="attendances-in-other-units"
              message={
                <>
                  {i18n.attendances.staff.attendancesInOtherUnits}
                  <OtherAttendanceList
                    attendances={otherUnitAttendancesOnDate}
                    groups={groups}
                    date={date}
                  />
                </>
              }
            />
            <Gap $size="s" />
          </>
        )}

        {continuationAttendance && (
          <>
            <ListGrid $rowGap="xxs" $labelWidth="auto" $mobileMaxWidth="0">
              <div>
                {groups.find((g) => g.id === continuationAttendance.groupId)
                  ?.name ?? i18n.attendances.noGroup}
              </div>
              <TimesDiv>
                <FixedSpaceRow $justifyContent="space-between">
                  <DateInfoDiv>
                    {continuationAttendance.arrived
                      .toLocalDate()
                      .format('dd.MM.')}
                  </DateInfoDiv>
                  <div />
                  <DateInfoDiv>
                    {continuationAttendance.departed
                      ?.toLocalDate()
                      ?.format('dd.MM.')}
                  </DateInfoDiv>
                </FixedSpaceRow>
              </TimesDiv>
              <div>
                {i18n.attendances.staffTypes[continuationAttendance.type]}
              </div>
              <TimesDiv>
                <FixedSpaceRow
                  data-qa="continuation-attendance"
                  $justifyContent="space-between"
                >
                  <TimeDiv>
                    {continuationAttendance.arrived.toLocalTime().format()}
                  </TimeDiv>
                  <div>–</div>
                  <TimeDiv>
                    {continuationAttendance.departed?.toLocalTime()?.format()}*
                  </TimeDiv>
                </FixedSpaceRow>
              </TimesDiv>
            </ListGrid>
            <Gap $size="xs" />
            <ContinuationInfo>
              {i18n.attendances.staff.continuationAttendance}
            </ContinuationInfo>
            <Gap $size="xs" />
            <Button
              text={i18n.attendances.staff.editContinuationAttendance}
              icon={faArrowLeft}
              appearance="link"
              onClick={() =>
                navigate(
                  routes.staffAttendanceEdit(
                    unitOrGroup,
                    staffMember.employeeId,
                    continuationAttendance?.arrived.toLocalDate()
                  ).value
                )
              }
            />
            <Gap />
          </>
        )}

        {boundRows.length > 0 ? (
          boundRows.map((row, index) => (
            <React.Fragment key={index}>
              <StaffAttendanceEditor
                date={date}
                groups={groups}
                otherAttendances={otherAttendances}
                form={row}
                onDelete={() =>
                  rows.update((prev) => [
                    ...prev.slice(0, index),
                    ...prev.slice(index + 1)
                  ])
                }
              />
              <Gap $size="m" />
            </React.Fragment>
          ))
        ) : (
          <>
            <div>{i18n.attendances.staff.noRows}</div>
            <Gap $size="s" />
          </>
        )}
        {overlappingAttendancesOnNextDay.length > 0 && (
          <>
            <AlertBox
              wide
              noMargin
              data-qa="overlapping-attendance-warning"
              message={
                <>
                  {i18n.attendances.staff.overlappingAttendanceNextDay}
                  <OtherAttendanceList
                    attendances={overlappingAttendancesOnNextDay}
                    groups={groups}
                  />
                </>
              }
            />
            <Gap $size="s" />
          </>
        )}
        {rowsInputInfo &&
          rows.validationError() !== 'overlapWithOtherAttendance' && (
            <InputFieldUnderRow className={classNames(rowsInputInfo.status)}>
              <span data-qa="rows-info">{rowsInputInfo.text}</span>
              <UnderRowStatusIcon status={rowsInputInfo.status} />
            </InputFieldUnderRow>
          )}
        {rows.isValid() &&
          rows.state.every((attendance) => attendance.departedTime !== '') && (
            <Button
              appearance="link"
              onClick={() => {
                const latest = rows
                  .value()
                  .reduce<StaffAttendanceUpsert | null>(
                    (latest, attendance) =>
                      latest !== null &&
                      latest.period.start.isAfter(attendance.period.start)
                        ? latest
                        : attendance,
                    null
                  )
                rows.update((prev) => [
                  ...prev,
                  initialRowState(i18n, groups, {
                    id: null,
                    type: latest?.type ?? 'PRESENT',
                    groupId: latest?.groupId ?? null,
                    period: new DateTimeRange(
                      latest?.period.end ??
                        HelsinkiDateTime.fromLocal(
                          date,
                          LocalTime.nowInHelsinkiTz()
                        ),
                      null
                    ),
                    hasStaffOccupancyEffect: staffMember.occupancyEffect
                  })
                ])
              }}
              data-qa="add"
              text={i18n.attendances.staff.add}
            />
          )}
      </ContentArea>
      <ContentArea $opaque $paddingHorizontal="s">
        <FixedSpaceRow $justifyContent="space-between">
          <Button
            data-qa="cancel"
            onClick={() =>
              navigate(
                routes.staffAttendance(unitOrGroup, staffMember.employeeId)
                  .value
              )
            }
            text={i18n.common.cancel}
          />
          <Button
            primary
            data-qa="save"
            onClick={() => setMode('pin')}
            disabled={!form.isValid()}
            text={i18n.common.saveChanges}
            icon={faLockAlt}
          />
        </FixedSpaceRow>
      </ContentArea>
    </>
  )
}

const StaffAttendanceEditor = ({
  date,
  groups,
  otherAttendances,
  form,
  onDelete
}: {
  date: LocalDate
  groups: GroupInfo[]
  otherAttendances: OtherAttendance[]
  form: BoundForm<typeof staffAttendanceForm>
  onDelete: () => void
}) => {
  const { i18n } = useTranslation()
  const {
    type,
    groupEditMode,
    groupId,
    arrivedDate,
    arrivedTime,
    departedTime,
    occupancyEffect
  } = useFormFields(form)

  const groupIdDomValue = groupId.state.domValue
  const groupIdInputInfo = groupId.inputInfo()
  const arrivedDateValue = arrivedDate.value()
  const departed =
    arrivedTime.isValid() && departedTime.isValid()
      ? getDeparted(arrivedDateValue, arrivedTime.value(), departedTime.value())
      : null
  const dateLabelVisible =
    departed && !departed.toLocalDate().isEqual(arrivedDateValue)

  const arrived =
    arrivedTime.isValid() && departedTime.isValid()
      ? HelsinkiDateTime.fromLocal(arrivedDateValue, arrivedTime.value())
      : null
  const overlapping = arrived
    ? otherAttendances.filter((other) =>
        attendancesOverlap({ arrived, departed }, other)
      )
    : []
  const arrivalOverlaps =
    arrived !== null &&
    overlapping.some((other) => isDuringAttendance(arrived, other))
  const overlapInfo = {
    text: i18n.attendances.staff.validationErrors.timeFormat,
    status: 'warning' as const
  }

  return (
    <FlexColumn>
      <FixedSpaceRow $alignItems="baseline" data-qa="group">
        {groupEditMode.value() ? (
          <SelectF bind={groupId} data-qa="group-selector" />
        ) : (
          <>
            <Button
              appearance="inline"
              text={
                groupIdDomValue !== emptyGroupIdDomValue
                  ? (groups.find((group) => group.id === groupIdDomValue)
                      ?.name ?? '-')
                  : i18n.attendances.noGroup
              }
              onClick={() => groupEditMode.update(() => true)}
              data-qa="group-name"
            />
            {groupIdInputInfo && (
              <InputFieldUnderRow
                className={classNames(groupIdInputInfo.status)}
              >
                <span>{groupIdInputInfo.text}</span>
                <UnderRowStatusIcon status={groupIdInputInfo.status} />
              </InputFieldUnderRow>
            )}
          </>
        )}
      </FixedSpaceRow>
      <Gap $size="s" />
      <FixedSpaceRow $alignItems="end" $flexWrap="wrap">
        {isStaffAttendanceTypesEnabled && (
          <div>
            <SelectF bind={type} data-qa="type" />
          </div>
        )}
        <FixedSpaceRow $alignItems="end">
          <div>
            {dateLabelVisible && (
              <DateLabel>{arrivedDate.value().format('d.M.')}</DateLabel>
            )}
            <TimeInputF
              bind={arrivedTime}
              readonly={!arrivedDateValue.isEqual(date)}
              data-qa="arrived"
              {...(arrivalOverlaps ? { info: overlapInfo } : {})}
            />
          </div>
          <span>–</span>
          <div>
            {departed && dateLabelVisible && (
              <DateLabel>{departed.toLocalDate().format('d.M.')}</DateLabel>
            )}
            <TimeInputF
              bind={departedTime}
              info={
                overlapping.length > 0 && !arrivalOverlaps
                  ? overlapInfo
                  : departedTime.state === ''
                    ? { text: i18n.attendances.staff.open, status: 'warning' }
                    : departedTime.inputInfo()
              }
              data-qa="departed"
            />
          </div>
          <IconContainer>
            <IconOnlyButton
              icon={faTrash}
              onClick={onDelete}
              aria-label={i18n.common.remove}
              data-qa="remove"
            />
          </IconContainer>
        </FixedSpaceRow>
      </FixedSpaceRow>
      <Gap $size="s" />
      {isStaffAttendanceTypesEnabled &&
      typesWithoutGroup.has(type.value()) ? null : (
        <CheckboxF
          bind={occupancyEffect}
          label={i18n.staff.staffOccupancyEffect}
          data-qa="occupancy-effect"
        />
      )}
    </FlexColumn>
  )
}

const OtherAttendanceList = React.memo(function OtherAttendanceList({
  attendances,
  groups,
  date
}: {
  attendances: OtherAttendance[]
  groups: GroupInfo[]
  date?: LocalDate
}) {
  const { i18n } = useTranslation()
  return (
    <OtherAttendanceUl>
      {attendances.map(({ unitName, groupId, arrived, departed }, i) => {
        const currentDate = date ?? arrived.toLocalDate()
        return (
          <li key={i}>
            {unitName ??
              groups.find((g) => g.id === groupId)?.name ??
              i18n.attendances.noGroup}
            :{' '}
            <strong>
              {arrived.toLocalDate().isBefore(currentDate)
                ? LocalTime.MIN.format()
                : arrived.toLocalTime().format()}{' '}
              –{' '}
              {departed
                ? departed.toLocalDate().isEqual(currentDate)
                  ? departed.toLocalTime().format()
                  : '→'
                : ''}
            </strong>
          </li>
        )
      })}
    </OtherAttendanceUl>
  )
})

const OtherAttendanceUl = styled.ul`
  margin: ${defaultMargins.xs} 0 0;
  padding-left: ${defaultMargins.m};
`

const DateLabel = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g70};
`

const IconContainer = styled.div`
  margin-left: ${defaultMargins.X3L};
`

const TimesDiv = styled.div`
  width: 120px;
`

const TimeDiv = styled.div`
  width: 40px;
`

const DateInfoDiv = styled(TimeDiv)`
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-weight: 600;
  font-size: 14px;
`

const ContinuationInfo = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-style: italic;
`
