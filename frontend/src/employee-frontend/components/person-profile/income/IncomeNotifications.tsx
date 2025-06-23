// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import type { PersonId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { Button } from 'lib-components/atoms/buttons/Button'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import { incomeNotificationsQuery } from '../queries'

const StyledCollapsibleContentArea = styled(CollapsibleContentArea)`
  width: 50%;
`

export const IncomeNotifications = React.memo(function IncomeNotifications({
  personId
}: {
  personId: PersonId
}) {
  const { i18n } = useTranslation()

  const incomeNotifications = useQueryResult(
    incomeNotificationsQuery({ personId })
  )

  const sortedIncomeNotifications = useMemo(
    () =>
      incomeNotifications.map((res) => orderBy(res, (n) => n.created, 'desc')),
    [incomeNotifications]
  )

  const initialNotificationAmount = 10
  const [notificationLimit, setNotificationLimit] = React.useState(
    initialNotificationAmount
  )
  const [open, setOpen] = React.useState(false)

  return renderResult(sortedIncomeNotifications, (notifications) => (
    <StyledCollapsibleContentArea
      data-qa="income-notifications-collapsible"
      aria-label={i18n.common.showMore}
      paddingHorizontal="zero"
      opaque
      open={open}
      toggleOpen={() => setOpen(!open)}
      title={
        <H3 noMargin>
          {i18n.personProfile.incomeStatement.notificationsTitle}
        </H3>
      }
    >
      {notifications.length > 0 ? (
        <FixedSpaceColumn spacing="xs">
          <Label>{i18n.personProfile.incomeStatement.notificationSent}</Label>
          <UnorderedList>
            {notifications
              .filter((_, i) => i < notificationLimit)
              .map((n, i) => (
                <li key={i} data-qa="income-notification-sent-info">
                  {n.created.format()} (
                  {
                    i18n.personProfile.incomeStatement.notificationTypes[
                      n.notificationType
                    ]
                  }
                  )
                </li>
              ))}
          </UnorderedList>
          <Gap size="s" />
          {notifications.length > notificationLimit && (
            <Button
              text={i18n.common.showMore}
              appearance="inline"
              onClick={() => setNotificationLimit(notificationLimit + 10)}
            />
          )}
          {notificationLimit > initialNotificationAmount && (
            <Button
              text={i18n.common.showLess}
              appearance="inline"
              onClick={() => setNotificationLimit(notificationLimit - 10)}
            />
          )}
        </FixedSpaceColumn>
      ) : (
        <span>{i18n.personProfile.incomeStatement.noNotifications}</span>
      )}
    </StyledCollapsibleContentArea>
  ))
})
