// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { cloneDeep } from 'lodash'
import React, { Dispatch, Fragment, SetStateAction } from 'react'
import { ContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { CheckboxQuestion as CheckboxQuestionElem } from '../components/CheckboxQuestion'
import { MultiSelectQuestion as MultiSelectQuestionElem } from '../components/MultiSelectQuestion'
import { RadioGroupQuestion as RadioGroupQuestionElem } from '../components/RadioGroupQuestion'
import { TextQuestion as TextQuestionElem } from '../components/TextQuestion'
import {
  CheckboxQuestion,
  isCheckboxQuestion,
  isMultiSelectQuestion,
  isRadioGroupQuestion,
  isTextQuestion,
  MultiSelectQuestion,
  QuestionOption,
  RadioGroupQuestion,
  TextQuestion,
  VasuContent,
  VasuSection
} from '../vasu-content'

const getDynamicQuestionNumber = (
  sectionOffset: number,
  sectionIndex: number,
  questionIndex: number
) => `${sectionOffset + sectionIndex + 1}.${questionIndex + 1}`

interface Props {
  sections: VasuSection[]
  sectionIndex: number
  setContent?: Dispatch<SetStateAction<VasuContent>>
}
export function DynamicSections({
  sections,
  sectionIndex: sectionOffset,
  setContent
}: Props) {
  const renderGapsBetweenSections = !!setContent
  const content = sections.map((section, sectionIndex) => {
    return (
      <Fragment key={section.name}>
        <ContentArea opaque>
          <H2>
            {sectionIndex + 1 + sectionOffset}. {section.name}
          </H2>
          {section.questions.map((question, questionIndex) => {
            const questionNumber = getDynamicQuestionNumber(
              sectionOffset,
              sectionIndex,
              questionIndex
            )
            const isLastQuestion =
              questionIndex === section.questions.length - 1
            return (
              <Fragment key={question.name}>
                {isTextQuestion(question) ? (
                  <TextQuestionElem
                    question={question}
                    questionNumber={questionNumber}
                    onChange={
                      setContent
                        ? (value: string) =>
                            setContent((prev) => {
                              const clone = cloneDeep(prev)
                              const question1 = clone.sections[sectionIndex]
                                .questions[questionIndex] as TextQuestion
                              question1.value = value
                              return clone
                            })
                        : undefined
                    }
                  />
                ) : isCheckboxQuestion(question) ? (
                  <CheckboxQuestionElem
                    question={question}
                    questionNumber={questionNumber}
                    onChange={
                      setContent
                        ? (checked: boolean) =>
                            setContent((prev) => {
                              const clone = cloneDeep(prev)
                              const question1 = clone.sections[sectionIndex]
                                .questions[questionIndex] as CheckboxQuestion
                              question1.value = checked
                              return clone
                            })
                        : undefined
                    }
                  />
                ) : isRadioGroupQuestion(question) ? (
                  <RadioGroupQuestionElem
                    questionNumber={questionNumber}
                    question={question}
                    selectedValue={
                      (
                        sections[sectionIndex].questions[
                          questionIndex
                        ] as RadioGroupQuestion
                      ).value
                    }
                    onChange={
                      setContent
                        ? (selectedOption: QuestionOption) =>
                            setContent((prev) => {
                              const clone = cloneDeep(prev)
                              const question1 = clone.sections[sectionIndex]
                                .questions[questionIndex] as RadioGroupQuestion
                              question1.value = selectedOption.key
                              return clone
                            })
                        : undefined
                    }
                  />
                ) : isMultiSelectQuestion(question) ? (
                  <MultiSelectQuestionElem
                    question={question}
                    questionNumber={questionNumber}
                    selectedValues={
                      (
                        sections[sectionIndex].questions[
                          questionIndex
                        ] as MultiSelectQuestion
                      ).value
                    }
                    onChange={
                      setContent
                        ? (option, checked) =>
                            setContent((prev) => {
                              const clone = cloneDeep(prev)
                              const question1 = clone.sections[sectionIndex]
                                .questions[questionIndex] as MultiSelectQuestion
                              if (
                                checked &&
                                !question1.value.includes(option.key)
                              )
                                question1.value.push(option.key)
                              if (!checked)
                                question1.value = question1.value.filter(
                                  (i) => i !== option.key
                                )
                              return clone
                            })
                        : undefined
                    }
                  />
                ) : undefined}
                {!isLastQuestion && <Gap size={'L'} />}
              </Fragment>
            )
          })}
        </ContentArea>
        {renderGapsBetweenSections && <Gap size={'L'} />}
      </Fragment>
    )
  })
  return <>{content}</>
}
