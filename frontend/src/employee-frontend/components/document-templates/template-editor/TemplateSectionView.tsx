// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faArrowDown, faArrowUp, faPen, faPlus, faTrash } from 'Icons'
import React, { useState } from 'react'
import styled from 'styled-components'

import { swapElements } from 'lib-common/array'
import { BoundForm, useFormElems, useFormFields } from 'lib-common/form/hooks'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../../state/i18n'
import { templateSectionForm } from '../templates'

import TemplateQuestionModal from './TemplateQuestionModal'
import TemplateQuestionView from './TemplateQuestionView'
import TemplateSectionModal from './TemplateSectionModal'

const QuestionList = styled(FixedSpaceColumn)`
  padding: ${defaultMargins.m};
`

const Wrapper = styled.div<{ $readOnly: boolean }>`
  .section-actions {
    display: none;
  }

  border-style: dashed;
  border-width: 1px;
  border-color: transparent;
  &:hover {
    border-color: ${(p) =>
      p.$readOnly ? 'transparent' : colors.grayscale.g35};
    ${(p) => (p.$readOnly ? '' : `border: ${colors.grayscale.g35} 1px dashed;`)}

    .section-actions {
      display: flex;
    }
  }
`

const HeaderRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
`

interface Props {
  bind: BoundForm<typeof templateSectionForm>
  onMoveUp: () => void
  onMoveDown: () => void
  onDelete: () => void
  first: boolean
  last: boolean
  readOnly: boolean
}

export default React.memo(function TemplateSectionView({
  bind,
  onMoveUp,
  onMoveDown,
  onDelete,
  first,
  last,
  readOnly
}: Props) {
  const { i18n } = useTranslation()
  const [editing, setEditing] = useState(false)
  const [creatingQuestion, setCreatingQuestion] = useState(false)
  const { label, questions } = useFormFields(bind)
  const questionElems = useFormElems(questions)

  return (
    <Wrapper $readOnly={readOnly}>
      <HeaderRow>
        <H2>{label.value()}</H2>
        {!readOnly && (
          <FixedSpaceRow className="section-actions">
            <IconButton
              icon={faPlus}
              aria-label={i18n.documentTemplates.templateEditor.addQuestion}
              onClick={() => setCreatingQuestion(true)}
            />
            <IconButton
              icon={faPen}
              aria-label={i18n.common.edit}
              onClick={() => setEditing(true)}
            />
            <IconButton
              icon={faArrowUp}
              aria-label={i18n.documentTemplates.templateEditor.moveUp}
              disabled={first}
              onClick={onMoveUp}
            />
            <IconButton
              icon={faArrowDown}
              aria-label={i18n.documentTemplates.templateEditor.moveDown}
              disabled={last}
              onClick={onMoveDown}
            />
            <IconButton
              icon={faTrash}
              aria-label={i18n.common.remove}
              disabled={questionElems.length > 0}
              onClick={onDelete}
            />
          </FixedSpaceRow>
        )}
      </HeaderRow>
      <QuestionList spacing="L">
        {questionElems.map((question, index) => (
          <TemplateQuestionView
            key={question.state.state.id}
            bind={question}
            onMoveUp={() =>
              questions.update((old) => swapElements(old, index, index - 1))
            }
            onMoveDown={() =>
              questions.update((old) => swapElements(old, index, index + 1))
            }
            onDelete={() =>
              questions.update((old) => [
                ...old.slice(0, index),
                ...old.slice(index + 1)
              ])
            }
            first={index === 0}
            last={index === questionElems.length - 1}
            readOnly={readOnly}
          />
        ))}
      </QuestionList>
      {editing && (
        <TemplateSectionModal
          onSave={(s) => {
            bind.set(s)
            setEditing(false)
          }}
          onCancel={() => setEditing(false)}
          initialState={bind.state}
        />
      )}
      {creatingQuestion && (
        <TemplateQuestionModal
          onSave={(q) => {
            questions.update((old) => [...old, q])
            setCreatingQuestion(false)
          }}
          onCancel={() => setCreatingQuestion(false)}
        />
      )}
    </Wrapper>
  )
})
