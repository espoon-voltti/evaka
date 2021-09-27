// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useTranslation } from '../../state/i18n'
import { ChildContext } from '../../state'
import { UUID } from '../../types'
import { Loading } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import * as _ from 'lodash'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { RequireRole } from '../../utils/roles'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import {
  createPedagogicalDocument,
  getChildPedagogicalDocuments
} from '../../api/child/pedagogical-documents'
import { PedagogicalDocument } from 'lib-common/generated/api-types/pedadocument'
import PedagogicalDocumentRow from './PedagogicalDocumentRow'
import { UIContext } from '../../state/ui'

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

  const [open, setOpen] = useState(startOpen)
  const { uiMode, toggleUiMode } = useContext(UIContext)

  const loadData = () => {
    setPedagogicalDocuments(Loading.of())
    void getChildPedagogicalDocuments(id).then(setPedagogicalDocuments)
  }

  useEffect(loadData, [id, open, setPedagogicalDocuments])

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

  function renderPedagigicalDocuments() {
    if (pedagogicalDocuments.isLoading) {
      return <Loader />
    } else if (pedagogicalDocuments.isFailure) {
      return <div>{i18n.common.loadingFailed}</div>
    } else
      return _.orderBy(pedagogicalDocuments.value, ['created'], ['desc']).map(
        (pedagogicalDocument: PedagogicalDocument) => (
          <PedagogicalDocumentRow
            key={pedagogicalDocument.id}
            id={pedagogicalDocument.id}
            childId={pedagogicalDocument.childId}
            attachment={pedagogicalDocument.attachment}
            description={pedagogicalDocument.description}
            created={pedagogicalDocument.created}
            updated={pedagogicalDocument.updated}
            initInEditMode={
              uiMode == `edit-pedagogical-document-${pedagogicalDocument.id}`
            }
            handleRemovedDocument={loadData}
          />
        )
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
              <Th />
            </Tr>
          </Thead>
          <Tbody>{renderPedagigicalDocuments()}</Tbody>
        </Table>
      )}
    </CollapsibleContentArea>
  )
})

export default PedagogicalDocuments
