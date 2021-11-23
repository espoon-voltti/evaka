// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { cloneDeep } from 'lodash'
import React, { Dispatch, Fragment, SetStateAction } from 'react'
import { last } from 'lodash'
import styled from 'styled-components'
import { ContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { CheckboxQuestion as CheckboxQuestionElem } from '../components/CheckboxQuestion'
import { MultiSelectQuestion as MultiSelectQuestionElem } from '../components/MultiSelectQuestion'
import { RadioGroupQuestion as RadioGroupQuestionElem } from '../components/RadioGroupQuestion'
import { TextQuestion as TextQuestionElem } from '../components/TextQuestion'
import FollowupQuestionElem from '../components/FollowupQuestion'
import {
  CheckboxQuestion,
  Followup,
  isCheckboxQuestion,
  isFollowup,
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
import { VasuTranslations } from 'lib-customizations/employee'
import { VasuDocumentState } from '../api'

const getDynamicQuestionNumber = (
  sectionOffset: number,
  sectionIndex: number,
  questionIndex: number
) => `${sectionOffset + sectionIndex + 1}.${questionIndex + 1}`

interface Props {
  sections: VasuSection[]
  sectionIndex: number
  setContent?: Dispatch<SetStateAction<VasuContent>>
  state: VasuDocumentState
  translations: VasuTranslations
}

export function DynamicSections({
  sections,
  sectionIndex: sectionOffset,
  setContent,
  state,
  translations
}: Props) {
  const content = sections.map((section, sectionIndex) => {
    const isLastQuestionFollowup = last(section.questions)?.type === 'FOLLOWUP'
    return (
      <Fragment key={section.name}>
        <SectionContent opaque padBottom={!isLastQuestionFollowup}>
          <H2>
            {sectionIndex + 1 + sectionOffset}. {section.name}
          </H2>
          <Questions>
            {section.questions.map((question, questionIndex) => {
              const questionNumber = getDynamicQuestionNumber(
                sectionOffset,
                sectionIndex,
                questionIndex
              )
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
                      translations={translations}
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
                      translations={translations}
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
                                  .questions[
                                  questionIndex
                                ] as RadioGroupQuestion
                                question1.value = selectedOption.key
                                return clone
                              })
                          : undefined
                      }
                      translations={translations}
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
                                  .questions[
                                  questionIndex
                                ] as MultiSelectQuestion
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
                      translations={translations}
                    />
                  ) : isFollowup(question) && state !== 'DRAFT' ? (
                    <FollowupQuestionElem
                      question={question}
                      questionNumber={questionNumber}
                      translations={translations}
                      onChange={
                        setContent
                          ? (value: string) =>
                              setContent((prev) => {
                                const clone = cloneDeep(prev)
                                const question1 = clone.sections[sectionIndex]
                                  .questions[questionIndex] as Followup
                                question1.value = value
                                return clone
                              })
                          : undefined
                      }
                    />
                  ) : undefined}
                </Fragment>
              )
            })}
          </Questions>
        </SectionContent>
      </Fragment>
    )
  })
  return <>{content}</>
}

const SectionContent = styled(ContentArea)<{ padBottom: boolean }>`
  /* make selector specific enough */
  && {
    padding-bottom: ${(p) => (p.padBottom ? defaultMargins.L : '0px')};
  }
`

const Questions = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: ${defaultMargins.L};
`
