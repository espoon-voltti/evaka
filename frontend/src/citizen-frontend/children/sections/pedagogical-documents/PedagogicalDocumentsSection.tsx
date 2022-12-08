// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faChevronRight } from 'Icons'
import React, { useCallback, useContext, useState } from 'react'
import styled from 'styled-components'

import { renderResult } from 'citizen-frontend/async-rendering'
import { getAttachmentUrl } from 'citizen-frontend/attachments'
import CollapsibleOrWholePageContainer from 'citizen-frontend/children/ResponsiveWholePageCollapsible'
import { ChildrenContext } from 'citizen-frontend/children/state'
import { useTranslation } from 'citizen-frontend/localization'
import {
  Attachment,
  PedagogicalDocumentCitizen
} from 'lib-common/generated/api-types/pedagogicaldocument'
import { UUID } from 'lib-common/types'
import { useUniqueId } from 'lib-common/utils/useUniqueId'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { tabletMin } from 'lib-components/breakpoints'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  MobileOnly,
  TabletAndDesktop
} from 'lib-components/layout/responsive-layout'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { Dimmed, fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import {
  faArrowDown,
  faChevronDown,
  faChevronLeft,
  faChevronUp
} from 'lib-icons'

import {
  useMarkPedagogicalDocumentReadMutation,
  usePedagogicalDocumentsQueryResult
} from '../../../state/children/childrenApi'

const AttachmentLink = React.memo(function AttachmentLink({
  pedagogicalDocument,
  attachment,
  onRead,
  dataQa
}: {
  pedagogicalDocument: PedagogicalDocumentCitizen
  attachment: Attachment
  onRead: (doc: PedagogicalDocumentCitizen) => void
  dataQa: string
}) {
  const afterOpen = useCallback(
    () => onRead(pedagogicalDocument),
    [onRead, pedagogicalDocument]
  )
  return (
    <FileDownloadButton
      key={attachment.id}
      file={attachment}
      getFileUrl={getAttachmentUrl}
      afterOpen={afterOpen}
      icon
      data-qa={dataQa}
    />
  )
})

const AttachmentDownloadButton = React.memo(function AttachmentDownloadButton({
  pedagogicalDocument,
  attachment,
  onRead,
  dataQa
}: {
  pedagogicalDocument: PedagogicalDocumentCitizen
  attachment: Attachment
  onRead: (doc: PedagogicalDocumentCitizen) => void
  dataQa: string
}) {
  const t = useTranslation()
  const afterOpen = useCallback(
    () => onRead(pedagogicalDocument),
    [onRead, pedagogicalDocument]
  )
  return (
    <FileDownloadButton
      key={attachment.id}
      file={attachment}
      getFileUrl={getAttachmentUrl}
      afterOpen={afterOpen}
      icon={faArrowDown}
      data-qa={dataQa}
      text={t.fileDownload.download}
    />
  )
})

const AttachmentRow = React.memo(function AttachmentRow({
  pedagogicalDocument,
  attachment,
  onRead,
  dataQa
}: {
  pedagogicalDocument: PedagogicalDocumentCitizen
  attachment: Attachment
  onRead: (doc: PedagogicalDocumentCitizen) => void
  dataQa: string
}) {
  return (
    <AttachmentRowContainer data-qa={dataQa}>
      <AttachmentLink
        pedagogicalDocument={pedagogicalDocument}
        attachment={attachment}
        onRead={onRead}
        dataQa={`${dataQa}-attachment`}
      />
      <AttachmentDownloadButton
        pedagogicalDocument={pedagogicalDocument}
        attachment={attachment}
        onRead={onRead}
        dataQa={`${dataQa}-download`}
      />
    </AttachmentRowContainer>
  )
})

export const ResponsiveDescription = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.s};

  @media (max-width: ${tabletMin}) {
    flex-direction: row;
  }
