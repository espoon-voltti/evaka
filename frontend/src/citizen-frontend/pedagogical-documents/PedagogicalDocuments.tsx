// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { uniq } from 'lodash'
import React, {
  useCallback,
  useContext,
  useEffect,
  useState,
  useMemo
} from 'react'
import styled from 'styled-components'

import {
  Attachment,
  PedagogicalDocumentCitizen
} from 'lib-common/generated/api-types/pedagogicaldocument'
import LocalDate from 'lib-common/local-date'
import { useApiState } from 'lib-common/utils/useRestApi'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { tabletMin } from 'lib-components/breakpoints'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { fontWeights, H1 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faArrowDown, faChevronDown, faChevronUp } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { getAttachmentBlob } from '../attachments'
import { useTranslation } from '../localization'
import { OverlayContext } from '../overlay/state'

import { getPedagogicalDocuments, markPedagogicalDocumentRead } from './api'
import { PedagogicalDocumentsContext, PedagogicalDocumentsState } from './state'

const AttachmentLink = React.memo(function AttachmentLink({
  pedagogicalDocument,
  attachment,
  onRead,
  onAttachmentUnavailable,
  dataQa
}: {
  pedagogicalDocument: PedagogicalDocumentCitizen
  attachment: Attachment
  onRead: (doc: PedagogicalDocumentCitizen) => void
  onAttachmentUnavailable: () => void
  dataQa: string
}) {
  return (
    <FileDownloadButton
      key={attachment.id}
      file={attachment}
      fileFetchFn={getAttachmentBlob}
      afterFetch={() => onRead(pedagogicalDocument)}
      onFileUnavailable={onAttachmentUnavailable}
      icon
      data-qa={dataQa}
      openInBrowser={true}
    />
  )
})

const AttachmentDownloadButton = React.memo(function AttachmentDownloadButton({
  pedagogicalDocument,
  attachment,
  onRead,
  onAttachmentUnavailable,
  dataQa
}: {
  pedagogicalDocument: PedagogicalDocumentCitizen
  attachment: Attachment
  onRead: (doc: PedagogicalDocumentCitizen) => void
  onAttachmentUnavailable: () => void
  dataQa: string
}) {
  const t = useTranslation()
  return (
    <FileDownloadButton
      key={attachment.id}
      file={attachment}
      fileFetchFn={getAttachmentBlob}
      afterFetch={() => onRead(pedagogicalDocument)}
      onFileUnavailable={onAttachmentUnavailable}
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
  onAttachmentUnavailable,
  dataQa
}: {
  pedagogicalDocument: PedagogicalDocumentCitizen
  attachment: Attachment
  onRead: (doc: PedagogicalDocumentCitizen) => void
  onAttachmentUnavailable: () => void
  dataQa: string
}) {
  return (
    <AttachmentRowContainer data-qa={dataQa}>
      <AttachmentLink
        pedagogicalDocument={pedagogicalDocument}
        attachment={attachment}
        onRead={onRead}
        onAttachmentUnavailable={onAttachmentUnavailable}
        dataQa={`${dataQa}-attachment`}
      />
      <AttachmentDownloadButton
        pedagogicalDocument={pedagogicalDocument}
        attachment={attachment}
        onRead={onRead}
        onAttachmentUnavailable={onAttachmentUnavailable}
        dataQa={`${dataQa}-download`}
      />
    </AttachmentRowContainer>
  )
})

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

  return (
    <FixedSpaceRow spacing="xs" alignItems="end">
      <ExpandableText
        expanded={expanded}
        clampLines={clampLines}
        data-qa={dataQa}
      >
        {item.description}
      </ExpandableText>
      {shouldShowExpandButton && (
        <IconButton
          onClick={toggleExpanded}
          data-qa={`${dataQa}-button`}
          icon={expanded ? faChevronUp : faChevronDown}
          altText={t.pedagogicalDocuments.toggleExpandText}
        />
      )}
    </FixedSpaceRow>
  )
})

const PedagogicalDocumentsDisplay = React.memo(
  function PedagogicalDocumentsDisplay({
    items,
    onRead,
    onAttachmentUnavailable
  }: {
    items: PedagogicalDocumentCitizen[]
    onRead: (doc: PedagogicalDocumentCitizen) => void
    onAttachmentUnavailable: () => void
  }) {
    const t = useTranslation()

    const moreThanOneChild = useMemo(
      () => uniq(items.map((doc) => doc.childId)).length > 1,
      [items]
    )

    return (
      <>
        <Mobile>
          <ContentArea opaque paddingVertical="L" paddingHorizontal="zero">
            <PaddedDiv>
              <H1 noMargin>{t.pedagogicalDocuments.title}</H1>
              <p>{t.pedagogicalDocuments.description}</p>
            </PaddedDiv>
            {items.length > 0 && (
              <PedagogicalDocumentsList
                items={items}
                showChildrenNames={moreThanOneChild}
                onRead={onRead}
                onAttachmentUnavailable={onAttachmentUnavailable}
              />
            )}
          </ContentArea>
        </Mobile>
        <Desktop>
          <ContentArea opaque paddingVertical="L">
            <H1 noMargin>{t.pedagogicalDocuments.title}</H1>
            <p>{t.pedagogicalDocuments.description}</p>
            {items.length > 0 && (
              <PedagogicalDocumentsTable
                items={items}
                showChildrenNames={moreThanOneChild}
                onRead={onRead}
                onAttachmentUnavailable={onAttachmentUnavailable}
              />
            )}
          </ContentArea>
        </Desktop>
      </>
    )
  }
)

