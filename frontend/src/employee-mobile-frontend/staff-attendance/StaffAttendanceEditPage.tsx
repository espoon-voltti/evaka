// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine } from 'lib-common/api'
import { localDate, localTime, string } from 'lib-common/form/fields'
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
import { nonBlank } from 'lib-common/form/validators'
import {
  GroupInfo,
  StaffAttendanceType,
  StaffAttendanceUpsert,
  StaffMember,
  StaffMemberAttendance,
  staffAttendanceTypes
} from 'lib-common/generated/api-types/attendance'
import { PinLoginStatus } from 'lib-common/generated/api-types/pairing'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Button from 'lib-components/atoms/buttons/Button'
import { ButtonLink } from 'lib-components/atoms/buttons/ButtonLink'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { PlainPinInputF } from 'lib-components/molecules/PinInput'
import { H2, H3, H4, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employeeMobile'
import { faTrash } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { Translations, useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'
import { UnitContext } from '../common/unit'

import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { staffAttendanceMutation, staffAttendanceQuery } from './queries'
import { toStaff } from './utils'

const typesWithoutGroup: StaffAttendanceType[] = ['TRAINING', 'OTHER_WORK']

const staffAttendanceRow = mapped(
  validated(
    object({
      id: value<UUID | null>(),
      type: required(oneOf<StaffAttendanceType>()),
      groupId: oneOf<UUID | null>(),
      arrivedDate: required(localDate()),
      arrivedTime: required(localTime()),
      departedDate: required(localDate()),
      departedTime: localTime()
    }),
    (output) => {
      if (!typesWithoutGroup.includes(output.type) && output.groupId === null) {
        return { groupId: 'required' }
      }
      if (
        output.departedTime !== undefined &&
        HelsinkiDateTime.fromLocal(
          output.departedDate,
          output.departedTime
        ).isEqualOrBefore(
          HelsinkiDateTime.fromLocal(output.arrivedDate, output.arrivedTime)
        )
      ) {
        return { departedTime: 'timeFormat' }
      }
      return undefined
    }
  ),
  (output): StaffAttendanceUpsert => ({
    id: output.id,
    type: output.type,
    groupId: output.groupId ?? null,
    arrived: HelsinkiDateTime.fromLocal(output.arrivedDate, output.arrivedTime),
    departed:
      output.departedTime !== undefined
        ? HelsinkiDateTime.fromLocal(output.departedDate, output.departedTime)
        : null
  })
)

const staffAttendanceRows = array(staffAttendanceRow)

const staffAttendancesForm = object({
  pinCode: validated(string(), nonBlank),
  rows: staffAttendanceRows
})

const initialFormState = (
  i18n: Translations,
  date: LocalDate,
  groups: GroupInfo[],
  attendances: StaffMemberAttendance[]
): StateOf<typeof staffAttendancesForm> => ({
  pinCode: '',
  rows: sortBy(attendances, (attendance) => attendance.arrived).map(
    (attendance) => initialRowState(i18n, date, groups, attendance)
  )
})

const initialRowState = (
  i18n: Translations,
  date: LocalDate,
  groups: GroupInfo[],
  attendance: StaffMemberAttendance | StaffAttendanceUpsert
): StateOf<typeof staffAttendanceRow> => ({
  id: attendance.id,
  type: {
    domValue: attendance.type,
    options: staffAttendanceTypes.map((type) => ({
      domValue: type,
      value: type,
      label: i18n.attendances.staffTypes[type]
    }))
  },
  groupId: {
    domValue: attendance.groupId || '',
    options: [
      { domValue: '', value: null, label: i18n.attendances.noGroup },
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

export default React.memo(function StaffAttendanceEditPage() {
  const { unitId, employeeId } = useNonNullableParams<{
    unitId: string
    employeeId: string
  }>()
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
        <StaffAttendanceEditor
          unitId={unitId}
          employeeId={employeeId}
          groups={groups}
          staffMember={staffMember}
        />
      )}
    </StaffMemberPageContainer>
  ))
})

const StaffAttendanceEditor = ({
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
  const form = useForm(
    staffAttendancesForm,
    () => initialFormState(i18n, date, groups, staffMember.attendances),
    i18n.attendances.staff.validationErrors
  )
  const { pinCode, rows } = useFormFields(form)
  const boundRows = useFormElems(rows)
  const [errorCode, setErrorCode] = useState<string>()

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
            {form.state.rows
              .map(
                ({ arrivedTime, departedTime }) =>
                  `${arrivedTime}–${departedTime}`
              )
              .join(', ')}
          </span>
        </div>
        <H3>{i18n.attendances.staff.rows}</H3>
        {boundRows.map((row, index) => (
          <StaffAttendanceRowEditor
            key={index}
            date={date}
            row={row}
            onDelete={() =>
              rows.update((prev) => [
                ...prev.slice(0, index),
                ...prev.slice(index + 1)
              ])
            }
          />
        ))}
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
          <div>
            <Label>{i18n.pin.pinCode}</Label>
            <PlainPinInputF
              bind={pinCode}
              info={
                errorCode !== undefined && errorCode in i18n.pin.status
                  ? {
                      text: i18n.pin.status[errorCode as PinLoginStatus],
                      status: 'warning'
                    }
                  : undefined
              }
            />
            <Gap size="s" />
            <MutateButton
              primary
              data-qa="save"
              text={i18n.common.saveChanges}
              mutation={staffAttendanceMutation}
              onClick={() => ({
                unitId,
                request: {
                  employeeId,
                  pinCode: pinCode.value(),
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
                  pinCode.update(() => '')
                }
              }}
              disabled={!form.isValid()}
            />
          </div>
        </FixedSpaceRow>
      </ContentArea>
    </>
  )
}

const StaffAttendanceRowEditor = ({
  date,
  row,
  onDelete
}: {
  date: LocalDate
  row: BoundForm<typeof staffAttendanceRow>
  onDelete: () => void
}) => {
  const { i18n, lang } = useTranslation()
  const { type, groupId, arrivedDate, arrivedTime, departedTime } =
    useFormFields(row)

  const isArrivedPastDate = arrivedDate.value().isBefore(date)

  return (
    <FixedSpaceRow alignItems="baseline" flexWrap="wrap">
      {featureFlags.staffAttendanceTypes && (
        <div>
          <SelectF bind={type} data-qa="type" />
        </div>
      )}
      <div>
        <SelectF bind={groupId} data-qa="group" />
      </div>
      {isArrivedPastDate && (
        <>{arrivedDate.value().format('EEEEEE d.M.', lang)}</>
      )}
      <TimeInputF bind={arrivedTime} data-qa="arrived" />
      <span>–</span>
      {isArrivedPastDate && <>{date.format('EEEEEE d.M.', lang)}</>}
      <TimeInputF
        bind={departedTime}
        info={
          departedTime.state === ''
            ? { text: 'Avoin', status: 'warning' }
            : departedTime.inputInfo()
        }
        data-qa="departed"
      />
      <IconButton
        icon={faTrash}
        onClick={onDelete}
        aria-label={i18n.common.remove}
        data-qa="remove"
      />
    </FixedSpaceRow>
  )
}
