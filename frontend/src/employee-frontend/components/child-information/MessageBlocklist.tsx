// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  getChildRecipients,
  updateChildRecipient
} from '@evaka/employee-frontend/api/person'
import { Recipient } from '@evaka/employee-frontend/components/messages/types'
import { ChildContext } from '@evaka/employee-frontend/state'
import { Loading } from '@evaka/lib-common/api'
import { UUID } from '@evaka/lib-common/types'
import Checkbox from '@evaka/lib-components/atoms/form/Checkbox'
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td
} from '@evaka/lib-components/layout/Table'
import CollapsibleSection from '@evaka/lib-components/molecules/CollapsibleSection'
import { P } from '@evaka/lib-components/typography'
import { faEnvelope } from '@fortawesome/free-solid-svg-icons'
import React, { useContext } from 'react'
import { useEffect } from 'react'
import { useTranslation } from '../../state/i18n'
import { useRestApi } from '@evaka/lib-common/utils/useRestApi'
import { SpinnerSegment } from '@evaka/lib-components/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/atoms/state/ErrorSegment'
import { UIContext } from '@evaka/employee-frontend/state/ui'

interface Props {
  id: UUID
  open: boolean
}

const MessageBlocklist = React.memo(function ChildDetails({ id, open }: Props) {
  const { i18n } = useTranslation()

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
            {recipients.isLoading && <SpinnerSegment />}
            {recipients.isFailure && (
              <ErrorSegment title={i18n.common.loadingFailed} />
            )}
            {recipients.isSuccess &&
              recipients.value.map((recipient: Recipient) => (
                <Tr key={recipient.personId}>
                  <Td>{`${recipient.lastName} ${recipient.firstName}`}</Td>
                  <Td>
                    {getRoleString(recipient.headOfChild, recipient.guardian)}
                  </Td>
                  <Td>
                    <Checkbox
                      label={`${recipient.firstName} ${recipient.lastName}`}
                      hiddenLabel
                      checked={!recipient.blocklisted}
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
