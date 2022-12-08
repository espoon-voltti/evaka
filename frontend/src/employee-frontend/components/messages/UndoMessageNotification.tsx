// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import differenceInSeconds from 'date-fns/differenceInSeconds'
import React, { useCallback, useContext, useEffect, useState } from 'react'

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { faRedo } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import { CancelableMessage, MessageContext } from './MessageContext'
import { undoBulletin, undoMessage, undoMessageReply } from './api'

export const UndoMessage = React.memo(function UndoMessageToast({
  message,
  close
}: {
  message: CancelableMessage
  close: () => void
}) {
  const { i18n } = useTranslation()
  const { refreshMessages, selectThread } = useContext(MessageContext)
  const [secondsLeft, setSecondsLeft] = useState<number>(() =>
    getSecondsLeft(message.sentAt)
  )

  const cancelMessage = useCallback(() => {
    if (!message) {
      return
    }

    const request =
      'contentId' in message
        ? undoMessage(message.accountId, message.contentId)
        : 'messageId' in message
        ? undoMessageReply(message.accountId, message.messageId)
        : undoBulletin(message.accountId, message.bulletinId)

    void request.then((result) => {
      if (result.isSuccess) {
        close()
        refreshMessages(message.accountId)
        if (!('messageId' in message)) {
          selectThread(undefined)
        }
      }
    })
  }, [close, message, refreshMessages, selectThread])

  useEffect(() => {
    const timeoutId = setTimeout(
      () => (secondsLeft > 0 ? setSecondsLeft(secondsLeft - 1) : close()),
      1000
    )
    return () => clearTimeout(timeoutId)
  }, [close, secondsLeft])

  return (
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
  )
})

function getSecondsLeft(sentAt: HelsinkiDateTime) {
  return differenceInSeconds(
    sentAt.addSeconds(11).toSystemTzDate(),
    HelsinkiDateTime.now().toSystemTzDate()
  )
}
