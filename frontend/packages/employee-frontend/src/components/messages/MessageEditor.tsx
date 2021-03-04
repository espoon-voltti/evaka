// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import ReactSelect from 'react-select'
import _ from 'lodash'
import { faTimes, faTrash } from '@evaka/lib-icons'
import colors from '@evaka/lib-components/src/colors'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'
import { Label } from '@evaka/lib-components/src/typography'
import { useTranslation } from '../../state/i18n'
import { Bulletin, IdAndName } from './types'

type Props = {
  bulletin: Bulletin
  groups: IdAndName[]
  onChange: (change: Partial<Bulletin>) => void
  onDeleteDraft: () => void
  onClose: () => void
  onSend: () => void
}

export default React.memo(function MessageEditor({
  bulletin,
  groups,
  onChange,
  onDeleteDraft,
  onClose,
  onSend
}: Props) {
  const { i18n } = useTranslation()

  return (
    <Container>
      <TopBar>
        <span>{i18n.messages.messageEditor.newBulletin}</span>
        <IconButton icon={faTimes} onClick={onClose} white />
      </TopBar>
      <FormArea>
        <div>{i18n.messages.messageEditor.to.label}</div>
        <Gap size={'xs'} />
        <div data-qa="group-selector">
          <ReactSelect
            options={_.sortBy(groups, (g) => g.name.toLowerCase())}
            getOptionLabel={(u) => u.name}
            getOptionValue={(u) => u.id}
            value={
              groups.find((group) => group.id === bulletin.groupId) ?? null
            }
            onChange={(val) =>
              onChange({
                groupId: val
                  ? 'length' in val
                    ? val.length > 0
                      ? val[0].id
                      : null
                    : val.id
                  : null
              })
            }
            noOptionsMessage={() => i18n.messages.messageEditor.to.noOptions}
            placeholder={i18n.messages.messageEditor.to.placeholder}
          />
        </div>

        <Gap size={'s'} />

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
            disabled={
              !bulletin.groupId ||
              !groups.find((group) => group.id === bulletin.groupId)
            }
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
