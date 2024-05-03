// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo, useState } from 'react'

import { wrapResult } from 'lib-common/api'
import { Recipient } from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H2, P } from 'lib-components/typography'

import {
  editRecipient,
  getRecipients
} from '../../generated/api-clients/messaging'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { formatPersonName } from '../../utils'
import { renderResult } from '../async-rendering'

const getRecipientsResult = wrapResult(getRecipients)
const editRecipientResult = wrapResult(editRecipient)

interface Props {
  childId: UUID
  startOpen: boolean
}

export default React.memo(function MessageBlocklist({
  childId,
  startOpen
}: Props) {
  const { i18n } = useTranslation()

  const { setErrorMessage } = useContext(UIContext)
  const { permittedActions } = useContext(ChildContext)
  const [open, setOpen] = useState(startOpen)
  const [recipients, loadData] = useApiState(
    () => getRecipientsResult({ childId }),
    [childId]
  )
  const [saving, setSaving] = useState(false)

  const allowUpdate = useMemo(
    () => permittedActions.has('UPDATE_CHILD_RECIPIENT'),
    [permittedActions]
  )
  const onChange = useCallback(
    async (personId: UUID, checked: boolean) => {
      setSaving(true)
      const res = await editRecipientResult({
        childId,
        personId,
        body: {
          blocklisted: !checked
        }
      })
      if (res.isFailure) {
        setErrorMessage({
          type: 'error',
          title: i18n.common.error.unknown,
          text: i18n.common.error.saveFailed,
          resolveLabel: i18n.common.ok
        })
      }
      setSaving(false)
      void loadData()
    },
    [i18n, childId, loadData, setErrorMessage]
  )

  return (
    <div data-qa="child-message-blocklist">
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.messaging.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="child-message-blocklist-collapsible"
      >
        <P>{i18n.childInformation.messaging.info}</P>
        {renderResult(recipients, (recipients) => (
          <Table>
            <Thead>
              <Tr>
                <Th>{i18n.childInformation.messaging.name}</Th>
                <Th>{i18n.childInformation.messaging.notBlocklisted}</Th>
              </Tr>
            </Thead>
            <Tbody>
              {recipients.map((recipient) => (
                <RecipientRow
                  key={recipient.personId}
                  recipient={recipient}
                  disabled={!allowUpdate || saving}
                  onChange={onChange}
                />
              ))}
            </Tbody>
          </Table>
        ))}
      </CollapsibleContentArea>
    </div>
  )
})

const RecipientRow = React.memo(function RecipientRow({
  recipient,
  disabled,
  onChange
}: {
  recipient: Recipient
  disabled: boolean
  onChange: (personId: UUID, checked: boolean) => void
}) {
  const { i18n } = useTranslation()

  const handleChange = useCallback(
    (checked: boolean) => onChange(recipient.personId, checked),
    [onChange, recipient.personId]
  )

  return (
    <Tr data-qa={`recipient-${recipient.personId}`}>
      <Td>{formatPersonName(recipient, i18n, true)}</Td>
      <Td>
        <Checkbox
          label={formatPersonName(recipient, i18n, true)}
          hiddenLabel
          checked={!recipient.blocklisted}
          data-qa="blocklist-checkbox"
          disabled={disabled}
          onChange={handleChange}
        />
      </Td>
    </Tr>
  )
})
