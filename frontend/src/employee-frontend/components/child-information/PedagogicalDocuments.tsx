// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { useTranslation } from '../../state/i18n'
import { ChildContext } from '../../state'
import { UUID } from '../../types'
import { UIContext } from '../../state/ui'
import { Loading, Success } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import * as _ from 'lodash'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { DateTd, NameTd } from '../PersonProfile'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { RequireRole } from '../../utils/roles'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import {
  createPedagogicalDocument,
  getChildPedagogicalDocuments
} from '../../api/child/pedagogical-documents'
import { PedagogicalDocument } from 'lib-common/generated/api-types/pedadocument'
import FileUpload from 'lib-components/molecules/FileUpload'
import { getAttachmentBlob } from '../../api/attachments'

interface Props {
  id: UUID
  startOpen: boolean
}

const PedagogicalDocuments = React.memo(function PedagogicalDocuments({
  id,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { pedagogicalDocuments, setPedagogicalDocuments } =
    useContext(ChildContext)
  const { toggleUiMode } = useContext(UIContext)

  const [open, setOpen] = useState(startOpen)

  const loadData = () => {
    setPedagogicalDocuments(Loading.of())
    void getChildPedagogicalDocuments(id).then(setPedagogicalDocuments)
  }

  useEffect(loadData, [id, setPedagogicalDocuments])

  const createNewDocument = () => {
    const emptyDocument = { childId: id, description: '', attachmentId: null }
    void createPedagogicalDocument(emptyDocument).then(loadData)
    toggleUiMode('create-new-pedagogical-document')
  }

  const handleAttachmentUpload = useCallback(
    async (
      file: File,
      onUploadProgress: (progressEvent: ProgressEvent) => void
    ) => Success.of('0901f5d6-ae89-49de-88bc-0706b2bad207'),
    []
  )

  const handleAttachmentDelete = useCallback(
    async (id: UUID) => Success.of(),
    []
  )

  function renderPedagigicalDocuments() {
    if (pedagogicalDocuments.isLoading) {
      return <Loader />
    } else if (pedagogicalDocuments.isFailure) {
      return <div>{i18n.common.loadingFailed}</div>
    } else
      return _.orderBy(pedagogicalDocuments.value, ['created'], ['desc']).map(
        (pedagogicalDocument: PedagogicalDocument) => {
          return (
            <Tr
              key={`${pedagogicalDocument.id}`}
              data-qa="table-pedagogical-document-row"
            >
              <DateTd data-qa="pedagogical-document-start-date">
                {pedagogicalDocument.created.toLocaleDateString()}
              </DateTd>
              <NameTd data-qa="pedagogical-document-document">
                {
                  <FileUpload
                    slimSingleFile
                    disabled={false}
                    data-qa="upload-message-attachment"
                    files={[]}
                    i18n={i18n.fileUpload}
                    onDownloadFile={getAttachmentBlob}
                    onUpload={handleAttachmentUpload}
                    onDelete={handleAttachmentDelete}
                  />
                }
              </NameTd>
              <Td data-qa="pedagogical-document-description">
                {pedagogicalDocument.description}
              </Td>
              <Td data-qa="pedagogical-document-actions">{'todo actionit'}</Td>
            </Tr>
          )
        }
      )
  }

  return (
    <CollapsibleContentArea
      title={
        <H2 noMargin>{i18n.childInformation.pedagogicalDocument.title}</H2>
      }
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="pedagogical-documents-collapsible"
    >
      <RequireRole oneOf={['SERVICE_WORKER', 'ADMIN']}>
        <AddButtonRow
          text={i18n.childInformation.pedagogicalDocument.create}
          onClick={() => createNewDocument()} // todo
          data-qa="button-create-pedagogical-document"
        />
      </RequireRole>

      {pedagogicalDocuments.isLoading && <Loader />}
      {pedagogicalDocuments.isFailure && <div>{i18n.common.loadingFailed}</div>}
      {pedagogicalDocuments.isSuccess && (
        <Table data-qa="table-of-pedagogical-documents">
          <Thead>
            <Tr>
              <Th>{i18n.childInformation.pedagogicalDocument.date}</Th>
              <Th>{i18n.childInformation.pedagogicalDocument.document}</Th>
              <Th>{i18n.childInformation.pedagogicalDocument.description}</Th>
              <Th></Th>
            </Tr>
          </Thead>
          <Tbody>{renderPedagigicalDocuments()}</Tbody>
        </Table>
      )}
    </CollapsibleContentArea>
  )
})

export default PedagogicalDocuments
