// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Dimmed, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import React, { useCallback } from 'react'
import styled from 'styled-components'
import { InlineAsyncButton } from '../components'
import { Note } from './notes'

const ValidTo = styled(Dimmed)`
  font-style: italic;
`

export interface StaticLabels {
  edit: string
  remove: string
  validTo: (date: string) => string
}

interface Props {
  note: Note
  onRemove: (id: UUID) => Promise<Result<unknown>>
  editable: boolean
  onEdit: () => void
  labels: StaticLabels
}

export const StaticStickyNote = React.memo(function StaticStickyNote({
  note,
  editable,
  onEdit,
  onRemove,
  labels
}: Props) {
  const removeNote = useCallback(() => onRemove(note.id), [note.id, onRemove])
  return (
    <ContentArea opaque paddingHorizontal="s" data-qa="sticky-note">
      <P noMargin data-qa="sticky-note-note" preserveWhiteSpace>
        {note.note}
      </P>
      <Gap size="xs" />
      <ValidTo data-qa="sticky-note-expires">
        {labels.validTo(note.expires.format())}
      </ValidTo>
      <Gap size="xs" />
      <FixedSpaceRow justifyContent="flex-end">
        <InlineButton
          data-qa="sticky-note-edit"
          disabled={!editable}
          onClick={onEdit}
          text={labels.edit}
        />
        <InlineAsyncButton
          data-qa="sticky-note-remove"
          onClick={removeNote}
          onSuccess={() => null}
          text={labels.remove}
        />
      </FixedSpaceRow>
    </ContentArea>
  )
})
