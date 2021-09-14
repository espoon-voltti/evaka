// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getChildRecipients, updateChildRecipient } from '../../api/person'
import { Recipient } from '../messages/types'
import { ChildContext } from '../../state'
import { Loading } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Table, Thead, Tr, Th, Tbody, Td } from 'lib-components/layout/Table'
import { H2, P } from 'lib-components/typography'
import React, { useContext, useState } from 'react'
import { useEffect } from 'react'
import { useTranslation } from '../../state/i18n'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { UIContext } from '../../state/ui'
import { CollapsibleContentArea } from 'lib-components/layout/Container'

interface Props {
  id: UUID
  startOpen: boolean
}

const MessageBlocklist = React.memo(function ChildDetails({
  id,
  startOpen
}: Props) {
  const { i18n } = useTranslation()

  const { setErrorMessage } = useContext(UIContext)
  const { recipients, setRecipients, permittedActions } =
    useContext(ChildContext)

  const [open, setOpen] = useState(startOpen)

  const loadData = useRestApi(getChildRecipients, setRecipients)

  useEffect(() => loadData(id), [id, loadData])

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
            {recipients.isLoading && (
              <Tr>
                <Td />
                <Td>
                  <SpinnerSegment />
                </Td>
                <Td />
              </Tr>
            )}
            {recipients.isFailure && (
              <Tr>
                <Td />
                <Td>
                  <ErrorSegment title={i18n.common.loadingFailed} />
                </Td>
                <Td />
              </Tr>
            )}
            {recipients.isSuccess &&
              recipients.value.map((recipient: Recipient) => (
                <Tr
                  key={recipient.personId}
                  data-qa={`recipient-${recipient.personId}`}
                >
                  <Td>{`${recipient.lastName} ${recipient.firstName}`}</Td>
                  <Td>
                    {getRoleString(recipient.headOfChild, recipient.guardian)}
                  </Td>
                  <Td>
                    <Checkbox
                      label={`${recipient.firstName} ${recipient.lastName}`}
                      hiddenLabel
                      checked={!recipient.blocklisted}
                      data-qa={'blocklist-checkbox'}
                      disabled={!permittedActions.has('UPDATE_CHILD_RECIPIENT')}
                      onChange={(checked: boolean) => {
                        setRecipients(Loading.of())
                        void updateChildRecipient(
                          id,
                          recipient.personId,
                          !checked
                        ).then((res) => {
                          if (res.isFailure) {
                            setErrorMessage({
                              type: 'error',
                              title: i18n.common.error.unknown,
                              text: i18n.common.error.saveFailed,
                              resolveLabel: i18n.common.ok
                            })
                          }
                          loadData(id)
                        })
                      }}
                    />
                  </Td>
                </Tr>
              ))}
          </Tbody>
        </Table>
      </CollapsibleContentArea>
    </div>
  )
})

export default MessageBlocklist
