// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useMemo, useState } from 'react'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights, H2, Label } from 'lib-components/typography'
import InputField from 'lib-components/atoms/form/InputField'
import LocalDate from 'lib-common/local-date'
import DatePicker, {
  DatePickerSpacer
} from 'lib-components/molecules/date-picker/DatePicker'
import { ErrorKey, regexp, TIME_REGEXP } from 'lib-common/form-validation'
import FiniteDateRange from 'lib-common/finite-date-range'
import Combobox from 'lib-components/atoms/form/Combobox'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { postReservations } from '../../../api/unit'
import { useTranslation } from '../../../state/i18n'
import { errorToInputInfo } from '../../../utils/validation/input-info-helper'
import {
  DailyReservationRequest,
  TimeRange
} from 'lib-common/generated/api-types/reservations'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Child } from 'lib-common/api-types/reservations'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { faPlus, faTrash } from 'lib-icons'
import styled from 'styled-components'
import Checkbox from 'lib-components/atoms/form/Checkbox'

interface Props {
  onClose: () => void
  onReload: () => void
  child: Child
}

type Repetition = 'DAILY' | 'WEEKLY' | 'SHIFT_CARE'

interface ReservationFormData {
  startDate: string
  endDate: string
  repetition: Repetition
  startTime: string
  endTime: string
  weeklyTimes: Array<TimeRange | undefined>
  shiftCareTimes: Record<string, ShiftCareDailyTimes>
}

type ShiftCareDailyTimes = [TimeRange] | [TimeRange, TimeRange] | undefined

type ReservationErrors = Partial<
  Record<
    keyof Omit<ReservationFormData, 'weeklyTimes' | 'shiftCareTimes'>,
    ErrorKey
  > & {
    weeklyTimes: Array<
      | { startTime: ErrorKey | undefined; endTime: ErrorKey | undefined }
      | undefined
    >
  } & {
    shiftCareTimes: Record<
      string,
      | Array<{
          startTime: ErrorKey | undefined
          endTime: ErrorKey | undefined
        }>
      | undefined
    >
  }
