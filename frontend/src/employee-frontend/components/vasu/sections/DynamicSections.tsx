// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import cloneDeep from 'lodash/cloneDeep'
import last from 'lodash/last'
import React, { Dispatch, Fragment, SetStateAction } from 'react'
import styled, { css } from 'styled-components'

import {
  CheckboxQuestion,
  Followup,
  FollowupEntry,
  MultiFieldQuestion,
  MultiSelectQuestion,
  RadioGroupQuestion,
  TextQuestion
} from 'lib-common/api-types/vasu'
import {
  VasuContent,
  VasuSection,
  VasuDocumentState
} from 'lib-common/generated/api-types/vasu'
import { ContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { VasuTranslations } from 'lib-customizations/employee'

import { CheckboxQuestion as CheckboxQuestionElem } from '../components/CheckboxQuestion'
import DateQuestionElem from '../components/DateQuestion'
import FollowupQuestionElem from '../components/FollowupQuestion'
import MultiFieldListQuestionElem from '../components/MultiFieldListQuestion'
import MultiFieldQuestionElem from '../components/MultiFieldQuestion'
import { MultiSelectQuestion as MultiSelectQuestionElem } from '../components/MultiSelectQuestion'
import ParagraphElem from '../components/Paragraph'
import {
  RadioGroupQuestion as RadioGroupQuestionElem,
  RadioGroupSelectedValue
} from '../components/RadioGroupQuestion'
import StaticInfoSubsection from '../components/StaticInfoSubsection'
import { TextQuestion as TextQuestionElem } from '../components/TextQuestion'
import { VasuMetadata } from '../use-vasu'
import {
  getQuestionNumber,
  isCheckboxQuestion,
  isDateQuestion,
  isFollowup,
  isMultiFieldListQuestion,
  isMultiFieldQuestion,
  isMultiSelectQuestion,
  isParagraph,
  isRadioGroupQuestion,
  isStaticInfoSubsection,
  isTextQuestion
} from '../vasu-content'

interface Props {
  sections: VasuSection[]
  sectionIndex: number
  setContent?: Dispatch<SetStateAction<VasuContent>>
  state: VasuDocumentState
  translations: VasuTranslations
  vasu: VasuMetadata
}

export function DynamicSections({
  sections,
  sectionIndex: sectionOffset,
  setContent,
  state,
  translations,
  vasu
}: Props) {
  const content = sections.map((section, sectionIndex) => {
    if (section.hideBeforeReady && state === 'DRAFT') {
      return null
    }

    const dependantsById = new Map(
      section.questions
        .filter((q) => q.id)
        .map((question) => {
          if ('value' in question) {
            return [question.id, !!question.value]
          }

          return [question.id, false]
        })
    )

    const questionsToShow = section.questions.filter(
      ({ dependsOn }) =>
        !dependsOn || dependsOn.every((id) => dependantsById.get(id))
    )

    const highlightSection = !!setContent && section.hideBeforeReady
    const isLastQuestionFollowup = last(section.questions)?.type === 'FOLLOWUP'
    return (
      <Fragment key={section.name}>
        <SectionContent
          opaque
          highlighted={highlightSection}
          padBottom={!isLastQuestionFollowup || state === 'DRAFT'}
          data-qa="vasu-document-section"
        >
          <H2 noMargin>
            {sectionIndex + 1 + sectionOffset}. {section.name}
          </H2>
          <Gap size="m" />
          <Questions>
            {questionsToShow.map((question, questionIndex) => {
              const questionNumber = getQuestionNumber(
                sectionOffset + sectionIndex,
                section.questions,
                question
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
                        question.value !== null
                          ? {
                              key: question.value,
                              range: question.dateRange
                            }
                          : null
                      }
                      onChange={
                        setContent
                          ? (selectedOption: RadioGroupSelectedValue) =>
                              setContent((prev) => {
                                const clone = cloneDeep(prev)
                                const question1 = clone.sections[sectionIndex]
                                  .questions[
                                  questionIndex
                                ] as RadioGroupQuestion
                                question1.value = selectedOption.key
                                question1.dateRange = selectedOption.range
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
                          ? (option, value, date) =>
                              setContent((prev) => {
                                const clone = cloneDeep(prev)
                                const question1 = clone.sections[sectionIndex]
                                  .questions[
                                  questionIndex
                                ] as MultiSelectQuestion
                                if (typeof value == 'boolean') {
                                  if (
                                    value &&
                                    !question1.value.includes(option.key)
                                  )
                                    question1.value.push(option.key)
                                  if (!value) {
                                    question1.value = question1.value.filter(
                                      (i) => i !== option.key
                                    )
                                    if (question1.textValue)
                                      delete question1.textValue[option.key]
                                  }
                                } else {
                                  question1.textValue
                                    ? (question1.textValue[option.key] = value)
                                    : (question.textValue = {
                                        [option.key]: value
                                      })
                                }
                                if (date !== undefined) {
                                  if (question1.dateValue) {
                                    if (date) {
                                      question1.dateValue[option.key] = date
                                    } else {
                                      delete question1.dateValue[option.key]
                                    }
                                  } else if (date) {
                                    question1.dateValue = {
                                      [option.key]: date
                                    }
                                  }
                                }
                                return clone
                              })
                          : undefined
                      }
                      translations={translations}
                    />
                  ) : isMultiFieldQuestion(question) ? (
                    <MultiFieldQuestionElem
                      question={question}
                      questionNumber={questionNumber}
                      translations={translations}
                      onChange={
                        setContent
                          ? (index, value) =>
                              setContent((prev) => {
                                const clone = cloneDeep(prev)
                                const question1 = clone.sections[sectionIndex]
                                  .questions[
                                  questionIndex
                                ] as MultiFieldQuestion
                                question1.value[index] = value
                                return clone
                              })
                          : undefined
                      }
                    />
                  ) : isMultiFieldListQuestion(question) ? (
                    <MultiFieldListQuestionElem
                      question={question}
                      questionNumber={questionNumber}
                      translations={translations}
                      onChange={
                        setContent
                          ? (value) =>
                              setContent((prev) => {
                                const clone = cloneDeep(prev)
                                clone.sections[sectionIndex].questions[
                                  questionIndex
                                ] = {
                                  ...question,
                                  value
                                }
                                return clone
                              })
                          : undefined
                      }
                    />
                  ) : isDateQuestion(question) ? (
                    <DateQuestionElem
                      question={question}
                      questionNumber={questionNumber}
                      translations={translations}
                      onChange={
                        setContent
                          ? (value) =>
                              setContent((prev) => {
                                const clone = cloneDeep(prev)
                                clone.sections[sectionIndex].questions[
                                  questionIndex
                                ] = {
                                  ...question,
                                  value
                                }
                                return clone
                              })
                          : undefined
                      }
                    />
                  ) : isFollowup(question) && state !== 'DRAFT' ? (
                    <FollowupQuestionElem
                      question={question}
                      questionNumber={questionNumber}
                      translations={translations}
                      onChange={
                        setContent
                          ? (value: FollowupEntry[]) =>
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
                  ) : isParagraph(question) ? (
                    <ParagraphElem question={question} />
                  ) : isStaticInfoSubsection(question) ? (
                    <StaticInfoSubsection
                      type={vasu.type}
                      basics={vasu.basics}
                      templateRange={vasu.templateRange}
                      translations={translations}
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

export const SectionContent = styled(ContentArea)<{
  highlighted: boolean
  padBottom: boolean
}>`
  border-left: 5px solid
    ${({ highlighted, theme }) =>
      highlighted ? theme.colors.main.m1 : 'transparent'};
  padding: ${defaultMargins.L};
  padding-left: calc(${defaultMargins.L} - 5px);
  ${({ padBottom }) =>
    !padBottom
      ? css`
          padding-bottom: 0;
        `
      : ''}
`

const Questions = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: ${defaultMargins.L};
`
