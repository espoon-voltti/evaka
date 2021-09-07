import React, { useEffect, useState } from 'react'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, Label } from 'lib-components/typography'
import FormModal from 'lib-components/molecules/modals/FormModal'
import InputField from 'lib-components/atoms/form/InputField'
import LocalDate from 'lib-common/local-date'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import {
  ErrorsOf,
  getErrorCount,
  regexp,
  TIME_REGEXP
} from 'lib-common/form-validation'
import { Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Combobox from 'lib-components/atoms/form/Combobox'
import { Gap } from 'lib-components/white-space'
import { CalendarChild, postReservations } from '../../../api/unit'
import { useTranslation } from '../../../state/i18n'
import { errorToInputInfo } from '../../../utils/validation/input-info-helper'
import { DailyReservationRequest } from 'lib-common/api-types/reservations'

interface Props {
  onClose: () => void
  onReload: () => void
  child: CalendarChild
}

type Repetition = 'DAILY' | 'WEEKLY'

interface ReservationFormData {
  startDate: string
  endDate: string
  repetition: Repetition
  startTime: string
  endTime: string
  weeklyTimes: {
    startTime: string
    endTime: string
  }[]
}

type ReservationErrors = ErrorsOf<ReservationFormData>

export default React.memo(function ReservationModalSingleChild({
  onClose,
  onReload,
  child
}: Props) {
  const { i18n, lang } = useTranslation()

  const [formData, setFormData] = useState<ReservationFormData>({
    startDate: LocalDate.today().format(),
    endDate: '',
    repetition: 'DAILY',
    startTime: '',
    endTime: '',
    weeklyTimes: [0, 1, 2, 3, 4].map(() => ({
      startTime: '',
      endTime: ''
    }))
  })

  const updateForm = (updated: Partial<ReservationFormData>) => {
    setFormData((prev) => ({
      ...prev,
      ...updated
    }))
  }

  const [errors, setErrors] = useState<ReservationErrors>({
    startDate: undefined,
    endDate: undefined
  })

  const validate = () => {
    const startDate = LocalDate.parseFiOrNull(formData.startDate)
    setErrors({
      startDate:
        startDate === null
          ? 'validDate'
          : LocalDate.parseFiOrThrow(formData.startDate).isBefore(
              LocalDate.today()
            )
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
        formData.repetition !== 'DAILY'
          ? undefined
          : formData.startTime === ''
          ? 'required'
          : regexp(formData.startTime, TIME_REGEXP, 'timeFormat'),
      endTime:
        formData.repetition !== 'DAILY'
          ? undefined
          : formData.endTime === ''
          ? 'required'
          : regexp(formData.endTime, TIME_REGEXP, 'timeFormat'), // TODO: not before start?
      weeklyTimes: {
        arrayErrors: undefined,
        itemErrors:
          formData.repetition !== 'WEEKLY'
            ? [0, 1, 2, 3, 4].map(() => ({
                startTime: undefined,
                endTime: undefined
              }))
            : formData.weeklyTimes.map((times) => ({
                startTime:
                  times.startTime === ''
                    ? times.endTime !== ''
                      ? 'required'
                      : undefined
                    : regexp(times.startTime, TIME_REGEXP, 'timeFormat'),
                endTime:
                  times.endTime === ''
                    ? times.startTime !== ''
                      ? 'required'
                      : undefined
                    : regexp(times.endTime, TIME_REGEXP, 'timeFormat')
              }))
      }
    })
  }

  const [showAllErrors, setShowAllErrors] = useState(false)

  useEffect(validate, [formData])

  const [postResult, setPostResult] = useState<Result<null>>()

  useEffect(() => {
    if (postResult?.isSuccess) {
      onReload()
      onClose()
    }
  }, [postResult, onReload, onClose])

  const getDailyReservations = (): DailyReservationRequest[] => {
    const range = new FiniteDateRange(
      LocalDate.parseFiOrThrow(formData.startDate),
      LocalDate.parseFiOrThrow(formData.endDate)
    )
    switch (formData.repetition) {
      case 'DAILY':
        return [...range.dates()].map((date) => ({
          childId: child.id,
          date,
          reservation: {
            startTime: formData.startTime,
            endTime: formData.endTime
          }
        }))
      case 'WEEKLY':
        return [...range.dates()]
          .filter((d) => !d.isWeekend())
          .map((date) => {
            const startTime =
              formData.weeklyTimes[date.getIsoDayOfWeek() - 1].startTime
            const endTime =
              formData.weeklyTimes[date.getIsoDayOfWeek() - 1].endTime
            const noReservation = startTime === '' || endTime === ''

            return {
              childId: child.id,
              date,
              reservation: noReservation ? null : { startTime, endTime }
            }
          })
    }
  }

  return (
    <FormModal
      mobileFullScreen
      title={i18n.unit.attendanceReservations.reservationModal.title}
      resolve={{
        action: () => {
          if (getErrorCount(errors) > 0) {
            setShowAllErrors(true)
            return
          }
          void postReservations(getDailyReservations()).then((result) =>
            setPostResult(result)
          )
        },
        label: i18n.common.confirm,
        disabled: postResult?.isLoading
      }}
      reject={{
        action: onClose,
        label: i18n.common.cancel
      }}
    >
      <H2>
        {i18n.unit.attendanceReservations.reservationModal.selectedChildren}
      </H2>
      <div>
        {child.lastName} {child.firstName}
      </div>

      <H2>{i18n.unit.attendanceReservations.reservationModal.dateRange}</H2>
      <Label>
        {i18n.unit.attendanceReservations.reservationModal.dateRangeLabel}
      </Label>
      <FixedSpaceRow>
        <DatePicker
          date={formData.startDate}
          onChange={(date) => updateForm({ startDate: date })}
          locale={lang}
          isValidDate={(date) => !date.isBefore(LocalDate.today())}
          info={errorToInputInfo(errors.startDate, i18n.validationErrors)}
          hideErrorsBeforeTouched={!showAllErrors}
        />
        <span>-</span>
        <DatePicker
          date={formData.endDate}
          onChange={(date) => updateForm({ endDate: date })}
          locale={lang}
          isValidDate={(date) => !date.isBefore(LocalDate.today())}
          info={errorToInputInfo(errors.endDate, i18n.validationErrors)}
          hideErrorsBeforeTouched={!showAllErrors}
          initialMonth={LocalDate.today()}
        />
      </FixedSpaceRow>

      <H2>{i18n.unit.attendanceReservations.reservationModal.times}</H2>

      <Label>{i18n.unit.attendanceReservations.reservationModal.repeats}</Label>
      <Combobox<Repetition>
        items={['DAILY', 'WEEKLY']}
        selectedItem={formData.repetition}
        onChange={(value) => {
          if (value) updateForm({ repetition: value })
        }}
        clearable={false}
        getItemLabel={(item) =>
          i18n.unit.attendanceReservations.reservationModal.repetitions[item]
        }
      />

      <Gap size="s" />

      {formData.repetition === 'DAILY' && (
        <FixedSpaceRow>
          <Label>
            {i18n.unit.attendanceReservations.reservationModal.businessDays}
          </Label>
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
      )}

      {formData.repetition === 'WEEKLY' && (
        <FixedSpaceColumn>
          {[0, 1, 2, 3, 4].map((i) => (
            <FixedSpaceRow key={`day-${i}`}>
              <Label style={{ width: '40px' }}>
                {i18n.common.datetime.weekdaysShort[i]}
              </Label>
              <InputField
                value={formData.weeklyTimes[i].startTime}
                type={'time'}
                onChange={(value) =>
                  updateForm({
                    weeklyTimes: [
                      ...formData.weeklyTimes.slice(0, i),
                      {
                        startTime: value,
                        endTime: formData.weeklyTimes[i].endTime
                      },
                      ...formData.weeklyTimes.slice(i + 1)
                    ]
                  })
                }
                info={errorToInputInfo(
                  errors.weeklyTimes?.itemErrors[i].startTime,
                  i18n.validationErrors
                )}
                hideErrorsBeforeTouched={!showAllErrors}
              />
              <span>-</span>
              <InputField
                value={formData.weeklyTimes[i].endTime}
                type={'time'}
                onChange={(value) =>
                  updateForm({
                    weeklyTimes: [
                      ...formData.weeklyTimes.slice(0, i),
                      {
                        startTime: formData.weeklyTimes[i].startTime,
                        endTime: value
                      },
                      ...formData.weeklyTimes.slice(i + 1)
                    ]
                  })
                }
                info={errorToInputInfo(
                  errors.weeklyTimes?.itemErrors[i].endTime,
                  i18n.validationErrors
                )}
                hideErrorsBeforeTouched={!showAllErrors}
              />
            </FixedSpaceRow>
          ))}
        </FixedSpaceColumn>
      )}

      {postResult?.isFailure && (
        <ErrorSegment
          title={i18n.unit.attendanceReservations.reservationModal.postError}
        />
      )}
    </FormModal>
  )
})
