import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, Label } from 'lib-components/typography'
import React, { useEffect, useMemo, useState } from 'react'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { useLang, useTranslation } from '../localization'
import { postReservations, ReservationChild } from './api'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import LocalDate from 'lib-common/local-date'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { UUID } from 'lib-common/types'
import {
  ErrorsOf,
  errorToInputInfo,
  getErrorCount,
  regexp,
  TIME_REGEXP
} from '../form-validation'
import { Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'

interface Props {
  onClose: () => void
  onReload: () => void
  availableChildren: ReservationChild[]
}

interface ReservationFormData {
  selectedChildren: UUID[]
  startDate: string
  endDate: string
  startTime: string
  endTime: string
}

type ReservationErrors = ErrorsOf<ReservationFormData>

export default React.memo(function ReservationModal({
  onClose,
  onReload,
  availableChildren
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()
  const minDate = useMemo(() => LocalDate.today(), []) // TODO: use deadline

  const [formData, setFormData] = useState<ReservationFormData>({
    selectedChildren: availableChildren.map((child) => child.id),
    startDate: minDate.format(),
    endDate: '',
    startTime: '',
    endTime: ''
  })

  const updateForm = (updated: Partial<ReservationFormData>) => {
    setFormData((prev) => ({
      ...prev,
      ...updated
    }))
  }

  const [errors, setErrors] = useState<ReservationErrors>({
    selectedChildren: undefined,
    startDate: undefined,
    endDate: undefined
  })

  const validate = () => {
    const startDate = LocalDate.parseFiOrNull(formData.startDate)
    setErrors({
      selectedChildren: {
        arrayErrors:
          formData.selectedChildren.length > 0 ? undefined : 'required',
        itemErrors: []
      },
      startDate:
        startDate === null
          ? 'validDate'
          : LocalDate.parseFiOrThrow(formData.startDate).isBefore(minDate)
          ? 'dateTooEarly'
          : undefined,
      endDate:
        LocalDate.parseFiOrNull(formData.endDate) === null
          ? 'validDate'
          : startDate &&
            LocalDate.parseFiOrThrow(formData.endDate).isBefore(startDate)
          ? 'dateTooEarly'
          : undefined,
      startTime:
        formData.startTime === ''
          ? 'required'
          : regexp(formData.startTime, TIME_REGEXP, 'timeFormat'),
      endTime:
        formData.endTime === ''
          ? 'required'
          : regexp(formData.endTime, TIME_REGEXP, 'timeFormat') // TODO: not before start?
    })
  }

  const [showAllErrors, setShowAllErrors] = useState(false)

  useEffect(validate, [formData, minDate])

  const [postResult, setPostResult] = useState<Result<null>>()

  useEffect(() => {
    if (postResult?.isSuccess) {
      onReload()
      onClose()
    }
  }, [postResult, onReload, onClose])

  return (
    <FormModal
      mobileFullScreen
      title={i18n.calendar.reservationModal.title}
      resolve={{
        action: () => {
          if (getErrorCount(errors) > 0) {
            setShowAllErrors(true)
            return
          }
          const range = new FiniteDateRange(
            LocalDate.parseFiOrThrow(formData.startDate),
            LocalDate.parseFiOrThrow(formData.endDate)
          )
          void postReservations(
            formData.selectedChildren,
            [...range.dates()].map((date) => ({
              date,
              startTime: formData.startTime,
              endTime: formData.endTime
            }))
          ).then((result) => setPostResult(result))
        },
        label: i18n.common.confirm,
        disabled:
          postResult?.isLoading || formData.selectedChildren.length === 0
      }}
      reject={{
        action: onClose,
        label: i18n.common.cancel
      }}
    >
      <H2>{i18n.calendar.reservationModal.selectChildren}</H2>
      <FixedSpaceColumn>
        {availableChildren.map((child) => (
          <Checkbox
            key={child.id}
            label={child.preferredName || child.firstName.split(' ')[0]}
            checked={formData.selectedChildren.includes(child.id)}
            onChange={(selected) => {
              if (selected) {
                updateForm({
                  selectedChildren: [...formData.selectedChildren, child.id]
                })
              } else {
                updateForm({
                  selectedChildren: formData.selectedChildren.filter(
                    (id) => id !== child.id
                  )
                })
              }
            }}
          />
        ))}
      </FixedSpaceColumn>

      <H2>{i18n.calendar.reservationModal.dateRange}</H2>
      <Label>{i18n.calendar.reservationModal.dateRangeLabel}</Label>
      <FixedSpaceRow>
        <DatePicker
          date={formData.startDate}
          onChange={(date) => updateForm({ startDate: date })}
          locale={lang}
          isValidDate={(date) => !date.isBefore(minDate)}
          info={errorToInputInfo(errors.startDate, i18n.validationErrors)}
          hideErrorsBeforeTouched={!showAllErrors}
        />
        <span>-</span>
        <DatePicker
          date={formData.endDate}
          onChange={(date) => updateForm({ endDate: date })}
          locale={lang}
          isValidDate={(date) => !date.isBefore(minDate)}
          info={errorToInputInfo(errors.endDate, i18n.validationErrors)}
          hideErrorsBeforeTouched={!showAllErrors}
        />
      </FixedSpaceRow>

      <H2>{i18n.calendar.reservationModal.times}</H2>
      <FixedSpaceRow>
        <Label>{i18n.calendar.reservationModal.businessDays}</Label>
        <InputField
          value={formData.startTime}
          type={'time'}
          onChange={(value) => updateForm({ startTime: value })}
          info={errorToInputInfo(errors.startTime, i18n.validationErrors)}
          hideErrorsBeforeTouched={!showAllErrors}
        />
        <span>-</span>
        <InputField
          value={formData.endTime}
          type={'time'}
          onChange={(value) => updateForm({ endTime: value })}
          info={errorToInputInfo(errors.endTime, i18n.validationErrors)}
          hideErrorsBeforeTouched={!showAllErrors}
        />
      </FixedSpaceRow>

      {postResult?.isFailure && (
        <ErrorSegment title={i18n.calendar.reservationModal.postError} />
      )}
    </FormModal>
  )
})
