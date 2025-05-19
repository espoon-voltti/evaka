// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import sortBy from 'lodash/sortBy'
import React, { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
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
  StaffAttendanceType,
  StaffAttendanceUpsert,
  StaffMember,
  StaffMemberAttendance
} from 'lib-common/generated/api-types/attendance'
import { staffAttendanceTypes } from 'lib-common/generated/api-types/attendance'
import type {
  EmployeeId,
  GroupId,
  StaffAttendanceRealtimeId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { EMPTY_PIN, PinInputF } from 'lib-components/molecules/PinInput'
import { H2, H3, H4, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employeeMobile'
import { faLockAlt, faTrash, faArrowLeft } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { FlexColumn } from '../common/components'
import type { Translations } from '../common/i18n'
import { useTranslation } from '../common/i18n'
import type { UnitOrGroup } from '../common/unit-or-group'
import { unitInfoQuery } from '../units/queries'

import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { staffAttendanceMutation, staffAttendanceQuery } from './queries'
import { toStaff } from './utils'

const typesWithoutGroup: StaffAttendanceType[] = ['TRAINING', 'OTHER_WORK']
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
      if (!typesWithoutGroup.includes(output.type) && output.groupId === null) {
        return { groupId: 'required' }
      }
      const departed = getDeparted(
        output.arrivedDate,
        output.arrivedTime,
        output.departedTime
      )
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
    arrived: HelsinkiDateTime.fromLocal(output.arrivedDate, output.arrivedTime),
    departed: getDeparted(
      output.arrivedDate,
      output.arrivedTime,
      output.departedTime
    ),
    hasStaffOccupancyEffect: ['TRAINING', 'OTHER_WORK'].includes(output.type)
      ? false
      : output.occupancyEffect
  })
)

const staffAttendancesForm = object({
  rows: validated(array(staffAttendanceForm), (output) =>
    output.some((row, index, array) =>
      array.some(
        (other, otherIndex) =>
          index !== otherIndex &&
          (row.departed === null ||
            !row.departed.isEqualOrBefore(other.arrived)) &&
          (other.departed === null ||
            !other.departed.isEqualOrBefore(row.arrived))
      )
    )
      ? 'overlap'
      : undefined
  )
})

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
): StateOf<typeof staffAttendancesForm> => ({
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
): StateOf<typeof staffAttendanceForm> => ({
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
  arrivedDate: localDate.fromDate(attendance.arrived.toLocalDate()),
  arrivedTime: attendance.arrived.toLocalTime().format(),
  departedTime: attendance.departed?.toLocalTime().format() ?? '',
  occupancyEffect: isUpsert(attendance)
    ? attendance.hasStaffOccupancyEffect
    : attendance.occupancyCoefficient > 0
})

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
  const combinedResult = useMemo(
    () =>
      combine(unitInfoResponse, staffAttendanceResponse).map(
        ([{ groups, staff }, { staff: staffMember }]) => ({
          groups: sortBy(groups, (group) => group.name),
          staff: staff.find((s) => s.id === employeeId),
          staffMember: staffMember.find((s) => s.employeeId === employeeId)
        })
      ),
    [employeeId, unitInfoResponse, staffAttendanceResponse]
  )

  return renderResult(combinedResult, ({ groups, staff, staffMember }) => (
    <StaffMemberPageContainer>
      {staffMember === undefined || staff === undefined ? (
        <ErrorSegment title={i18n.attendances.staff.errors.employeeNotFound} />
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
        />
      )}
    </StaffMemberPageContainer>
  ))
})

