// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import sortBy from 'lodash/sortBy'
import React, { useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
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
import {
  BoundForm,
  useForm,
  useFormElems,
  useFormFields
} from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import {
  GroupInfo,
  StaffAttendanceType,
  StaffAttendanceUpsert,
  StaffMember,
  StaffMemberAttendance,
  staffAttendanceTypes
} from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRequiredParams from 'lib-common/useRequiredParams'
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import Title from 'lib-components/atoms/Title'
import Button from 'lib-components/atoms/buttons/Button'
import { ButtonLink } from 'lib-components/atoms/buttons/ButtonLink'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { EMPTY_PIN, PinInputF } from 'lib-components/molecules/PinInput'
import { H2, H3, H4, Label } from 'lib-components/typography'
import { Gap, defaultMargins } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employeeMobile'
import { faLockAlt, faTrash } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { Translations, useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'
import { UnitContext } from '../common/unit'

import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { staffAttendanceMutation, staffAttendanceQuery } from './queries'
import { toStaff } from './utils'

const typesWithoutGroup: StaffAttendanceType[] = ['TRAINING', 'OTHER_WORK']
const emptyGroupIdDomValue = ''

const staffAttendanceForm = mapped(
  validated(
    object({
      id: value<UUID | null>(),
      type: required(oneOf<StaffAttendanceType>()),
      groupEditMode: required(boolean()),
      groupId: required(oneOf<UUID | null>()),
      arrivedDate: required(localDate()),
      arrivedTime: required(localTime()),
      departedDate: required(localDate()),
      departedTime: localTime()
    }),
    (output) => {
      if (!typesWithoutGroup.includes(output.type) && output.groupId === null) {
        return { groupId: 'required' }
      }
      if (output.departedTime !== undefined) {
        const departed = HelsinkiDateTime.fromLocal(
          output.departedDate,
          output.departedTime
        )
        const arrived = HelsinkiDateTime.fromLocal(
          output.arrivedDate,
          output.arrivedTime
        )
        if (departed.isEqualOrBefore(arrived)) {
          return { departedTime: 'timeFormat' }
        }
        if (departed.isAfter(HelsinkiDateTime.now())) {
          return { departedTime: 'future' }
        }
      }
      return undefined
    }
  ),
  (output): StaffAttendanceUpsert => ({
    id: output.id,
    type: output.type,
    groupId: output.groupId,
    arrived: HelsinkiDateTime.fromLocal(output.arrivedDate, output.arrivedTime),
    departed:
      output.departedTime !== undefined
        ? HelsinkiDateTime.fromLocal(output.departedDate, output.departedTime)
        : null
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
  rows: sortBy(attendances, (attendance) => attendance.arrived).map(
    (attendance) => initialRowState(i18n, date, groups, attendance)
  )
})

const initialRowState = (
  i18n: Translations,
  date: LocalDate,
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
  departedDate: localDate.fromDate(attendance.departed?.toLocalDate() ?? date),
  departedTime: attendance.departed?.toLocalTime().format() ?? ''
})

const initialPinCodeForm = (): StateOf<typeof pinForm> => ({
  pinCode: EMPTY_PIN
})

export default React.memo(function StaffAttendanceEditPage() {
  const { unitId, employeeId } = useRequiredParams('unitId', 'employeeId')
  const { i18n } = useTranslation()
  const { unitInfoResponse } = useContext(UnitContext)
  const staffAttendanceResponse = useQueryResult(staffAttendanceQuery(unitId))
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
          unitId={unitId}
          employeeId={employeeId}
          groups={groups}
          staffMember={staffMember}
        />
      )}
    </StaffMemberPageContainer>
  ))
})

