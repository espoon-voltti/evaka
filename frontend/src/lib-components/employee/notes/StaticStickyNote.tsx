// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { InformationText, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { InlineAsyncButton } from './InlineAsyncButton'
import { Note } from './notes'

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
      <InformationText data-qa="sticky-note-expires">
        {labels.validTo(note.expires.format())}
      </InformationText>
      <Gap size="xs" />
      <FixedSpaceRow justifyContent="flex-end">
        <Button
          appearance="inline"
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
