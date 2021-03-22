// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  getChildRecipients,
  updateChildRecipient
} from '@evaka/employee-frontend/api/person'
import { Recipient } from '@evaka/employee-frontend/components/messages/types'
import { ChildContext } from '@evaka/employee-frontend/state'
import { Failure, Loading } from '@evaka/lib-common/api'
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
import _ from 'lodash'
import React, { useContext } from 'react'
import { useState } from 'react'
import { useEffect } from 'react'
import { useTranslation } from '../../state/i18n'

interface Props {
  id: UUID
  open: boolean
}

interface NullableCheckboxProps {
  checked: boolean | undefined
  onChange: (checked: boolean) => void
}

interface CheckedByUUID {
  id: UUID
  checked: boolean
}

const NullableCheckbox = ({ checked, onChange }: NullableCheckboxProps) => {
  const isDefined = checked === true || checked === false
  return isDefined ? (
    <Checkbox label={''} checked={checked as boolean} onChange={onChange} />
  ) : null
}

const MessageBlocklist = React.memo(function ChildDetails({ id, open }: Props) {
  const { i18n } = useTranslation()

  const { recipients, setRecipients } = useContext(ChildContext)

  const loadData = () => {
    setRecipients(Loading.of())
    void getChildRecipients(id).then(setRecipients)
  }

  const [checkboxStateById, setCheckboxStateById] = useState<CheckedByUUID[]>(
    []
  )

  useEffect(loadData, [id])
  useEffect(() => {
    recipients.isSuccess &&
      setCheckboxStateById(
        recipients.value.map((recipient) => ({
          id: recipient.personId,
          checked: !recipient.blocklisted
        }))
      )
  }, [recipients])

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
            {recipients.isSuccess &&
              recipients.value.map((recipient: Recipient) => (
                <Tr key={recipient.personId}>
                  <Td>{`${recipient.lastName} ${recipient.firstName}`}</Td>
                  <Td>
                    {getRoleString(recipient.headOfChild, recipient.guardian)}
                  </Td>
                  <Td>
                    <NullableCheckbox
                      checked={
                        checkboxStateById.find(
                          (c) => c.id === recipient.personId
                        )?.checked
                      }
                      onChange={(checked: boolean) => {
                        setCheckboxStateById(
                          checkboxStateById.map((c) =>
                            c.id === recipient.personId
                              ? { id: c.id, checked: checked }
                              : c
                          )
                        )
                        updateChildRecipient(
                          id,
                          recipient.personId,
                          !checked
                        ).catch((e) => Failure.fromError(e))
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
