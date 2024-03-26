// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { useQueryResult } from 'lib-common/query'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights, H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronRight } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { UserContext } from '../auth/state'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'

import { unitStatsQuery } from './queries'

export default React.memo(function UnitList() {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)

  const unitIds = user.map((user) => user?.unitIds).getOrElse(undefined)
  const units = useQueryResult(unitStatsQuery({ unitIds: unitIds ?? [] }), {
    enabled: !!unitIds,
    refetchOnMount: 'always'
  })

  return (
    <>
      <TopBar title={i18n.units.title} />
      <Gap size="xs" />
      <FixedSpaceColumn spacing="xs">
        {renderResult(units, (units) => (
          <ContentArea paddingVertical="s" opaque>
            <FixedSpaceColumn spacing="m">
              {units.map(
                ({
                  id,
                  name,
                  presentChildren,
                  totalChildren,
                  presentStaff,
                  presentStaffOther,
                  totalStaff,
                  utilization
                }) => (
                  <UnitContainer
                    key={id}
                    to={`/units/${id}`}
                    data-qa={`unit-${id}`}
                  >
                    <FixedSpaceColumn spacing="s" fullWidth>
                      <H2 noMargin>{name}</H2>
                      <UnitRow spacing="m">
                        <div>
                          <Stat data-qa="child-count">
                            {presentChildren}/{totalChildren}
                          </Stat>
                          <StatDesc>{i18n.units.children}</StatDesc>
                        </div>
                        <div>
                          <Stat data-qa="staff-count">
                            {presentStaff}
                            {presentStaffOther ? (
                              <OtherStaff>+{presentStaffOther}</OtherStaff>
                            ) : null}
                            /{totalStaff}
                          </Stat>
                          <StatDesc>{i18n.units.staff}</StatDesc>
                        </div>
                        <div>
                          <Stat data-qa="utilization">
                            {utilization.toFixed
                              ? utilization.toFixed(1)
                              : utilization}{' '}
                            %
                          </Stat>
                          <StatDesc>{i18n.units.utilization}</StatDesc>
                        </div>
                      </UnitRow>
                    </FixedSpaceColumn>
                    <FontAwesomeIcon
                      icon={faChevronRight}
                      size="lg"
                      color={colors.main.m2}
                    />
                  </UnitContainer>
                )
              )}
            </FixedSpaceColumn>
          </ContentArea>
        ))}
        <Description>
          <span>{i18n.units.description}</span>
        </Description>
      </FixedSpaceColumn>
    </>
  )
})

const UnitContainer = styled(Link)`
  display: flex;
  flex-direction: row;
  color: ${(p) => p.theme.colors.grayscale.g100};
  align-items: center;
`

const UnitRow = styled(FixedSpaceRow)`
  flex: 1;
`

const Stat = styled.div`
  font-family: Montserrat, sans-serif;
  font-size: 20px;
  font-weight: ${fontWeights.semibold};
  color: ${colors.grayscale.g100};
`

const OtherStaff = styled.span`
  color: ${colors.grayscale.g70};
`

const StatDesc = styled.div`
  color: ${colors.grayscale.g70};
  font-weight: ${fontWeights.semibold};
`

const Description = styled.div`
  margin: ${defaultMargins.s};
`
