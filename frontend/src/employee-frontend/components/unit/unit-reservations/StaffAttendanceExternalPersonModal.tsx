// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { Failure } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { ErrorsOf, getErrorCount } from 'lib-common/form-validation'
import { UpsertStaffAndExternalAttendanceRequest } from 'lib-common/generated/api-types/attendance'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import InputField, {
  InputFieldUnderRow
} from 'lib-components/atoms/form/InputField'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { InlineAsyncButton } from 'lib-components/employee/notes/InlineAsyncButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { fontWeights, H1, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { postStaffAndExternalAttendances } from '../../../api/staff-attendance'
import { useTranslation } from '../../../state/i18n'
import { isTimeValid } from '../../../utils/validation/validations'

type FormState = {
  date: LocalDate | null
  arrivalTime: string
  departureTime: string
  name: string
  groupId: UUID
}

const initialFormState = (groupId: UUID): FormState => ({
  date: LocalDate.todayInHelsinkiTz(),
  arrivalTime: LocalTime.nowInHelsinkiTz().format(),
  departureTime: '',
  name: '',
  groupId
})

const validateForm = (
  attendanceId: UUID | null,
  formState: FormState
):
  | [UpsertStaffAndExternalAttendanceRequest]
  | [undefined, ErrorsOf<FormState>] => {
  const errors: ErrorsOf<FormState> = {
    date: !formState.date ? 'required' : undefined,
    arrivalTime: !formState.arrivalTime
      ? 'timeRequired'
      : isTimeValid(formState.arrivalTime)
      ? undefined
      : 'timeFormat',
    departureTime: !formState.departureTime
      ? undefined
      : isTimeValid(formState.departureTime)
      ? undefined
      : 'timeFormat',
    name: formState.name.length < 3 ? 'required' : undefined,
    groupId: !formState.groupId ? 'required' : undefined
  }

  if (getErrorCount(errors) > 0 || !formState.date) {
    return [undefined, errors]
  }

  return [
    {
      externalAttendances: [
        {
          attendanceId,
          arrived: HelsinkiDateTime.fromLocal(
            formState.date,
            LocalTime.parse(formState.arrivalTime, 'HH:mm')
          ),
          departed: formState.departureTime
            ? HelsinkiDateTime.fromLocal(
                formState.date,
                LocalTime.parse(formState.departureTime, 'HH:mm')
              )
            : null,
          name: formState.name,
          groupId: formState.groupId
        }
      ],
      staffAttendances: []
    }
  ]
}

type ExternalPersonModalProps = {
  onClose: () => void
  onSave: () => void
  unitId: UUID
  groups: DaycareGroup[]
  defaultGroupId: UUID
}

