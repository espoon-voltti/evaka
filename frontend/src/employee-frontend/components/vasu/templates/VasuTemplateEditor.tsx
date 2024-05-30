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
import { unstable_usePrompt as usePrompt, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result, wrapResult } from 'lib-common/api'
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
import { swapElements } from 'lib-common/array'
import {
  CurriculumType,
  VasuSection,
  VasuTemplate
} from 'lib-common/generated/api-types/vasu'
import useRouteParams from 'lib-common/useRouteParams'
import { useRestApi } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
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
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Bold, H1, H2, H3, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { vasuTranslations } from 'lib-customizations/employee'
import { faArrowDown, faArrowUp, faPlus, faTrash } from 'lib-icons'

import {
  getTemplate,
  putTemplateContent
} from '../../../generated/api-clients/vasu'
import { useTranslation } from '../../../state/i18n'
import { useWarnOnUnsavedChanges } from '../../../utils/useWarnOnUnsavedChanges'
import { renderResult } from '../../async-rendering'
import QuestionInfo from '../QuestionInfo'
import { FixedSpaceRowOrColumns } from '../components/MultiFieldQuestion'
import { OptionContainer } from '../components/RadioGroupQuestionOption'
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

import CreateParagraphModal from './CreateParagraphModal'
import CreateQuestionModal from './CreateQuestionModal'

const getTemplateResult = wrapResult(getTemplate)
const putTemplateContentResult = wrapResult(putTemplateContent)

