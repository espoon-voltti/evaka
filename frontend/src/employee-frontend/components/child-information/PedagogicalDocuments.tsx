// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { PedagogicalDocument } from 'lib-common/generated/api-types/pedagogicaldocument'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  InfoButton,
  ExpandingInfoBox
} from 'lib-components/molecules/ExpandingInfo'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2, P } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faQuestion } from 'lib-icons'

import {
  createPedagogicalDocument,
  deletePedagogicalDocument,
  getChildPedagogicalDocuments
} from '../../generated/api-clients/pedagogicaldocument'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

import PedagogicalDocumentRow from './PedagogicalDocumentRow'

const createPedagogicalDocumentResult = wrapResult(createPedagogicalDocument)
const getChildPedagogicalDocumentsResult = wrapResult(
  getChildPedagogicalDocuments
)
const deletePedagogicalDocumentResult = wrapResult(deletePedagogicalDocument)

interface Props {
  childId: UUID
  startOpen: boolean
}

export default React.memo(function PedagogicalDocuments({
  childId,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions, placements } = useContext(ChildContext)
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)

  const [pedagogicalDocuments, loadData] = useApiState(
    () => getChildPedagogicalDocumentsResult({ childId }),
    [childId]
  )

  const [open, setOpen] = useState(startOpen)
  const [tobeDeleted, setTobeDeleted] = useState<
    PedagogicalDocument | undefined
  >(undefined)

  useEffect(() => {
    if (open) {
      void loadData()
    }
  }, [open, loadData])

  const [expandingInfo, setExpandingInfo] = useState<string>()
  const toggleExpandingInfo = useCallback(
    (info: string) =>
      setExpandingInfo(expandingInfo === info ? undefined : info),
    [expandingInfo]
  )
  const closeExpandingInfo = useCallback(() => setExpandingInfo(undefined), [])

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
    return deletePedagogicalDocumentResult({ documentId: tobeDeleted.id }).then(
      loadData
    )
  }

  const createNewDocument = () => {
    const emptyDocument = { childId, description: '', attachmentId: null }
    void createPedagogicalDocumentResult({ body: emptyDocument })
      .then(
        (result) =>
          result.isSuccess &&
          toggleUiMode(`edit-pedagogical-document-${result.value.id}`)
      )
      .then(loadData)
  }

  const DeletePedagogicalDocumentModal = () => (
    <InfoModal
      type="warning"
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
          !ps.placements.some((placement) =>
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

      {!!i18n.childInformation.pedagogicalDocument.explanation && (
        <P noMargin>
          {i18n.childInformation.pedagogicalDocument.explanation}
          <InfoButtonWithMargin
            onClick={() =>
              toggleExpandingInfo(
                i18n.childInformation.pedagogicalDocument.explanationInfo
              )
            }
            aria-label={i18n.common.openExpandingInfo}
          />
        </P>
      )}
      {!!expandingInfo && (
        <ExpandingInfoBox info={expandingInfo} close={closeExpandingInfo} />
      )}

      {renderResult(pedagogicalDocuments, (pedagogicalDocuments) => (
        <Table data-qa="table-of-pedagogical-documents">
          <Thead>
            <Tr>
              <Th>{i18n.childInformation.pedagogicalDocument.date}</Th>
              <Th>
                {i18n.childInformation.pedagogicalDocument.description}
                {!!i18n.childInformation.pedagogicalDocument
                  .descriptionInfo && (
                  <InfoButtonWithMargin
                    onClick={() =>
                      toggleExpandingInfo(
                        i18n.childInformation.pedagogicalDocument
                          .descriptionInfo
                      )
                    }
                    aria-label={i18n.common.openExpandingInfo}
                  />
                )}
              </Th>
              <Th>
                {i18n.childInformation.pedagogicalDocument.document}
                {!!i18n.childInformation.pedagogicalDocument.documentInfo && (
                  <InfoButtonWithMargin
                    onClick={() =>
                      toggleExpandingInfo(
                        i18n.childInformation.pedagogicalDocument.documentInfo
                      )
                    }
                    aria-label={i18n.common.openExpandingInfo}
                  />
                )}
              </Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {orderBy(pedagogicalDocuments, ['created'], ['desc']).map(
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

const InfoButtonWithMargin = styled(InfoButton)`
  margin-left: ${defaultMargins.xs};
`
