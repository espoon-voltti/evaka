// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faArrowDown, faArrowUp, faPen, faTrash } from 'Icons'
import React, { useState } from 'react'
import styled from 'styled-components'

import { BoundForm } from 'lib-common/form/hooks'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import colors from 'lib-customizations/common'

import {
  DocumentQuestionView,
  questionForm
} from '../question-descriptors/questions'

import TemplateQuestionModal from './TemplateQuestionModal'

const Wrapper = styled.div<{ $readOnly: boolean }>`
  .question-actions {
    display: none;
  }

  border-style: dashed;
  border-width: 1px;
  border-color: transparent;
  &:hover {
    border-color: ${(p) =>
      p.$readOnly ? 'transparent' : colors.grayscale.g35};
    ${(p) => (p.$readOnly ? '' : `border: ${colors.grayscale.g35} 1px dashed;`)}

    .question-actions {
      display: flex;
    }
  }
`

interface Props {
  bind: BoundForm<typeof questionForm>
  onMoveUp: () => void
  onMoveDown: () => void
  onDelete: () => void
  first: boolean
  last: boolean
  readOnly: boolean
}

export default React.memo(function TemplateQuestionView({
  bind,
  onMoveUp,
  onMoveDown,
  onDelete,
  first,
  last,
  readOnly
}: Props) {
  const [editing, setEditing] = useState(false)

  return (
    <Wrapper $readOnly={readOnly}>
      <FixedSpaceRow justifyContent="space-between" alignItems="start">
        <DocumentQuestionView bind={bind} />

        {!readOnly && (
          <FixedSpaceRow className="question-actions">
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
            <IconButton icon={faTrash} aria-label="Poista" onClick={onDelete} />
          </FixedSpaceRow>
        )}
        {editing && (
          <TemplateQuestionModal
            initialState={bind.state}
            onSave={(q) => {
              bind.set(q)
              setEditing(false)
            }}
            onCancel={() => setEditing(false)}
          />
        )}
      </FixedSpaceRow>
    </Wrapper>
  )
})
