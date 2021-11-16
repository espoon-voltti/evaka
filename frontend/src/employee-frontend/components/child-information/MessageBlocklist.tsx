// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getChildRecipients, updateChildRecipient } from '../../api/person'
import { ChildContext } from '../../state'
import { UUID } from 'lib-common/types'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H2, P } from 'lib-components/typography'
import React, { useContext, useState } from 'react'
import { useTranslation } from '../../state/i18n'
import { useApiState } from 'lib-common/utils/useRestApi'
import { UIContext } from '../../state/ui'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { UnwrapResult } from '../async-rendering'

interface Props {
  id: UUID
  startOpen: boolean
}

export default React.memo(function ChildDetails({ id, startOpen }: Props) {
  const { i18n } = useTranslation()

  const { setErrorMessage } = useContext(UIContext)
  const { permittedActions } = useContext(ChildContext)
  const [recipients, loadData] = useApiState(() => getChildRecipients(id), [id])
  const [open, setOpen] = useState(startOpen)

  const getRoleString = (headOfChild: boolean, guardian: boolean) => {
    if (headOfChild && guardian) {
      return `${
        i18n.childInformation.messaging.guardian
      } (${i18n.childInformation.messaging.headOfChild.toLowerCase()})`
    } else if (headOfChild && !guardian) {
      return `${i18n.childInformation.messaging.headOfChild}`
    } else if (!headOfChild && guardian) {
      return `${i18n.childInformation.messaging.guardian}`
    } else {
      return null
    }
  }

  const onChange = async (personId: UUID, checked: boolean) => {
    const res = await updateChildRecipient(id, personId, !checked)
    if (res.isFailure) {
      setErrorMessage({
        type: 'error',
        title: i18n.common.error.unknown,
        text: i18n.common.error.saveFailed,
        resolveLabel: i18n.common.ok
      })
    }
    loadData()
  }

  return (
    <div className="child-message-blocklist">
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.messaging.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="child-message-blocklist-collapsible"
      >
        <P>{i18n.childInformation.messaging.info}</P>
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.childInformation.messaging.name}</Th>
              <Th>{i18n.childInformation.messaging.role}</Th>
              <Th>{i18n.childInformation.messaging.notBlocklisted}</Th>
            </Tr>
          </Thead>
          <Tbody>
            <UnwrapResult result={recipients}>
              {(recipients) => (
                <>
                  {recipients.map((recipient) => (
                    <Tr
                      key={recipient.personId}
                      data-qa={`recipient-${recipient.personId}`}
                    >
                      <Td>{`${recipient.lastName} ${recipient.firstName}`}</Td>
                      <Td>
                        {getRoleString(
                          recipient.headOfChild,
                          recipient.guardian
                        )}
                      </Td>
                      <Td>
                        <Checkbox
                          label={`${recipient.firstName} ${recipient.lastName}`}
                          hiddenLabel
                          checked={!recipient.blocklisted}
                          data-qa={'blocklist-checkbox'}
                          disabled={
                            !permittedActions.has('UPDATE_CHILD_RECIPIENT')
                          }
                          onChange={(checked) =>
                            onChange(recipient.personId, checked)
                          }
                        />
                      </Td>
                    </Tr>
                  ))}
                </>
              )}
            </UnwrapResult>
          </Tbody>
        </Table>
      </CollapsibleContentArea>
    </div>
  )
})
