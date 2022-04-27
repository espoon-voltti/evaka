// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatPreferredName } from 'lib-common/names'
import {
  Repetition,
  ReservationFormData,
  TimeRangeErrors,
  TimeRanges,
  validateForm
} from 'lib-common/reservations'
import { SelectionChip } from 'lib-components/atoms/Chip'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import {
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import DatePicker, {
  DatePickerSpacer
} from 'lib-components/molecules/date-picker/DatePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { fontWeights, H2, Label, Light } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPlus, faTrash } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { errorToInputInfo } from '../input-info-helper'
import { useLang, useTranslation } from '../localization'

import { postReservations } from './api'

interface Props {
  onClose: () => void
  onReload: () => void
  availableChildren: ReservationChild[]
  reservableDays: FiniteDateRange[]
  firstReservableDate: LocalDate
}

export default React.memo(function ReservationModal({
  onClose,
  onReload,
  availableChildren,
  reservableDays,
  firstReservableDate
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const [formData, setFormData] = useState<ReservationFormData>({
    selectedChildren: availableChildren.map((child) => child.id),
    startDate: firstReservableDate.format(),
    endDate: '',
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
    () => validateForm(reservableDays, formData),
    [reservableDays, formData]
  )

  const shiftCareRange = useMemo(() => {
    if (formData.repetition !== 'IRREGULAR') return

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

  const childrenInShiftCare = useMemo(
    () => availableChildren.some(({ inShiftCareUnit }) => inShiftCareUnit),
    [availableChildren]
  )

  const includedDays = useMemo(() => {
    const combinedOperationDays = availableChildren.reduce<number[]>(
      (totalOperationDays, child) => [
        ...new Set([...totalOperationDays, ...child.maxOperationalDays])
      ],
      []
    )

    return [1, 2, 3, 4, 5, 6, 7].filter((day) =>
      combinedOperationDays.includes(day)
    )
  }, [availableChildren])

  const isValidDate = useCallback(
    (date: LocalDate) => reservableDays.some((r) => r.includes(date)),
    [reservableDays]
  )

  return (
    <ModalAccessibilityWrapper>
      <AsyncFormModal
        mobileFullScreen
        title={i18n.calendar.reservationModal.title}
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
        resolveDisabled={formData.selectedChildren.length === 0}
        rejectAction={onClose}
        rejectLabel={i18n.common.cancel}
      >
        <H2>{i18n.calendar.reservationModal.selectChildren}</H2>
        <Label>{i18n.calendar.reservationModal.selectChildrenLabel}</Label>
        <Gap size="xs" />
        <FixedSpaceFlexWrap>
          {availableChildren.map((child) => (
            <SelectionChip
              key={child.id}
              text={formatPreferredName(child)}
              selected={formData.selectedChildren.includes(child.id)}
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
              data-qa={`child-${child.id}`}
            />
          ))}
        </FixedSpaceFlexWrap>

        <H2>{i18n.calendar.reservationModal.dateRange}</H2>
        <Label>{i18n.calendar.reservationModal.selectRecurrence}</Label>
        <Select<Repetition>
          items={['DAILY', 'WEEKLY', 'IRREGULAR']}
          selectedItem={formData.repetition}
          onChange={(value) => {
            if (value) updateForm({ repetition: value })
          }}
          getItemLabel={(item) =>
            i18n.calendar.reservationModal.repetitions[item]
          }
          data-qa="repetition"
        />
        <Gap size="s" />

        <ExpandingInfo
          width="auto"
          ariaLabel={i18n.common.openExpandingInfo}
          info={i18n.calendar.reservationModal.dateRangeInfo(
            reservableDays[0].end
          )}
        >
          <Label>{i18n.calendar.reservationModal.dateRangeLabel}</Label>
        </ExpandingInfo>
        <FixedSpaceRow>
          <DatePicker
            date={formData.startDate}
            onChange={(date) => updateForm({ startDate: date })}
            locale={lang}
            isValidDate={isValidDate}
            info={errorToInputInfo(
              validationResult.errors?.startDate,
              i18n.validationErrors
            )}
            hideErrorsBeforeTouched={!showAllErrors}
            data-qa="start-date"
          />
          <DatePickerSpacer />
          <DatePicker
            date={formData.endDate}
            onChange={(date) => updateForm({ endDate: date })}
            locale={lang}
            isValidDate={isValidDate}
            info={errorToInputInfo(
              validationResult.errors?.endDate,
              i18n.validationErrors
            )}
            hideErrorsBeforeTouched={!showAllErrors}
            initialMonth={
              LocalDate.parseFiOrNull(formData.startDate) ??
              reservableDays[0]?.start
            }
            data-qa="end-date"
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
              updateTimes={(dailyTimes) => updateForm({ dailyTimes })}
              validationErrors={validationResult.errors?.dailyTimes}
              showAllErrors={showAllErrors}
              allowExtraTimeRange={childrenInShiftCare}
              dataQaPrefix="daily"
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
                  validationErrors={
                    validationResult.errors?.weeklyTimes?.[index]
                  }
                  showAllErrors={showAllErrors}
                  allowExtraTimeRange={childrenInShiftCare}
                  dataQaPrefix={`weekly-${index}`}
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
                      allowExtraTimeRange={childrenInShiftCare}
                      dataQaPrefix={`irregular-${date.formatIso()}`}
                    />
                  )}
                </Fragment>
              ))
            ) : (
              <MissingDateRange>
                {i18n.calendar.reservationModal.missingDateRange}
              </MissingDateRange>
            )
          ) : null}
        </TimeInputGrid>
      </AsyncFormModal>
    </ModalAccessibilityWrapper>
  )
})

const TimeInputs = React.memo(function TimeInputs(props: {
  label: JSX.Element
  times: TimeRanges | undefined
  updateTimes: (v: TimeRanges | undefined) => void
  validationErrors: TimeRangeErrors[] | undefined
  showAllErrors: boolean
  allowExtraTimeRange: boolean
  dataQaPrefix?: string
}) {
  const i18n = useTranslation()

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
          placeholder={i18n.calendar.reservationModal.start}
          data-qa={
            props.dataQaPrefix
              ? `${props.dataQaPrefix}-start-time-0`
              : undefined
          }
        />
        <span>–</span>
        <TimeInput
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
          placeholder={i18n.calendar.reservationModal.end}
          data-qa={
            props.dataQaPrefix ? `${props.dataQaPrefix}-end-time-0` : undefined
          }
        />
      </FixedSpaceRow>
      {!extraTimeRange && props.allowExtraTimeRange ? (
        <IconButton
          icon={faPlus}
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
              placeholder={i18n.calendar.reservationModal.start}
              data-qa={
                props.dataQaPrefix
                  ? `${props.dataQaPrefix}-start-time-1`
                  : undefined
              }
            />
            <span>–</span>
            <TimeInput
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
              placeholder={i18n.calendar.reservationModal.end}
              data-qa={
                props.dataQaPrefix
                  ? `${props.dataQaPrefix}-end-time-1`
                  : undefined
              }
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
