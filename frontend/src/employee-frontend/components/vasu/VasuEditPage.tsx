// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Button from 'lib-components/atoms/buttons/Button'
import { cloneDeep } from 'lodash'
import React, { Fragment } from 'react'
import { useHistory } from 'react-router'
import { RouteComponentProps } from 'react-router-dom'
import styled from 'styled-components'
import { DATE_FORMAT_TIME_ONLY, formatDate } from '../../../lib-common/date'
import { UUID } from '../../../lib-common/types'
import Checkbox from '../../../lib-components/atoms/form/Checkbox'
import Radio from '../../../lib-components/atoms/form/Radio'
import Spinner from '../../../lib-components/atoms/state/Spinner'
import {
  Container,
  ContentArea
} from '../../../lib-components/layout/Container'
import { FixedSpaceColumn } from '../../../lib-components/layout/flex-helpers'
import StickyFooter from '../../../lib-components/layout/StickyFooter'
import { Dimmed, H2, Label } from '../../../lib-components/typography'
import { defaultMargins, Gap } from '../../../lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import { CheckboxQuestion as CheckboxQuestionElem } from './components/CheckboxQuestion'
import { TextQuestion as TextQuestionElem } from './components/TextQuestion'
import { EditableAuthorsSection } from './sections/AuthorsSection'
import { EditableEvaluationDiscussionSection } from './sections/EvaluationDiscussionSection'
import { EditableVasuDiscussionSection } from './sections/VasuDiscussionSection'
import { VasuEvents } from './sections/VasuEvents'
import { VasuHeader } from './sections/VasuHeader'
import { useVasu, VasuStatus } from './use-vasu'
import {
  CheckboxQuestion,
  isCheckboxQuestion,
  isMultiSelectQuestion,
  isRadioGroupQuestion,
  isTextQuestion,
  MultiSelectQuestion,
  RadioGroupQuestion,
  TextQuestion
} from './vasu-content'

const FooterContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.s};
`

const StatusContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;

  ${Spinner} {
    margin: ${defaultMargins.xs};
    height: ${defaultMargins.s};
    width: ${defaultMargins.s};
  }
`

