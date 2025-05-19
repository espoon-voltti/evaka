// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo } from 'react'

import { PersonId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import { incomeNotificationsQuery } from '../queries'

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

  return renderResult(sortedIncomeNotifications, (notifications) =>
    notifications.length > 0 ? (
      <FixedSpaceColumn spacing="xs">
        <Label>{i18n.personProfile.incomeStatement.notificationSent}</Label>
        <UnorderedList>
          {notifications.map((n, i) => (
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
      </FixedSpaceColumn>
    ) : (
      <span>{i18n.personProfile.incomeStatement.noNotifications}</span>
    )
  )
})
