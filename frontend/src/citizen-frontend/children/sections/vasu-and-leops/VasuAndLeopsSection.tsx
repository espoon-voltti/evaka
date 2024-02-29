// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'

import RequireAuth from 'citizen-frontend/RequireAuth'
import { renderResult } from 'citizen-frontend/async-rendering'
import { useUser } from 'citizen-frontend/auth/state'
import ResponsiveWholePageCollapsible from 'citizen-frontend/children/ResponsiveWholePageCollapsible'
import { VasuStateChip } from 'citizen-frontend/children/sections/vasu-and-leops/vasu/components/VasuStateChip'
import { useTranslation } from 'citizen-frontend/localization'
import {
  ChildDocumentCitizenSummary,
  DocumentType,
  documentTypes
} from 'lib-common/generated/api-types/document'
import { VasuDocumentSummary } from 'lib-common/generated/api-types/vasu'
import { useQuery, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { tabletMin } from 'lib-components/breakpoints'
import { ChildDocumentStateChip } from 'lib-components/document-templates/ChildDocumentStateChip'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  Desktop,
  MobileAndTablet
} from 'lib-components/layout/responsive-layout'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Dimmed, H3 } from 'lib-components/typography'
import { Gap, defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faExclamation, faLockAlt } from 'lib-icons'

import {
  childDocumentSummariesQuery,
  unreadChildDocumentsCountQuery
} from '../../../child-documents/queries'

import {
  childVasuSummariesQuery,
  unreadVasuDocumentsCountQuery
} from './queries'

const VasuTableContainer = styled.table`
  width: 100%;
  border-collapse: collapse;
`

const VasuTr = styled.tr<{ unread?: boolean }>`
  border-top: 1px solid ${(p) => p.theme.colors.grayscale.g15};

  & td {
    vertical-align: top;
    padding: ${defaultMargins.s};
  }

  ${(p) => (p.unread ? `border-left: 4px solid ${colors.status.success};` : '')}

  * {
    ${(p) => (p.unread ? 'font-weight: bold;' : '')}
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
                {vasu.name}
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

const AssistanceDocumentsTable = React.memo(function AssistanceDocumentsTable({
  summaries
}: {
  summaries: ChildDocumentCitizenSummary[]
}) {
  return (
    <VasuTableContainer>
      <tbody>
        {summaries.map((document) => (
          <VasuTr
            key={document.id}
            data-qa={`child-document-${document.id}`}
            unread={document.unread}
          >
            <DateTd data-qa={`published-at-${document.id}`}>
              {document.publishedAt?.toLocalDate().format() ?? ''}
            </DateTd>
            <LinkTd>
              <Link
                to={`/child-documents/${document.id}`}
                data-qa="child-document-link"
              >
                {document.templateName}
              </Link>
            </LinkTd>
            <StateTd>
              <ChildDocumentStateChip status={document.status} />
            </StateTd>
          </VasuTr>
        ))}
      </tbody>
    </VasuTableContainer>
  )
})

const MobileRowContainer = styled.div<{ unread: boolean }>`
  border-top: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  padding: ${defaultMargins.s};

  > * {
    ${(p) => (p.unread ? 'font-weight: bold;' : '')}
  }
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

export default React.memo(function PedagogicalDocumentsSection({
  childId
}: {
  childId: UUID
}) {
  const i18n = useTranslation()

  const [open, setOpen] = useState(false)

  const user = useUser()

  const { data: unreadVasuDocumentsCount } = useQuery(
    unreadVasuDocumentsCountQuery()
  )

  const { data: unreadChildDocumentsCount } = useQuery(
    unreadChildDocumentsCountQuery()
  )

  const unreadCount = useMemo(
    () =>
      (unreadVasuDocumentsCount?.[childId] ?? 0) +
      (unreadChildDocumentsCount?.[childId] ?? 0),
    [childId, unreadVasuDocumentsCount, unreadChildDocumentsCount]
  )

  return (
    <ResponsiveWholePageCollapsible
      title={i18n.children.vasu.title}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      countIndicator={unreadCount > 0 ? unreadCount : undefined}
      data-qa="collapsible-vasu"
      contentPadding="zero"
      icon={user?.authLevel === 'WEAK' ? faLockAlt : undefined}
    >
      <RequireAuth>
        <PaddingBox>
          <FixedSpaceColumn>
            <ExpandingInfo
              info={i18n.children.vasu.givePermissionToShareInfoVasuInfoText}
            >
              {i18n.children.vasu.givePermissionToShareInfoVasu}
            </ExpandingInfo>
            <H3>{i18n.children.vasu.plansTitle}</H3>
            <VasuAndLeopsContent childId={childId} />
            <>
              <H3>{i18n.children.vasu.hojksTitle}</H3>
              <ChildDocumentsContent childId={childId} types={['HOJKS']} />
            </>
            <H3>{i18n.children.vasu.otherDocumentsTitle}</H3>
            <ChildDocumentsContent
              childId={childId}
              types={documentTypes.filter((type) => type !== 'HOJKS')}
            />
          </FixedSpaceColumn>
        </PaddingBox>
      </RequireAuth>
    </ResponsiveWholePageCollapsible>
  )
})

const VasuAndLeopsContent = React.memo(function VasuAndLeopsContent({
  childId
}: {
  childId: UUID
}) {
  const vasus = useQueryResult(childVasuSummariesQuery(childId))
  const i18n = useTranslation()

  const user = useUser()

  return (
    <>
      {renderResult(vasus, ({ data: items, permissionToShareRequired }) =>
        items.length === 0 ? (
          <PaddingBox>
            <Gap size="s" />
            <Dimmed>{i18n.children.vasu.noVasus}</Dimmed>
          </PaddingBox>
        ) : (
          <>
            <MobileAndTablet>
              {items.map((vasu) => (
                <MobileRowContainer key={vasu.id} unread={false}>
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
                    {vasu.name}
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
    </>
  )
})

const ChildDocumentsContent = React.memo(function OtherDocumentsContent({
  childId,
  types
}: {
  childId: UUID
  types: DocumentType[]
}) {
  const i18n = useTranslation()
  const documentsResult = useQueryResult(
    childDocumentSummariesQuery({ childId })
  ).map((docs) => docs.filter((doc) => types.includes(doc.type)))

  return (
    <>
      {renderResult(documentsResult, (documents) =>
        documents.length === 0 ? (
          <PaddingBox>
            <Gap size="s" />
            <Dimmed>{i18n.children.vasu.noDocuments}</Dimmed>
          </PaddingBox>
        ) : (
          <>
            <MobileAndTablet>
              {documents.map((document) => (
                <MobileRowContainer key={document.id} unread={document.unread}>
                  <FixedSpaceRow justifyContent="space-between">
                    <span data-qa={`published-at-${document.id}`}>
                      {document.publishedAt?.toLocalDate().format() ?? ''}
                    </span>
                    <ChildDocumentStateChip status={document.status} />
                  </FixedSpaceRow>
                  <Gap size="xs" />
                  <Link
                    to={`/child-documents/${document.id}`}
                    data-qa="child-document-link"
                  >
                    {document.templateName}
                  </Link>
                </MobileRowContainer>
              ))}
            </MobileAndTablet>
            <Desktop>
              <AssistanceDocumentsTable summaries={documents} />
            </Desktop>
          </>
        )
      )}
    </>
  )
})
