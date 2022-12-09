// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import FocusLock from 'react-focus-lock'
import styled from 'styled-components'

import { renderResult } from 'citizen-frontend/async-rendering'
import { Result } from 'lib-common/api'
import {
  CitizenMessageBody,
  GetReceiversResponse
} from 'lib-common/generated/api-types/messaging'
import { formatPreferredName } from 'lib-common/names'
import { SelectionChip } from 'lib-components/atoms/Chip'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { desktopMin } from 'lib-components/breakpoints'
import { FixedSpaceFlexWrap } from 'lib-components/layout/flex-helpers'
import { Bold, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import { useChildrenQueryResult } from '../state/children/childrenApi'

const emptyMessage: CitizenMessageBody = {
  title: '',
  content: '',
  recipients: [],
  children: []
}

const areRequiredFieldsFilledForMessage = (msg: CitizenMessageBody): boolean =>
  !!(msg.recipients.length && msg.content && msg.title)

interface Props {
  receiverOptions: GetReceiversResponse
  onSend: (messageBody: CitizenMessageBody) => Promise<Result<unknown>>
  onSuccess: () => void
  onFailure: () => void
  onClose: () => void
  displaySendError: boolean
}

export default React.memo(function MessageEditor({
  receiverOptions,
  onSend,
  onSuccess,
  onFailure,
  onClose,
  displaySendError
}: Props) {
  const i18n = useTranslation()
  const user = useUser()

  const [message, setMessage] = useState<CitizenMessageBody>(emptyMessage)

  const title = message.title || i18n.messages.messageEditor.newMessage

  const send = useCallback(() => onSend(message), [message, onSend])
  const sendEnabled = areRequiredFieldsFilledForMessage(message)

  const children = useChildrenQueryResult()

  const childIds = useMemo(
    () =>
      message.recipients.length === 0
        ? receiverOptions.messageAccountsToChildren.values
        : message.recipients.flatMap(
            (acc) => receiverOptions.messageAccountsToChildren[acc.id]
          ),
    [message.recipients, receiverOptions]
  )

  useEffect(
    function autoselectOnlyChild() {
      if (childIds && childIds.length === 1) {
        setMessage((message) => ({
          ...message,
          children: [childIds[0]]
        }))
      }
    },
    [childIds]
  )

  return (
    <ModalAccessibilityWrapper>
      <FocusLock>
        <Container>
          <TopBar>
            <Title>{title}</Title>
            <IconButton
              icon={faTimes}
              onClick={() => onClose()}
              white
              data-qa="close-message-editor-btn"
              aria-label={i18n.common.close}
            />
          </TopBar>
          <FormArea>
            {user && (
              <>
                <Bold>{i18n.messages.sender}</Bold>
                <Gap size="xs" />
                <P noMargin>{`${user.firstName} ${user.lastName}`}</P>
              </>
            )}
            <label>
              <Bold>{i18n.messages.messageEditor.recipients}</Bold>
              <Gap size="xs" />
              <MultiSelect
                placeholder={i18n.messages.messageEditor.search}
                value={message.recipients}
                options={receiverOptions.messageAccounts}
                onChange={(change) =>
                  setMessage((message) => ({
                    ...message,
                    recipients: change
                  }))
                }
                noOptionsMessage={i18n.messages.messageEditor.noResults}
                getOptionId={({ id }) => id}
                getOptionLabel={({ name }) => name}
                autoFocus={true}
                data-qa="select-receiver"
              />
            </label>

            <Gap size="s" />
            {childIds && childIds.length > 1 && (
              <>
                <label>
                  <Bold>{i18n.messages.messageEditor.children}</Bold>
                  {renderResult(children, (children) => (
                    <FixedSpaceFlexWrap horizontalSpacing="xs">
                      {children
                        .filter((child) => childIds.includes(child.id))
                        .map((child) => (
                          <SelectionChip
                            key={child.id}
                            text={formatPreferredName(child)}
                            selected={message.children.includes(child.id)}
                            onChange={(selected) => {
                              if (selected) {
                                setMessage((message) => ({
                                  ...message,
                                  children: [...message.children, child.id]
                                }))
                              } else {
                                setMessage((message) => ({
                                  ...message,
                                  children: message.children.filter(
                                    (id) => id !== child.id
                                  )
                                }))
                              }
                            }}
                            data-qa={`child-${child.id}`}
                          />
                        ))}
                    </FixedSpaceFlexWrap>
                  ))}
                </label>
                <Gap size="s" />
              </>
            )}

            <label>
              <Bold>{i18n.messages.messageEditor.subject}</Bold>
              <InputField
                value={message.title ?? ''}
                onChange={(updated) =>
                  setMessage((message) => ({ ...message, title: updated }))
                }
                data-qa="input-title"
              />
            </label>

            <Gap size="s" />

            <label>
              <Bold>{i18n.messages.messageEditor.message}</Bold>
              <Gap size="s" />
              <StyledTextArea
                value={message.content}
                onChange={(updated) =>
                  setMessage((message) => ({
                    ...message,
                    content: updated.target.value
                  }))
                }
                data-qa="input-content"
              />
            </label>

            <Gap size="s" />
            {displaySendError && (
              <ErrorMessage>
                {i18n.messages.messageEditor.messageSendError}
              </ErrorMessage>
            )}
            <BottomRow>
              <Button
                text={i18n.messages.messageEditor.discard}
                onClick={onClose}
              />
              <span />
              <AsyncButton
                primary
                text={i18n.messages.messageEditor.send}
                disabled={!sendEnabled}
                onClick={send}
                onSuccess={onSuccess}
                onFailure={onFailure}
                data-qa="send-message-btn"
              />
            </BottomRow>
          </FormArea>
        </Container>
      </FocusLock>
    </ModalAccessibilityWrapper>
  )
})

const Container = styled.div`
  width: 100%;
  max-width: 680px;
  height: 100%;
  max-height: 700px;
  position: absolute;
  z-index: 100;
  right: 0;
  bottom: 0;
  box-shadow: 0 8px 8px 8px rgba(15, 15, 15, 0.15);
  display: flex;
  flex-direction: column;
  background-color: ${colors.grayscale.g0};
  @media (max-width: ${desktopMin}) {
    width: 100vw;
    height: 100%;
    max-width: 100vw;
    max-height: 100%;
    position: fixed;
    top: 0;
    left: 0;
    overflow-y: scroll;
  }
`

const ErrorMessage = styled.div`
  color: ${colors.status.danger};
`

const TopBar = styled.div`
  width: 100%;
  height: 60px;
  background-color: ${colors.main.m2};
  color: ${colors.grayscale.g0};
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.m};
`

const Title = styled.span`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: ${defaultMargins.s};
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
  min-height: 100px;
`

const BottomRow = styled.div`
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
`
