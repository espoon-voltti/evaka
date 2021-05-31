import React from 'react'
import styled from 'styled-components'
import { Result } from '../../lib-common/api'
import { UUID } from '../../lib-common/types'
import Button from '../atoms/buttons/Button'
import { TextArea } from '../atoms/form/InputField'
import ButtonContainer from '../layout/ButtonContainer'
import { defaultMargins } from '../white-space'

const MultiRowTextArea = styled(TextArea)`
  height: unset;
  margin-top: ${defaultMargins.xs};
  border: 1px solid ${({ theme: { colors } }) => colors.greyscale.medium};

  &:focus {
    border: 1px solid ${({ theme: { colors } }) => colors.greyscale.medium};
  }
`
const EditorRow = styled.div`
  & + & {
    margin-top: ${defaultMargins.s};
  }
`
const Label = styled.span`
  font-weight: 600;
`

interface Labels {
  recipients: string
  message: string
  messagePlaceholder?: string
  send: string
  sending: string
}

interface Props {
  recipients: { id: UUID; name: string }[]
  onSubmit: () => void
  replyState: Result<void> | undefined
  onUpdateContent: (content: string) => void
  replyContent: string
  i18n: Labels
}

export function MessageReplyEditor({
  i18n,
  onSubmit,
  onUpdateContent,
  recipients,
  replyContent,
  replyState
}: Props) {
  return (
    <>
      <EditorRow>
        <Label>{i18n.recipients}:</Label>{' '}
        {recipients.map((r) => r.name).join(', ')}
      </EditorRow>
      <EditorRow>
        <Label>{i18n.message}</Label>
        <MultiRowTextArea
          rows={4}
          placeholder={i18n.messagePlaceholder}
          value={replyContent}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            onUpdateContent(e.target.value)
          }
        />
      </EditorRow>
      <EditorRow>
        <ButtonContainer justify="flex-start">
          <Button
            text={replyState?.isLoading ? i18n.sending : i18n.send}
            primary
            data-qa="message-send-btn"
            onClick={onSubmit}
            disabled={replyState && !replyState.isSuccess}
          />
        </ButtonContainer>
      </EditorRow>
    </>
  )
}
