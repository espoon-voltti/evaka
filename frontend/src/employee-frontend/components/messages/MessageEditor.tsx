import { Result } from 'lib-common/api'
import { UpdateStateFn } from 'lib-common/form-state'
import { UUID } from 'lib-common/types'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes, faTrash } from 'lib-icons'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import Select from '../../components/common/Select'
import { useTranslation } from '../../state/i18n'
import * as api from './api'
import {
  deselectAll,
  getReceiverOptions,
  getSelected,
  getSelectedBottomElements,
  SelectorNode,
  updateSelector
} from './SelectorNode'
import { DraftContent, MessageBody, UpsertableDraftContent } from './types'

type Message = UpsertableDraftContent & {
  senderId: UUID
  recipientAccountIds: UUID[]
}

const emptyMessage = {
  content: '',
  recipientIds: [],
  recipientNames: [],
  recipientAccountIds: [],
  title: '',
  type: 'MESSAGE' as const
}

const getInitialMessage = (
  draft: DraftContent | undefined,
  senderId: UUID
): Message =>
  draft
    ? { ...draft, senderId, recipientAccountIds: [] }
    : { senderId, ...emptyMessage }

const areRequiredFieldsFilledForMessage = (msg: Message): boolean =>
  !!(msg.recipientAccountIds?.length && msg.type && msg.content && msg.title)

const createReceiverTree = (tree: SelectorNode, selectedIds: UUID[]) =>
  selectedIds.reduce(
    (acc, next) => updateSelector(acc, { selectorId: next, selected: true }),
    deselectAll(tree)
  )

type Option = {
  label: string
  value: string
}

interface Props {
  defaultSender: Option
  senderOptions: Option[]
  availableReceivers: SelectorNode
  onSend: (
    accountId: UUID,
    messageBody: MessageBody,
    draftId: string | undefined
  ) => void
  onClose: (didChanges: boolean) => void
  onDiscard: (accountId: UUID, draftId?: UUID) => void
  draftContent?: DraftContent
}