`

const ItemDescription = React.memo(function ItemDescription({
  item,
  clampLines,
  dataQa
}: {
  item: PedagogicalDocumentCitizen
  clampLines: number
  dataQa: string
}) {
  const [expanded, setExpanded] = useState(false)
  const t = useTranslation()

  const toggleExpanded = useCallback(() => {
    setExpanded((prev) => !prev)
  }, [])

  // A description with more than 50 characters per line will be collapsed
  const shouldShowExpandButton = item.description.length > 50 * clampLines

  const ariaId = useUniqueId('expandable-text')

  return (
    <ResponsiveDescription>
      <ExpandableText
        expanded={expanded}
        clampLines={clampLines}
        data-qa={dataQa}
        id={ariaId}
      >
        {item.description}
      </ExpandableText>
      {shouldShowExpandButton && (
        <div>
          <TabletAndDesktop>
            <InlineButton
              onClick={toggleExpanded}
              data-qa={`${dataQa}-button`}
              icon={expanded ? faChevronUp : faChevronDown}
              aria-expanded={expanded}
              aria-controls={ariaId}
              text={
                expanded
                  ? t.children.pedagogicalDocuments.collapseReadMore
                  : t.children.pedagogicalDocuments.readMore
              }
            />
          </TabletAndDesktop>
          <MobileOnly>
            <IconButton
              onClick={toggleExpanded}
              data-qa={`${dataQa}-button`}
              icon={expanded ? faChevronUp : faChevronDown}
              aria-expanded={expanded}
              aria-controls={ariaId}
              aria-label={
                expanded
                  ? t.children.pedagogicalDocuments.collapseReadMore
                  : t.children.pedagogicalDocuments.readMore
              }
            />
          </MobileOnly>
        </div>
      )}
    </ResponsiveDescription>
  )
})

const PedagogicalDocumentsDisplay = React.memo(
  function PedagogicalDocumentsDisplay({
    items,
    onRead
  }: {
    items: PedagogicalDocumentCitizen[]
    onRead: (doc: PedagogicalDocumentCitizen) => void
  }) {
    const t = useTranslation()
    return items.length === 0 ? (
      <NoDocumentsContainer>
        <Dimmed>{t.children.pedagogicalDocuments.noDocuments}</Dimmed>
      </NoDocumentsContainer>
    ) : (
      <>
        <MobileOnly>
          <PedagogicalDocumentsList items={items} onRead={onRead} />
        </MobileOnly>
        <TabletAndDesktop>
          <PedagogicalDocumentsTable items={items} onRead={onRead} />
        </TabletAndDesktop>
      </>
    )
  }
)

const PedagogicalDocumentsList = React.memo(function PedagogicalDocumentsList({
  items,
  onRead
}: {
  items: PedagogicalDocumentCitizen[]
  onRead: (doc: PedagogicalDocumentCitizen) => void
}) {
  return (
    <>
      {items.map((item) => (
        <ListItem key={item.id} documentIsRead={item.isRead} spacing="xs">
          <ListItemHead>
            <span>{item.created.toLocalDate().format()}</span>
          </ListItemHead>
          <ItemDescription
            item={item}
            clampLines={1}
            dataQa={`pedagogical-document-list-description-${item.id}`}
          />
          <AttachmentsContainer>
            {item.attachments.map((attachment) => (
              <AttachmentRow
                key={attachment.id}
                pedagogicalDocument={item}
                attachment={attachment}
                onRead={onRead}
                dataQa={`list-attachment-${attachment.id}`}
              />
            ))}
          </AttachmentsContainer>
        </ListItem>
      ))}
    </>
  )
})

const PAGE_SIZE = 10

const Pagination = React.memo(function Pagination({
  page,
  total,
  setPage
}: {
  page: number
  total: number
  setPage: (page: number) => void
}) {
  const t = useTranslation()

  return (
    <FixedSpaceRow alignItems="center" justifyContent="flex-end">
      <IconButton
        icon={faChevronLeft}
        aria-label={t.children.pedagogicalDocuments.previousPage}
        disabled={page === 0}
        onClick={() => setPage(page - 1)}
      />
      <div>{t.children.pedagogicalDocuments.pageCount(page + 1, total)}</div>
      <IconButton
        icon={faChevronRight}
        aria-label={t.children.pedagogicalDocuments.nextPage}
        disabled={page + 1 >= total}
        onClick={() => setPage(page + 1)}
      />
    </FixedSpaceRow>
  )
})

const PedagogicalDocumentsTable = React.memo(
  function PedagogicalDocumentsTable({
    items,
    onRead
  }: {
    items: PedagogicalDocumentCitizen[]
    onRead: (doc: PedagogicalDocumentCitizen) => void
  }) {
    const t = useTranslation()

    const [page, setPage] = useState(0)

    return (
      <div>
        <Pagination
          total={Math.ceil(items.length / PAGE_SIZE)}
          page={page}
          setPage={setPage}
        />
        <Gap size="s" />
        <Table>
          <Thead>
            <Tr>
              <Th>{t.children.pedagogicalDocuments.table.date}</Th>
              <Th>{t.children.pedagogicalDocuments.table.description}</Th>
              <Th>{t.children.pedagogicalDocuments.table.document}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {items
              .slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)
              .map((item) => (
                <ItemTr key={item.id} documentIsRead={item.isRead}>
                  <DateTd data-qa={`pedagogical-document-date-${item.id}`}>
                    {!item.isRead && <UnreadIndicator />}
                    {item.created.toLocalDate().format()}
                  </DateTd>
                  <DescriptionTd>
                    <ItemDescription
                      item={item}
                      clampLines={3}
                      dataQa={`pedagogical-document-description-${item.id}`}
                    />
                  </DescriptionTd>
                  <AttachmentsTd>
                    <AttachmentsContainer>
                      {item.attachments.map((attachment) => (
                        <AttachmentRow
                          key={attachment.id}
                          pedagogicalDocument={item}
                          attachment={attachment}
                          onRead={onRead}
                          dataQa={`attachment-${attachment.id}`}
                        />
                      ))}
                    </AttachmentsContainer>
                  </AttachmentsTd>
                </ItemTr>
              ))}
          </Tbody>
        </Table>
        <Gap size="s" />
        <Pagination
          total={Math.ceil(items.length / PAGE_SIZE)}
          page={page}
          setPage={setPage}
        />
      </div>
    )
  }
)

export default React.memo(function PedagogicalDocumentsSection({
  childId
}: {
  childId: UUID
}) {
  const [open, setOpen] = useState(false)

  const pedagogicalDocuments = usePedagogicalDocumentsQueryResult(childId)
  const [markPedagogicalDocumentRead] = useMarkPedagogicalDocumentReadMutation()

  const { unreadPedagogicalDocumentsCount } = useContext(ChildrenContext)

  const onRead = useCallback(
    (doc: PedagogicalDocumentCitizen) => {
      void markPedagogicalDocumentRead(doc.id)
    },
    [markPedagogicalDocumentRead]
  )

  const t = useTranslation()

  return (
    <CollapsibleOrWholePageContainer
      title={t.children.pedagogicalDocuments.title}
      opaque
      open={open}
      toggleOpen={() => setOpen(!open)}
      data-qa="collapsible-pedagogical-documents"
      countIndicator={unreadPedagogicalDocumentsCount?.[childId]}
      contentPadding="zero"
    >
      {renderResult(pedagogicalDocuments, (items) => (
        <PedagogicalDocumentsDisplay items={items} onRead={onRead} />
      ))}
    </CollapsibleOrWholePageContainer>
  )
})

const ItemTr = styled(Tr)<{ documentIsRead: boolean }>`
  font-weight: ${(p) =>
    p.documentIsRead ? fontWeights.normal : fontWeights.semibold};
  position: relative;

  button {
    font-weight: ${fontWeights.semibold};
  }
