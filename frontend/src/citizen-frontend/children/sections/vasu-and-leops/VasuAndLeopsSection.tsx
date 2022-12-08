// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'

import { renderResult } from 'citizen-frontend/async-rendering'
import { useUser } from 'citizen-frontend/auth/state'
import ResponsiveWholePageCollapsible from 'citizen-frontend/children/ResponsiveWholePageCollapsible'
import { VasuStateChip } from 'citizen-frontend/children/sections/vasu-and-leops/vasu/components/VasuStateChip'
import { useTranslation } from 'citizen-frontend/localization'
import { VasuDocumentSummary } from 'lib-common/generated/api-types/vasu'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { tabletMin } from 'lib-components/breakpoints'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  Desktop,
  MobileAndTablet
} from 'lib-components/layout/responsive-layout'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { Dimmed, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faExclamation } from 'lib-icons'

import { useUnreadVasuDocumentsCountQuery } from '../../../state/children/childrenApi'

import { getChildVasuSummaries } from './api'

const VasuTableContainer = styled.table`
  width: 100%;
  border-collapse: collapse;
`

const VasuTr = styled.tr`
  border-top: 1px solid ${(p) => p.theme.colors.grayscale.g15};

  & td {
    vertical-align: top;
    padding: ${defaultMargins.s};
  }
`

const LinkTd = styled.td`
  width: 40%;
`

const StateTd = styled.td`
  width: 10%;
`

const DateTd = styled.td`
  width: 10%;
`

const PermissionTd = styled.td`
  width: 40%;
`

const PermissionToShareText = styled.span`
  padding-left: 6px;
`

const PermissionToShare = React.memo(function PermissionToShare() {
  const i18n = useTranslation()
  const theme = useTheme()

  return (
    <>
      <RoundIcon
        content={faExclamation}
        color={theme.colors.status.warning}
        size="s"
        data-qa="attention-indicator"
      />
      <PermissionToShareText>
        {i18n.children.vasu.givePermissionToShareReminder}
      </PermissionToShareText>
    </>
  )
})

const VasuTable = React.memo(function VasuTable({
  summaries,
  permissionToShareRequired
}: {
  summaries: VasuDocumentSummary[]
  permissionToShareRequired: boolean
}) {
  const i18n = useTranslation()
  const user = useUser()

  return (
    <VasuTableContainer>
      <tbody>
        {summaries.map((vasu) => (
          <VasuTr key={vasu.id} data-qa={`vasu-${vasu.id}`}>
            <DateTd data-qa={`published-at-${vasu.id}`}>
              {vasu.publishedAt?.toLocalDate().format() ?? ''}
            </DateTd>
            <LinkTd>
              <Link to={`/vasu/${vasu.id}`} data-qa="vasu-link">
                {`${vasu.name}`}
              </Link>
            </LinkTd>
            <StateTd data-qa={`state-chip-${vasu.id}`}>
              <VasuStateChip
                state={vasu.documentState}
                labels={i18n.children.vasu.states}
              />
            </StateTd>
            {permissionToShareRequired && (
              <PermissionTd data-qa={`permission-to-share-needed-${vasu.id}`}>
                {!vasu.guardiansThatHaveGivenPermissionToShare.some(
                  (guardianId) => guardianId === user?.id
                ) && <PermissionToShare />}
              </PermissionTd>
            )}
          </VasuTr>
        ))}
      </tbody>
    </VasuTableContainer>
  )
})

const MobileRowContainer = styled.div`
  border-top: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  padding: ${defaultMargins.s};
`

const MobilePermissionToShareContainer = styled.div`
  padding-top: 16px;
  display: flex;
  flex-direction: row;
`

const PaddingBox = styled.div`
  @media (max-width: ${tabletMin}) {
    padding: 0 ${defaultMargins.s};
  }
`

const ParagraphInfoButton = styled(InfoButton)`
  margin-left: ${defaultMargins.xs};
`

export default React.memo(function VasuAndLeopsSection({
  childId
}: {
  childId: UUID
}) {
  const [vasus] = useApiState(() => getChildVasuSummaries(childId), [childId])

  const i18n = useTranslation()

  const [open, setOpen] = useState(false)
  const [infoOpen, setInfoOpen] = useState(false)

  const user = useUser()

  const { data: unreadVasuDocumentsCount } = useUnreadVasuDocumentsCountQuery()

  return (
    <ResponsiveWholePageCollapsible
      title={i18n.children.vasu.title}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      countIndicator={unreadVasuDocumentsCount?.[childId]}
      data-qa="collapsible-vasu"
      contentPadding="zero"
    >
      {renderResult(vasus, ({ data: items, permissionToShareRequired }) =>
        items.length === 0 ? (
          <PaddingBox>
            <Gap size="s" />
            <Dimmed>{i18n.children.vasu.noVasus}</Dimmed>
          </PaddingBox>
        ) : (
          <>
            <PaddingBox>
              <P>
                {i18n.children.vasu.givePermissionToShareInfoVasu}
                <ParagraphInfoButton
                  aria-label={i18n.common.openExpandingInfo}
                  onClick={() => setInfoOpen(!infoOpen)}
                  open={infoOpen}
                />
              </P>
              {infoOpen && (
                <ExpandingInfoBox
                  close={() => setInfoOpen(false)}
                  info={
                    i18n.children.vasu.givePermissionToShareInfoVasuInfoText
                  }
                  width="full"
                  closeLabel=""
                />
              )}
            </PaddingBox>
            <MobileAndTablet>
              {items.map((vasu) => (
                <MobileRowContainer key={vasu.id}>
                  <FixedSpaceRow justifyContent="space-between">
                    <span data-qa={`published-at-${vasu.id}`}>
                      {vasu.publishedAt?.toLocalDate().format() ?? ''}
                    </span>
                    <VasuStateChip
                      state={vasu.documentState}
                      labels={i18n.children.vasu.states}
                    />
                  </FixedSpaceRow>
                  <Gap size="xs" />
                  <Link to={`/vasu/${vasu.id}`} data-qa="vasu-link">
                    {`${vasu.name}`}
                  </Link>
                  {permissionToShareRequired &&
                    !vasu.guardiansThatHaveGivenPermissionToShare.some(
                      (guardianId) => guardianId === user?.id
                    ) && (
                      <MobilePermissionToShareContainer>
                        <PermissionToShare />
                      </MobilePermissionToShareContainer>
                    )}
                </MobileRowContainer>
              ))}
            </MobileAndTablet>
            <Desktop>
              <VasuTable
                summaries={items}
                permissionToShareRequired={permissionToShareRequired}
              />
            </Desktop>
          </>
        )
      )}
    </ResponsiveWholePageCollapsible>
  )
})