const StaffAttendancesEditor = ({
  date,
  unitOrGroup,
  employeeId,
  groups,
  staffMember
}: {
  date: LocalDate
  unitOrGroup: UnitOrGroup
  employeeId: EmployeeId
  groups: GroupInfo[]
  staffMember: StaffMember
}) => {
  const unitId = unitOrGroup.unitId
  const { i18n, lang } = useTranslation()
  const navigate = useNavigate()

  const [mode, setMode] = useState<'editor' | 'pin'>('editor')
  const form = useForm(
    staffAttendancesForm,
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
        <ContentArea opaque paddingHorizontal="s">
          <Title centered noMargin>
            {i18n.pin.pinCode}
          </Title>
          <PinInputF bind={pinCode} invalid={errorCode === 'WRONG_PIN'} />
        </ContentArea>
        <ContentArea opaque paddingHorizontal="s">
          <FixedSpaceRow justifyContent="space-between">
            <LegacyButton
              data-qa="cancel"
              onClick={() => {
                pinCode.update(() => EMPTY_PIN)
                setErrorCode(undefined)
                setMode('editor')
              }}
            >
              {i18n.common.cancel}
            </LegacyButton>
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
      <ContentArea opaque paddingHorizontal="s">
        <H4 primary>{toStaff(staffMember).name}</H4>
        <H2 primary>{date.format('EEEEEE d.M.yyyy', lang)}</H2>
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

        {continuationAttendance && (
          <>
            <ListGrid rowGap="xxs" labelWidth="auto" mobileMaxWidth="0">
              <div>
                {groups.find((g) => g.id === continuationAttendance.groupId)
                  ?.name ?? i18n.attendances.noGroup}
              </div>
              <TimesDiv>
                <FixedSpaceRow justifyContent="space-between">
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
                  justifyContent="space-between"
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
            <Gap size="xs" />
            <ContinuationInfo>
              {i18n.attendances.staff.continuationAttendance}
            </ContinuationInfo>
            <Gap size="xs" />
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
                form={row}
                onDelete={() =>
                  rows.update((prev) => [
                    ...prev.slice(0, index),
                    ...prev.slice(index + 1)
                  ])
                }
              />
              <Gap size="m" />
            </React.Fragment>
          ))
        ) : (
          <>
            <div>{i18n.attendances.staff.noRows}</div>
            <Gap size="s" />
          </>
        )}
        {rowsInputInfo && (
          <InputFieldUnderRow className={classNames(rowsInputInfo.status)}>
            <span>{rowsInputInfo.text}</span>
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
                      latest.arrived.isAfter(attendance.arrived)
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
                    arrived:
                      latest?.departed ??
                      HelsinkiDateTime.fromLocal(
                        date,
                        LocalTime.nowInHelsinkiTz()
                      ),
                    departed: null,
                    hasStaffOccupancyEffect: staffMember.occupancyEffect
                  })
                ])
              }}
              data-qa="add"
              text={i18n.attendances.staff.add}
            />
          )}
      </ContentArea>
      <ContentArea opaque paddingHorizontal="s">
        <FixedSpaceRow justifyContent="space-between">
          <LegacyButton
            data-qa="cancel"
            onClick={() =>
              navigate(
                routes.staffAttendance(unitOrGroup, staffMember.employeeId)
                  .value
              )
            }
          >
            {i18n.common.cancel}
          </LegacyButton>
          <LegacyButton
            primary
            data-qa="save"
            onClick={() => setMode('pin')}
            disabled={!form.isValid()}
          >
            <FontAwesomeIcon icon={faLockAlt} /> {i18n.common.saveChanges}
          </LegacyButton>
        </FixedSpaceRow>
      </ContentArea>
    </>
  )
}

const StaffAttendanceEditor = ({
  date,
  groups,
  form,
  onDelete
}: {
  date: LocalDate
  groups: GroupInfo[]
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

  return (
    <FlexColumn>
      <FixedSpaceRow alignItems="baseline" data-qa="group">
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
      <Gap size="s" />
      <FixedSpaceRow alignItems="end" flexWrap="wrap">
        {featureFlags.staffAttendanceTypes && (
          <div>
            <SelectF bind={type} data-qa="type" />
          </div>
        )}
        <FixedSpaceRow alignItems="end">
          <div>
            {dateLabelVisible && (
              <DateLabel>{arrivedDate.value().format('d.M.')}</DateLabel>
            )}
            <TimeInputF
              bind={arrivedTime}
              readonly={!arrivedDateValue.isEqual(date)}
              data-qa="arrived"
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
                departedTime.state === ''
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
      <Gap size="s" />
      {featureFlags.staffAttendanceTypes &&
      ['TRAINING', 'OTHER_WORK'].includes(type.value()) ? null : (
        <CheckboxF
          bind={occupancyEffect}
          label={i18n.staff.staffOccupancyEffect}
          data-qa="occupancy-effect"
        />
      )}
    </FlexColumn>
  )
}

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
