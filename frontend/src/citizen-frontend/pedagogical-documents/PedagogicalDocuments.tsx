// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { useApiState } from 'lib-common/utils/useRestApi'
import { useTranslation } from '../localization'
import { defaultMargins, Gap } from 'lib-components/white-space'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { fontWeights, H1 } from 'lib-components/typography'
import { getPedagogicalDocuments, markPedagogicalDocumentRead } from './api'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import LocalDate from 'lib-common/local-date'
import { PedagogicalDocumentCitizen } from 'lib-common/generated/api-types/pedagogicaldocument'
import { getAttachmentBlob } from '../attachments'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { OverlayContext } from '../overlay/state'
import { faArrowDown, faChevronDown, faChevronUp } from 'lib-icons'
import styled from 'styled-components'
import { PedagogicalDocumentsContext, PedagogicalDocumentsState } from './state'
import { tabletMin } from 'lib-components/breakpoints'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { renderResult } from '../async-rendering'

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

  const Attachment = React.memo(function Attachment({
    item,
    dataQa
  }: {
    item: PedagogicalDocumentCitizen
    dataQa: string
  }) {
    return (
      <>
        {item && item.attachment && (
          <FileDownloadButton
            key={item.attachment.id}
            file={item.attachment}
            fileFetchFn={getAttachmentBlob}
            afterFetch={() => onRead(item)}
            onFileUnavailable={onAttachmentUnavailable}
            icon
            data-qa={dataQa}
            openInBrowser={true}
          />
        )}
      </>
    )
  })

  const AttachmentDownloadButton = React.memo(
    function AttachmentDownloadButton({
      item,
      dataQa
    }: {
      item: PedagogicalDocumentCitizen
      dataQa: string
    }) {
      return (
        <>
          {item.attachment && (
            <FileDownloadButton
              key={item.attachment.id}
              file={item.attachment}
              fileFetchFn={getAttachmentBlob}
              afterFetch={() => onRead(item)}
              onFileUnavailable={onAttachmentUnavailable}
              icon={faArrowDown}
              data-qa={dataQa}
              text={t.fileDownload.download}
            />
          )}
        </>
      )
    }
  )

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

    const toggleExpanded = () => {
      setExpanded(!expanded)
    }

    // A description with more than 50 characters per line will be collapsed
    const shouldShowExpandButton =
      item?.description && item.description.length > 50 * clampLines

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
      items
    }: {
      items: PedagogicalDocumentCitizen[]
    }) {
      const moreThanOneChild =
        items.reduce(
          (childIds, doc) =>
            childIds.includes(doc.childId)
              ? childIds
              : [...childIds, doc.childId],
          [] as string[]
        ).length > 1
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
                />
              )}
            </ContentArea>
          </Desktop>
        </>
      )
    }
  )

  const PedagogicalDocumentsList = React.memo(
    function PedagogicalDocumentsList({
      items,
      showChildrenNames
    }: {
      items: PedagogicalDocumentCitizen[]
      showChildrenNames: boolean
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
              <Attachment
                item={item}
                dataQa={`pedagogical-document-list-attachment-${item.id}`}
              />
              <AttachmentDownloadButton
                item={item}
                dataQa={`pedagogical-document-list-attachment-download-${item.id}`}
              />
            </ListItem>
          ))}
        </>
      )
    }
  )

  const PedagogicalDocumentsTable = React.memo(
    function PedagogicalDocumentsTable({
      items,
      showChildrenNames
    }: {
      items: PedagogicalDocumentCitizen[]
      showChildrenNames: boolean
    }) {
      return (
        <Table>
          <Thead>
            <Tr>
              <Th>{t.pedagogicalDocuments.table.date}</Th>
              {showChildrenNames && (
                <Th>{t.pedagogicalDocuments.table.child}</Th>
              )}
              <Th>{t.pedagogicalDocuments.table.description}</Th>
              <Th>{t.pedagogicalDocuments.table.document}</Th>
              <Th />
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
                <NameTd>
                  <Attachment
                    item={item}
                    dataQa={`pedagogical-document-attachment-${item.id}`}
                  />
                </NameTd>
                <ActionsTd>
                  <AttachmentDownloadButton
                    item={item}
                    dataQa={`pedagogical-document-attachment-download-${item.id}`}
                  />
                </ActionsTd>
              </ItemTr>
            ))}
          </Tbody>
        </Table>
      )
    }
  )

  return (
    <>
      <Container>
        <Gap size="s" />
        {renderResult(pedagogicalDocuments, (items) => (
          <PedagogicalDocumentsDisplay items={items} />
        ))}
      </Container>
    </>
  )
})

const ItemTr = styled(Tr)`
  font-weight: ${(props: { documentIsRead: boolean }) =>
    props.documentIsRead ? fontWeights.normal : fontWeights.semibold};

  button {
    font-weight: ${fontWeights.semibold};
  }
`

const DateTd = styled(Td)`
  width: 15%;
`

const NameTd = styled(Td)`
  width: 20%;
`

const DescriptionTd = styled(Td)`
  width: 45%;
  white-space: pre-line;
`

const ActionsTd = styled(Td)`
  width: 20%;
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

const ListItem = styled(FixedSpaceColumn)`
  padding: ${defaultMargins.s};
  ${(props: { documentIsRead: boolean }) =>
    !props.documentIsRead && `padding-left: calc(${defaultMargins.s} - 6px)`};
  border-top: 1px solid #b1b1b1;
  border-left: ${(props: { documentIsRead: boolean }) =>
    props.documentIsRead ? 'none' : '6px solid #249fff'};
  font-weight: ${fontWeights.semibold};

  & > div {
    font-weight: ${(props: { documentIsRead: boolean }) =>
      props.documentIsRead ? fontWeights.normal : fontWeights.semibold};
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
