// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading } from 'lib-common/api'
import { UnitStats } from 'lib-common/generated/api-types/attendance'
import { useApiState } from 'lib-common/utils/useRestApi'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights, H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronRight } from 'lib-icons'

import { getMobileUnitStats } from '../api/unit'
import { useTranslation } from '../state/i18n'
import { UserContext } from '../state/user'

import { renderResult } from './async-rendering'
import TopBar from './common/TopBar'

export default React.memo(function UnitList() {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)

  const [units] = useApiState(() => {
    const unitIds = user.map((user) => user?.unitIds).getOrElse(undefined)
    if (unitIds) {
      return getMobileUnitStats(unitIds)
    }
    return Promise.resolve(Loading.of<UnitStats[]>())
  }, [user])

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
                  totalStaff,
                  utilization
                }) => {
                  return (
                    <UnitContainer key={id} to={`/units/${id}`}>
                      <FixedSpaceColumn spacing="s" fullWidth>
                        <H2 noMargin>{name}</H2>
                        <UnitRow spacing="m">
                          <div>
                            <Stat>
                              {presentChildren}/{totalChildren}
                            </Stat>
                            <StatDesc>{i18n.units.children}</StatDesc>
                          </div>
                          <div>
                            <Stat>
                              {presentStaff}/{totalStaff}
                            </Stat>
                            <StatDesc>{i18n.units.staff}</StatDesc>
                          </div>
                          <div>
                            <Stat>
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
                }
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

const StatDesc = styled.div`
  color: ${colors.grayscale.g70};
  font-weight: ${fontWeights.semibold};
`

const Description = styled.div`
  margin: ${defaultMargins.s};
`
