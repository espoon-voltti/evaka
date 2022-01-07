// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faQuestion } from 'lib-icons'
import * as _ from 'lodash'
import React, { useContext, useEffect, useState } from 'react'
import { PedagogicalDocument } from 'lib-common/generated/api-types/pedagogicaldocument'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2 } from 'lib-components/typography'
import {
  createPedagogicalDocument,
  deletePedagogicalDocument,
  getChildPedagogicalDocuments
} from '../../api/child/pedagogical-documents'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'
import PedagogicalDocumentRow from './PedagogicalDocumentRow'

interface Props {
  id: UUID
  startOpen: boolean
}

export default React.memo(function PedagogicalDocuments({
  id,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions, placements } = useContext(ChildContext)
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)

  const [pedagogicalDocuments, loadData] = useApiState(
    () => getChildPedagogicalDocuments(id),
    [id]
  )

  const [open, setOpen] = useState(startOpen)
  const [tobeDeleted, setTobeDeleted] = useState<
    PedagogicalDocument | undefined
  >(undefined)

  useEffect(() => {
    if (open) loadData()
  }, [open, loadData])

  const handleDelete = (document: PedagogicalDocument) => {
    setTobeDeleted(document)
    toggleUiMode('delete-pedagogical-document')
  }

  const confirmDelete = async () => {
    await deleteDocument()
    clearUiMode()
    setTobeDeleted(undefined)
  }

  const deleteDocument = async () => {
    if (!tobeDeleted) return
    return deletePedagogicalDocument(tobeDeleted.id).then(loadData)
  }

  const createNewDocument = () => {
    const emptyDocument = { childId: id, description: '', attachmentId: null }
    void createPedagogicalDocument(emptyDocument)
      .then(
        (result) =>
          result.isSuccess &&
          toggleUiMode(`edit-pedagogical-document-${result.value.id}`)
      )
      .then(loadData)
  }

  const DeletePedagogicalDocumentModal = () => (
    <InfoModal
      iconColour={'orange'}
      title={i18n.childInformation.pedagogicalDocument.removeConfirmation}
      text={i18n.childInformation.pedagogicalDocument.removeConfirmationText}
      icon={faQuestion}
      reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
      resolve={{ action: confirmDelete, label: i18n.common.remove }}
    />
  )

  if (
    !permittedActions.has('READ_PEDAGOGICAL_DOCUMENTS') ||
    placements
      .map(
        (ps) =>
          !ps.some((placement) =>
            placement.daycare.enabledPilotFeatures.includes('VASU_AND_PEDADOC')
          )
      )
      .getOrElse(true)
  ) {
    return null
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
      {permittedActions.has('CREATE_PEDAGOGICAL_DOCUMENT') && (
        <AddButtonRow
          text={i18n.childInformation.pedagogicalDocument.create}
          onClick={() => createNewDocument()}
          data-qa="button-create-pedagogical-document"
          disabled={pedagogicalDocuments.isLoading}
        />
      )}

      {renderResult(pedagogicalDocuments, (pedagogicalDocuments) => (
        <Table data-qa="table-of-pedagogical-documents">
          <Thead>
            <Tr>
              <Th>{i18n.childInformation.pedagogicalDocument.date}</Th>
              <Th>{i18n.childInformation.pedagogicalDocument.description}</Th>
              <Th>{i18n.childInformation.pedagogicalDocument.document}</Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {_.orderBy(pedagogicalDocuments, ['created'], ['desc']).map(
              (pedagogicalDocument: PedagogicalDocument) => (
                <PedagogicalDocumentRow
                  key={pedagogicalDocument.id}
                  id={pedagogicalDocument.id}
                  childId={pedagogicalDocument.childId}
                  attachments={pedagogicalDocument.attachments}
                  description={pedagogicalDocument.description}
                  created={pedagogicalDocument.created}
                  updated={pedagogicalDocument.updated}
                  initInEditMode={
                    uiMode ==
                    `edit-pedagogical-document-${pedagogicalDocument.id}`
                  }
                  onReload={loadData}
                  onDelete={handleDelete}
                />
              )
            )}
          </Tbody>
        </Table>
      ))}
      {uiMode === 'delete-pedagogical-document' && (
        <DeletePedagogicalDocumentModal />
      )}
    </CollapsibleContentArea>
  )
})
