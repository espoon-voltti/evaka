// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { MultiSelectQuestion, QuestionOption } from 'lib-common/api-types/vasu'
import LocalDate from 'lib-common/local-date'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Bold, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { VasuTranslations } from 'lib-customizations/employee'

import QuestionInfo from '../QuestionInfo'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import { QuestionProps } from './question-props'

interface Props extends QuestionProps<MultiSelectQuestion> {
  onChange?: (
    option: QuestionOption,
    value: boolean | string,
    dateValue: LocalDate | null,
    dateRangeValue: { start: LocalDate; end: LocalDate } | null
  ) => void
  translations: VasuTranslations
}

const SubText = styled(P)`
  margin-top: -${defaultMargins.xs} 0 ${defaultMargins.xs} 46px;
`

export function MultiSelectQuestion({
  onChange,
  question,
  questionNumber,
  translations
}: Props) {
  const { name, options, info, value, textValue, dateValue, dateRangeValue } =
    question
  return (
    <div>
      <QuestionInfo info={info}>
        <Label>
          {questionNumber} {name}
        </Label>
      </QuestionInfo>
      {onChange ? (
        <>
          <Gap size="m" />
          <FixedSpaceColumn>
            {options.map((option) =>
              option.isIntervention ? (
                <QuestionInfo key={option.key} info={option.info ?? null}>
                  <Bold>{option.name}</Bold>
                </QuestionInfo>
              ) : (
                <MultiSelectQuestionOption
                  key={option.key}
                  question={question}
                  option={option}
                  onChange={onChange}
                />
              )
            )}
          </FixedSpaceColumn>
        </>
      ) : (
        <ValueOrNoRecord
          text={options
            .filter((option) => value.includes(option.key))
            .map((o) => {
              const date = dateValue?.[o.key]
              const dateStr = date ? ` ${date.format()}` : ''
              const dateRange = dateRangeValue?.[o.key]
              const dateRangeStr = dateRange
                ? ` ${dateRange.start.format()}–${dateRange.end.format()}`
                : ''
              const subTextStr = o.subText ? `\n${o.subText}` : ''
              const name = `${o.name}${dateStr}${dateRangeStr}${subTextStr}`
              return textValue && textValue[o.key]
                ? `${name}: ${textValue[o.key]}`
                : name
            })
            .join(', ')}
          translations={translations}
          dataQa={`value-or-no-record-${questionNumber}`}
        />
      )}
    </div>
  )
}

const MultiSelectQuestionOption = React.memo(
  function MultiSelectQuestionOption({
    question: { value, maxSelections, textValue, dateValue, dateRangeValue },
    option,
    onChange
  }: {
    question: MultiSelectQuestion
    option: QuestionOption
    onChange: (
      option: QuestionOption,
      value: boolean | string,
      dateValue: LocalDate | null,
      dateRange: { start: LocalDate; end: LocalDate } | null
    ) => void
  }) {
    const checked = value.includes(option.key)
    const date = dateValue?.[option.key] ?? null
    const dateRange = dateRangeValue?.[option.key] ?? null
    const [startDate, setStartDate] = useState(dateRange?.start ?? null)
    const [endDate, setEndDate] = useState(dateRange?.end ?? null)

    const handleChange = useCallback(
      (
        checkedOrText: boolean | string,
        date: LocalDate | null,
        startDate: LocalDate | null,
        endDate: LocalDate | null
      ) => {
        onChange(
          option,
          checkedOrText,
          option.date ? date : null,
          option.dateRange && startDate && endDate
            ? { start: startDate, end: endDate }
            : null
        )
      },
      [onChange, option]
    )

    const handleCheckboxChange = useCallback(
      (newChecked: boolean) => {
        if (!newChecked) {
          setStartDate(null)
          setEndDate(null)
        }
        handleChange(newChecked, date, startDate, endDate)
      },
      [date, endDate, handleChange, startDate]
    )

    const handleDateChange = useCallback(
      (newDate: LocalDate | null) => {
        handleChange(checked, newDate, startDate, endDate)
      },
      [checked, endDate, handleChange, startDate]
    )

    const handleStartDateChange = useCallback(
      (newStartDate: LocalDate | null) => {
        setStartDate(newStartDate)
        handleChange(checked, date, newStartDate, endDate)
      },
      [checked, date, endDate, handleChange]
    )

    const handleEndDateChange = useCallback(
      (newEndDate: LocalDate | null) => {
        setEndDate(newEndDate)
        handleChange(checked, date, startDate, newEndDate)
      },
      [checked, date, handleChange, startDate]
    )

    const handleTextChange = useCallback(
      (newText: string) => {
        handleChange(newText, date, startDate, endDate)
      },
      [date, endDate, handleChange, startDate]
    )

    return (
      <ExpandingInfo key={option.key} info={option.info}>
        <FixedSpaceRow>
          <Checkbox
            key={option.key}
            checked={checked}
            label={option.name}
            onChange={handleCheckboxChange}
            data-qa={`multi-select-question-option-${option.key}`}
            disabled={
              !!maxSelections &&
              value.length >= maxSelections &&
              !value.includes(option.key)
            }
          />
          {option.date ? (
            <DatePicker
              locale="fi"
              date={date}
              onChange={handleDateChange}
              data-qa={`multi-select-question-option-${option.key}-date`}
              hideErrorsBeforeTouched
            />
          ) : option.dateRange ? (
            <div>
              <DatePicker
                locale="fi"
                date={startDate}
                onChange={handleStartDateChange}
                data-qa={`multi-select-question-option-${option.key}-start-date`}
                hideErrorsBeforeTouched
              />
              <span>-</span>
              <DatePicker
                locale="fi"
                date={endDate}
                onChange={handleEndDateChange}
                data-qa={`multi-select-question-option-${option.key}-end-date`}
                hideErrorsBeforeTouched
              />
            </div>
          ) : null}
        </FixedSpaceRow>
        {option.subText ? <SubText noMargin>{option.subText}</SubText> : null}
        {option.textAnswer && textValue && (
          <InputField
            value={textValue[option.key] || ''}
            onChange={handleTextChange}
            placeholder={option.name}
            data-qa={`multi-select-question-option-text-input-${option.key}`}
          />
        )}
      </ExpandingInfo>
    )
  }
)