export default React.memo(function VasuEditPage({
  match
}: RouteComponentProps<{ id: UUID }>) {
  const { id } = match.params
  const { i18n } = useTranslation()
  const history = useHistory()

  const {
    vasu,
    content,
    setContent,
    authorsContent,
    setAuthorsContent,
    vasuDiscussionContent,
    setVasuDiscussionContent,
    evaluationDiscussionContent,
    setEvaluationDiscussionContent,
    status
  } = useVasu(id)

  function formatVasuStatus(status: VasuStatus): string | null {
    switch (status.state) {
      case 'loading-error':
        return i18n.common.error.unknown
      case 'save-error':
        return i18n.common.error.saveFailed
      case 'loading':
      case 'saving':
      case 'dirty':
      case 'clean':
        return status.savedAt
          ? `${i18n.common.saved} ${formatDate(
              status.savedAt,
              DATE_FORMAT_TIME_ONLY
            )}`
          : null
    }
  }
  const textualVasuStatus = formatVasuStatus(status)
  const showSpinner = status.state === 'saving'

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
    return (
      <>
        <Label>
          {getDynamicQuestionNumber(sectionIndex, questionIndex)}{' '}
          {question.name}
        </Label>
        <Gap size={'m'} />
        <FixedSpaceColumn>
          {question.options.map((option) => {
            const question1 = content.sections[sectionIndex].questions[
              questionIndex
            ] as RadioGroupQuestion
            return (
              <Radio
                key={option.key}
                checked={question1.value === option.key}
                label={option.name}
                onChange={() =>
                  setContent((prev) => {
                    const clone = cloneDeep(prev)
                    const question1 = clone.sections[sectionIndex].questions[
                      questionIndex
                    ] as RadioGroupQuestion
                    question1.value = option.key
                    return clone
                  })
                }
              />
            )
          })}
        </FixedSpaceColumn>
      </>
    )
  }

  function renderMultiSelectQuestion(
    question: MultiSelectQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    return (
      <>
        <Label>
          {getDynamicQuestionNumber(sectionIndex, questionIndex)}{' '}
          {question.name}
        </Label>
        <Gap size={'m'} />
        <FixedSpaceColumn>
          {question.options.map((option) => {
            const question1 = content.sections[sectionIndex].questions[
              questionIndex
            ] as MultiSelectQuestion
            return (
              <Checkbox
                key={option.key}
                checked={question1.value.includes(option.key)}
                label={option.name}
                onChange={(checked) =>
                  setContent((prev) => {
                    const clone = cloneDeep(prev)
                    const question1 = clone.sections[sectionIndex].questions[
                      questionIndex
                    ] as MultiSelectQuestion
                    if (checked && !question1.value.includes(option.key))
                      question1.value.push(option.key)
                    if (!checked)
                      question1.value = question1.value.filter(
                        (i) => i !== option.key
                      )
                    return clone
                  })
                }
              />
            )
          })}
        </FixedSpaceColumn>
      </>
    )
  }

  return (
    <Container>
      <Gap size={'L'} />
      {vasu && (
        <>
          <VasuHeader document={vasu} />
          <Gap size={'L'} />
          <EditableAuthorsSection
            sectionIndex={0}
            content={authorsContent}
            setContent={setAuthorsContent}
          />
          <Gap size={'L'} />
          {content.sections.map((section, sectionIndex) => {
            return (
              <Fragment key={section.name}>
                <ContentArea opaque>
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
                          <TextQuestionElem
                            question={question}
                            questionNumber={questionNumber}
                            onChange={(value) =>
                              setContent((prev) => {
                                const clone = cloneDeep(prev)
                                const question1 = clone.sections[sectionIndex]
                                  .questions[questionIndex] as TextQuestion
                                question1.value = value
                                return clone
                              })
                            }
                          />
                        ) : isCheckboxQuestion(question) ? (
                          <CheckboxQuestionElem
                            question={question}
                            questionNumber={questionNumber}
                            onChange={(checked) =>
                              setContent((prev) => {
                                const clone = cloneDeep(prev)
                                const question1 = clone.sections[sectionIndex]
                                  .questions[questionIndex] as CheckboxQuestion
                                question1.value = checked
                                return clone
                              })
                            }
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
                        <Gap size={'L'} />
                      </Fragment>
                    )
                  })}
                </ContentArea>
                <Gap size={'L'} />
              </Fragment>
            )
          })}
          <EditableVasuDiscussionSection
            sectionIndex={content.sections.length + dynamicSectionsOffset}
            content={vasuDiscussionContent}
            setContent={setVasuDiscussionContent}
          />
          <Gap size={'L'} />
          {vasu.documentState !== 'DRAFT' && (
            <>
              <EditableEvaluationDiscussionSection
                sectionIndex={
                  content.sections.length + dynamicSectionsOffset + 1
                }
                content={evaluationDiscussionContent}
                setContent={setEvaluationDiscussionContent}
              />
              <Gap size={'L'} />
            </>
          )}
          <VasuEvents
            document={vasu}
            vasuDiscussionDate={vasuDiscussionContent.discussionDate}
            evaluationDiscussionDate={
              evaluationDiscussionContent.discussionDate
            }
          />
          <Gap size={'L'} />
        </>
      )}
      <StickyFooter>
        <FooterContainer>
          <StatusContainer>
            <Dimmed>{textualVasuStatus}</Dimmed>
            {showSpinner && <Spinner />}
          </StatusContainer>
          {vasu && (
            <Button
              text={i18n.vasu.checkInPreview}
              disabled={status.state != 'clean'}
              onClick={() => history.push(`/vasu/${vasu.id}`)}
              primary
            />
          )}
        </FooterContainer>
      </StickyFooter>
    </Container>
  )
})
