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
import React, { useCallback, useContext } from 'react'
import styled from 'styled-components'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { InlineAsyncButton } from '../components'
import { Note } from './notes'

const ValidTo = styled(Dimmed)`
  font-style: italic;
`

interface Props {
  note: Note
  onRemove: (id: UUID) => Promise<Result<unknown>>
  editable: boolean
  onEdit: () => void
}

export const StaticStickyNote = React.memo(function StaticStickyNote({
  note,
  editable,
  onEdit,
  onRemove
}: Props) {
  const { i18n } = useTranslation()
  const { reloadAttendances } = useContext(ChildAttendanceContext)
  const reloadOnSuccess = useCallback(
    (res: Result<unknown>) => res.map(() => reloadAttendances()),
    [reloadAttendances]
  )
  const removeNote = useCallback(
    () => onRemove(note.id).then(reloadOnSuccess),
    [note.id, onRemove, reloadOnSuccess]
  )
  return (
    <ContentArea opaque paddingHorizontal="s" data-qa="sticky-note">
      <P noMargin data-qa="sticky-note-note" preserveWhiteSpace>
        {note.note}
      </P>
      <Gap size="xs" />
      <ValidTo data-qa="sticky-note-expires">
        {i18n.common.validTo(note.expires.format())}
      </ValidTo>
      <Gap size="xs" />
      <FixedSpaceRow justifyContent="flex-end">
        <InlineButton
          data-qa="sticky-note-edit"
          disabled={!editable}
          onClick={onEdit}
          text={i18n.common.edit}
        />
        <InlineAsyncButton
          data-qa="sticky-note-remove"
          onClick={removeNote}
          onSuccess={reloadAttendances}
          text={i18n.common.remove}
        />
      </FixedSpaceRow>
    </ContentArea>
  )
})
