// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H1, H2, H3 } from 'lib-components/typography'
import React, { Fragment, useEffect, useState } from 'react'
import { useHistory } from 'react-router'
import { Prompt, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { Loading, Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import TextArea from 'lib-components/atoms/form/TextArea'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowDown, faArrowUp, faPlus, faTrash } from 'lib-icons'
import { useTranslation } from '../../../state/i18n'
import { useWarnOnUnsavedChanges } from '../../../utils/useWarnOnUnsavedChanges'
import {
  CheckboxQuestion,
  isCheckboxQuestion,
  isMultiSelectQuestion,
  isRadioGroupQuestion,
  isTextQuestion,
  MultiSelectQuestion,
  RadioGroupQuestion,
  TextQuestion,
  VasuQuestion
} from '../vasu-content'
import {
  getVasuTemplate,
  updateVasuTemplateContents,
  VasuTemplate
} from './api'
import CreateQuestionModal from './CreateQuestionModal'

export default React.memo(function VasuTemplateEditor() {
  const { id } = useParams<{ id: UUID }>()
  const { i18n } = useTranslation()
  const h = useHistory()

  const [template, setTemplate] = useState<Result<VasuTemplate>>(Loading.of())
  const [dirty, setDirty] = useState(false)

  const [sectionNameEdit, setSectionNameEdit] = useState<number | null>(null)
  const [addingQuestion, setAddingQuestion] = useState<number[] | null>(null) // [section, question]

  const loadTemplate = useRestApi(getVasuTemplate, setTemplate)
  useEffect(() => loadTemplate(id), [id, loadTemplate])
  useWarnOnUnsavedChanges(dirty, i18n.vasuTemplates.unsavedWarning)

  const readonly = !(template.isSuccess && template.value.documentCount === 0)

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
              questions: []
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

  function renderTextQuestion(
    question: TextQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    return (
      <>
        <H3 noMargin>{`${sectionIndex + 1}.${questionIndex + 1}. ${
          question.name
        }`}</H3>

        {question.multiline ? (
          <TextArea value={question.value} />
        ) : (
          <InputField value={question.value} width={'L'} />
        )}
      </>
    )
  }

  function renderCheckboxQuestion(
    question: CheckboxQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    return (
      <Checkbox
        checked={question.value}
        label={`${sectionIndex + 1}.${questionIndex + 1}. ${question.name}`}
      />
    )
  }

  function renderRadioGroupQuestion(
    question: RadioGroupQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    return (
      <FixedSpaceColumn spacing="s">
        <H3 noMargin>{`${sectionIndex + 1}.${questionIndex + 1}. ${
          question.name
        }`}</H3>
        {question.options.map((opt) => (
          <Radio checked={false} label={opt.name} key={opt.key} />
        ))}
      </FixedSpaceColumn>
    )
  }

  function renderMultiSelectQuestion(
    question: MultiSelectQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    return (
      <FixedSpaceColumn spacing="s">
        <H3 noMargin>{`${sectionIndex + 1}.${questionIndex + 1}. ${
          question.name
        }`}</H3>
        {question.options.map((opt) => (
          <Checkbox checked={false} label={opt.name} key={opt.key} />
        ))}
      </FixedSpaceColumn>
    )
  }

  function renderQuestion(
    question: VasuQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    if (isTextQuestion(question)) {
      return renderTextQuestion(question, sectionIndex, questionIndex)
    } else if (isCheckboxQuestion(question)) {
      return renderCheckboxQuestion(question, sectionIndex, questionIndex)
    } else if (isRadioGroupQuestion(question)) {
      return renderRadioGroupQuestion(question, sectionIndex, questionIndex)
    } else if (isMultiSelectQuestion(question)) {
      return renderMultiSelectQuestion(question, sectionIndex, questionIndex)
    } else {
      return null
    }
  }

  return (
    <Container>
      <Prompt when={dirty} message={i18n.vasuTemplates.unsavedWarning} />

      <Gap size={'L'} />
      <ContentArea opaque>
        {template.isLoading && <SpinnerSegment />}
        {template.isFailure && <ErrorSegment />}
        {template.isSuccess && (
          <>
            <H1 noMargin>{template.value.name}</H1>
            <div>
              {i18n.vasuTemplates.valid}: {template.value.valid.format()}
            </div>

            <Gap />

            <FixedSpaceColumn>
              {template.value.content.sections.map((section, sectionIndex) => (
                <Fragment key={`section-${sectionIndex}`}>
                  <SectionContainer>
                    {sectionNameEdit === sectionIndex ? (
                      <InputField
                        value={section.name}
                        onBlur={() => setSectionNameEdit(null)}
                        onChange={(value) => renameSection(sectionIndex, value)}
                        onKeyPress={(e) => {
                          if (e.key === 'Enter') setSectionNameEdit(null)
                        }}
                        readonly={readonly}
                      />
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
                        {`${sectionIndex + 1}. ${section.name}`}
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
                              template.value.content.sections.length - 1
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
                    <Gap />
                    <FixedSpaceColumn>
                      {section.questions.map((question, questionIndex) => (
                        <Fragment
                          key={`question-${sectionIndex}-${questionIndex}`}
                        >
                          <QuestionContainer>
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
                              question,
                              sectionIndex,
                              questionIndex
                            )}
                          </QuestionContainer>
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
                  </SectionContainer>
                  {!readonly && (
                    <AddNewContainer
                      showOnHover={
                        sectionIndex <
                        template.value.content.sections.length - 1
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
            {template.value.content.sections.length === 0 && !readonly && (
              <AddNewContainer showOnHover={false}>
                <InlineButton
                  onClick={() => addSection(0)}
                  text={i18n.vasuTemplates.addNewSection}
                  icon={faPlus}
                />
              </AddNewContainer>
            )}

            <Gap />

            <Button
              text={i18n.common.save}
              primary
              onClick={() => {
                void updateVasuTemplateContents(
                  id,
                  template.value.content
                ).then((res) => {
                  if (res.isSuccess) {
                    setDirty(false)
                    h.goBack()
                  }
                })
              }}
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
          </>
        )}
      </ContentArea>
    </Container>
  )
})

const ElementContainer = styled.div`
  position: relative;
  padding: 24px 80px 24px 24px;

  border: dashed 1px ${colors.greyscale.white};
  border-radius: 8px;

  &:hover {
    border-color: ${colors.greyscale.medium};
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

const SectionContainer = styled(ElementContainer)`
  width: 60%;
`

const QuestionContainer = styled(ElementContainer)``

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
