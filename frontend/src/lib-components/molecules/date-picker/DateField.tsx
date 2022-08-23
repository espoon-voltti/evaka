// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createCalendar } from '@internationalized/date'
import {
  AriaDateFieldProps,
  useDateField,
  useDateSegment
} from '@react-aria/datepicker'
import {
  useDateFieldState,
  DateSegment,
  DateFieldState
} from '@react-stately/datepicker'
import { DateValue } from '@react-types/datepicker'
import classNames from 'classnames'
import React from 'react'
import styled from 'styled-components'

import { Language } from 'lib-common/generated/api-types/daycare'

const DateInput = styled.div`
  display: flex;
`

export default React.memo(function DateField(
  props: AriaDateFieldProps<DateValue> & { locale: Language }
) {
  const state = useDateFieldState({
    ...props,
    locale: props.locale,
    createCalendar
  })

  const ref = React.useRef<HTMLDivElement>(null)
  const { labelProps, fieldProps } = useDateField(props, state, ref)

  return (
    <DateInput
      {...fieldProps}
      ref={ref}
      onFocus={(ev) => {
        props.onFocus?.(ev)
        labelProps.onFocus?.(ev)
      }}
      onBlur={(ev) => {
        props.onBlur?.(ev)
        labelProps.onBlur?.(ev)
      }}
    >
      {state.segments.map((segment, i) => (
        <DateSegment key={i} segment={segment} state={state} />
      ))}
    </DateInput>
  )
})

const Segment = styled.div`
  outline: none;
  padding: 2px 4px;

  &.placeholder {
    color: ${(p) => p.theme.colors.grayscale.g70};
  }

  &:focus {
    background-color: ${(p) => p.theme.colors.main.m2};
    border-radius: 4px;
    color: ${(p) => p.theme.colors.grayscale.g0};
  }
`

function DateSegment({
  segment,
  state
}: {
  segment: DateSegment
  state: DateFieldState
}) {
  const ref = React.useRef<HTMLDivElement>(null)
  const { segmentProps } = useDateSegment(segment, state, ref)

  return (
    <Segment
      {...segmentProps}
      ref={ref}
      className={classNames({ placeholder: segment.isPlaceholder })}
      data-qa={`date-picker-segment-${segment.type}`}
    >
      {segment.type === 'day' || segment.type === 'month'
        ? segment.text.padStart(2, '0')
        : segment.text}
    </Segment>
  )
}
