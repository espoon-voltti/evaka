// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { postStaffAndExternalAttendances } from 'employee-frontend/api/staff-attendance'
import { useTranslation } from 'employee-frontend/state/i18n'
import { isTimeValid } from 'employee-frontend/utils/validation/validations'
import { Failure, Result } from 'lib-common/api'
import { ErrorsOf, getErrorCount } from 'lib-common/form-validation'
import { UpsertStaffAndExternalAttendanceRequest } from 'lib-common/generated/api-types/attendance'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
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

import GroupSelector from '../tab-calendar/GroupSelector'

type PersonArrivalData = {
  arrivalDate: LocalDate
  arrivalTime: string
  name: string
  groupId: UUID | null
}

const defaultPersonArrival = (groupId: UUID | null): PersonArrivalData => ({
  arrivalDate: LocalDate.todayInHelsinkiTz(),
  arrivalTime: LocalTime.nowInHelsinkiTz().format('HH:mm'),
  name: '',
  groupId
})

const validateForm = (
  formData: PersonArrivalData
):
  | [UpsertStaffAndExternalAttendanceRequest]
  | [undefined, ErrorsOf<PersonArrivalData>] => {
  const errors: ErrorsOf<PersonArrivalData> = {
    arrivalDate: !formData.arrivalDate ? 'required' : undefined,
    arrivalTime: !formData.arrivalTime
      ? 'timeRequired'
      : isTimeValid(formData.arrivalTime)
      ? undefined
      : 'timeFormat',
    name: formData.name.length < 3 ? 'required' : undefined,
    groupId: !formData.groupId ? 'required' : undefined
  }

  if (getErrorCount(errors) > 0) {
    return [undefined, errors]
  }

  return [
    {
      externalAttendances: [
        {
          attendanceId: null,
          departed: null,
          arrived: HelsinkiDateTime.fromLocal(
            formData.arrivalDate,
            LocalTime.parse(formData.arrivalTime, 'HH:mm')
          ),
          name: formData.name,
          // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
          groupId: formData.groupId!
        }
      ],
      staffAttendances: []
    }
  ]
}

type AddPersonModalProps = {
  onClose: () => void
  onSave: () => void
  unitId: UUID
  groups: Result<DaycareGroup[]>
  defaultGroupId: UUID | null
}

export const AddPersonModal = React.memo(function AddPersonModal({
  onClose,
  onSave,
  unitId,
  groups,
  defaultGroupId
}: AddPersonModalProps) {
  const { i18n, lang } = useTranslation()

  const [formData, setFormData] = useState<PersonArrivalData>(
    defaultPersonArrival(defaultGroupId)
  )

  const [requestBody, errors] = useMemo(
    () => validateForm(formData),
    [formData]
  )

  const submit = useCallback(() => {
    if (requestBody === undefined)
      return Promise.reject(Failure.of({ message: 'validation error' }))
    return postStaffAndExternalAttendances(unitId, requestBody)
  }, [requestBody, unitId])

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
              date={formData.arrivalDate}
              locale={lang}
              onChange={(date) =>
                setFormData((prev) => ({
                  ...prev,
                  arrivalDate: date || prev.arrivalDate
                }))
              }
              data-qa="add-person-arrival-date-picker"
              required
              errorTexts={i18n.validationErrors}
              info={
                errors?.arrivalDate && {
                  text: i18n.validationErrors[errors.arrivalDate],
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
          <GroupSelector
            groups={groups}
            data-qa="add-person-group-select"
            selected={formData.groupId}
            onSelect={(val) =>
              setFormData((prev) => ({ ...prev, groupId: val }))
            }
            // Modal is accessible only when realtime staff attendance is enabled
            realtimeStaffAttendanceEnabled={true}
            onlyRealGroups
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