`

const UnreadIndicator = styled.div`
  background-color: ${(p) => p.theme.colors.status.success};
  width: 6px;
  position: absolute;
  left: 0.5px;
  top: 0.5px;
  bottom: 0.5px;
`

const DateTd = styled(Td)`
  width: 15%;
`

const DescriptionTd = styled(Td)`
  width: 35%;
  white-space: pre-line;
`

const AttachmentsTd = styled(Td)`
  width: 50%;
`

const AttachmentsContainer = styled.div`
  display: flex;
  flex-direction: column;
`

const AttachmentRowContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  padding-bottom: 8px;
`

const ListItem = styled(FixedSpaceColumn)<{ documentIsRead: boolean }>`
  padding: ${defaultMargins.s};
  ${(p) =>
    !p.documentIsRead && `padding-left: calc(${defaultMargins.s} - 6px)`};
  border-top: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  border-left: ${(p) =>
    p.documentIsRead ? 'none' : `6px solid ${p.theme.colors.status.success}`};
  font-weight: ${fontWeights.semibold};

  & > div {
    font-weight: ${(p) =>
      p.documentIsRead ? fontWeights.normal : fontWeights.semibold};
  }
`

const ListItemHead = styled.div`
  display: flex;

  & > :first-child {
    flex: 1;
  }
`

type ExpandableTextProps = {
  expanded: boolean
  clampLines: number
}
const ExpandableText = styled.span<ExpandableTextProps>`
  flex: 1;
  white-space: pre-line;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: ${(props) =>
    props.expanded ? 'none' : props.clampLines};
`

const NoDocumentsContainer = styled.div`
  padding: ${defaultMargins.s};
  padding-bottom: 0;
`
