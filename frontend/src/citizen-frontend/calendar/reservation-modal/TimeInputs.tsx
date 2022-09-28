// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { errorToInputInfo } from 'citizen-frontend/input-info-helper'
import { useTranslation } from 'citizen-frontend/localization'
import { TimeRangeErrors, TimeRanges } from 'lib-common/reservations'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { faPlus, fasUserMinus, faTrash, faUserMinus } from 'lib-icons'

import { emptyTimeRange } from './utils'

interface BaseTimeInputProps {
  label: JSX.Element
  validationErrors: TimeRangeErrors[] | undefined
  showAllErrors: boolean
  allowExtraTimeRange: boolean
  dataQaPrefix?: string
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

interface TimeInputWithAbsencesProps extends BaseTimeInputProps {
  times: TimeRanges | 'absent' | 'day-off' | 'not-editable' | undefined
  updateTimes: (
    v: TimeRanges | 'absent' | 'day-off' | 'not-editable' | undefined
  ) => void
  showAbsences: true
}

interface TimeInputWithoutAbsencesProps extends BaseTimeInputProps {
  times: TimeRanges | undefined
  updateTimes: (v: TimeRanges | undefined) => void
  showAbsences: false
}

type TimeInputProps = TimeInputWithAbsencesProps | TimeInputWithoutAbsencesProps

export default React.memo(function TimeInputs(props: TimeInputProps) {
  const i18n = useTranslation()
  const [infoOpen, setInfoOpen] = useState(false)
  const onInfoClick = useCallback(() => setInfoOpen((prev) => !prev), [])

  if (!props.times) {
    return (
      <>
        <div>{props.label}</div>
        <div />
        <div />
      </>
    )
  }

  if (props.times === 'absent') {
    return (
      <>
        <div>{props.label}</div>
        <div>{i18n.calendar.reservationModal.absent}</div>
        <div>
          <IconButton
            data-qa={
              props.dataQaPrefix
                ? `${props.dataQaPrefix}-absent-button`
                : undefined
            }
            icon={fasUserMinus}
            onClick={() => props.updateTimes(emptyTimeRange)}
            aria-label={i18n.calendar.absentDisable}
          />
        </div>
      </>
    )
  }

  if (props.times === 'not-editable') {
    return (
      <>
        <div>{props.label}</div>
        <div>{i18n.calendar.reservationModal.absent}</div>
        <div>
          <InfoButton
            onClick={onInfoClick}
            aria-label={i18n.common.openExpandingInfo}
            data-qa="not-editable-info-button"
          />
        </div>
        {infoOpen && (
          <SpanRow>
            <ExpandingInfoBox
              data-qa={
                props.dataQaPrefix
                  ? `${props.dataQaPrefix}-absence-by-employee-info-box`
                  : undefined
              }
              close={onInfoClick}
              aria-label={i18n.calendar.absenceMarkedByEmployee}
              info={i18n.calendar.contactStaffToEditAbsence}
              closeLabel={i18n.common.close}
              width="full"
            />
          </SpanRow>
        )}
      </>
    )
  }

  if (props.times === 'day-off') {
    return (
      <>
        <div>{props.label}</div>
        <div>{i18n.calendar.reservationModal.dayOff}</div>
        <div />
      </>
    )
  }

  const [timeRange, extraTimeRange] = props.times
  return (
    <>
      <div>{props.label}</div>
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
          onFocus={props.onFocus}
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
          onFocus={props.onFocus}
        />
      </FixedSpaceRow>
      <FixedSpaceRow>
        {props.showAbsences && (
          <IconButton
            data-qa={
              props.dataQaPrefix
                ? `${props.dataQaPrefix}-absent-button`
                : undefined
            }
            icon={faUserMinus}
            onClick={() => props.updateTimes('absent')}
            aria-label={i18n.calendar.absentEnable}
          />
        )}
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
            aria-label={i18n.common.add}
          />
        ) : (
          <div />
        )}
      </FixedSpaceRow>
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
              onFocus={props.onFocus}
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
              onFocus={props.onFocus}
            />
          </FixedSpaceRow>
          <div>
            <IconButton
              icon={faTrash}
              onClick={() => props.updateTimes([timeRange])}
              aria-label={i18n.common.delete}
            />
          </div>
        </>
      ) : null}
    </>
  )
})

const SpanRow = styled.div`
  max-width: 380px;
  grid-column: 1/-1;
  grid-row: span 3;
`
