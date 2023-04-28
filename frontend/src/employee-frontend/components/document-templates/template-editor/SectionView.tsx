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

import { sectionForm } from '../forms'

import QuestionModal from './QuestionModal'
import QuestionView from './QuestionView'
import SectionModal from './SectionModal'

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
  bind: BoundForm<typeof sectionForm>
  onMoveUp: () => void
  onMoveDown: () => void
  onDelete: () => void
  first: boolean
  last: boolean
  readOnly: boolean
}

export default React.memo(function SectionView({
  bind,
  onMoveUp,
  onMoveDown,
  onDelete,
  first,
  last,
  readOnly
}: Props) {
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
              aria-label="Lisää kysymys"
              onClick={() => setCreatingQuestion(true)}
            />
            <IconButton
              icon={faPen}
              aria-label="Muokkaa"
              onClick={() => setEditing(true)}
            />
            <IconButton
              icon={faArrowUp}
              aria-label="Siirrä ylös"
              disabled={first}
              onClick={onMoveUp}
            />
            <IconButton
              icon={faArrowDown}
              aria-label="Siirrä alas"
              disabled={last}
              onClick={onMoveDown}
            />
            <IconButton
              icon={faTrash}
              aria-label="Poista"
              disabled={questionElems.length > 0}
              onClick={onDelete}
            />
          </FixedSpaceRow>
        )}
      </HeaderRow>
      <QuestionList spacing="L">
        {questionElems.map((question, index) => (
          <QuestionView
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
        <SectionModal
          onSave={(s) => {
            bind.set(s)
            setEditing(false)
          }}
          onCancel={() => setEditing(false)}
          initialState={bind.state}
        />
      )}
      {creatingQuestion && (
        <QuestionModal
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
