import { Result } from 'lib-common/api'
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
import React, { useCallback, useEffect, useState } from 'react'
import styled from 'styled-components'
import Select from '../../components/common/Select'
import { useTranslation } from '../../state/i18n'
import * as api from './api'
import {
  getSelected,
  getSelectedBottomElements,
  SelectorChange,
  SelectorNode,
  updateSelector
} from './SelectorNode'
import { DraftContent, MessageBody } from './types'

const emptyMessageBody: MessageBody = {
  title: '',
  content: '',
  type: 'MESSAGE',
  recipientAccountIds: []
}

const draftContentToInitialState = (
  draft: DraftContent | undefined
): MessageBody =>
  draft
    ? {
        content: draft.content ?? '',
        recipientAccountIds: draft.recipientAccountIds ?? [],
        title: draft.title ?? '',
        type: draft.type ?? 'MESSAGE'
      }
    : emptyMessageBody

type Option = {
  label: string
  value: string
}

interface Props {
  defaultSender: Option
  senderOptions: Option[]
  selectedReceivers: SelectorNode
  receiverOptions: Option[]
  setSelectedReceivers: React.Dispatch<
    React.SetStateAction<SelectorNode | undefined>
  >
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
  selectedReceivers,
  receiverOptions,
  setSelectedReceivers,
  onSend,
  onDiscard,
  onClose,
  draftContent
}: Props) {
  const { i18n } = useTranslation()
  const [newDraftRequested, setNewDraftRequested] = useState<boolean>(false)
  const [draftId, setDraftId] = useState<UUID | undefined>(draftContent?.id)
  const [draftSaveResult, setDraftSaveResult] = useState<Result<void>>()
  const [selectorChange, setSelectorChange] = useState<SelectorChange>()
  const [selectedReceiverOptions, setSelectedReceiverOptions] = useState<
    Option[]
  >(getSelected(selectedReceivers))
  const [sender, setSender] = useState<Option>(defaultSender)
  const [messageBody, setMessageBody] = useState<MessageBody>(
    draftContentToInitialState(draftContent)
  )

  const [contentTouched, setContentTouched] = useState(false)
  const saveDraft = useRestApi(api.saveDraft, setDraftSaveResult)
  const initDraft = useRestApi(api.initDraft, (res: Result<UUID>) => {
    setNewDraftRequested(false)
    if (res.isSuccess) {
      setDraftId(res.value)
    }
  })

  // initialize draft when requested
  useEffect(() => {
    if (newDraftRequested && !draftId) {
      initDraft(sender.value)
    }
  }, [draftId, newDraftRequested, initDraft, sender.value])

  // save draft on input change or ask for a new draft to be initialized
  const debouncedMessageBody = useDebounce(messageBody, 2000)
  useEffect(() => {
    const shouldSaveDraft =
      contentTouched &&
      (debouncedMessageBody.title || debouncedMessageBody.content)
    if (!shouldSaveDraft) {
      return
    }
    if (!draftId) {
      setNewDraftRequested(true)
    } else {
      saveDraft(sender.value, draftId, debouncedMessageBody)
    }
  }, [draftId, debouncedMessageBody, saveDraft, sender.value, contentTouched])

  useEffect(() => {
    if (selectorChange) {
      setSelectedReceivers((selectedReceivers: SelectorNode | undefined) =>
        selectedReceivers
          ? updateSelector(selectedReceivers, selectorChange)
          : undefined
      )
    }
  }, [selectorChange, setSelectedReceivers])

  useEffect(() => {
    setSelectedReceiverOptions(getSelected(selectedReceivers))
    setMessageBody((oldMessage) => ({
      ...oldMessage,
      recipientAccountIds: getSelectedBottomElements(selectedReceivers)
    }))
  }, [selectedReceivers])

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
    messageBody.title ||
    i18n.messages.messageEditor.newMessage

  const onCloseHandler = useCallback(() => onClose(!!draftSaveResult), [
    draftSaveResult,
    onClose
  ])

  return (
    <Container>
      <TopBar>
        <span>{title}</span>
        <IconButton icon={faTimes} onClick={onCloseHandler} white />
      </TopBar>
      <FormArea>
        <div>
          <Gap size={'xs'} />
          <div>{i18n.messages.messageEditor.sender}</div>
        </div>
        <Select
          options={senderOptions}
          onChange={(selected) => setSender(selected as Option)}
          value={sender}
          data-qa="select-sender"
        />
        <div>
          <Gap size={'xs'} />
          <div>{i18n.messages.messageEditor.receivers}</div>
        </div>
        <MultiSelect
          placeholder={i18n.common.search}
          value={selectedReceiverOptions}
          options={receiverOptions}
          onChange={(newSelection: Option[]) => {
            if (newSelection.length < selectedReceiverOptions.length) {
              const values = newSelection.map((option) => option.value)
              const deselected = selectedReceiverOptions.find(
                (option) => !values.includes(option.value)
              )
              if (deselected) {
                setSelectorChange({
                  selectorId: deselected.value,
                  selected: false
                })
              }
            } else {
              const values = selectedReceiverOptions.map(
                (option) => option.value
              )
              const newlySelected = newSelection.find(
                (option) => !values.includes(option.value)
              )
              if (newlySelected) {
                setSelectorChange({
                  selectorId: newlySelected.value,
                  selected: true
                })
              }
            }
          }}
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
            checked={messageBody.type === 'MESSAGE'}
            onChange={() => setMessageBody({ ...messageBody, type: 'MESSAGE' })}
          />
          <Radio
            label={i18n.messages.messageEditor.type.bulletin}
            checked={messageBody.type === 'BULLETIN'}
            onChange={() =>
              setMessageBody({ ...messageBody, type: 'BULLETIN' })
            }
          />
        </FixedSpaceRow>
        <Gap size={'xs'} />
        <div>{i18n.messages.messageEditor.title}</div>
        <Gap size={'xs'} />
        <InputField
          value={messageBody.title}
          onChange={(title) => {
            setMessageBody((body) => ({ ...body, title: title }))
            setContentTouched(true)
          }}
          data-qa={'input-title'}
        />
        <Gap size={'s'} />

        <Label>{i18n.messages.messageEditor.message}</Label>
        <Gap size={'xs'} />
        <StyledTextArea
          value={messageBody.content}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
            setMessageBody((body) => ({ ...body, content: e.target.value }))
            setContentTouched(true)
          }}
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
            onClick={() => onSend(sender.value, messageBody, draftId)}
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
