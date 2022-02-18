// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
import { H1, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

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
      {renderResult(units, (units) => (
        <ContentArea opaque>
          <H1 noMargin centered>
            {i18n.units.title}
          </H1>
          <Gap size="L" />
          <FixedSpaceColumn spacing="m">
            {units.map(
              ({
                id,
                name,
                presentChildren,
                totalChildren,
                presentStaff,
                totalStaff
              }) => {
                return (
                  <UnitContainer key={id} to={`/units/${id}`}>
                    <H2 noMargin>{name}</H2>
                    <Gap size="xxs" />
                    <FixedSpaceRow spacing="m">
                      <span>{`${i18n.units.children} ${presentChildren}/${totalChildren}`}</span>
                      <span>{`${i18n.units.staff} ${presentStaff}/${totalStaff}`}</span>
                    </FixedSpaceRow>
                  </UnitContainer>
                )
              }
            )}
          </FixedSpaceColumn>
          <Gap size="s" />
        </ContentArea>
      ))}
    </>
  )
})

const UnitContainer = styled(Link)`
  display: flex;
  flex-direction: column;
  color: ${(p) => p.theme.colors.grayscale.g100};
`