>

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
    })),
    shiftCareTimes: {}
  })

  const updateForm = (updated: Partial<ReservationFormData>) => {
    setFormData((prev) => ({
      ...prev,
      ...updated
    }))
  }

  const [showAllErrors, setShowAllErrors] = useState(false)
  const validationResult = useMemo(
    () => validateForm(child.id, formData),
    [child.id, formData]
  )

  const shiftCareRange = useMemo(() => {
    if (formData.repetition !== 'SHIFT_CARE') return

    const parsedStartDate = LocalDate.parseFiOrNull(formData.startDate)
    const parsedEndDate = LocalDate.parseFiOrNull(formData.endDate)

    if (
      !parsedStartDate ||
      !parsedEndDate ||
      parsedEndDate.isBefore(parsedStartDate)
    ) {
      return
    }

    return [...new FiniteDateRange(parsedStartDate, parsedEndDate).dates()]
  }, [formData.repetition, formData.startDate, formData.endDate])

  return (
    <AsyncFormModal
      mobileFullScreen
      title={i18n.unit.attendanceReservations.reservationModal.title}
      resolve={{
        action: () => {
          if (validationResult.errors) {
            setShowAllErrors(true)
            return Promise.resolve('AsyncButton.cancel')
          } else {
            return postReservations(validationResult.requestPayload)
          }
        },
        onSuccess: () => {
          onReload()
          onClose()
        },
        label: i18n.common.confirm
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

      <H2>{i18n.unit.attendanceReservations.reservationModal.repetition}</H2>
      <Label>{i18n.common.select}</Label>
      <Combobox<Repetition>
        items={['DAILY', 'WEEKLY', 'SHIFT_CARE']}
        selectedItem={formData.repetition}
        onChange={(value) => {
          if (value) updateForm({ repetition: value })
        }}
        clearable={false}
        getItemLabel={(item) =>
          i18n.unit.attendanceReservations.reservationModal.repetitions[item]
        }
        data-qa="repetition"
      />

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
          info={errorToInputInfo(
            validationResult.errors?.startDate,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={!showAllErrors}
        />
        <DatePickerSpacer />
        <DatePicker
          date={formData.endDate}
          onChange={(date) => updateForm({ endDate: date })}
          locale={lang}
          isValidDate={(date) => !date.isBefore(LocalDate.today())}
          info={errorToInputInfo(
            validationResult.errors?.endDate,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={!showAllErrors}
          initialMonth={LocalDate.today()}
        />
      </FixedSpaceRow>
      <Gap size="s" />

      {formData.repetition === 'DAILY' && (
        <FixedSpaceRow alignItems="center">
          <Label>
            {i18n.unit.attendanceReservations.reservationModal.businessDays}
          </Label>
          <InputField
            value={formData.startTime}
            type={'time'}
            onChange={(value) => updateForm({ startTime: value })}
            info={errorToInputInfo(
              validationResult.errors?.startTime,
              i18n.validationErrors
            )}
            hideErrorsBeforeTouched={!showAllErrors}
            data-qa="daily-start-time"
          />
          <span>–</span>
          <InputField
            value={formData.endTime}
            type={'time'}
            onChange={(value) => updateForm({ endTime: value })}
            info={errorToInputInfo(
              validationResult.errors?.endTime,
              i18n.validationErrors
            )}
            hideErrorsBeforeTouched={!showAllErrors}
            data-qa="daily-end-time"
          />
        </FixedSpaceRow>
      )}

      {formData.repetition === 'WEEKLY' && (
        <FixedSpaceColumn>
          {[0, 1, 2, 3, 4].map((i) => {
            const times = formData.weeklyTimes[i]

            return (
              <FixedSpaceRow key={`day-${i}`} alignItems="center">
                <Checkbox
                  label={i18n.common.datetime.weekdaysShort[i]}
                  checked={!!times}
                  onChange={(checked) =>
                    updateForm({
                      weeklyTimes: [
                        ...formData.weeklyTimes.slice(0, i),
                        checked
                          ? {
                              startTime: '',
                              endTime: ''
                            }
                          : undefined,
                        ...formData.weeklyTimes.slice(i + 1)
                      ]
                    })
                  }
                />
                {times ? (
                  <>
                    <InputField
                      value={times.startTime}
                      type={'time'}
                      onChange={(value) =>
                        updateForm({
                          weeklyTimes: [
                            ...formData.weeklyTimes.slice(0, i),
                            {
                              startTime: value,
                              endTime: times.endTime
                            },
                            ...formData.weeklyTimes.slice(i + 1)
                          ]
                        })
                      }
                      info={errorToInputInfo(
                        validationResult.errors?.weeklyTimes?.[i]?.startTime,
                        i18n.validationErrors
                      )}
                      hideErrorsBeforeTouched={!showAllErrors}
                      data-qa={`weekly-start-time-${i}`}
                    />
                    <span>–</span>
                    <InputField
                      value={times.endTime}
                      type={'time'}
                      onChange={(value) =>
                        updateForm({
                          weeklyTimes: [
                            ...formData.weeklyTimes.slice(0, i),
                            {
                              startTime: times.startTime,
                              endTime: value
                            },
                            ...formData.weeklyTimes.slice(i + 1)
                          ]
                        })
                      }
                      info={errorToInputInfo(
                        validationResult.errors?.weeklyTimes?.[i]?.endTime,
                        i18n.validationErrors
                      )}
                      hideErrorsBeforeTouched={!showAllErrors}
                      data-qa={`weekly-end-time-${i}`}
                    />
                  </>
                ) : null}
              </FixedSpaceRow>
            )
          })}
        </FixedSpaceColumn>
      )}

      {formData.repetition === 'SHIFT_CARE' ? (
        <FixedSpaceColumn>
          {shiftCareRange ? (
            shiftCareRange.map((date, index) => {
              const [timeRange, extraTimeRange]:
                | [TimeRange]
                | [TimeRange, TimeRange] = formData.shiftCareTimes[
                date.formatIso()
              ] ?? [
                {
                  startTime: '',
                  endTime: ''
                }
              ]

              return (
                <Fragment key={`shift-care-${date.formatIso()}`}>
                  {index !== 0 && date.getIsoDayOfWeek() === 1 ? (
                    <Separator />
                  ) : null}
                  {index === 0 || date.getIsoDayOfWeek() === 1 ? (
                    <Week>
                      {i18n.common.datetime.week} {date.getIsoWeek()}
                    </Week>
                  ) : null}
                  <FixedSpaceRow key={`day-${date.formatIso()}`}>
                    <Label>
                      {`${
                        i18n.common.datetime.weekdaysShort[
                          date.getIsoDayOfWeek() - 1
                        ]
                      } ${date.format('d.M.')}`}
                    </Label>
                    <FixedSpaceColumn>
                      <FixedSpaceRow alignItems="center">
                        <InputField
                          value={timeRange.startTime ?? ''}
                          type="time"
                          onChange={(value) => {
                            const updatedRange = {
                              startTime: value,
                              endTime: timeRange.endTime ?? ''
                            }

                            updateForm({
                              shiftCareTimes: {
                                ...formData.shiftCareTimes,
                                [date.formatIso()]: extraTimeRange
                                  ? [updatedRange, extraTimeRange]
                                  : [updatedRange]
                              }
                            })
                          }}
                          info={errorToInputInfo(
                            validationResult.errors?.shiftCareTimes?.[
                              date.formatIso()
                            ]?.[0]?.startTime,
                            i18n.validationErrors
                          )}
                          hideErrorsBeforeTouched={!showAllErrors}
                          data-qa={`shift-care-start-time-${date.formatIso()}`}
                        />
                        <span>–</span>
                        <InputField
                          value={timeRange.endTime ?? ''}
                          type="time"
                          onChange={(value) => {
                            const updatedRange = {
                              startTime: timeRange.startTime ?? '',
                              endTime: value
                            }

                            updateForm({
                              shiftCareTimes: {
                                ...formData.shiftCareTimes,
                                [date.formatIso()]: extraTimeRange
                                  ? [updatedRange, extraTimeRange]
                                  : [updatedRange]
                              }
                            })
                          }}
                          info={errorToInputInfo(
                            validationResult.errors?.shiftCareTimes?.[
                              date.formatIso()
                            ]?.[0]?.endTime,
                            i18n.validationErrors
                          )}
                          hideErrorsBeforeTouched={!showAllErrors}
                          data-qa={`shift-care-end-time-${date.formatIso()}`}
                        />
                        {extraTimeRange ? null : (
                          <IconButton
                            icon={faPlus}
                            onClick={() =>
                              updateForm({
                                shiftCareTimes: {
                                  ...formData.shiftCareTimes,
                                  [date.formatIso()]: [
                                    timeRange,
                                    {
                                      startTime: '',
                                      endTime: ''
                                    }
                                  ]
                                }
                              })
                            }
                          />
                        )}
                      </FixedSpaceRow>
                      {extraTimeRange ? (
                        <FixedSpaceRow alignItems="center">
                          <InputField
                            value={extraTimeRange.startTime ?? ''}
                            type="time"
                            onChange={(value) =>
                              updateForm({
                                shiftCareTimes: {
                                  ...formData.shiftCareTimes,
                                  [date.formatIso()]: [
                                    timeRange,
                                    {
                                      startTime: value,
                                      endTime: extraTimeRange.endTime ?? ''
                                    }
                                  ]
                                }
                              })
                            }
                            info={errorToInputInfo(
                              validationResult.errors?.shiftCareTimes?.[
                                date.formatIso()
                              ]?.[1]?.startTime,
                              i18n.validationErrors
                            )}
                            hideErrorsBeforeTouched={!showAllErrors}
                            data-qa={`shift-care-start-time-${date.formatIso()}`}
                          />
                          <span>–</span>
                          <InputField
                            value={extraTimeRange.endTime ?? ''}
                            type="time"
                            onChange={(value) =>
                              updateForm({
                                shiftCareTimes: {
                                  ...formData.shiftCareTimes,
                                  [date.formatIso()]: [
                                    timeRange,
                                    {
                                      startTime: extraTimeRange.startTime ?? '',
                                      endTime: value
                                    }
                                  ]
                                }
                              })
                            }
                            info={errorToInputInfo(
                              validationResult.errors?.shiftCareTimes?.[
                                date.formatIso()
                              ]?.[1]?.endTime,
                              i18n.validationErrors
                            )}
                            hideErrorsBeforeTouched={!showAllErrors}
                            data-qa={`shift-care-end-time-${date.formatIso()}`}
                          />
                          <IconButton
                            icon={faTrash}
                            onClick={() =>
                              updateForm({
                                shiftCareTimes: {
                                  ...formData.shiftCareTimes,
                                  [date.formatIso()]: [timeRange]
                                }
                              })
                            }
                          />
                        </FixedSpaceRow>
                      ) : null}
                    </FixedSpaceColumn>
                  </FixedSpaceRow>
                </Fragment>
              )
            })
          ) : (
            <MissingDateRange>
              {
                i18n.unit.attendanceReservations.reservationModal
                  .missingDateRange
              }
            </MissingDateRange>
          )}
        </FixedSpaceColumn>
      ) : null}
    </AsyncFormModal>
  )
})

type ValidationResult =
  | { errors: ReservationErrors }
  | { errors: undefined; requestPayload: DailyReservationRequest[] }

function validateForm(
  childId: string,
  formData: ReservationFormData
): ValidationResult {
  const errors: ReservationErrors = {}

  const startDate = LocalDate.parseFiOrNull(formData.startDate)
  if (startDate === null) {
    errors['startDate'] = 'validDate'
  }

  const endDate = LocalDate.parseFiOrNull(formData.endDate)
  if (endDate === null) {
    errors['endDate'] = 'validDate'
  } else if (startDate && endDate.isBefore(startDate)) {
    errors['endDate'] = 'dateTooEarly'
  }

  if (formData.repetition === 'DAILY') {
    if (!formData.startTime) {
      errors['startTime'] = 'required'
    } else {
      errors['startTime'] = regexp(
        formData.startTime,
        TIME_REGEXP,
        'timeFormat'
      )
    }

    if (!formData.endTime) {
      errors['endTime'] = 'required'
    } else {
      errors['endTime'] = regexp(formData.endTime, TIME_REGEXP, 'timeFormat')
    }
  }

  if (formData.repetition === 'WEEKLY') {
    errors['weeklyTimes'] = formData.weeklyTimes.map((times) =>
      times
        ? {
            startTime:
              times.startTime === ''
                ? 'required'
                : regexp(times.startTime, TIME_REGEXP, 'timeFormat'),
            endTime:
              times.endTime === ''
                ? 'required'
                : regexp(times.endTime, TIME_REGEXP, 'timeFormat')
          }
        : undefined
    )
  }

  if (formData.repetition === 'SHIFT_CARE') {
    errors['shiftCareTimes'] = Object.fromEntries(
      Object.entries(formData.shiftCareTimes).map(([date, times]) => [
        date,
        times
          ? times.map((time) => ({
              startTime:
                time.startTime === ''
                  ? time.endTime !== ''
                    ? 'required'
                    : undefined
                  : regexp(time.startTime, TIME_REGEXP, 'timeFormat'),
              endTime:
                time.endTime === ''
                  ? time.startTime !== ''
                    ? 'required'
                    : undefined
                  : regexp(time.endTime, TIME_REGEXP, 'timeFormat')
            }))
          : undefined
      ])
    )
  }

  if (errorsExist(errors)) {
    return { errors }
  }

  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  const dateRange = new FiniteDateRange(startDate!, endDate!)
  const dates = [...dateRange.dates()]

  return {
    errors: undefined,
    requestPayload:
      formData.repetition === 'DAILY'
        ? dates.map((date) => ({
            childId,
            date,
            reservations: [
              {
                startTime: formData.startTime,
                endTime: formData.endTime
              }
            ]
          }))
        : formData.repetition === 'WEEKLY'
        ? dates.map((date) => {
            const startTime =
              formData.weeklyTimes[date.getIsoDayOfWeek() - 1]?.startTime ?? ''
            const endTime =
              formData.weeklyTimes[date.getIsoDayOfWeek() - 1]?.endTime ?? ''

            return {
              childId,
              date,
              reservations:
                startTime && endTime ? [{ startTime, endTime }] : null
            }
          })
        : Object.entries(formData.shiftCareTimes)
            .filter(([isoDate]) => {
              const date = LocalDate.tryParseIso(isoDate)
              return date && dateRange.includes(date)
            })
            .map(([isoDate, times]) => ({
              childId,
              date: LocalDate.parseIso(isoDate),
              reservations: times ? times : null
            }))
  }
}

function errorsExist(errors: ReservationErrors): boolean {
  const {
    weeklyTimes: weeklyErrors,
    shiftCareTimes: shiftCareErrors,
    ...otherErrors
  } = errors

  for (const error of Object.values(otherErrors)) {
    if (error) return true
  }

  for (const error of weeklyErrors ?? []) {
    if (error?.startTime || error?.endTime) return true
  }

  for (const errors of Object.values(shiftCareErrors ?? {})) {
    if (errors?.some((error) => error.startTime || error.endTime)) return true
  }

  return false
}

const Week = styled.div`
  color: ${({ theme }) => theme.colors.main.dark};
  font-weight: ${fontWeights.semibold};
`

const Separator = styled.div`
  border-top: 2px dotted ${(p) => p.theme.colors.greyscale.lighter};
  margin: ${defaultMargins.m} 0;
`

const MissingDateRange = styled.span`
  color: ${({ theme }) => theme.colors.greyscale.dark};
  font-style: italic;
`
