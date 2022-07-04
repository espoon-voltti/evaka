// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import last from 'lodash/last'
import React, { Fragment } from 'react'
import styled, { css } from 'styled-components'

import { MultiSelectQuestion } from 'lib-common/api-types/vasu'
import {
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
import { RadioGroupQuestion as RadioGroupQuestionElem } from '../components/RadioGroupQuestion'
import StaticInfoSubsection from '../components/StaticInfoSubsection'
import { TextQuestion as TextQuestionElem } from '../components/TextQuestion'
import { CitizenVasuMetadata } from '../use-vasu'
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
  state: VasuDocumentState
  translations: VasuTranslations
  vasu: CitizenVasuMetadata
}

export function CitizenDynamicSections({
  sections,
  sectionIndex: sectionOffset,
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

    const isLastQuestionFollowup = last(section.questions)?.type === 'FOLLOWUP'
    return (
      <Fragment key={section.name}>
        <SectionContent
          opaque
          highlighted={false}
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
                      translations={translations}
                    />
                  ) : isCheckboxQuestion(question) ? (
                    <CheckboxQuestionElem
                      question={question}
                      questionNumber={questionNumber}
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
                      translations={translations}
                    />
                  ) : isMultiFieldQuestion(question) ? (
                    <MultiFieldQuestionElem
                      question={question}
                      questionNumber={questionNumber}
                      translations={translations}
                    />
                  ) : isMultiFieldListQuestion(question) ? (
                    <MultiFieldListQuestionElem
                      question={question}
                      questionNumber={questionNumber}
                      translations={translations}
                    />
                  ) : isDateQuestion(question) ? (
                    <DateQuestionElem
                      question={question}
                      questionNumber={questionNumber}
                      translations={translations}
                    />
                  ) : isFollowup(question) && state !== 'DRAFT' ? (
                    <FollowupQuestionElem
                      question={question}
                      questionNumber={questionNumber}
                      translations={translations}
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
