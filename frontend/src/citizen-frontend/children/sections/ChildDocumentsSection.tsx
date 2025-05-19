// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import {
  ChildDocumentCitizenSummary,
  DocumentType,
  documentTypes
} from 'lib-common/generated/api-types/document'
import { ChildId } from 'lib-common/generated/api-types/shared'
import { useQuery, useQueryResult } from 'lib-common/query'
import { tabletMin } from 'lib-components/breakpoints'
import { ChildDocumentStateChip } from 'lib-components/document-templates/ChildDocumentStateChip'
import { getDocumentCategory } from 'lib-components/document-templates/documents'
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

import RequireAuth from '../../RequireAuth'
import { renderResult } from '../../async-rendering'
import { useUser } from '../../auth/state'
import {
  childDocumentSummariesQuery,
  unreadChildDocumentsCountQuery
} from '../../child-documents/queries'
import ResponsiveWholePageCollapsible from '../../children/ResponsiveWholePageCollapsible'
import { useTranslation } from '../../localization'

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

const DetailsTd = styled.td`
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
            <DetailsTd>
              <Answered document={document} />
              <DecisionValidity document={document} />
            </DetailsTd>
            <StateTd>
              <ChildDocumentStateChip
                status={document.decision?.status ?? document.status}
              />
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
  childId: ChildId
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
  childId: ChildId
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
                    <Answered document={document} />
                    <DecisionValidity document={document} />
                  </FixedSpaceRow>
                  <Gap size="xs" />
                  <FixedSpaceRow justifyContent="space-between">
                    <Link
                      to={`/child-documents/${document.id}`}
                      data-qa="child-document-link"
                    >
                      {document.templateName}
                    </Link>
                    <ChildDocumentStateChip
                      status={document.decision?.status ?? document.status}
                    />
                  </FixedSpaceRow>
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

const Answered = ({ document }: { document: ChildDocumentCitizenSummary }) => {
  const i18n = useTranslation()

  if (getDocumentCategory(document.type) !== 'external') {
    return null
  }

  if (document.answeredAt === null) {
    return (
      <span data-qa={`answered-${document.id}`}>
        {i18n.children.childDocuments.unanswered}
      </span>
    )
  }

  return (
    <span data-qa={`answered-${document.id}`}>
      <span>
        {i18n.children.childDocuments.answered},{' '}
        {document.answeredAt?.toLocalDate().format()}
      </span>
      {document.answeredBy !== null && (
        <span>
          ,{' '}
          {document.answeredBy.type === 'CITIZEN'
            ? document.answeredBy.name
            : i18n.children.childDocuments.answeredByEmployee}
        </span>
      )}
    </span>
  )
}

const DecisionValidity = ({
  document
}: {
  document: ChildDocumentCitizenSummary
}) => {
  const i18n = useTranslation()

  if (
    getDocumentCategory(document.type) !== 'decision' ||
    document.decision === null ||
    document.decision.validity === null
  ) {
    return null
  }

  return (
    <div data-qa={`decision-validity-${document.id}`}>
      {i18n.children.childDocuments.validityPeriod}{' '}
      {document.decision.validity.format()}
    </div>
  )
}
