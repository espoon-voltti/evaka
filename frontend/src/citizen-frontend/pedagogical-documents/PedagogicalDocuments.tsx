// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { useTranslation } from '../localization'
import { Gap } from 'lib-components/white-space'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
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
            <Th>{t.pedagogicalDocuments.table.document}</Th>
            <Th>{t.pedagogicalDocuments.table.description}</Th>
            <Th />
          </Tr>
        </Thead>
        <Tbody>
          {items.map((item) => (
            <Tr key={item.id}>
              <DateTd
                data-qa={`pedagogical-document-date-${item.id}`}
                documentIsRead={item.isRead}
              >
                {LocalDate.fromSystemTzDate(item.created).format()}
              </DateTd>
              <NameTd>
                {item.attachment && (
                  <FileDownloadButton
                    key={item.attachment.id}
                    file={item.attachment}
                    fileFetchFn={getAttachmentBlob}
                    afterFetch={() => onRead(item)}
                    onFileUnavailable={onAttachmentUnavailable}
                    icon
                    data-qa={`pedagogical-document-attachment-${item.id}`}
                    openInBrowser={true}
                  />
                )}
              </NameTd>
              <DescriptionTd
                documentIsRead={item.isRead}
                data-qa={`pedagogical-document-description-${item.id}`}
              >
                {item.description}
              </DescriptionTd>
              <ActionsTd>
                {item.attachment && (
                  <FileDownloadButton
                    key={item.attachment.id}
                    file={item.attachment}
                    fileFetchFn={getAttachmentBlob}
                    afterFetch={() => markPedagogicalDocumentRead(item.id)}
                    onFileUnavailable={onAttachmentUnavailable}
                    icon={faArrowDown}
                    data-qa="pedagogical-document-attachment-download"
                    text={t.fileDownload.download}
                  />
                )}
              </ActionsTd>
            </Tr>
          ))}
        </Tbody>
      </Table>
    )
  }

  return (
    <>
      <Container>
        <Gap size="s" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.pedagogicalDocuments.title}</H1>
          <p>{t.pedagogicalDocuments.description}</p>
          {pedagogicalDocuments.mapAll({
            loading() {
              return <SpinnerSegment />
            },
            failure() {
              return <ErrorSegment />
            },
            success(items) {
              return (
                items.length > 0 && <PedagogicalDocumentsTable items={items} />
              )
            }
          })}
        </ContentArea>
      </Container>
    </>
  )
}

const DateTd = styled(Td)`
  width: 15%;
  font-weight: ${(props: { documentIsRead: boolean }) =>
    props.documentIsRead ? 400 : 600};
`

const NameTd = styled(Td)`
  width: 20%;
`

const DescriptionTd = styled(Td)`
  width: 45%;
  font-weight: ${(props: { documentIsRead: boolean }) =>
    props.documentIsRead ? 400 : 600};
`

const ActionsTd = styled(Td)`
  width: 20%;
`
