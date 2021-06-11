// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useEffect, useState } from 'react'

import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import { Label } from 'lib-components/typography'

import { TextArea } from 'lib-components/atoms/form/InputField'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Button from 'lib-components/atoms/buttons/Button'
import {
  CheckboxQuestion,
  getVasuDocument,
  isCheckboxQuestion,
  isMultiSelectQuestion,
  isRadioGroupQuestion,
  isTextQuestion,
  MultiSelectQuestion,
  putVasuDocument,
  RadioGroupQuestion,
  TextQuestion,
  VasuDocumentResponse
} from 'employee-frontend/api/child/vasu'
import { Loading, Result } from 'lib-common/api'
import { cloneDeep } from 'lodash'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { UUID } from 'lib-common/types'
import { RouteComponentProps } from 'react-router-dom'

export default React.memo(function VasuPage({
  match
}: RouteComponentProps<{ id: UUID }>) {
  const { id } = match.params
  function submit() {
    if (vasuDocument.isSuccess) {
      void putVasuDocument(vasuDocument.value.id, vasuDocument.value.content)
    }
  }

  useEffect(() => {
    void getVasuDocument(id).then(setVasuDocument)
  }, [id])

  // TODO: move these to their own components when the spec is more stable
  function renderTextQuestion(
    question: TextQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    return (
      <>
        <Label>{question.name}</Label>
        <TextArea
          value={question.value}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
            setVasuDocument((prev) => {
              return prev.map((prevData) => {
                const clone = cloneDeep(prevData)
                const question1 = clone.content.sections[sectionIndex]
                  .questions[questionIndex] as TextQuestion
                question1.value = e.target.value
                return clone
              })
            })
          }}
        />
      </>
    )
  }

  function renderCheckboxQuestion(
    question: CheckboxQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    return (
      <>
        <Checkbox
          checked={question.value}
          label={question.name}
          onChange={(checked) => {
            setVasuDocument((prev) => {
              return prev.map((prevData) => {
                const clone = cloneDeep(prevData)
                const question1 = clone.content.sections[sectionIndex]
                  .questions[questionIndex] as CheckboxQuestion
                question1.value = checked
                return clone
              })
            })
          }}
        />
      </>
    )
  }
  function renderRadioGroupQuestion(
    question: RadioGroupQuestion,
    sectionIndex: number,
    questionIndex: number
  ) {
    return (
      <>
        <Label>{question.name}</Label>
        <Gap size={'m'} />
        <FixedSpaceColumn>
          {vasuDocument.isSuccess &&
            question.optionNames.map((optionName, optionIndex) => {
              const question1 = vasuDocument.value.content.sections[
                sectionIndex
              ].questions[questionIndex] as RadioGroupQuestion
              return (
                <Radio
                  key={optionName}
                  checked={question1.value === optionIndex}
                  label={optionName}
                  onChange={() => {
                    setVasuDocument((prev) => {
                      return prev.map((prevData) => {
                        const clone = cloneDeep(prevData)
                        const question1 = clone.content.sections[sectionIndex]
                          .questions[questionIndex] as RadioGroupQuestion
                        question1.value = optionIndex
                        return clone
                      })
                    })
                  }}
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
        <Label>{question.name}</Label>
        <Gap size={'m'} />
        <FixedSpaceColumn>
          {vasuDocument.isSuccess &&
            question.optionNames.map((optionName, optionIndex) => {
              const question1 = vasuDocument.value.content.sections[
                sectionIndex
              ].questions[questionIndex] as MultiSelectQuestion
              return (
                <Checkbox
                  key={optionName}
                  checked={question1.value.includes(optionIndex)}
                  label={optionName}
                  onChange={(checked) => {
                    setVasuDocument((prev) => {
                      return prev.map((prevData) => {
                        const clone = cloneDeep(prevData)
                        const question1 = clone.content.sections[sectionIndex]
                          .questions[questionIndex] as MultiSelectQuestion
                        if (checked && !question1.value.includes(optionIndex))
                          question1.value.push(optionIndex)
                        if (!checked)
                          question1.value = question1.value.filter(
                            (i) => i !== optionIndex
                          )
                        return clone
                      })
                    })
                  }}
                />
              )
            })}
        </FixedSpaceColumn>
      </>
    )
  }

  const [vasuDocument, setVasuDocument] = useState<
    Result<VasuDocumentResponse>
  >(Loading.of())
  return (
    <Container>
      <Gap size={'L'} />

      {vasuDocument.isSuccess &&
        vasuDocument.value.content.sections.map((section, sectionIndex) => {
          return (
            <Fragment key={section.name}>
              <ContentArea opaque>
                <h2>{section.name}</h2>
                {section.questions.map((question, questionIndex) => (
                  <Fragment key={question.name}>
                    {isTextQuestion(question)
                      ? renderTextQuestion(
                          question,
                          sectionIndex,
                          questionIndex
                        )
                      : isCheckboxQuestion(question)
                      ? renderCheckboxQuestion(
                          question,
                          sectionIndex,
                          questionIndex
                        )
                      : isRadioGroupQuestion(question)
                      ? renderRadioGroupQuestion(
                          question,
                          sectionIndex,
                          questionIndex
                        )
                      : isMultiSelectQuestion(question)
                      ? renderMultiSelectQuestion(
                          question,
                          sectionIndex,
                          questionIndex
                        )
                      : undefined}
                    <Gap size={'L'} />
                  </Fragment>
                ))}
              </ContentArea>
              <Gap size={'L'} />
            </Fragment>
          )
        })}
      <Button text={'submit'} onClick={submit} />
    </Container>
  )
})
