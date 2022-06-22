// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useMemo, useState } from 'react'
import styled from 'styled-components'

import { Child } from 'lib-common/api-types/reservations'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import {
  Repetition,
  ReservationFormData,
  TimeRangeErrors,
  TimeRanges,
  validateForm
} from 'lib-common/reservations'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import DatePicker, {
  DatePickerSpacer
} from 'lib-components/molecules/date-picker/DatePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { fontWeights, H2, Label, Light } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPlus, faTrash } from 'lib-icons'

import { postReservations } from '../../../api/unit'
import { useTranslation } from '../../../state/i18n'
import { errorToInputInfo } from '../../../utils/validation/input-info-helper'

interface Props {
  onClose: () => void
  onReload: () => void
  child: Child
  isShiftCareUnit: boolean
  operationalDays: number[]
}

const reservableDates = new FiniteDateRange(
  LocalDate.todayInSystemTz(),
  LocalDate.todayInSystemTz().addYears(1)
)

export default React.memo(function ReservationModalSingleChild({
  onClose,
  onReload,
  child,
  isShiftCareUnit,
  operationalDays
}: Props) {
  const { i18n, lang } = useTranslation()

  const [formData, setFormData] = useState<ReservationFormData>({
    selectedChildren: [child.id],
    startDate: LocalDate.todayInSystemTz(),
    endDate: null,
    repetition: 'DAILY',
    dailyTimes: [
      {
        startTime: '',
        endTime: ''
      }
    ],
    weeklyTimes: [0, 1, 2, 3, 4, 5, 6].map(() => [
      {
        startTime: '',
        endTime: ''
      }
    ]),
    irregularTimes: {}
  })

  const updateForm = (updated: Partial<ReservationFormData>) => {
    setFormData((prev) => ({
      ...prev,
      ...updated
    }))
  }

  const [showAllErrors, setShowAllErrors] = useState(false)
  const validationResult = useMemo(
    () => validateForm([reservableDates], formData),
    [formData]
  )

  const shiftCareRange = useMemo(() => {
    if (formData.repetition !== 'IRREGULAR') return

    const parsedStartDate = formData.startDate
    const parsedEndDate = formData.endDate

    if (
      !parsedStartDate ||
      !parsedEndDate ||
      parsedEndDate.isBefore(parsedStartDate)
    ) {
      return
    }

    return [...new FiniteDateRange(parsedStartDate, parsedEndDate).dates()]
  }, [formData.repetition, formData.startDate, formData.endDate])

  const includedDays = useMemo(() => {
    return [1, 2, 3, 4, 5, 6, 7].filter((day) => operationalDays.includes(day))
  }, [operationalDays])

  return (
    <AsyncFormModal
      mobileFullScreen
      title={i18n.unit.attendanceReservations.reservationModal.title}
      resolveAction={() => {
        if (validationResult.errors) {
          setShowAllErrors(true)
          return
        } else {
          return postReservations(validationResult.requestPayload)
        }
      }}
      onSuccess={() => {
        onReload()
        onClose()
      }}
      resolveLabel={i18n.common.confirm}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
    >
      <H2>
        {i18n.unit.attendanceReservations.reservationModal.selectedChildren}
      </H2>
      <div>
        {child.lastName} {child.firstName}
      </div>

      <H2>{i18n.unit.attendanceReservations.reservationModal.repetition}</H2>
      <Label>{i18n.common.select}</Label>
      <Select<Repetition>
        items={['DAILY', 'WEEKLY', 'IRREGULAR']}
        selectedItem={formData.repetition}
        onChange={(value) => {
          if (value) updateForm({ repetition: value })
        }}
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
          data-qa="reservation-start-date"
          onChange={(date) => updateForm({ startDate: date })}
          locale={lang}
          isInvalidDate={(date) =>
            reservableDates.includes(date)
              ? null
              : i18n.validationErrors.unselectableDate
          }
          minDate={reservableDates.start}
          maxDate={reservableDates.end}
          info={errorToInputInfo(
            validationResult.errors?.startDate,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={!showAllErrors}
          errorTexts={i18n.validationErrors}
        />
        <DatePickerSpacer />
        <DatePicker
          date={formData.endDate}
          data-qa="reservation-end-date"
          onChange={(date) => updateForm({ endDate: date })}
          locale={lang}
          isInvalidDate={(date) =>
            reservableDates.includes(date)
              ? null
              : i18n.validationErrors.unselectableDate
          }
          minDate={reservableDates.start}
          maxDate={reservableDates.end}
          info={errorToInputInfo(
            validationResult.errors?.endDate,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={!showAllErrors}
          initialMonth={LocalDate.todayInSystemTz()}
          errorTexts={i18n.validationErrors}
        />
      </FixedSpaceRow>
      <Gap size="m" />

      <TimeInputGrid>
        {formData.repetition === 'DAILY' && (
          <TimeInputs
            label={
              <Label>{`${
                i18n.common.datetime.weekdaysShort[includedDays[0] - 1]
              }-${
                i18n.common.datetime.weekdaysShort[
                  includedDays[includedDays.length - 1] - 1
                ]
              }`}</Label>
            }
            times={formData.dailyTimes}
            updateTimes={(dailyTimes) =>
              updateForm({
                dailyTimes
              })
            }
            validationErrors={validationResult.errors?.dailyTimes}
            showAllErrors={showAllErrors}
            allowExtraTimeRange={isShiftCareUnit}
          />
        )}

        {formData.repetition === 'WEEKLY' &&
          formData.weeklyTimes.map((times, index) =>
            includedDays.includes(index + 1) ? (
              <TimeInputs
                key={`day-${index}`}
                label={
                  <Checkbox
                    label={i18n.common.datetime.weekdaysShort[index]}
                    checked={!!times}
                    onChange={(checked) =>
                      updateForm({
                        weeklyTimes: [
                          ...formData.weeklyTimes.slice(0, index),
                          checked
                            ? [
                                {
                                  startTime: '',
                                  endTime: ''
                                }
                              ]
                            : undefined,
                          ...formData.weeklyTimes.slice(index + 1)
                        ]
                      })
                    }
                  />
                }
                times={times}
                updateTimes={(times) =>
                  updateForm({
                    weeklyTimes: [
                      ...formData.weeklyTimes.slice(0, index),
                      times,
                      ...formData.weeklyTimes.slice(index + 1)
                    ]
                  })
                }
                validationErrors={validationResult.errors?.weeklyTimes?.[index]}
                showAllErrors={showAllErrors}
                allowExtraTimeRange={isShiftCareUnit}
              />
            ) : null
          )}

        {formData.repetition === 'IRREGULAR' ? (
          shiftCareRange ? (
            shiftCareRange.map((date, index) => (
              <Fragment key={`shift-care-${date.formatIso()}`}>
                {index !== 0 && date.getIsoDayOfWeek() === 1 ? (
                  <Separator />
                ) : null}
                {index === 0 || date.getIsoDayOfWeek() === 1 ? (
                  <Week>
                    {i18n.common.datetime.week} {date.getIsoWeek()}
                  </Week>
                ) : null}
                {includedDays.includes(date.getIsoDayOfWeek()) && (
                  <TimeInputs
                    label={<Label>{date.format('EEEEEE d.M.', lang)}</Label>}
                    times={
                      formData.irregularTimes[date.formatIso()] ?? [
                        {
                          startTime: '',
                          endTime: ''
                        }
                      ]
                    }
                    updateTimes={(times) =>
                      updateForm({
                        irregularTimes: {
                          ...formData.irregularTimes,
                          [date.formatIso()]: times
                        }
                      })
                    }
                    validationErrors={
                      validationResult.errors?.irregularTimes?.[
                        date.formatIso()
                      ]
                    }
                    showAllErrors={showAllErrors}
                    allowExtraTimeRange={isShiftCareUnit}
                  />
                )}
              </Fragment>
            ))
          ) : (
            <MissingDateRange>
              {
                i18n.unit.attendanceReservations.reservationModal
                  .missingDateRange
              }
            </MissingDateRange>
          )
        ) : null}
      </TimeInputGrid>
    </AsyncFormModal>
  )
})

const TimeInputs = React.memo(function TimeInputs(props: {
  label: JSX.Element
  times: TimeRanges | undefined
  updateTimes: (v: TimeRanges | undefined) => void
  validationErrors: TimeRangeErrors[] | undefined
  showAllErrors: boolean
  allowExtraTimeRange: boolean
}) {
  const { i18n } = useTranslation()

  if (!props.times) {
    return (
      <>
        {props.label}
        <div />
        <div />
      </>
    )
  }

  const [timeRange, extraTimeRange] = props.times
  return (
    <>
      {props.label}
      <FixedSpaceRow alignItems="center">
        <TimeInput
          data-qa="reservation-start-time"
          value={timeRange.startTime ?? ''}
          onChange={(value) => {
            const updatedRange = {
              startTime: value,
              endTime: timeRange.endTime ?? ''
            }

            props.updateTimes(
              extraTimeRange ? [updatedRange, extraTimeRange] : [updatedRange]
            )
          }}
          info={errorToInputInfo(
            props.validationErrors?.[0]?.startTime,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={!props.showAllErrors}
        />
        <span>–</span>
        <TimeInput
          data-qa="reservation-end-time"
          value={timeRange.endTime ?? ''}
          onChange={(value) => {
            const updatedRange = {
              startTime: timeRange.startTime ?? '',
              endTime: value
            }

            props.updateTimes(
              extraTimeRange ? [updatedRange, extraTimeRange] : [updatedRange]
            )
          }}
          info={errorToInputInfo(
            props.validationErrors?.[0]?.endTime,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={!props.showAllErrors}
        />
      </FixedSpaceRow>
      {!extraTimeRange && props.allowExtraTimeRange ? (
        <IconButton
          icon={faPlus}
          data-qa="add-new-reservation-timerange"
          onClick={() =>
            props.updateTimes([
              timeRange,
              {
                startTime: '',
                endTime: ''
              }
            ])
          }
        />
      ) : (
        <div />
      )}
      {extraTimeRange ? (
        <>
          <div />
          <FixedSpaceRow alignItems="center">
            <TimeInput
              data-qa="reservation-start-time"
              value={extraTimeRange.startTime ?? ''}
              onChange={(value) =>
                props.updateTimes([
                  timeRange,
                  {
                    startTime: value,
                    endTime: extraTimeRange.endTime ?? ''
                  }
                ])
              }
              info={errorToInputInfo(
                props.validationErrors?.[1]?.startTime,
                i18n.validationErrors
              )}
              hideErrorsBeforeTouched={!props.showAllErrors}
            />
            <span>–</span>
            <TimeInput
              data-qa="reservation-end-time"
              value={extraTimeRange.endTime ?? ''}
              onChange={(value) =>
                props.updateTimes([
                  timeRange,
                  {
                    startTime: extraTimeRange.startTime ?? '',
                    endTime: value
                  }
                ])
              }
              info={errorToInputInfo(
                props.validationErrors?.[1]?.endTime,
                i18n.validationErrors
              )}
              hideErrorsBeforeTouched={!props.showAllErrors}
            />
          </FixedSpaceRow>
          <IconButton
            icon={faTrash}
            onClick={() => props.updateTimes([timeRange])}
          />
        </>
      ) : null}
    </>
  )
})

const TimeInputGrid = styled.div`
  display: grid;
  grid-template-columns: max-content max-content auto;
  grid-column-gap: ${defaultMargins.s};
  grid-row-gap: ${defaultMargins.s};
  align-items: center;
`

const Week = styled.div`
  color: ${(p) => p.theme.colors.main.m1};
  font-weight: ${fontWeights.semibold};
  grid-column-start: 1;
  grid-column-end: 4;
`

const Separator = styled.div`
  border-top: 2px dotted ${(p) => p.theme.colors.grayscale.g15};
  margin: ${defaultMargins.s} 0;
  grid-column-start: 1;
  grid-column-end: 4;
`

const MissingDateRange = styled(Light)`
  grid-column-start: 1;
  grid-column-end: 4;
`
