// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { faTimes, faTrash } from 'lib-icons'
import { UpdateStateFn } from 'lib-common/form-state'
import colors from 'lib-customizations/common'
import InputField from 'lib-components/atoms/form/InputField'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Button from 'lib-components/atoms/buttons/Button'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { Label } from 'lib-components/typography'
import { useTranslation } from '../../state/i18n'
import { Bulletin } from './types'
import { SelectorChange } from 'employee-frontend/components/messages/receiver-selection-utils'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import Select from '../common/Select'

type Option = {
  label: string
  value: string
}

type Props = {
  bulletin: Bulletin
  senderOptions: string[]
  onChange: UpdateStateFn<Bulletin>
  onDeleteDraft: () => void
  onClose: () => void
  onSend: () => void
  selectedReceivers: Option[]
  receiverOptions: Option[]
  updateSelection: (selectorChange: SelectorChange) => void
}

export default React.memo(function MessageEditor({
  bulletin,
  senderOptions,
  onChange,
  onDeleteDraft,
  onClose,
  onSend,
  selectedReceivers: selected,
  receiverOptions: options,
  updateSelection
}: Props) {
  const { i18n } = useTranslation()

  const mappedSenderOptions = senderOptions.map((name) => ({
    value: name,
    label: name
  }))

  return (
    <Container>
      <TopBar>
        <span>{i18n.messages.messageEditor.newBulletin}</span>
        <IconButton icon={faTimes} onClick={onClose} white />
      </TopBar>
      <FormArea>
        <div>
          <Gap size={'xs'} />
          <div>{i18n.messages.messageEditor.receivers}</div>
          <Gap size={'xs'} />
          <MultiSelect
            placeholder={i18n.common.search}
            value={selected}
            options={options}
            onChange={(newSelection) => {
              if (newSelection.length < selected.length) {
                const values = newSelection.map((option) => option.value)
                const deselected = selected.find(
                  (option) => !values.includes(option.value)
                )
                if (deselected) {
                  updateSelection({
                    selectorId: deselected.value,
                    selected: false
                  })
                }
              } else {
                const values = selected.map((option) => option.value)
                const newlySelected = newSelection.find(
                  (option) => !values.includes(option.value)
                )
                if (newlySelected) {
                  updateSelection({
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
        </div>
        <Gap size={'xs'} />
        <div>{i18n.messages.messageEditor.sender}</div>
        <Gap size={'xs'} />
        <Select
          options={mappedSenderOptions}
          onChange={(selected) =>
            selected &&
            'value' in selected &&
            onChange({ sender: selected.value })
          }
          value={
            mappedSenderOptions.find(
              ({ value }) => value === bulletin.sender
            ) || null
          }
          data-qa="select-sender"
        />
        <Gap size={'xs'} />
        <div>{i18n.messages.messageEditor.title}</div>
        <Gap size={'xs'} />
        <InputField
          value={bulletin.title}
          onChange={(title) => onChange({ title })}
          data-qa={'input-title'}
        />
        <Gap size={'s'} />

        <Label>{i18n.messages.messageEditor.message}</Label>
        <Gap size={'xs'} />
        <StyledTextArea
          value={bulletin.content}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            onChange({ content: e.target.value })
          }
          data-qa={'input-content'}
        />
        <Gap size={'s'} />
        <BottomRow>
          <InlineButton
            onClick={onDeleteDraft}
            text={i18n.messages.messageEditor.deleteDraft}
            icon={faTrash}
          />
          <Button
            text={i18n.messages.messageEditor.send}
            primary
            onClick={onSend}
            data-qa="send-bulletin-btn"
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
