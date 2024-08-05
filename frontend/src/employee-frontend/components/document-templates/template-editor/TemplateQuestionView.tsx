// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { BoundForm } from 'lib-common/form/hooks'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import colors from 'lib-customizations/common'
import { faArrowDown, faArrowUp, faPen, faTrash } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import {
  type templateQuestionForm,
  TemplateQuestionPreview
} from '../templates'

import TemplateQuestionModal from './TemplateQuestionModal'

const Wrapper = styled.div<{ $readOnly: boolean }>`
  .question-actions {
    visibility: hidden;
  }

  border-style: dashed;
  border-width: 1px;
  border-color: transparent;
  &:hover {
    border-color: ${(p) =>
      p.$readOnly ? 'transparent' : colors.grayscale.g35};
    ${(p) => (p.$readOnly ? '' : `border: ${colors.grayscale.g35} 1px dashed;`)}

    .question-actions {
      visibility: visible;
    }
  }
`

interface Props {
  bind: BoundForm<typeof templateQuestionForm>
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
  const { i18n } = useTranslation()
  const [editing, setEditing] = useState(false)

  return (
    <Wrapper $readOnly={readOnly}>
      <FixedSpaceRow justifyContent="space-between" alignItems="start">
        <TemplateQuestionPreview bind={bind} />

        {!readOnly && (
          <FixedSpaceRow className="question-actions">
            <IconOnlyButton
              icon={faPen}
              aria-label={i18n.common.edit}
              onClick={() => setEditing(true)}
            />
            <IconOnlyButton
              icon={faArrowUp}
              aria-label={i18n.documentTemplates.templateEditor.moveUp}
              disabled={first}
              onClick={onMoveUp}
            />
            <IconOnlyButton
              icon={faArrowDown}
              aria-label={i18n.documentTemplates.templateEditor.moveDown}
              disabled={last}
              onClick={onMoveDown}
            />
            <IconOnlyButton
              icon={faTrash}
              aria-label={i18n.common.remove}
              onClick={onDelete}
            />
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