const StaffAttendancesEditor = ({
  unitId,
  employeeId,
  groups,
  staffMember
}: {
  unitId: UUID
  employeeId: UUID
  groups: GroupInfo[]
  staffMember: StaffMember
}) => {
  const { groupRoute } = useSelectedGroup()
  const { i18n, lang } = useTranslation()
  const navigate = useNavigate()
  const [date] = useState(LocalDate.todayInHelsinkiTz())
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
            <Button
              data-qa="cancel"
              onClick={() => {
                pinCode.update(() => EMPTY_PIN)
                setErrorCode(undefined)
                setMode('editor')
              }}
            >
              {i18n.common.cancel}
            </Button>
            <MutateButton
              primary
              data-qa="confirm"
              text={i18n.common.confirm}
              mutation={staffAttendanceMutation}
              onClick={() => ({
                unitId,
                request: {
                  employeeId,
                  pinCode: pinCode.value().join(''),
                  date,
                  rows: rows.value()
                }
              })}
              onSuccess={() =>
                navigate(
                  `${groupRoute}/staff-attendance/${staffMember.employeeId}`
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
        {staffMember.spanningPlan && (
          <div>
            <Label>{i18n.attendances.staff.plan}</Label>{' '}
            <>
              {staffMember.spanningPlan.start.toLocalTime().format()}–
              {staffMember.spanningPlan.end.toLocalTime().format()}
            </>
          </div>
        )}
        <div>
          <Label>{i18n.attendances.staff.realization}</Label>{' '}
          <span>
            {staffMember.attendances
              .map(
                ({ arrived, departed }) =>
                  `${arrived.toLocalTime().format()}–${
                    departed !== null ? departed.toLocalTime().format() : ''
                  }`
              )
              .join(', ')}
          </span>
        </div>
        <H3>{i18n.attendances.staff.rows}</H3>
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
            <ButtonLink
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
                  initialRowState(i18n, date, groups, {
                    id: null,
                    type: latest?.type ?? 'PRESENT',
                    groupId: latest?.groupId ?? null,
                    arrived: latest?.departed ?? HelsinkiDateTime.now(),
                    departed: null
                  })
                ])
              }}
              data-qa="add"
            >
              {i18n.attendances.staff.add}
            </ButtonLink>
          )}
      </ContentArea>
      <ContentArea opaque paddingHorizontal="s">
        <FixedSpaceRow justifyContent="space-between">
          <Button
            data-qa="cancel"
            onClick={() =>
              navigate(
                `${groupRoute}/staff-attendance/${staffMember.employeeId}`
              )
            }
          >
            {i18n.common.cancel}
          </Button>
          <Button
            primary
            data-qa="save"
            onClick={() => setMode('pin')}
            disabled={!form.isValid()}
          >
            <FontAwesomeIcon icon={faLockAlt} /> {i18n.common.saveChanges}
          </Button>
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
    departedDate,
    departedTime
  } = useFormFields(form)

  const groupIdDomValue = groupId.state.domValue
  const groupIdInputInfo = groupId.inputInfo()
  const arrivedDateValue = arrivedDate.value()
  const departedDateValue = departedDate.value()
  const dateLabelVisible = !arrivedDateValue.isEqual(departedDateValue)

  return (
    <>
      <FixedSpaceRow alignItems="baseline" data-qa="group">
        {groupEditMode.value() ? (
          <SelectF bind={groupId} data-qa="group-selector" />
        ) : (
          <>
            <InlineButton
              text={
                groupIdDomValue !== emptyGroupIdDomValue
                  ? groups.find((group) => group.id === groupIdDomValue)
                      ?.name ?? '-'
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
            {dateLabelVisible && (
              <DateLabel>{departedDate.value().format('d.M.')}</DateLabel>
            )}
            <TimeInputF
              bind={departedTime}
              readonly={!departedDateValue.isEqual(date)}
              info={
                departedTime.state === ''
                  ? { text: i18n.attendances.staff.open, status: 'warning' }
                  : departedTime.inputInfo()
              }
              data-qa="departed"
            />
          </div>
          <IconContainer>
            <IconButton
              icon={faTrash}
              onClick={onDelete}
              aria-label={i18n.common.remove}
              data-qa="remove"
            />
          </IconContainer>
        </FixedSpaceRow>
      </FixedSpaceRow>
    </>
  )
}

const DateLabel = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g70};
`

const IconContainer = styled.div`
  margin-left: ${defaultMargins.X3L};
`
