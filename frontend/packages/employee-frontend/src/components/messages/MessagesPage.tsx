// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import ReactSelect from 'react-select'
import _ from 'lodash'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { useDebounce } from '@evaka/lib-common/src/utils/useDebounce'
import Container from '@evaka/lib-components/src/layout/Container'
import { H1 } from '@evaka/lib-components/src/typography'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import { Gap } from '@evaka/lib-components/src/white-space'
import { Bulletin, IdAndName } from './types'
import {
  deleteDraftBulletin,
  getGroups,
  getUnits,
  initNewBulletin,
  sendBulletin,
  updateDraftBulletin
} from './api'
import MessageEditor from './MessageEditor'

export default React.memo(function MessagesPage() {
  const [units, setUnits] = useState<Result<IdAndName[]>>(Loading.of())
  const [groups, setGroups] = useState<Result<IdAndName[]> | null>(null)
  const [editorMessage, setEditorMessage] = useState<Bulletin | null>()
  const debouncedMessage = useDebounce(editorMessage, 2000)

  const [unit, setUnit] = useState<IdAndName | null>(null)

  const loadUnits = useRestApi(getUnits, setUnits)
  useEffect(() => loadUnits(), [])

  const loadGroups = useRestApi(getGroups, setGroups)
  useEffect(() => {
    if (unit) loadGroups(unit.id)
  }, [unit])

  const onCreateNew = () => {
    if (editorMessage) return

    void initNewBulletin().then(
      (res) => res.isSuccess && setEditorMessage(res.value)
    )
  }

  useEffect(() => {
    if (debouncedMessage) {
      const { id, groupId, title, content } = debouncedMessage
      void updateDraftBulletin(id, groupId, title, content)
    }
  }, [debouncedMessage])

  const onDeleteDraft = () => {
    if (!editorMessage) return

    void deleteDraftBulletin(editorMessage.id).then(() =>
      setEditorMessage(null)
    )
  }

  const onSend = () => {
    if (!editorMessage) return

    const { id, groupId, title, content } = editorMessage
    void updateDraftBulletin(id, groupId, title, content)
      .then(() => sendBulletin(editorMessage.id))
      .then(() => setEditorMessage(null))
  }

  return (
    <Container>
      <H1>Viestit</H1>

      <ReactSelect
        options={units.isSuccess ? _.sortBy(units.value, (u) => u.name) : []}
        getOptionLabel={(u) => u.name}
        getOptionValue={(u) => u.id}
        value={unit}
        onChange={(val) =>
          setUnit(
            val
              ? 'length' in val
                ? val.length > 0
                  ? val[0]
                  : null
                : val
              : null
          )
        }
      />

      <Gap size={'s'} />

      <Button
        text={'Uusi tiedote'}
        primary
        onClick={onCreateNew}
        disabled={!unit || groups?.isSuccess !== true}
      />

      {editorMessage && groups?.isSuccess && (
        <MessageEditor
          bulletin={editorMessage}
          groups={groups.value}
          onChange={(change) =>
            setEditorMessage((old) => (old ? { ...old, ...change } : old))
          }
          onClose={() => setEditorMessage(null)}
          onDeleteDraft={onDeleteDraft}
          onSend={onSend}
        />
      )}
    </Container>
  )
})
