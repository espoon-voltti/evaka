import React, { useCallback, useContext, useEffect, useState } from 'react'
import { MessageContext } from '../../state/messages'
import { UUID } from 'lib-common/types'
import {
  MessageReceiversResponse,
  PostMessageBody
} from 'lib-common/generated/api-types/messaging'
import {
  receiverAsSelectorNode,
  SelectorNode
} from 'lib-components/employee/messages/SelectorNode'
import {
  deleteDraft,
  initDraft,
  postMessage,
  saveDraft,
  getReceivers
} from '../../api/messages'
import MessageEditor from 'lib-components/employee/messages/MessageEditor'
import {
  deleteAttachment,
  getAttachmentBlob,
  saveMessageAttachment
} from '../../api/attachments'
import { featureFlags } from 'lib-customizations/employee'
import { useTranslation } from '../../state/i18n'
import { useParams } from 'react-router-dom'
import { Result } from 'lib-common/api'

export default function MessageEditorPage() {
  const { i18n } = useTranslation()
  const { childId, unitId } = useParams<{
    unitId: UUID
    groupId: UUID | 'all'
    childId: UUID
  }>()
  const {
    nestedAccounts,
    selectedAccount,
    selectedUnit,
    refreshMessages,
    loadNestedAccounts
  } = useContext(MessageContext)

  useEffect(() => loadNestedAccounts(unitId), [loadNestedAccounts, unitId])

  const [sending, setSending] = useState(false)

  const [selectedReceivers, setSelectedReceivers] = useState<SelectorNode>()

  useEffect(() => {
    if (!unitId) return

    void getReceivers(unitId).then(
      (result: Result<MessageReceiversResponse[]>) => {
        if (result.isSuccess) {
          const child = result.value.map(({ receivers }) =>
            receivers.find((r) => r.childId === childId)
          )[0]
          if (!child) return
          setSelectedReceivers(receiverAsSelectorNode(child))
        }
      }
    )
  }, [childId, unitId, setSelectedReceivers])

  const onSend = useCallback(
    (accountId: UUID, messageBody: PostMessageBody) => {
      setSending(true)
      void postMessage(accountId, messageBody).then((res) => {
        if (res.isSuccess) {
          refreshMessages(accountId)
          history.back()
        } else {
          console.error('Failed to send message')
        }
        setSending(false)
      })
    },
    [refreshMessages]
  )

  const onDiscard = (accountId: UUID, draftId: UUID) => {
    void deleteDraft(accountId, draftId)
      .then(() => refreshMessages(accountId))
      .then(() => history.back())
  }

  const onHide = (didChanges: boolean) => {
    if (didChanges) {
      refreshMessages()
    }
    history.back()
  }

  return (
    <>
      {nestedAccounts.isSuccess &&
        selectedReceivers &&
        selectedAccount &&
        selectedUnit && (
          <MessageEditor
            availableReceivers={selectedReceivers}
            attachmentsEnabled={
              featureFlags.experimental?.messageAttachments ?? false
            }
            defaultSender={{
              value: selectedAccount.id,
              label: selectedAccount.name
            }}
            deleteAttachment={deleteAttachment}
            draftContent={undefined}
            getAttachmentBlob={getAttachmentBlob}
            i18n={{
              ...i18n.messages.messageEditor,
              ...i18n.fileUpload,
              ...i18n.common
            }}
            initDraftRaw={initDraft}
            mobileVersion={true}
            nestedAccounts={nestedAccounts.value}
            onClose={onHide}
            onDiscard={onDiscard}
            onSend={onSend}
            saveDraftRaw={saveDraft}
            saveMessageAttachment={saveMessageAttachment}
            selectedUnit={selectedUnit}
            sending={sending}
          />
        )}
    </>
  )
}
