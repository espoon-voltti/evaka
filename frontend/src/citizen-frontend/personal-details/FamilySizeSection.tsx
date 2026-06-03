// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'
import { tabletMin } from 'lib-components/breakpoints'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { H2, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { renderResult } from '../async-rendering'
import { AuthContext } from '../auth/state'
import { useTranslation } from '../localization'

import { familyQuery } from './queries'

const Columns = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: ${defaultMargins.XL};
  row-gap: ${defaultMargins.L};
  max-width: 960px;

  @media (max-width: ${tabletMin}) {
    grid-template-columns: 1fr;
  }
`

const ColumnHeader = styled.div`
  border-bottom: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  padding-bottom: ${defaultMargins.xxs};
  margin-bottom: ${defaultMargins.s};
`

export default React.memo(function FamilySizeSection() {
  const t = useTranslation()
  const i18n = t.personalDetails.familySizeSection
  const { user } = useContext(AuthContext)
  const family = useQueryResult(familyQuery())

  return renderResult(combine(user, family), ([user, family]) => (
    <div data-qa="family-size-section">
      <H2>{i18n.title}</H2>
      {i18n.description}
      <Columns>
        <FixedSpaceColumn $spacing="s" data-qa="family-adults">
          <ColumnHeader>
            <Label>
              {i18n.adults} {family.adults.length}
            </Label>
          </ColumnHeader>
          {family.adults.map((member) => (
            <div
              key={member.personId}
              data-qa={`family-member-${member.personId}`}
            >
              <PersonName person={member} format="First Last" />
              {user?.id === member.personId ? ` ${i18n.self}` : ''}
            </div>
          ))}
        </FixedSpaceColumn>
        <FixedSpaceColumn $spacing="s" data-qa="family-children">
          <ColumnHeader>
            <Label>
              {i18n.children} {family.children.length}
            </Label>
          </ColumnHeader>
          {family.children.map((member) => (
            <div
              key={member.personId}
              data-qa={`family-member-${member.personId}`}
            >
              <PersonName person={member} format="First Last" />
            </div>
          ))}
        </FixedSpaceColumn>
      </Columns>
    </div>
  ))
})