export default React.memo(function MessageEditor({
  defaultSender,
  senderOptions,
  availableReceivers,
  onSend,
  onDiscard,
  onClose,
  draftContent
}: Props) {
  const { i18n } = useTranslation()
  const [newDraftRequested, setNewDraftRequested] = useState<boolean>(false)
  const [draftId, setDraftId] = useState<UUID | undefined>(draftContent?.id)
  const [draftSaveResult, setDraftSaveResult] = useState<Result<void>>()

  const [receiverTree, setReceiverTree] = useState<SelectorNode>(
    draftContent
      ? createReceiverTree(availableReceivers, draftContent.recipientIds)
      : availableReceivers
  )
  const selectedReceivers = useMemo(() => getSelected(receiverTree), [
    receiverTree
  ])
  const receiverOptions = useMemo(() => getReceiverOptions(receiverTree), [
    receiverTree
  ])

  const [sender, setSender] = useState<Option>(defaultSender)
  const [message, setMessage] = useState<Message>(
    getInitialMessage(draftContent, sender.value)
  )
  const [contentTouched, setContentTouched] = useState(false)
  const saveDraft = useRestApi(api.saveDraft, setDraftSaveResult)
  const initDraft = useRestApi(api.initDraft, (res: Result<UUID>) => {
    setNewDraftRequested(false)
    if (res.isSuccess) {
      setDraftId(res.value)
    }
  })

  // request for a new draft to be initialized if needed
  useEffect(() => {
    if (contentTouched && !draftId) {
      setNewDraftRequested(true)
    }
  }, [draftId, contentTouched])

  // initialize draft when requested
  useEffect(() => {
    if (newDraftRequested && !draftId) {
      initDraft(sender.value)
    }
  }, [draftId, newDraftRequested, initDraft, sender.value])

  // save draft with debounce
  const [draftToSave, setDraftToSave] = useState<Message>()
  useEffect(() => {
    contentTouched && setDraftToSave(message)
  }, [contentTouched, message])
  const debouncedDraft = useDebounce(draftToSave, 2000)
  useEffect(() => {
    if (draftId && debouncedDraft) {
      const { senderId, recipientAccountIds: _, ...rest } = debouncedDraft
      saveDraft(senderId, draftId, rest)
      setDraftToSave(undefined)
    }
  }, [draftId, debouncedDraft, saveDraft])

  // update receiver tree on selector changes
  const updateReceivers = useCallback((newSelection: Option[]) => {
    setReceiverTree((old) =>
      createReceiverTree(
        old,
        newSelection.map((s) => s.value)
      )
    )
    setContentTouched(true)
  }, [])

  // update selected receivers on receiver tree changes
  useEffect(() => {
    const selected = getSelected(receiverTree)
    const recipientAccountIds = getSelectedBottomElements(receiverTree)
    setMessage((old) => ({
      ...old,
      recipientAccountIds,
      recipientIds: selected.map((s) => s.value),
      recipientNames: selected.map((s) => s.label)
    }))
  }, [receiverTree])

  // update saving/saved/error status in title with debounce
  const [titleStatus, setTitleStatus] = useState<string>()
  const debouncedTitleStatus = useDebounce(titleStatus, 250)
  useEffect(() => {
    if (!draftSaveResult) return
    setTitleStatus(
      draftSaveResult.isLoading
        ? `${i18n.common.saving}...`
        : draftSaveResult.isSuccess
        ? i18n.common.saved
        : i18n.common.error.unknown
    )

    const clearStatus = () => setTitleStatus(undefined)
    const timeoutHandle = setTimeout(clearStatus, 1500)
    return () => clearTimeout(timeoutHandle)
  }, [i18n, draftSaveResult])

  const title =
    debouncedTitleStatus ||
    message.title ||
    i18n.messages.messageEditor.newMessage

  const updateMessage: UpdateStateFn<Message> = useCallback((changes) => {
    setMessage((old) => ({ ...old, ...changes }))
    setContentTouched(true)
  }, [])

  const sendEnabled = areRequiredFieldsFilledForMessage(message)
  const sendHandler = useCallback(() => {
    const { senderId, content, title, type, recipientAccountIds } = message
    onSend(senderId, { content, title, type, recipientAccountIds }, draftId)
  }, [onSend, message, draftId])

  const closeEnabled = !draftToSave
  const onCloseHandler = useCallback(() => {
    onClose(!!draftSaveResult)
  }, [draftSaveResult, onClose])

  return (
    <Container>
      <TopBar>
        <span>{title}</span>
        <IconButton
          icon={faTimes}
          onClick={onCloseHandler}
          white
          disabled={!closeEnabled}
        />
      </TopBar>
      <FormArea>
        <div>
          <Gap size={'xs'} />
          <div>{i18n.messages.messageEditor.sender}</div>
        </div>
        <Select
          options={senderOptions}
          onChange={(selected) => {
            setSender(selected as Option)
            setContentTouched(true)
          }}
          value={sender}
          data-qa="select-sender"
        />
        <div>
          <Gap size={'xs'} />
          <div>{i18n.messages.messageEditor.receivers}</div>
        </div>
        <MultiSelect
          placeholder={i18n.common.search}
          value={selectedReceivers}
          options={receiverOptions}
          onChange={updateReceivers}
          noOptionsMessage={i18n.common.noResults}
          getOptionId={({ value }) => value}
          getOptionLabel={({ label }) => label}
          data-qa="select-receiver"
        />
        <Gap size={'xs'} />
        <div>{i18n.messages.messageEditor.type.label}</div>
        <Gap size={'xs'} />
        <FixedSpaceRow>
          <Radio
            label={i18n.messages.messageEditor.type.message}
            checked={message.type === 'MESSAGE'}
            onChange={() => updateMessage({ type: 'MESSAGE' })}
          />
          <Radio
            label={i18n.messages.messageEditor.type.bulletin}
            checked={message.type === 'BULLETIN'}
            onChange={() => updateMessage({ type: 'BULLETIN' })}
          />
        </FixedSpaceRow>
        <Gap size={'xs'} />
        <div>{i18n.messages.messageEditor.title}</div>
        <Gap size={'xs'} />
        <InputField
          value={message.title ?? ''}
          onChange={(title) => updateMessage({ title })}
          data-qa={'input-title'}
        />
        <Gap size={'s'} />

        <Label>{i18n.messages.messageEditor.message}</Label>
        <Gap size={'xs'} />
        <StyledTextArea
          value={message.content}
          onChange={(e) => updateMessage({ content: e.target.value })}
          data-qa={'input-content'}
        />
        <Gap size={'s'} />
        <BottomRow>
          <InlineButton
            onClick={() => onDiscard(sender.value, draftId)}
            text={i18n.messages.messageEditor.deleteDraft}
            icon={faTrash}
          />
          <Button
            text={i18n.messages.messageEditor.send}
            primary
            disabled={!sendEnabled}
            onClick={sendHandler}
            data-qa="send-message-btn"
          />
        </BottomRow>
      </FormArea>
    </Container>
  )
})

const Container = styled.div`
  width: 680px;
  height: 100%;
  max-height: 700px;
  position: absolute;
  z-index: 100;
  right: 0;
  bottom: 0;
  box-shadow: 0 8px 8px 8px rgba(15, 15, 15, 0.15);
  display: flex;
  flex-direction: column;
  background-color: ${colors.greyscale.white};
`

const TopBar = styled.div`
  width: 100%;
  height: 60px;
  background-color: ${colors.primary};
  color: ${colors.greyscale.white};
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.m};
`

const FormArea = styled.div`
  width: 100%;
  flex-grow: 1;
  padding: ${defaultMargins.m};
  display: flex;
  flex-direction: column;
`

const StyledTextArea = styled.textarea`
  width: 100%;
  resize: none;
  flex-grow: 1;
`

const BottomRow = styled.div`
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
`
