// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import RequireAuth from 'citizen-frontend/RequireAuth'
import { renderResult } from 'citizen-frontend/async-rendering'
import { useUser } from 'citizen-frontend/auth/state'
import ResponsiveWholePageCollapsible from 'citizen-frontend/children/ResponsiveWholePageCollapsible'
import { useTranslation } from 'citizen-frontend/localization'
import {
  ChildDocumentCitizenSummary,
  DocumentType,
  documentTypes
} from 'lib-common/generated/api-types/document'
import { useQuery, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
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
import { Dimmed, H3 } from 'lib-components/typography'
import { Gap, defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faLockAlt } from 'lib-icons'

import {
  childDocumentSummariesQuery,
  unreadChildDocumentsCountQuery
} from '../../child-documents/queries'

const DocumentsTableContainer = styled.table`
  width: 100%;
  border-collapse: collapse;
`

const DocumentTr = styled.tr<{ unread?: boolean }>`
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

const ChildDocumentsTable = React.memo(function ChildDocumentsTable({
  summaries
}: {
  summaries: ChildDocumentCitizenSummary[]
}) {
  return (
    <DocumentsTableContainer>
      <tbody>
        {summaries.map((document) => (
          <DocumentTr
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
          </DocumentTr>
        ))}
      </tbody>
    </DocumentsTableContainer>
  )
})

const MobileRowContainer = styled.div<{ unread: boolean }>`
  border-top: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  padding: ${defaultMargins.s};

  > * {
    ${(p) => (p.unread ? 'font-weight: bold;' : '')}
  }
`

const PaddingBox = styled.div`
  @media (max-width: ${tabletMin}) {
    padding: 0 ${defaultMargins.s};
  }
`

export default React.memo(function ChildDocumentsSection({
  childId
}: {
  childId: UUID
}) {
  const i18n = useTranslation()

  const [open, setOpen] = useState(false)

  const user = useUser()

  const { data: unreadChildDocumentsCount } = useQuery(
    unreadChildDocumentsCountQuery()
  )

  const unreadCount = useMemo(
    () => unreadChildDocumentsCount?.[childId] ?? 0,
    [childId, unreadChildDocumentsCount]
  )

  return (
    <ResponsiveWholePageCollapsible
      title={i18n.children.childDocuments.title}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      countIndicator={unreadCount > 0 ? unreadCount : undefined}
      data-qa="collapsible-child-documents"
      contentPadding="zero"
      icon={user?.authLevel === 'WEAK' ? faLockAlt : undefined}
    >
      <RequireAuth>
        <PaddingBox>
          <FixedSpaceColumn>
            <H3>{i18n.children.childDocuments.plansTitle}</H3>
            <ChildDocumentsList
              childId={childId}
              types={['VASU', 'LEOPS', 'MIGRATED_VASU', 'MIGRATED_LEOPS']}
            />
            <H3>{i18n.children.childDocuments.hojksTitle}</H3>
            <ChildDocumentsList childId={childId} types={['HOJKS']} />
            <H3>{i18n.children.childDocuments.otherDocumentsTitle}</H3>
            <ChildDocumentsList
              childId={childId}
              types={documentTypes.filter(
                (type) =>
                  ![
                    'VASU',
                    'LEOPS',
                    'MIGRATED_VASU',
                    'MIGRATED_LEOPS',
                    'HOJKS'
                  ].includes(type)
              )}
            />
          </FixedSpaceColumn>
        </PaddingBox>
      </RequireAuth>
    </ResponsiveWholePageCollapsible>
  )
})

const ChildDocumentsList = React.memo(function ChildDocumentsList({
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
            <Dimmed>{i18n.children.childDocuments.noDocuments}</Dimmed>
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
              <ChildDocumentsTable summaries={documents} />
            </Desktop>
          </>
        )
      )}
    </>
  )
})
