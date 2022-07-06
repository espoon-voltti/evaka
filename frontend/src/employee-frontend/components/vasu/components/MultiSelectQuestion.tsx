// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { MultiSelectQuestion, QuestionOption } from 'lib-common/api-types/vasu'
import LocalDate from 'lib-common/local-date'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { VasuTranslations } from 'lib-customizations/employee'

import QuestionInfo from '../QuestionInfo'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import { QuestionProps } from './question-props'

interface Props extends QuestionProps<MultiSelectQuestion> {
  selectedValues: string[]
  onChange?: (
    option: QuestionOption,
    value: boolean | string,
    dateValue?: LocalDate | null
  ) => void
  translations: VasuTranslations
}

const SubText = styled(P)`
  margin: 0;
  margin-left: 46px;
  margin-top: -${defaultMargins.xs};
  margin-bottom: ${defaultMargins.xs};
`

export function MultiSelectQuestion({
  onChange,
  question: { name, options, info, textValue, dateValue },
  questionNumber,
  selectedValues,
  translations
}: Props) {
  const { i18n } = useTranslation()

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
            {options.map((option) => {
              return (
                <React.Fragment key={option.key}>
                  <FixedSpaceRow>
                    <Checkbox
                      key={option.key}
                      checked={selectedValues.includes(option.key)}
                      label={option.name}
                      onChange={(checked) => onChange(option, checked)}
                      data-qa={`multi-select-question-option-${option.key}`}
                    />
                    {option.date && (
                      <DatePicker
                        locale="fi"
                        date={dateValue?.[option.key] ?? null}
                        onChange={(date) =>
                          onChange(
                            option,
                            textValue?.[option.key] ??
                              selectedValues.includes(option.key),
                            date
                          )
                        }
                        data-qa={`multi-select-question-option-${option.key}-date`}
                        errorTexts={i18n.validationErrors}
                        hideErrorsBeforeTouched
                      />
                    )}
                  </FixedSpaceRow>
                  {!!option.subText && (
                    <SubText noMargin>{option.subText}</SubText>
                  )}
                  {option.textAnswer && textValue && (
                    <InputField
                      value={textValue[option.key] || ''}
                      onChange={(text) => onChange(option, text)}
                      placeholder={option.name}
                      data-qa={`multi-select-question-option-text-input-${option.key}`}
                    />
                  )}
                </React.Fragment>
              )
            })}
          </FixedSpaceColumn>
        </>
      ) : (
        <ValueOrNoRecord
          text={options
            .filter((option) => selectedValues.includes(option.key))
            .map((o) => {
              const date = dateValue?.[o.key]
              const name = `${o.name}${date ? ` ${date.format()}` : ''}${
                o.subText ? `\n${o.subText}` : ''
              }`

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