export default React.memo(function StaffAttendanceExternalPersonModal({
  onClose,
  onSave,
  unitId,
  groups,
  defaultGroupId
}: ExternalPersonModalProps) {
  const { i18n, lang } = useTranslation()

  const [formData, setFormData] = useState<FormState>(
    initialFormState(defaultGroupId)
  )

  const [requestBody, errors] = useMemo(
    () => validateForm(null, formData),
    [formData]
  )

  const submit = useCallback(() => {
    if (requestBody === undefined)
      return Promise.reject(Failure.of({ message: 'validation error' }))
    return postStaffAndExternalAttendances(unitId, requestBody)
  }, [requestBody, unitId])

  const openGroups = useMemo(() => groups.filter(isGroupOpen), [groups])

  return (
    <PlainModal margin="auto" data-qa="staff-attendance-add-person-modal">
      <Content>
        <Centered>
          <IconWrapper>
            <FontAwesomeIcon icon={faPlus} />
          </IconWrapper>
          <H1 noMargin>{i18n.unit.staffAttendance.addPerson}</H1>
          {i18n.unit.staffAttendance.addPersonModal.description}
        </Centered>

        <div>
          <FieldLabel>
            {i18n.unit.staffAttendance.addPersonModal.arrival}
          </FieldLabel>
          <FixedSpaceRow>
            <DatePicker
              date={formData.date}
              locale={lang}
              onChange={(date) =>
                setFormData((prev) => ({
                  ...prev,
                  date: date
                }))
              }
              data-qa="add-person-arrival-date-picker"
              required
              info={
                errors?.date && {
                  text: i18n.validationErrors[errors.date],
                  status: 'warning'
                }
              }
            />
            <TimeInput
              value={formData.arrivalTime}
              onChange={(value) =>
                setFormData((prev) => ({
                  ...prev,
                  arrivalTime: value
                }))
              }
              data-qa="add-person-arrival-time-input"
              info={
                errors?.arrivalTime && {
                  text: i18n.validationErrors[errors.arrivalTime],
                  status: 'warning'
                }
              }
            />
            {' – '}
            <TimeInput
              value={formData.departureTime}
              onChange={(value) =>
                setFormData((prev) => ({
                  ...prev,
                  departureTime: value
                }))
              }
              data-qa="add-person-departure-time-input"
              info={
                errors?.departureTime && {
                  text: i18n.validationErrors[errors.departureTime],
                  status: 'warning'
                }
              }
            />
          </FixedSpaceRow>
        </div>
        <div>
          <FieldLabel>
            {i18n.unit.staffAttendance.addPersonModal.name}
          </FieldLabel>
          <InputField
            value={formData.name}
            placeholder={
              i18n.unit.staffAttendance.addPersonModal.namePlaceholder
            }
            onChange={(val) => setFormData((prev) => ({ ...prev, name: val }))}
            data-qa="add-person-name-input"
            required
            info={
              errors?.name && {
                text: i18n.validationErrors[errors.name],
                status: 'warning'
              }
            }
          />
        </div>

        <div>
          <FieldLabel>
            {i18n.unit.staffAttendance.addPersonModal.group}
          </FieldLabel>
          <Select
            items={openGroups}
            selectedItem={
              openGroups.find(({ id }) => id === formData.groupId) ?? null
            }
            onChange={(group) => {
              if (group !== null) {
                setFormData((prev) => ({ ...prev, groupId: group.id }))
              }
            }}
            getItemValue={({ id }) => id}
            getItemLabel={({ name }) => name}
            placeholder={i18n.common.select}
            data-qa="add-person-group-select"
          />
          {errors?.groupId && (
            <InputFieldUnderRow className={classNames('warning')}>
              <span>{i18n.validationErrors[errors.groupId]}</span>
              <StatusIcon status="warning" />
            </InputFieldUnderRow>
          )}
        </div>

        <Gap size="xs" />
        <FixedSpaceRow justifyContent="space-between">
          <InlineButton
            text={i18n.common.cancel}
            data-qa="add-person-cancel-btn"
            onClick={onClose}
          />
          <InlineAsyncButton
            text={i18n.unit.staffAttendance.addPerson}
            data-qa="add-person-save-btn"
            onClick={submit}
            onSuccess={onSave}
          />
        </FixedSpaceRow>
      </Content>
    </PlainModal>
  )
})

function isGroupOpen(group: DaycareGroup) {
  return new DateRange(group.startDate, group.endDate).includes(
    LocalDate.todayInHelsinkiTz()
  )
}

const Content = styled.div`
  padding: ${defaultMargins.XL};
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.s};
`
const Centered = styled(FixedSpaceColumn)`
  align-self: center;
  text-align: center;
  gap: ${defaultMargins.L};
`
const IconWrapper = styled.div`
  align-self: center;
  height: 64px !important;
  width: 64px !important;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 0;

  font-size: 40px;
  color: ${(p) => p.theme.colors.grayscale.g0};
  font-weight: ${fontWeights.normal};
  background: ${(p) => p.theme.colors.main.m2};
  border-radius: 100%;
`
const FieldLabel = styled(Label)`
  margin-bottom: 8px;
`