const PedagogicalDocumentsList = React.memo(function PedagogicalDocumentsList({
  items,
  showChildrenNames,
  onRead,
  onAttachmentUnavailable
}: {
  items: PedagogicalDocumentCitizen[]
  showChildrenNames: boolean
  onRead: (doc: PedagogicalDocumentCitizen) => void
  onAttachmentUnavailable: () => void
}) {
  return (
    <>
      {items.map((item) => (
        <ListItem key={item.id} documentIsRead={item.isRead} spacing="xs">
          <ListItemHead>
            <span>{LocalDate.fromSystemTzDate(item.created).format()}</span>
            {showChildrenNames && (
              <span>{item.childPreferredName || item.childFirstName}</span>
            )}
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
                onAttachmentUnavailable={onAttachmentUnavailable}
                dataQa={`list-attachment-${attachment.id}`}
              />
            ))}
          </AttachmentsContainer>
        </ListItem>
      ))}
    </>
  )
})

const PedagogicalDocumentsTable = React.memo(
  function PedagogicalDocumentsTable({
    items,
    showChildrenNames,
    onRead,
    onAttachmentUnavailable
  }: {
    items: PedagogicalDocumentCitizen[]
    showChildrenNames: boolean
    onRead: (doc: PedagogicalDocumentCitizen) => void
    onAttachmentUnavailable: () => void
  }) {
    const t = useTranslation()
    return (
      <Table>
        <Thead>
          <Tr>
            <Th>{t.pedagogicalDocuments.table.date}</Th>
            {showChildrenNames && <Th>{t.pedagogicalDocuments.table.child}</Th>}
            <Th>{t.pedagogicalDocuments.table.description}</Th>
            <Th>{t.pedagogicalDocuments.table.document}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {items.map((item) => (
            <ItemTr key={item.id} documentIsRead={item.isRead}>
              <DateTd data-qa={`pedagogical-document-date-${item.id}`}>
                {LocalDate.fromSystemTzDate(item.created).format()}
              </DateTd>
              {showChildrenNames && (
                <Td data-qa={`pedagogical-document-child-name-${item.id}`}>
                  <div>{item.childPreferredName || item.childFirstName}</div>
                </Td>
              )}
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
                      onAttachmentUnavailable={onAttachmentUnavailable}
                      dataQa={`attachment-${attachment.id}`}
                    />
                  ))}
                </AttachmentsContainer>
              </AttachmentsTd>
            </ItemTr>
          ))}
        </Tbody>
      </Table>
    )
  }
)

export default React.memo(function PedagogicalDocuments() {
  const t = useTranslation()
  const [pedagogicalDocuments, loadData] = useApiState(
    getPedagogicalDocuments,
    []
  )

  const { refreshUnreadPedagogicalDocumentsCount } =
    useContext<PedagogicalDocumentsState>(PedagogicalDocumentsContext)

  useEffect(refreshUnreadPedagogicalDocumentsCount, [
    refreshUnreadPedagogicalDocumentsCount,
    pedagogicalDocuments
  ])

  const { setErrorMessage } = useContext(OverlayContext)

  const onAttachmentUnavailable = useCallback(
    () =>
      setErrorMessage({
        title: t.fileDownload.modalHeader,
        text: t.fileDownload.modalMessage,
        type: 'error'
      }),
    [t, setErrorMessage]
  )

  const onRead = (doc: PedagogicalDocumentCitizen) => {
    void markPedagogicalDocumentRead(doc.id).then(loadData)
  }

  return (
    <>
      <Container>
        <Gap size="s" />
        {renderResult(pedagogicalDocuments, (items) => (
          <PedagogicalDocumentsDisplay
            items={items}
            onRead={onRead}
            onAttachmentUnavailable={onAttachmentUnavailable}
          />
        ))}
      </Container>
    </>
  )
})

const ItemTr = styled(Tr)<{ documentIsRead: boolean }>`
  font-weight: ${(p) =>
    p.documentIsRead ? fontWeights.normal : fontWeights.semibold};

  button {
    font-weight: ${fontWeights.semibold};
  }
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

const Mobile = styled.div`
  display: none;

  @media (max-width: ${tabletMin}) {
    display: block;
  }
`

const Desktop = styled.div`
  display: none;

  @media (min-width: ${tabletMin}) {
    display: block;
  }
`

const PaddedDiv = styled.div`
  padding: 0 ${defaultMargins.s};
`

const ListItem = styled(FixedSpaceColumn)<{ documentIsRead: boolean }>`
  padding: ${defaultMargins.s};
  ${(p) =>
    !p.documentIsRead && `padding-left: calc(${defaultMargins.s} - 6px)`};
  border-top: 1px solid #b1b1b1;
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
