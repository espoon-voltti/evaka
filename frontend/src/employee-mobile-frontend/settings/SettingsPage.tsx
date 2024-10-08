// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'

import { renderResult } from '../async-rendering'
import { UserContext } from '../auth/state'
import BottomNavbar from '../common/BottomNavbar'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import { toUnitOrGroup } from '../common/unit-or-group'
import { MessageContextProvider } from '../messages/state'
import { RememberContext } from '../remember'
import { unitInfoQuery } from '../units/queries'

import { NotificationSettings } from './NotificationSettings'

export const SettingsPage = React.memo(function SettingsPage({
  unitId
}: {
  unitId: UUID
}) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()
  const { groupId } = useContext(RememberContext)
  const unitOrGroup = useMemo(
    () => toUnitOrGroup(unitId, groupId),
    [unitId, groupId]
  )
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))
  const { user: userResponse } = useContext(UserContext)

  return (
    <MessageContextProvider unitOrGroup={unitOrGroup}>
      <ContentArea opaque paddingVertical="zero" paddingHorizontal="zero">
        {renderResult(
          combine(userResponse, unitInfoResponse),
          ([user, unit]) => (
            <>
              <TopBar
                title={unit.name}
                unitId={unitId}
                onBack={
                  user && user.unitIds.length > 1
                    ? () => navigate('/units')
                    : undefined
                }
              />
              <ContentArea
                opaque={false}
                paddingVertical="zero"
                paddingHorizontal="s"
              >
                <H1>{i18n.common.settings}</H1>
                <NotificationSettings unitId={unitId} />
              </ContentArea>
            </>
          )
        )}
        <BottomNavbar selected="settings" unitOrGroup={unitOrGroup} />
      </ContentArea>
    </MessageContextProvider>
  )
})
