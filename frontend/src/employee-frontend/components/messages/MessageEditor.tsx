import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { faTimes, faTrash } from 'lib-icons'
import colors from 'lib-customizations/common'
import InputField from 'lib-components/atoms/form/InputField'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Button from 'lib-components/atoms/buttons/Button'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { Label } from 'lib-components/typography'
import { useTranslation } from '../../state/i18n'
import { MessageBody } from './types'
import {
  getSelected,
  getSelectedBottomElements,
  SelectorChange,
  SelectorNode,
  updateSelector
} from 'employee-frontend/components/messages/SelectorNode'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import Select from 'employee-frontend/components/common/Select'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { UUID } from 'lib-common/types'

const emptyMessageBody: MessageBody = {
  title: '',
  content: '',
  type: 'MESSAGE',
  recipientAccountIds: []
}

type Option = {
  label: string
  value: string
}

interface Props {
  defaultAccountSelection: Option
  accountOptions: Option[]
  selectedReceivers: SelectorNode
  receiverOptions: Option[]
  setSelectedReceivers: React.Dispatch<
    React.SetStateAction<SelectorNode | undefined>
  >
  onSend: (accountId: UUID, messageBody: MessageBody) => void
  hideEditor: () => void
}

export default React.memo(function MessageEditor({
  defaultAccountSelection,
  accountOptions,
  selectedReceivers,
  receiverOptions,
  setSelectedReceivers,
  onSend,
  hideEditor
}: Props) {
  const { i18n } = useTranslation()
  const [selectorChange, setSelectorChange] = useState<SelectorChange>()
  const [selectedReceiverOptions, setSelectedReceiverOptions] = useState<
    Option[]
  >(getSelected(selectedReceivers))
  const [selectedAccount, setSelectedAccount] = useState<Option>(
    defaultAccountSelection
  )
  const [messageBody, setMessageBody] = useState<MessageBody>(emptyMessageBody)

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

  return (
    <Container>
      <TopBar>
        <span>{i18n.messages.messageEditor.newBulletin}</span>
        <IconButton icon={faTimes} onClick={hideEditor} white />
      </TopBar>
      <FormArea>
        <div>
          <Gap size={'xs'} />
          <div>{i18n.messages.messageEditor.sender}</div>
        </div>
        <Select
          options={accountOptions}
          onChange={(selected) =>
            selected ? setSelectedAccount(selected as Option) : null
          }
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
          onChange={(title) => setMessageBody({ ...messageBody, title: title })}
          data-qa={'input-title'}
        />
        <Gap size={'s'} />

        <Label>{i18n.messages.messageEditor.message}</Label>
        <Gap size={'xs'} />
        <StyledTextArea
          value={messageBody.content}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            setMessageBody({ ...messageBody, content: e.target.value })
          }
          data-qa={'input-content'}
        />
        <Gap size={'s'} />
        <BottomRow>
          <InlineButton
            onClick={hideEditor}
            text={i18n.messages.messageEditor.deleteDraft}
            icon={faTrash}
          />
          <Button
            text={i18n.messages.messageEditor.send}
            primary
            onClick={() => onSend(selectedAccount.value, messageBody)}
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
