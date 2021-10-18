// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { useTranslation } from '../localization'
import { defaultMargins, Gap } from 'lib-components/white-space'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { fontWeights, H1 } from 'lib-components/typography'
import { Loading, Result } from 'lib-common/api'
import { getPedagogicalDocuments, markPedagogicalDocumentRead } from './api'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import LocalDate from 'lib-common/local-date'
import { PedagogicalDocumentCitizen } from 'lib-common/generated/api-types/pedagogicaldocument'
import { getAttachmentBlob } from '../attachments'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { OverlayContext } from '../overlay/state'
import { faArrowDown } from 'lib-icons'
import styled from 'styled-components'
import { PedagogicalDocumentsContext, PedagogicalDocumentsState } from './state'
import { tabletMin } from 'lib-components/breakpoints'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { faChevronDown, faChevronUp } from 'lib-icons'
import IconButton from 'lib-components/atoms/buttons/IconButton'

export default function PedagogicalDocuments() {
  const t = useTranslation()
  const [pedagogicalDocuments, setPedagogicalDocuments] = useState<
    Result<PedagogicalDocumentCitizen[]>
  >(Loading.of())

  const { refreshUnreadPedagogicalDocumentsCount } =
    useContext<PedagogicalDocumentsState>(PedagogicalDocumentsContext)

  useEffect(refreshUnreadPedagogicalDocumentsCount, [
    refreshUnreadPedagogicalDocumentsCount,
    pedagogicalDocuments
  ])

  const loadData = useRestApi(getPedagogicalDocuments, setPedagogicalDocuments)
  useEffect(loadData, [loadData])

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

  const Attachment = ({
    item,
    dataQa
  }: {
    item: PedagogicalDocumentCitizen
    dataQa: string
  }) => {
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
  }

  const AttachmentDownloadButton = ({
    item,
    dataQa
  }: {
    item: PedagogicalDocumentCitizen
    dataQa: string
  }) => {
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

  const ItemDescription = ({
    item,
    clampLines,
    dataQa
  }: {
    item: PedagogicalDocumentCitizen
    clampLines: number
    dataQa: string
  }) => {
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
          <ExpandButton
            onClick={toggleExpanded}
            data-qa={`${dataQa}-button`}
            icon={expanded ? faChevronUp : faChevronDown}
          />
        )}
      </FixedSpaceRow>
    )
  }

  const PedagogicalDocumentsDisplay = ({
    items
  }: {
    items: PedagogicalDocumentCitizen[]
  }) => {
    return (
      <>
        <Mobile>
          <ContentArea opaque paddingVertical="L" paddingHorizontal="zero">
            <PaddedDiv>
              <H1 noMargin>{t.pedagogicalDocuments.title}</H1>
              <p>{t.pedagogicalDocuments.description}</p>
            </PaddedDiv>
            <PedagogicalDocumentsList items={items} />
          </ContentArea>
        </Mobile>
        <Desktop>
          <ContentArea opaque paddingVertical="L">
            <H1 noMargin>{t.pedagogicalDocuments.title}</H1>
            <p>{t.pedagogicalDocuments.description}</p>
            <PedagogicalDocumentsTable items={items} />
          </ContentArea>
        </Desktop>
      </>
    )
  }

  const PedagogicalDocumentsList = ({
    items
  }: {
    items: PedagogicalDocumentCitizen[]
  }) => {
    return (
      <>
        {items.map((item) => (
          <ListItem key={item.id} documentIsRead={item.isRead} spacing="xs">
            <div>{LocalDate.fromSystemTzDate(item.created).format()}</div>
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

  const PedagogicalDocumentsTable = ({
    items
  }: {
    items: PedagogicalDocumentCitizen[]
  }) => {
    return (
      <Table>
        <Thead>
          <Tr>
            <Th>{t.pedagogicalDocuments.table.date}</Th>
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

  return (
    <>
      <Container>
        <Gap size="s" />
        {pedagogicalDocuments.mapAll({
          loading() {
            return <SpinnerSegment />
          },
          failure() {
            return <ErrorSegment />
          },
          success(items) {
            return (
              items.length > 0 && <PedagogicalDocumentsDisplay items={items} />
            )
          }
        })}
      </Container>
    </>
  )
}

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

const ExpandButton = styled(IconButton)`
  &:focus {
    border: none;
  }
`
