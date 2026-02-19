// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext, useMemo } from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

import { combine } from 'lib-common/api'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { SelectionChip, ChipWrapper } from 'lib-components/atoms/Chip'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H1, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employeeMobile'
import { faBell, faGlobe } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { UserContext } from '../auth/state'
import BottomNavbar from '../common/BottomNavbar'
import TopBar from '../common/TopBar'
import { I18nContext, useTranslation } from '../common/i18n'
import { toUnitOrGroup } from '../common/unit-or-group'
import { MessageContextProvider } from '../messages/state'
import { RememberContext } from '../remember'
import { unitInfoQuery } from '../units/queries'

import { NotificationSettings } from './NotificationSettings'

const SectionIcon = styled(FontAwesomeIcon)`
  font-size: 20px;
  color: ${(p) => p.theme.colors.main.m2};
`

export const SettingsPage = React.memo(function SettingsPage({
  unitId
}: {
  unitId: DaycareId
}) {
  const [, navigate] = useLocation()
  const { i18n } = useTranslation()
  const { lang, selectLang } = useContext(I18nContext)
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
                    ? () => navigate(routes.unitList().value)
                    : undefined
                }
              />
              <ContentArea
                opaque={false}
                paddingVertical="zero"
                paddingHorizontal="s"
              >
                <H1>{i18n.common.settings}</H1>
                {featureFlags.employeeLanguageSelection && (
                  <>
                    <FixedSpaceRow spacing="s" alignItems="center">
                      <SectionIcon icon={faGlobe} />
                      <H2 noMargin>{i18n.settings.language.title}</H2>
                    </FixedSpaceRow>
                    <Gap size="s" />
                    <ChipWrapper data-qa="language-selection">
                      <SelectionChip
                        text={i18n.settings.language.fi}
                        selected={lang === 'fi'}
                        onChange={selectLang('fi')}
                        data-qa="lang-fi"
                        translate="no"
                      />
                      <SelectionChip
                        text={i18n.settings.language.sv}
                        selected={lang === 'sv'}
                        onChange={selectLang('sv')}
                        data-qa="lang-sv"
                        translate="no"
                      />
                    </ChipWrapper>
                    <Gap size="L" />
                  </>
                )}
                {unit.features.includes('PUSH_NOTIFICATIONS') && (
                  <>
                    <FixedSpaceRow spacing="s" alignItems="center">
                      <SectionIcon icon={faBell} />
                      <H2 noMargin>{i18n.settings.notifications.title}</H2>
                    </FixedSpaceRow>
                    <Gap size="s" />
                    <NotificationSettings unitId={unitId} />
                  </>
                )}
              </ContentArea>
            </>
          )
        )}
        <BottomNavbar selected="settings" unitOrGroup={unitOrGroup} />
      </ContentArea>
    </MessageContextProvider>
  )
})
