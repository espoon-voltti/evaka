// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { IncomeNotification } from 'lib-common/generated/api-types/invoicing'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import colors from 'lib-customizations/common'
import { faExclamation } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'

const IncomeNotificationsContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: left;
`

const IncomeNotificationsListContainer = styled.div`
  margin-left: 6px;
`

const IncomeNotificationsBulletList = styled.ul`
  margin-top: 6px;
`

export const IncomeNotifications = React.memo(function IncomeNotifications({
  incomeNotifications
}: {
  incomeNotifications: IncomeNotification[]
}) {
  const { i18n } = useTranslation()

  const sortedIncomeNotifications = incomeNotifications.sort((a, b) =>
    a.created.toLocalDate().compareTo(b.created.toLocalDate())
  )

  return (
    <IncomeNotificationsContainer data-qa="income-notifications">
      <RoundIcon content={faExclamation} color={colors.main.m1} size="s" />
      <IncomeNotificationsListContainer>
        {i18n.personProfile.income.incomeNotifications.title}
        <IncomeNotificationsBulletList>
          {sortedIncomeNotifications.map((notification) => (
            <li
              key={notification.created.formatIso()}
              data-qa="income-notification-sent-date"
            >
              {notification.created.format()}
            </li>
          ))}
        </IncomeNotificationsBulletList>
      </IncomeNotificationsListContainer>
    </IncomeNotificationsContainer>
  )
})
