// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useState,
  useMemo
} from 'react'
import styled from 'styled-components'

import MessageEditor from 'employee-frontend/components/messages/MessageEditor'
import { Failure, Result, wrapResult } from 'lib-common/api'
import {
  PostMessageBody,
  SelectableRecipientsResponse
} from 'lib-common/generated/api-types/messaging'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import {
  MessageAccountId,
  MessageDraftId,
  MessageThreadFolderId
} from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'
import { useApiState } from 'lib-common/utils/useRestApi'
import Container from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'

import { getAttachmentUrl, messageAttachment } from '../../api/attachments'
import {
  createMessage,
  deleteDraftMessage
} from '../../generated/api-clients/messaging'
import { getPersonIdentity } from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { formatPersonName } from '../../utils'
import { footerHeight } from '../Footer'
import { headerHeight } from '../Header'

import { MessageContext } from './MessageContext'
import Sidebar from './Sidebar'
import MessageList from './ThreadListContainer'

const getPersonIdentityResult = wrapResult(getPersonIdentity)
const deleteDraftMessageResult = wrapResult(deleteDraftMessage)
const createMessageResult = wrapResult(createMessage)

const PanelContainer = styled.div`
  height: calc(
    100vh - ${headerHeight} - ${footerHeight} - ${defaultMargins.XL}
  );
  display: flex;
`

export default React.memo(function MessagesPage({
  showEditor: initialShowEditor
}: {
  showEditor?: boolean
}) {
  const {
    accounts,
    folders,
    selectedDraft,
    setSelectedDraft,
    selectedAccount,
    selectDefaultAccount,
    selectAccount,
    setSelectedThread,
    refreshMessages,
    prefilledRecipient,
    prefilledTitle,
    relatedApplicationId,
    accountAllowsNewMessage
  } = useContext(MessageContext)

  const { setErrorMessage } = useContext(UIContext)
  const { i18n } = useTranslation()

  const [prefilledRecipientPerson] = useApiState(
    (): Promise<Result<PersonJSON>> =>
      prefilledRecipient
        ? getPersonIdentityResult({ personId: prefilledRecipient })
        : Promise.resolve(Failure.of({ message: 'No person id given' })),
    [prefilledRecipient]
  )

  useEffect(() => refreshMessages(), [refreshMessages])
  const [sending, setSending] = useState(false)
  const [showEditor, setShowEditor] = useState<boolean>(
    initialShowEditor ?? false
  )

  // Select first account if no account is selected. This modifies MessageContextProvider state and so has
  // to be in useEffect. Calling setState() directly in the render function is only allowed if the state
  // lives in the same component.
  useEffect(() => {
    if (accounts.isSuccess && selectedAccount === undefined) {
      selectDefaultAccount()
    }
  }, [accounts, selectedAccount, selectDefaultAccount])

  // open editor when draft is selected
  useEffect(() => {
    if (selectedDraft) {
      setShowEditor(true)
    }
  }, [selectedDraft])

  const [recipients, setRecipients] = useState<SelectableRecipientsResponse[]>()

  const hideEditor = useCallback(() => {
    setShowEditor(false)
    setSelectedDraft(undefined)
  }, [setSelectedDraft])

  const onSend = useCallback(
    (
      accountId: MessageAccountId,
      messageBody: PostMessageBody,
      initialFolder: MessageThreadFolderId | null
    ) => {
      setSending(true)
      void createMessageResult({
        accountId,
        initialFolder,
        body: {
          ...messageBody,
          relatedApplicationId
        }
      }).then((res) => {
        if (res.isSuccess) {
          refreshMessages(accountId)
          const senderAccount = accounts
            .map((accounts) =>
              accounts.find((acc) => acc.account.id === accountId)
            )
            .getOrElse(undefined)
          if (senderAccount) {
            selectAccount({
              account: senderAccount.account,
              view: 'sent',
              unitId: senderAccount.daycareGroup?.unitId ?? null
            })
            if (res.value.createdId) {
              setSelectedThread(res.value.createdId)
            }
          }
          hideEditor()
        } else {
          setErrorMessage({
            type: 'error',
            title: i18n.common.error.unknown,
            resolveLabel: i18n.common.ok
          })
        }
        setSending(false)
      })
    },
    [
      accounts,
      hideEditor,
      i18n.common.error.unknown,
      i18n.common.ok,
      refreshMessages,
      selectAccount,
      setErrorMessage,
      setSelectedThread,
      relatedApplicationId
    ]
  )

  const onDiscard = (accountId: MessageAccountId, draftId: MessageDraftId) => {
    hideEditor()
    void deleteDraftMessageResult({ accountId, draftId }).then(() =>
      refreshMessages(accountId)
    )
  }

  const onHide = (didChanges: boolean) => {
    hideEditor()
    if (didChanges) {
      refreshMessages()
    }
  }

  const prefilledOrRecipients = useMemo(():
    | SelectableRecipientsResponse[]
    | undefined => {
    if (selectedAccount === undefined) {
      return undefined
    }
    if (selectedAccount.account.type === 'FINANCE' && selectedDraft) {
      return [
        {
          accountId: selectedAccount.account.id,
          receivers: selectedDraft.recipients.map((recipient, i) => {
            return {
              id: fromUuid(recipient.accountId),
              name: selectedDraft.recipientNames[i],
              type: 'CITIZEN'
            }
          })
        }
      ]
    }

    if (prefilledRecipient) {
      if (!prefilledRecipientPerson.isSuccess) {
        return undefined
      }
      const person = prefilledRecipientPerson.getOrElse(null)
      if (person !== null) {
        return [
          {
            accountId: selectedAccount.account.id,
            receivers: [
              {
                id: prefilledRecipient,
                name: formatPersonName(person, i18n, true),
                type: 'CITIZEN'
              }
            ]
          }
        ]
      }
    }
    return recipients
  }, [
    i18n,
    prefilledRecipient,
    prefilledRecipientPerson,
    recipients,
    selectedAccount,
    selectedDraft
  ])

  return (
    <Container>
      <PanelContainer>
        <Sidebar
          showEditor={() => setShowEditor(true)}
          setRecipients={setRecipients}
          enableNewMessage={accountAllowsNewMessage()}
        />
        {selectedAccount?.view && <MessageList {...selectedAccount} />}
        {showEditor &&
          accounts.isSuccess &&
          folders.isSuccess &&
          prefilledOrRecipients &&
          selectedAccount && (
            <MessageEditor
              selectableRecipients={prefilledOrRecipients}
              defaultSender={{
                value: selectedAccount.account.id,
                label: selectedAccount.account.name
              }}
              draftContent={selectedDraft}
              getAttachmentUrl={getAttachmentUrl}
              accounts={accounts.value}
              folders={folders.value}
              onClose={onHide}
              onDiscard={onDiscard}
              onSend={onSend}
              saveMessageAttachment={messageAttachment}
              sending={sending}
              defaultTitle={prefilledTitle ?? undefined}
            />
          )}
      </PanelContainer>
    </Container>
  )
})
