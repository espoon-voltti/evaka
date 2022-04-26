// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  Fragment,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'
import { useHistory } from 'react-router'
import { Prompt, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result, Success } from 'lib-common/api'
import {
  CheckboxQuestion,
  DateQuestion,
  Followup,
  MultiFieldListQuestion,
  MultiFieldQuestion,
  MultiSelectQuestion,
  Paragraph,
  RadioGroupQuestion,
  TextQuestion,
  VasuQuestion
} from 'lib-common/api-types/vasu'
import { VasuTemplate } from 'lib-common/generated/api-types/vasu'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import TextArea from 'lib-components/atoms/form/TextArea'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H1, H2, H3, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { vasuTranslations } from 'lib-customizations/employee'
import { faArrowDown, faArrowUp, faPlus, faTrash } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { useWarnOnUnsavedChanges } from '../../../utils/useWarnOnUnsavedChanges'
import { renderResult } from '../../async-rendering'
import QuestionInfo from '../QuestionInfo'
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
  isTextQuestion
} from '../vasu-content'

import CreateParagraphModal from './CreateParagraphModal'
import CreateQuestionModal from './CreateQuestionModal'
import { getVasuTemplate, updateVasuTemplateContents } from './api'

export default React.memo(function VasuTemplateEditor() {
  const { id } = useParams<{ id: UUID }>()
  const { i18n, lang } = useTranslation()
  const history = useHistory()

  const [template, setTemplate] = useState<Result<VasuTemplate>>(Loading.of())
  const [dirty, setDirty] = useState(false)

  const [sectionNameEdit, setSectionNameEdit] = useState<number | null>(null)
  const [addingQuestion, setAddingQuestion] = useState<[number, number] | null>(
    null
  ) // [section, question]
  const [addingParagraph, setAddingParagraph] = useState<[number, number]>() // [section, question]

  const loadTemplate = useRestApi(getVasuTemplate, setTemplate)
  useEffect(() => loadTemplate(id), [id, loadTemplate])
  useWarnOnUnsavedChanges(dirty, i18n.vasuTemplates.unsavedWarning)

  const onSave = useCallback(
    () =>
      template.isSuccess
        ? updateVasuTemplateContents(id, template.value.content)
        : Promise.resolve(Success.of()),
    [id, template]
  )
  const onSuccess = useCallback(() => {
    setDirty(false)
    history.goBack()
  }, [history])

  const readonly = !(template.isSuccess && template.value.documentCount === 0)

  const translations = useMemo(
    () => vasuTranslations[template.map((t) => t.language).getOrElse('FI')],
    [template]
  )

  function moveSection(index: number, dir: 'up' | 'down') {
    setDirty(true)
    setTemplate((res) =>
      res.map((tmp) => ({
        ...tmp,
        content: {
          ...tmp.content,
          sections: swapElements(
            tmp.content.sections,
            index,
            dir === 'up' ? index - 1 : index + 1
          )
        }
      }))
    )
  }

  function moveQuestion(
    sectionIndex: number,
    questionIndex: number,
    dir: 'up' | 'down'
  ) {
    setDirty(true)
    setTemplate((res) =>
      res.map((tmp) => ({
        ...tmp,
        content: {
          ...tmp.content,
          sections: tmp.content.sections.map((s, si) =>
            si === sectionIndex
              ? {
                  ...s,
                  questions: swapElements(
                    s.questions,
                    questionIndex,
                    dir === 'up' ? questionIndex - 1 : questionIndex + 1
                  )
                }
              : s
          )
        }
      }))
    )
  }

  function addSection(sectionIndex: number) {
    setDirty(true)
    setTemplate((res) =>
      res.map((tmp) => ({
        ...tmp,
        content: {
          ...tmp.content,
          sections: [
            ...tmp.content.sections.slice(0, sectionIndex),
            {
              name: 'Uusi osio',
              questions: [],
              hideBeforeReady: false
            },
            ...tmp.content.sections.slice(sectionIndex)
          ]
        }
      }))
    )
  }

  function addQuestion(
    question: VasuQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    setDirty(true)
    setTemplate((res) =>
      res.map((tmp) => ({
        ...tmp,
        content: {
          ...tmp.content,
          sections: tmp.content.sections.map((s, si) =>
            si === sectionIndex
              ? {
                  ...s,
                  questions: [
                    ...s.questions.slice(0, questionIndex),
                    question,
                    ...s.questions.slice(questionIndex)
                  ]
                }
              : s
          )
        }
      }))
    )
  }

  function renameSection(sectionIndex: number, value: string) {
    setDirty(true)
    setTemplate((res) =>
      res.map((tmp) => ({
        ...tmp,
        content: {
          ...tmp.content,
          sections: tmp.content.sections.map((s, si) =>
            si === sectionIndex
              ? {
                  ...s,
                  name: value
                }
              : s
          )
        }
      }))
    )
  }

  function toggleSectionHideBeforeReady(sectionIndex: number, value: boolean) {
    setDirty(true)
    setTemplate((res) =>
      res.map((tmp) => ({
        ...tmp,
        content: {
          ...tmp.content,
          sections: tmp.content.sections.map((s, si) =>
            si === sectionIndex
              ? {
                  ...s,
                  hideBeforeReady: value
                }
              : s
          )
        }
      }))
    )
  }

  function removeSection(sectionIndex: number) {
    setDirty(true)
    setTemplate((res) =>
      res.map((tmp) => ({
        ...tmp,
        content: {
          ...tmp.content,
          sections: [
            ...tmp.content.sections.slice(0, sectionIndex),
            ...tmp.content.sections.slice(sectionIndex + 1)
          ]
        }
      }))
    )
  }

  function removeQuestion(sectionIndex: number, questionIndex: number) {
    setDirty(true)
    setTemplate((res) =>
      res.map((tmp) => ({
        ...tmp,
        content: {
          ...tmp.content,
          sections: tmp.content.sections.map((s, si) =>
            si === sectionIndex
              ? {
                  ...s,
                  questions: [
                    ...s.questions.slice(0, questionIndex),
                    ...s.questions.slice(questionIndex + 1)
                  ]
                }
              : s
          )
        }
      }))
    )
  }

  const dynamicOffset = 1

  function renderTextQuestion(question: TextQuestion, questionNumber: string) {
    return (
      <React.Fragment>
        <QuestionInfo info={question.info}>
          <H3 noMargin>{`${questionNumber}. ${question.name}`}</H3>
        </QuestionInfo>

        {question.multiline ? (
          <TextArea value={question.value} />
        ) : (
          <InputField value={question.value} width="L" />
        )}
      </React.Fragment>
    )
  }

  function renderCheckboxQuestion(
    question: CheckboxQuestion,
    questionNumber: string
  ) {
    return (
      <QuestionInfo info={question.info}>
        <Checkbox
          checked={question.value}
          label={`${questionNumber}. ${question.name}`}
        />
      </QuestionInfo>
    )
  }

  function renderRadioGroupQuestion(
    question: RadioGroupQuestion,
    questionNumber: string
  ) {
    return (
      <FixedSpaceColumn spacing="s">
        <QuestionInfo info={question.info}>
          <H3 noMargin>{`${questionNumber}. ${question.name}`}</H3>
        </QuestionInfo>
        {question.options.map((opt) => (
          <Radio checked={false} label={opt.name} key={opt.key} />
        ))}
      </FixedSpaceColumn>
    )
  }

  function renderMultiSelectQuestion(
    question: MultiSelectQuestion,
    questionNumber: string
  ) {
    return (
      <FixedSpaceColumn spacing="s">
        <QuestionInfo info={question.info}>
          <H3 noMargin>{`${questionNumber}. ${question.name}`}</H3>
        </QuestionInfo>
        {question.options.map((opt) => (
          <Checkbox checked={false} label={opt.name} key={opt.key} />
        ))}
      </FixedSpaceColumn>
    )
  }

  function renderMultiFieldQuestion(
    question: MultiFieldQuestion,
    questionNumber: string
  ) {
    return (
      <FixedSpaceColumn spacing="xs">
        <QuestionInfo info={question.info}>
          <H3 noMargin>{`${questionNumber}. ${question.name}`}</H3>
        </QuestionInfo>
        <FixedSpaceFlexWrap>
          {question.keys.map((key) => (
            <FixedSpaceColumn key={key.name} spacing="xxs">
              <Label>{key.name}</Label>
              <InputField value="" width="m" />
            </FixedSpaceColumn>
          ))}
        </FixedSpaceFlexWrap>
      </FixedSpaceColumn>
    )
  }

  function renderMultiFieldListQuestion(
    question: MultiFieldListQuestion,
    questionNumber: string
  ) {
    return (
      <FixedSpaceColumn spacing="xs">
        <QuestionInfo info={question.info}>
          <H3 noMargin>{`${questionNumber}. ${question.name}`}</H3>
        </QuestionInfo>
        <FixedSpaceFlexWrap>
          {question.keys.map((key) => (
            <FixedSpaceColumn key={key.name} spacing="xxs">
              <Label>{key.name}</Label>
              <InputField value="" width="m" />
            </FixedSpaceColumn>
          ))}
        </FixedSpaceFlexWrap>
        <QuestionDetails>{i18n.vasuTemplates.autoGrowingList}</QuestionDetails>
      </FixedSpaceColumn>
    )
  }

  function renderDateQuestion(question: DateQuestion, questionNumber: string) {
    return (
      <FixedSpaceColumn spacing="xs">
        <QuestionInfo info={question.info}>
          <H3 noMargin>{`${questionNumber}. ${question.name}`}</H3>
        </QuestionInfo>
        <DatePicker date="" onChange={() => undefined} locale={lang} />
        {question.nameInEvents && (
          <QuestionDetails>
            {`${i18n.vasuTemplates.questionModal.dateIsTrackedInEvents}: ${question.nameInEvents}`}
          </QuestionDetails>
        )}
      </FixedSpaceColumn>
    )
  }

  function renderFollowup(question: Followup) {
    return (
      <FixedSpaceColumn spacing="s">
        <H2>{question.title}</H2>
        <QuestionInfo info={question.info}>
          <H3 noMargin>{question.name}</H3>
        </QuestionInfo>
        <TextArea value="" />
      </FixedSpaceColumn>
    )
  }

  function renderParagraph(question: Paragraph) {
    return (
      <FixedSpaceColumn spacing="s">
        {question.title ? <H3 noMargin>{question.title}</H3> : null}
        {question.paragraph ? <P noMargin>{question.paragraph}</P> : null}
      </FixedSpaceColumn>
    )
  }

  function renderQuestion(
    sectionQuestions: VasuQuestion[],
    question: VasuQuestion,
    sectionIndex: number
  ) {
    const questionNumber = getQuestionNumber(
      sectionIndex,
      sectionQuestions,
      question
    )
    if (isTextQuestion(question)) {
      return renderTextQuestion(question, questionNumber)
    } else if (isCheckboxQuestion(question)) {
      return renderCheckboxQuestion(question, questionNumber)
    } else if (isRadioGroupQuestion(question)) {
      return renderRadioGroupQuestion(question, questionNumber)
    } else if (isMultiSelectQuestion(question)) {
      return renderMultiSelectQuestion(question, questionNumber)
    } else if (isMultiFieldQuestion(question)) {
      return renderMultiFieldQuestion(question, questionNumber)
    } else if (isMultiFieldListQuestion(question)) {
      return renderMultiFieldListQuestion(question, questionNumber)
    } else if (isDateQuestion(question)) {
      return renderDateQuestion(question, questionNumber)
    } else if (isFollowup(question)) {
      return renderFollowup(question)
    } else if (isParagraph(question)) {
      return renderParagraph(question)
    } else {
      return null
    }
  }

  return (
    <Container>
      <Prompt when={dirty} message={i18n.vasuTemplates.unsavedWarning} />

      <ContentArea opaque>
        {renderResult(template, (template) => (
          <>
            <H1 noMargin>{template.name}</H1>
            <div>
              {i18n.vasuTemplates.valid}: {template.valid.format()}
            </div>

            <Gap />

            <FixedSpaceColumn>
              <ElementContainer>
                <H2>1. {translations.staticSections.basics.title}</H2>
              </ElementContainer>

              {template.content.sections.map((section, sectionIndex) => (
                <Fragment key={`section-${sectionIndex}`}>
                  <ElementContainer>
                    {sectionNameEdit === sectionIndex ? (
                      <>
                        <InputField
                          value={section.name}
                          onBlur={() => setSectionNameEdit(null)}
                          onChange={(value) =>
                            renameSection(sectionIndex, value)
                          }
                          onKeyPress={(e) => {
                            if (e.key === 'Enter') setSectionNameEdit(null)
                          }}
                          readonly={readonly}
                          width="L"
                        />
                        <Gap size="xs" />
                        <Checkbox
                          label={i18n.vasuTemplates.hideSectionBeforeReady}
                          checked={section.hideBeforeReady}
                          onChange={(value) =>
                            toggleSectionHideBeforeReady(sectionIndex, value)
                          }
                        />
                      </>
                    ) : (
                      <H2
                        noMargin
                        onClick={
                          readonly
                            ? undefined
                            : () => setSectionNameEdit(sectionIndex)
                        }
                        style={{ cursor: 'pointer' }}
                      >
                        {`${sectionIndex + dynamicOffset + 1}. ${section.name}`}
                      </H2>
                    )}
                    {!readonly && (
                      <div className="hover-toolbar">
                        <FixedSpaceRow spacing="xs" className="hover-toolbar">
                          <IconButton
                            icon={faArrowUp}
                            onClick={() => moveSection(sectionIndex, 'up')}
                            disabled={sectionIndex === 0}
                          />
                          <IconButton
                            icon={faArrowDown}
                            onClick={() => moveSection(sectionIndex, 'down')}
                            disabled={
                              sectionIndex ===
                              template.content.sections.length - 1
                            }
                          />
                          <IconButton
                            icon={faTrash}
                            onClick={() => removeSection(sectionIndex)}
                            disabled={section.questions.length > 0}
                          />
                        </FixedSpaceRow>
                      </div>
                    )}
                    {(sectionNameEdit !== sectionIndex || readonly) &&
                      section.hideBeforeReady && (
                        <>
                          <Gap size="xs" />
                          <QuestionDetails>
                            {i18n.vasuTemplates.hideSectionBeforeReady}
                          </QuestionDetails>
                        </>
                      )}
                    <Gap />
                    <FixedSpaceColumn>
                      {section.questions.map((question, questionIndex) => (
                        <Fragment
                          key={`question-${sectionIndex}-${questionIndex}`}
                        >
                          <ElementContainer>
                            {!readonly && (
                              <FixedSpaceRow
                                spacing="xs"
                                className="hover-toolbar"
                              >
                                <IconButton
                                  icon={faArrowUp}
                                  onClick={() =>
                                    moveQuestion(
                                      sectionIndex,
                                      questionIndex,
                                      'up'
                                    )
                                  }
                                  disabled={questionIndex === 0}
                                />
                                <IconButton
                                  icon={faArrowDown}
                                  onClick={() =>
                                    moveQuestion(
                                      sectionIndex,
                                      questionIndex,
                                      'down'
                                    )
                                  }
                                  disabled={
                                    questionIndex ===
                                    section.questions.length - 1
                                  }
                                />
                                <IconButton
                                  icon={faTrash}
                                  disabled={question.ophKey !== null}
                                  onClick={() =>
                                    removeQuestion(sectionIndex, questionIndex)
                                  }
                                />
                              </FixedSpaceRow>
                            )}
                            {renderQuestion(
                              section.questions,
                              question,
                              sectionIndex + dynamicOffset
                            )}
                          </ElementContainer>
                          {!readonly && (
                            <AddNewContainer
                              showOnHover={
                                questionIndex < section.questions.length - 1
                              }
                            >
                              <InlineButton
                                onClick={() =>
                                  setAddingQuestion([
                                    sectionIndex,
                                    questionIndex + 1
                                  ])
                                }
                                text={i18n.vasuTemplates.addNewQuestion}
                                icon={faPlus}
                                disabled={readonly}
                              />
                              <Gap size="L" horizontal />
                              <InlineButton
                                onClick={() =>
                                  setAddingParagraph([
                                    sectionIndex,
                                    questionIndex + 1
                                  ])
                                }
                                text={i18n.vasuTemplates.addNewParagraph}
                                icon={faPlus}
                                disabled={readonly}
                              />
                            </AddNewContainer>
                          )}
                        </Fragment>
                      ))}
                    </FixedSpaceColumn>
                    {section.questions.length === 0 && !readonly && (
                      <AddNewContainer showOnHover={false}>
                        <InlineButton
                          onClick={() => setAddingQuestion([sectionIndex, 0])}
                          text={i18n.vasuTemplates.addNewQuestion}
                          icon={faPlus}
                        />
                      </AddNewContainer>
                    )}
                  </ElementContainer>
                  {!readonly && (
                    <AddNewContainer
                      showOnHover={
                        sectionIndex < template.content.sections.length - 1
                      }
                    >
                      <InlineButton
                        onClick={() => addSection(sectionIndex + 1)}
                        text={i18n.vasuTemplates.addNewSection}
                        icon={faPlus}
                        disabled={readonly}
                      />
                    </AddNewContainer>
                  )}
                </Fragment>
              ))}
            </FixedSpaceColumn>
            {template.content.sections.length === 0 && !readonly && (
              <AddNewContainer showOnHover={false}>
                <InlineButton
                  onClick={() => addSection(0)}
                  text={i18n.vasuTemplates.addNewSection}
                  icon={faPlus}
                />
              </AddNewContainer>
            )}

            <Gap />

            <AsyncButton
              text={i18n.common.save}
              primary
              data-qa="save-template"
              onClick={onSave}
              onSuccess={onSuccess}
              disabled={readonly}
            />

            {addingQuestion !== null && !readonly && (
              <CreateQuestionModal
                onSave={(question) => {
                  if (addingQuestion === null) return
                  const [sectionIndex, questionIndex] = addingQuestion
                  addQuestion(question, sectionIndex, questionIndex)
                  setAddingQuestion(null)
                }}
                onCancel={() => setAddingQuestion(null)}
              />
            )}

            {addingParagraph && !readonly && (
              <CreateParagraphModal
                onSave={(question) => {
                  if (!addingParagraph) return
                  const [sectionIndex, questionIndex] = addingParagraph
                  addQuestion(question, sectionIndex, questionIndex)
                  setAddingParagraph(undefined)
                }}
                onCancel={() => setAddingParagraph(undefined)}
              />
            )}
          </>
        ))}
      </ContentArea>
    </Container>
  )
})

const ElementContainer = styled.div`
  position: relative;
  padding: 24px 80px 24px 24px;

  border: dashed 1px ${colors.grayscale.g0};
  border-radius: 8px;

  &:hover {
    border-color: ${colors.grayscale.g35};
  }

  &:not(:hover) {
    > .hover-toolbar {
      display: none;
    }
  }

  > .hover-toolbar {
    position: absolute;
    top: 8px;
    right: 8px;
  }
`

const QuestionDetails = styled.i`
  color: ${(p) => p.theme.colors.grayscale.g70};
`

const AddNewContainer = styled.div<{ showOnHover: boolean }>`
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  width: 60%;
  min-height: 24px;

  ${(p) =>
    p.showOnHover
      ? `
    &:not(:hover) {
      * {
        display: none;
      }
    }
  `
      : ''}
`

function swapElements<T>(arr: T[], index1: number, index2: number): T[] {
  const arrayCopy = [...arr]
  ;[arrayCopy[index1], arrayCopy[index2]] = [
    arrayCopy[index2],
    arrayCopy[index1]
  ]
  return arrayCopy
}
