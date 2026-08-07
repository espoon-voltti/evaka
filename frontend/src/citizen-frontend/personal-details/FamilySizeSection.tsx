// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { tabletMin } from 'lib-components/breakpoints'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faChevronDown, faChevronUp } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { AuthContext } from '../auth/state'
import { useTranslation } from '../localization'

import {
  DataRow,
  DataRowLabel,
  DataRowValue,
  SectionTitle,
  TitleAndEditRow
} from './components'
import { familyQuery } from './queries'

const DesktopRows = styled.div`
  @media (max-width: ${tabletMin}) {
    display: none;
  }
`

const MobileMemberList = styled.div`
  display: none;

  @media (max-width: ${tabletMin}) {
    display: block;
    border: 1px solid ${(p) => p.theme.colors.grayscale.g15};
    border-radius: 4px;
    padding: ${defaultMargins.s};
  }
`

const MemberListToggle = styled.button`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: ${defaultMargins.s};
  width: 100%;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-family: inherit;
  font-size: 1rem;
  font-weight: 600;
  color: inherit;
  text-align: left;
`

const ToggleChevron = styled(FontAwesomeIcon)`
  color: ${(p) => p.theme.colors.main.m2};
`

const MemberGroupLabel = styled.div`
  font-weight: 600;
`

const MemberName = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-size: 14px;
  font-weight: 600;
`

export default React.memo(function FamilySizeSection() {
  const t = useTranslation()
  const i18n = t.personalDetails.familySizeSection
  const { user } = useContext(AuthContext)
  const family = useQueryResult(familyQuery())

  const [memberListOpen, { toggle: toggleMemberList }] = useBoolean(false)

  return renderResult(combine(user, family), ([user, family]) => {
    const groups = [
      { dataQa: 'family-adults', label: i18n.adults, members: family.adults },
      {
        dataQa: 'family-children',
        label: i18n.children,
        members: family.children
      }
    ]
    const isSelf = (personId: PersonId) => user?.id === personId

    return (
      <div data-qa="family-size-section">
        <TitleAndEditRow>
          <SectionTitle $noMargin>{i18n.title}</SectionTitle>
        </TitleAndEditRow>
        {i18n.description}

        <Gap $size="s" />

        <DesktopRows>
          {groups.map(({ dataQa, label, members }) => (
            <DataRow key={label} data-qa={dataQa}>
              <DataRowLabel>
                {label} {members.length}
              </DataRowLabel>
              <DataRowValue>
                {members.map((member, index) => (
                  <React.Fragment key={member.personId}>
                    <span
                      data-qa={`family-member-${member.personId}`}
                      translate="no"
                    >
                      <PersonName person={member} format="First Last" />
                      {isSelf(member.personId) ? ` ${i18n.self}` : ''}
                    </span>
                    {index < members.length - 1 ? ', ' : ''}
                  </React.Fragment>
                ))}
              </DataRowValue>
            </DataRow>
          ))}
        </DesktopRows>

        <MobileMemberList>
          <MemberListToggle
            type="button"
            onClick={toggleMemberList}
            aria-expanded={memberListOpen}
            data-qa="family-member-list-toggle"
          >
            {memberListOpen ? (
              <span>
                {i18n.adults} {family.adults.length}
              </span>
            ) : (
              <span>
                {i18n.summary(family.adults.length, family.children.length)}
              </span>
            )}
            <ToggleChevron
              icon={memberListOpen ? faChevronUp : faChevronDown}
            />
          </MemberListToggle>
          {memberListOpen &&
            groups.map(({ label, members }, index) => (
              <React.Fragment key={label}>
                {index > 0 && (
                  <>
                    <Gap $size="xs" />
                    <MemberGroupLabel>
                      {label} {members.length}
                    </MemberGroupLabel>
                  </>
                )}
                <Gap $size="xs" />
                <FixedSpaceColumn $spacing="xs">
                  {members.map((member) => (
                    <MemberName key={member.personId} translate="no">
                      <PersonName person={member} format="First Last" />
                      {isSelf(member.personId) ? ` ${i18n.self}` : ''}
                    </MemberName>
                  ))}
                </FixedSpaceColumn>
              </React.Fragment>
            ))}
        </MobileMemberList>
      </div>
    )
  })
})
