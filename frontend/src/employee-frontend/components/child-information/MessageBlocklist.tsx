// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  getChildRecipients,
  updateChildRecipient
} from 'employee-frontend/api/person'
import { Recipient } from 'employee-frontend/components/messages/types'
import { ChildContext } from 'employee-frontend/state'
import { Loading } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Table, Thead, Tr, Th, Tbody, Td } from 'lib-components/layout/Table'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { P } from 'lib-components/typography'
import { faEnvelope } from '@fortawesome/free-solid-svg-icons'
import React, { useContext } from 'react'
import { useEffect } from 'react'
import { useTranslation } from '../../state/i18n'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { UIContext } from 'employee-frontend/state/ui'
import { UserContext } from 'employee-frontend/state/user'

interface Props {
  id: UUID
  open: boolean
}

const MessageBlocklist = React.memo(function ChildDetails({ id, open }: Props) {
  const { i18n } = useTranslation()

  const { roles } = useContext(UserContext)
  const { setErrorMessage } = useContext(UIContext)
  const { recipients, setRecipients } = useContext(ChildContext)

  const loadData = useRestApi(getChildRecipients, setRecipients)

  useEffect(() => loadData(id), [id])

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
      <CollapsibleSection
        data-qa="child-message-blocklist-collapsible"
        icon={faEnvelope}
        title={i18n.childInformation.messaging.title}
        startCollapsed={!open}
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
                <Td></Td>
                <Td>
                  <SpinnerSegment />
                </Td>
                <Td></Td>
              </Tr>
            )}
            {recipients.isFailure && (
              <Tr>
                <Td></Td>
                <Td>
                  <ErrorSegment title={i18n.common.loadingFailed} />
                </Td>
                <Td></Td>
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
                      disabled={
                        !roles.find((r) =>
                          [
                            'ADMIN',
                            'SERVICE_WORKER',
                            'UNIT_SUPERVISOR'
                          ].includes(r)
                        )
                      }
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
      </CollapsibleSection>
    </div>
  )
})

export default MessageBlocklist
