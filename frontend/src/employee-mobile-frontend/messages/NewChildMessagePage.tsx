// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import type { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import type {
  AuthorizedMessageAccount,
  SelectableRecipient
} from 'lib-common/generated/api-types/messaging'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import {
  constantQuery,
  useChainedQuery,
  useQueryResult
} from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'
import { faArrowLeft } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { UserContext } from '../auth/state'
import { childrenQuery } from '../child-attendance/queries'
import { useChild } from '../child-attendance/utils'
import TopBar from '../common/TopBar'
import { BackButtonInline } from '../common/components'
import { useTranslation } from '../common/i18n'

import MessageEditor from './MessageEditor'
import { messagingAccountsQuery, selectableRecipientsQuery } from './queries'

interface Props {
  unitId: DaycareId
  childGroupAccount: AuthorizedMessageAccount | undefined
  child: AttendanceChild
}

const NewChildMessagePage = React.memo(function NewChildMessagePage({
  unitId,
  childGroupAccount,
  child
}: Props) {
  const { i18n } = useTranslation()

  const navigate = useNavigate()
  const messageRecipients = useQueryResult(selectableRecipientsQuery())

  const recipients = useMemo(() => {
    const findChildRecipients = (
      recipient: SelectableRecipient
    ): SelectableRecipient[] =>
      recipient.type === 'CHILD' && recipient.id === child.id
        ? [recipient]
        : 'receivers' in recipient
          ? recipient.receivers.flatMap(findChildRecipients)
          : []

    return messageRecipients.map((accounts) =>
      accounts
        .map((account) => ({
          ...account,
          receivers: account.receivers.flatMap(findChildRecipients)
        }))
        .filter((account) => account.receivers.length > 0)
    )
  }, [child.id, messageRecipients])

  const onHide = useCallback(() => {
    void navigate(-1)
  }, [navigate])

  return renderResult(recipients, (recipients) =>
    childGroupAccount !== undefined && recipients.length > 0 ? (
      <MessageEditor
        unitId={unitId}
        account={childGroupAccount.account}
        availableRecipients={recipients}
        draft={undefined}
        onClose={onHide}
      />
    ) : (
      <ContentArea
        opaque
        paddingVertical="zero"
        paddingHorizontal="zero"
        fullHeight={true}
        data-qa="messages-editor-content-area"
      >
        <TopBar title={i18n.messages.newMessage} unitId={unitId} />
        <PaddedContainer>
          {recipients.length === 0 ? (
            <span data-qa="info-no-recipients">
              {i18n.messages.noRecipients}
            </span>
          ) : (
            <span data-qa="no-account-access">
              {i18n.messages.noAccountAccess}
            </span>
          )}
        </PaddedContainer>
        <BackButtonInline
          onClick={() => navigate(-1)}
          icon={faArrowLeft}
          text={i18n.common.back}
        />
      </ContentArea>
    )
  )
})

export default React.memo(function MessageEditorPageWrapper({
  unitId,
  childId
}: {
  unitId: DaycareId
  childId: UUID
}) {
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)

  const { user } = useContext(UserContext)
  const groupAccounts = useChainedQuery(
    user.map((u) =>
      u && u.pinLoginActive && u.employeeId
        ? messagingAccountsQuery(u.employeeId, { unitId })
        : constantQuery([])
    )
  )

  return renderResult(
    combine(child, groupAccounts),
    ([child, groupAccounts]) => {
      const childGroupAccount = child.groupId
        ? groupAccounts.find((a) => a.daycareGroup?.id === child.groupId)
        : undefined
      return (
        <NewChildMessagePage
          unitId={unitId}
          childGroupAccount={childGroupAccount}
          child={child}
        />
      )
    }
  )
})

const PaddedContainer = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
`
