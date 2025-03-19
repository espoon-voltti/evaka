// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { AuthorizedMessageAccount } from 'lib-common/generated/api-types/messaging'
import { DaycareId } from 'lib-common/generated/api-types/shared'
import {
  constantQuery,
  useChainedQuery,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { UserContext } from '../auth/state'
import { childrenQuery } from '../child-attendance/queries'
import { useChild } from '../child-attendance/utils'
import ChildNameBackButton from '../common/ChildNameBackButton'
import { useTranslation } from '../common/i18n'
import { UnitOrGroup } from '../common/unit-or-group'
import {
  FloatingPrimaryActionButton,
  TallContentArea
} from '../pairing/components'

import ReceivedThreadsList from './ReceivedThreadsList'
import { messagingAccountsQuery } from './queries'

interface Props {
  unitId: DaycareId
  groupAccounts: AuthorizedMessageAccount[]
  child: AttendanceChild
}

const ChildMessagesPage = React.memo(function ChildMessagesPage({
  unitId,
  groupAccounts,
  child
}: Props) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const childGroupAccount = child.groupId
    ? groupAccounts.find((a) => a.daycareGroup?.id === child.groupId)
    : undefined

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      <div>
        <ChildNameBackButton child={child} onClick={() => navigate(-1)} />
      </div>
      <ContentArea
        opaque
        paddingVertical="zero"
        paddingHorizontal="zero"
        fullHeight={true}
        data-qa="messages-editor-content-area"
      >
        {childGroupAccount === undefined ? (
          <PaddedContainer>
            <span data-qa="no-account-access">
              {i18n.messages.noAccountAccess}
            </span>
          </PaddedContainer>
        ) : (
          <>
            <ReceivedThreadsList
              account={childGroupAccount.account}
              groupAccounts={groupAccounts}
              onSelectThread={(threadId) => {
                const unitOrGroup: UnitOrGroup = child.groupId
                  ? { type: 'group', unitId, id: child.groupId }
                  : { type: 'unit', unitId }
                void navigate(
                  routes.receivedThread(unitOrGroup, threadId).value
                )
              }}
              childId={child.id}
            />
            <FloatingPrimaryActionButton
              text={i18n.messages.newMessage}
              icon={faPlus}
              onClick={() =>
                navigate(routes.newChildMessage(unitId, child.id).value)
              }
              data-qa="new-message-button"
            />
          </>
        )}
      </ContentArea>
    </TallContentArea>
  )
})

export default React.memo(function ChildMessagesPageWrapper({
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
      return (
        <ChildMessagesPage
          unitId={unitId}
          groupAccounts={groupAccounts}
          child={child}
        />
      )
    }
  )
})

const PaddedContainer = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
`
