// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import { RouteComponentProps } from 'react-router-dom'
import styled from 'styled-components'
import { UUID } from '../../../lib-common/types'
import '../../../lib-components/layout/ButtonContainer'
import {
  Container,
  ContentArea
} from '../../../lib-components/layout/Container'
import StickyFooter from '../../../lib-components/layout/StickyFooter'
import { H2 } from '../../../lib-components/typography'
import { defaultMargins, Gap } from '../../../lib-components/white-space'
import { CheckboxQuestion } from './components/CheckboxQuestion'
import { ReadOnlyValue } from './components/ReadOnlyValue'
import { TextQuestion } from './components/TextQuestion'
import { AuthorsSection } from './sections/AuthorsSection'
import { EvaluationDiscussionSection } from './sections/EvaluationDiscussionSection'
import { VasuDiscussionSection } from './sections/VasuDiscussionSection'
import { VasuEvents } from './sections/VasuEvents'
import { VasuHeader } from './sections/VasuHeader'
import { useVasu } from './use-vasu'
import {
  isCheckboxQuestion,
  isMultiSelectQuestion,
  isRadioGroupQuestion,
  isTextQuestion,
  MultiSelectQuestion,
  RadioGroupQuestion
} from './vasu-content'
import { VasuStateTransitionButtons } from './VasuStateTransitionButtons'

const FooterContainer = styled.div`
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: ${defaultMargins.s};
`

export default React.memo(function VasuPage({
  match
}: RouteComponentProps<{ id: UUID }>) {
  const { id } = match.params

  const {
    vasu,
    content,
    authorsContent,
    vasuDiscussionContent,
    evaluationDiscussionContent
  } = useVasu(id)

  const dynamicSectionsOffset = 1
  const getDynamicQuestionNumber = (
    sectionIndex: number,
    questionIndex: number
  ) => `${sectionIndex + 1 + dynamicSectionsOffset}.${questionIndex + 1}`

  function renderRadioGroupQuestion(
    question: RadioGroupQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    const { value: selectedValue } = content.sections[sectionIndex].questions[
      questionIndex
    ] as RadioGroupQuestion
    return (
      <ReadOnlyValue
        label={`${getDynamicQuestionNumber(sectionIndex, questionIndex)} ${
          question.name
        }`}
        value={
          question.options.find((option) => option.key === selectedValue)?.name
        }
      />
    )
  }

  function renderMultiSelectQuestion(
    question: MultiSelectQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    const { value: selectedValues } = content.sections[sectionIndex].questions[
      questionIndex
    ] as MultiSelectQuestion
    return (
      <ReadOnlyValue
        label={`${getDynamicQuestionNumber(sectionIndex, questionIndex)} ${
          question.name
        }`}
        value={question.options
          .filter((o) => selectedValues.includes(o.key))
          .map((o) => o.name)
          .join(', ')}
      />
    )
  }

  return (
    <Container>
      <Gap size={'L'} />
      {vasu && (
        <>
          <VasuHeader document={vasu} />
          <AuthorsSection sectionIndex={0} content={authorsContent} />

          {content.sections.map((section, sectionIndex) => (
            <ContentArea opaque key={section.name}>
              <H2>
                {sectionIndex + 1 + dynamicSectionsOffset}. {section.name}
              </H2>
              {section.questions.map((question, questionIndex) => {
                const questionNumber = getDynamicQuestionNumber(
                  sectionIndex,
                  questionIndex
                )
                return (
                  <Fragment key={question.name}>
                    {isTextQuestion(question) ? (
                      <TextQuestion
                        question={question}
                        questionNumber={questionNumber}
                      />
                    ) : isCheckboxQuestion(question) ? (
                      <CheckboxQuestion
                        question={question}
                        questionNumber={questionNumber}
                      />
                    ) : isRadioGroupQuestion(question) ? (
                      renderRadioGroupQuestion(
                        question,
                        sectionIndex,
                        questionIndex
                      )
                    ) : isMultiSelectQuestion(question) ? (
                      renderMultiSelectQuestion(
                        question,
                        sectionIndex,
                        questionIndex
                      )
                    ) : undefined}
                    {questionIndex < section.questions.length - 1 && (
                      <Gap size={'L'} />
                    )}
                  </Fragment>
                )
              })}
            </ContentArea>
          ))}
          <VasuDiscussionSection
            sectionIndex={content.sections.length + dynamicSectionsOffset}
            content={vasuDiscussionContent}
          />
          {vasu.documentState !== 'DRAFT' && (
            <EvaluationDiscussionSection
              sectionIndex={content.sections.length + dynamicSectionsOffset + 1}
              content={evaluationDiscussionContent}
            />
          )}
          <VasuEvents
            document={vasu}
            vasuDiscussionDate={vasuDiscussionContent.discussionDate}
            evaluationDiscussionDate={
              evaluationDiscussionContent.discussionDate
            }
          />
        </>
      )}
      <StickyFooter>
        <FooterContainer>
          {vasu && (
            <VasuStateTransitionButtons
              childId={vasu.child.id}
              documentId={vasu.id}
              state={vasu.documentState}
            />
          )}
        </FooterContainer>
      </StickyFooter>
    </Container>
  )
})
