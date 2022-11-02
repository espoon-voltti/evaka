// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import differenceInSeconds from 'date-fns/differenceInSeconds'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import { useTheme } from 'styled-components'

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import Toast from 'lib-components/molecules/Toast'
import { faCheck, faRedo } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import { CancelableMessage, MessageContext } from './MessageContext'
import { undoMessage } from './api'

export default React.memo(function UndoMessageNotification() {
  const { cancelableMessage } = useContext(MessageContext)
  return cancelableMessage ? (
    <UndoMessageToast cancelableMessage={cancelableMessage} />
  ) : null
})

const UndoMessageToast = React.memo(function UndoMessageToast({
  cancelableMessage
}: {
  cancelableMessage: CancelableMessage
}) {
  const theme = useTheme()
  const { i18n } = useTranslation()
  const { refreshMessages, setCancelableMessage, selectThread } =
    useContext(MessageContext)
  const [secondsLeft, setSecondsLeft] = useState<number>(() =>
    getSecondsLeft(cancelableMessage.sentAt)
  )

  const clearCancelableMessage = useCallback(
    () => setCancelableMessage(undefined),
    [setCancelableMessage]
  )
  const cancelMessage = useCallback(() => {
    if (!cancelableMessage) {
      return
    }

    void undoMessage(
      cancelableMessage.accountId,
      cancelableMessage.messageId,
      cancelableMessage.contentId
    ).then((result) => {
      if (result.isSuccess) {
        clearCancelableMessage()
        refreshMessages(cancelableMessage.accountId)
        if (cancelableMessage.contentId) {
          selectThread(undefined)
        }
      }
    })
  }, [cancelableMessage, clearCancelableMessage, refreshMessages, selectThread])

  useEffect(() => {
    const timeoutId = setTimeout(
      () =>
        secondsLeft > 0
          ? setSecondsLeft(secondsLeft - 1)
          : clearCancelableMessage(),
      1000
    )
    return () => clearTimeout(timeoutId)
  }, [clearCancelableMessage, secondsLeft])

  return (
    <Toast
      icon={faCheck}
      iconColor={theme.colors.main.m1}
      closeLabel={i18n.common.close}
      onClose={clearCancelableMessage}
      data-qa="undo-message-toast"
    >
      <FixedSpaceColumn spacing="xs">
        <div>{i18n.messages.undo.info}</div>
        <InlineButton
          icon={faRedo}
          text={`${i18n.common.cancel} (${i18n.messages.undo.secondsLeft(
            secondsLeft
          )})`}
          onClick={cancelMessage}
          data-qa="cancel-message"
        />
      </FixedSpaceColumn>
    </Toast>
  )
})

function getSecondsLeft(sentAt: HelsinkiDateTime) {
  return differenceInSeconds(
    sentAt.addSeconds(11).toSystemTzDate(),
    HelsinkiDateTime.now().toSystemTzDate()
  )
}