export default React.memo(function VasuTemplateEditor() {
  const { id } = useRouteParams(['id'])
  const { i18n, lang } = useTranslation()
  const navigate = useNavigate()

  const [template, setTemplate] = useState<Result<VasuTemplate>>(Loading.of())
  const [dirty, setDirty] = useState(false)

  const [sectionNameEdit, setSectionNameEdit] = useState<number | null>(null)
  const [addingQuestion, setAddingQuestion] = useState<
    [number, number, VasuSection] | null
  >(null) // [section, question]
  const [addingParagraph, setAddingParagraph] =
    useState<[number, number, VasuSection]>() // [section, question]

  const loadTemplate = useRestApi(getTemplateResult, setTemplate)
  useEffect(() => {
    void loadTemplate({ id })
  }, [id, loadTemplate])
  useWarnOnUnsavedChanges(dirty, i18n.vasuTemplates.unsavedWarning)
  usePrompt({
    message: i18n.vasuTemplates.unsavedWarning,
    when: dirty
  })

  const onSave = useCallback(
    () =>
      template.isSuccess
        ? putTemplateContentResult({ id, body: template.value.content })
        : undefined,
    [id, template]
  )
  const onSuccess = useCallback(() => {
    setDirty(false)
    navigate(-1)
  }, [navigate])

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

  const dynamicOffset = template
    .map((t) => (t.content.hasDynamicFirstSection ? 0 : 1))
    .getOrElse(0)

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
        {!!question.label && <H3>{`${questionNumber}. ${question.label}`}</H3>}
        <Checkbox
          checked={question.value}
          label={
            question.label || question.notNumbered
              ? question.name
              : `${questionNumber}. ${question.name}`
          }
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
        {question.options.map((opt) =>
          opt.isIntervention ? (
            <QuestionInfo key={opt.key} info={opt.info ?? null}>
              <Bold>{opt.name}</Bold>
            </QuestionInfo>
          ) : (
            <OptionContainer key={opt.key}>
              <Radio checked={false} label={opt.name} />
              {opt.dateRange && (
                <div>
                  <DatePicker
                    locale="fi"
                    date={null}
                    onChange={() => undefined}
                  />
                  <span>-</span>
                  <DatePicker
                    locale="fi"
                    date={null}
                    onChange={() => undefined}
                  />
                </div>
              )}
            </OptionContainer>
          )
        )}
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
        {question.options.map((opt) =>
          opt.isIntervention ? (
            <QuestionInfo key={opt.key} info={opt.info ?? null}>
              <Bold>{opt.name}</Bold>
            </QuestionInfo>
          ) : (
            <ExpandingInfo info={opt.info} key={opt.key} width="full">
              <FixedSpaceRow>
                <Checkbox checked={false} label={opt.name} key={opt.key} />
                {opt.date ? (
                  <DatePicker
                    date={null}
                    locale="fi"
                    onChange={() => undefined}
                    hideErrorsBeforeTouched
                  />
                ) : opt.dateRange ? (
                  <div>
                    <DatePicker
                      locale="fi"
                      date={null}
                      onChange={() => undefined}
                    />
                    <span>-</span>
                    <DatePicker
                      locale="fi"
                      date={null}
                      onChange={() => undefined}
                    />
                  </div>
                ) : null}
              </FixedSpaceRow>
              {!!opt.subText && <P noMargin>{opt.subText}</P>}
            </ExpandingInfo>
          )
        )}
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
        <FixedSpaceRowOrColumns columns={question.separateRows}>
          {question.keys.map((key) => (
            <FixedSpaceColumn key={key.name} spacing="xxs">
              <QuestionInfo info={key.info ?? null}>
                <Label>{key.name}</Label>
              </QuestionInfo>
              <InputField value="" width="m" />
            </FixedSpaceColumn>
          ))}
        </FixedSpaceRowOrColumns>
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
        <DatePicker date={null} onChange={() => undefined} locale={lang} />
        {!!question.nameInEvents && (
          <QuestionDetails>
            {`${i18n.vasuTemplates.questionModal.dateIsTrackedInEvents}: ${question.nameInEvents}`}
          </QuestionDetails>
        )}
      </FixedSpaceColumn>
    )
  }

  function renderFollowup(question: Followup, questionNumber: string) {
    return (
      <FixedSpaceColumn spacing="s">
        <H2>{question.title}</H2>
        <QuestionInfo info={question.info}>
          <H3 noMargin>{`${
            question.continuesNumbering ? `${questionNumber}. ` : ''
          }${question.name}`}</H3>
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

  function renderStaticInfoSubsection(type: CurriculumType) {
    const t = translations.staticSections.basics

    return (
      <FixedSpaceColumn spacing="xxs">
        <Label>{t.name}</Label>
        <div>Matti Meikäläinen</div>

        <Gap size="s" />

        <Label>{t.dateOfBirth}</Label>
        <div>10.08.2016</div>

        <Gap size="s" />

        <Label>{t.placements[type]}</Label>
        <div>Päiväkoti ja esikoulu B (Perhoset) 20.08.2021 - 19.12.2021</div>

        <Gap size="s" />

        <Label>{t.guardians}</Label>
        <div>Jaakko Meikäläinen</div>
        <div>Pirjo Meikäläinen</div>
      </FixedSpaceColumn>
    )
  }

  function renderQuestion(
    sectionQuestions: VasuQuestion[],
    question: VasuQuestion,
    sectionIndex: number,
    type: CurriculumType
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
      return renderFollowup(question, questionNumber)
    } else if (isParagraph(question)) {
      return renderParagraph(question)
    } else if (isStaticInfoSubsection(question)) {
      return renderStaticInfoSubsection(type)
    } else {
      return null
    }
  }

  return (
    <Container>
      <ContentArea opaque>
        {renderResult(template, (template) => (
          <>
            <H1 noMargin>{template.name}</H1>
            <div>
              {i18n.vasuTemplates.valid}: {template.valid.format()}
            </div>

            <Gap />

            <FixedSpaceColumn>
              {!template.content.hasDynamicFirstSection && (
                <ElementContainer>
                  <H2>1. {translations.staticSections.basics.title}</H2>
                </ElementContainer>
              )}

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
                            aria-label={i18n.vasuTemplates.moveUp}
                          />
                          <IconButton
                            icon={faArrowDown}
                            onClick={() => moveSection(sectionIndex, 'down')}
                            disabled={
                              sectionIndex ===
                              template.content.sections.length - 1
                            }
                            aria-label={i18n.vasuTemplates.moveDown}
                          />
                          <IconButton
                            icon={faTrash}
                            onClick={() => removeSection(sectionIndex)}
                            disabled={section.questions.length > 0}
                            aria-label={i18n.common.remove}
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
                                  aria-label={i18n.vasuTemplates.moveUp}
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
                                  aria-label={i18n.vasuTemplates.moveDown}
                                />
                                <IconButton
                                  icon={faTrash}
                                  disabled={question.ophKey !== null}
                                  onClick={() =>
                                    removeQuestion(sectionIndex, questionIndex)
                                  }
                                  aria-label={i18n.common.remove}
                                />
                              </FixedSpaceRow>
                            )}
                            {question.dependsOn &&
                              question.dependsOn.length > 0 && (
                                <QuestionDetails>
                                  {i18n.vasuTemplates.onlyVisibleWhen(
                                    question.dependsOn.map((dependingId) => {
                                      const depending = section.questions.find(
                                        (q) => q.id === dependingId
                                      )

                                      if (!depending) {
                                        return i18n.vasuTemplates
                                          .visibilityConditions.unknownQuestion
                                      }

                                      const questionNumber = getQuestionNumber(
                                        sectionIndex + dynamicOffset,
                                        section.questions,
                                        depending
                                      )

                                      if (depending.type === 'CHECKBOX') {
                                        return i18n.vasuTemplates.visibilityConditions.checked(
                                          questionNumber
                                        )
                                      }

                                      return i18n.vasuTemplates.visibilityConditions.answered(
                                        questionNumber
                                      )
                                    })
                                  )}
                                </QuestionDetails>
                              )}
                            {renderQuestion(
                              section.questions,
                              question,
                              sectionIndex + dynamicOffset,
                              template.type
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
                                    questionIndex + 1,
                                    section
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
                                    questionIndex + 1,
                                    section
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
                          onClick={() =>
                            setAddingQuestion([sectionIndex, 0, section])
                          }
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
                section={addingQuestion[2]}
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
                section={addingParagraph[2]}
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
