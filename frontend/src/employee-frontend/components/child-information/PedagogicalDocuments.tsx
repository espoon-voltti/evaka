// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useState } from 'react'
import styled from 'styled-components'

import type { PedagogicalDocument } from 'lib-common/generated/api-types/pedagogicaldocument'
import type {
  ChildId,
  PedagogicalDocumentId
} from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  InfoButton,
  ExpandingInfoBox
} from 'lib-components/molecules/ExpandingInfo'
import { P } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import PedagogicalDocumentRow from './PedagogicalDocumentRow'
import {
  childPedagogicalDocumentsQuery,
  createPedagogicalDocumentMutation
} from './queries'

interface Props {
  childId: ChildId
}

export default React.memo(function PedagogicalDocuments({ childId }: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(ChildContext)

  const [editingId, setEditingId] = useState<PedagogicalDocumentId | null>(null)

  const pedagogicalDocuments = useQueryResult(
    childPedagogicalDocumentsQuery({ childId })
  )

  const [expandingInfo, setExpandingInfo] = useState<string>()
  const toggleExpandingInfo = useCallback(
    (info: string) =>
      setExpandingInfo(expandingInfo === info ? undefined : info),
    [expandingInfo]
  )
  const closeExpandingInfo = useCallback(() => setExpandingInfo(undefined), [])

  const { mutateAsync: createPedagogicalDocument } = useMutationResult(
    createPedagogicalDocumentMutation
  )

  const createNewDocument = () => {
    const emptyDocument = { childId, description: '', attachmentId: null }
    void createPedagogicalDocument({ body: emptyDocument }).then((res) => {
      if (res.isSuccess) {
        setEditingId(res.value.id)
      }
    })
  }

  return (
    <>
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
              <Th>{i18n.childInformation.pedagogicalDocument.created}</Th>
              <Th>{i18n.childInformation.pedagogicalDocument.lastModified}</Th>
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
            {orderBy(pedagogicalDocuments, ['createdAt'], ['desc']).map(
              (pedagogicalDocument: PedagogicalDocument) => (
                <PedagogicalDocumentRow
                  key={pedagogicalDocument.id}
                  id={pedagogicalDocument.id}
                  childId={pedagogicalDocument.childId}
                  attachments={pedagogicalDocument.attachments}
                  description={pedagogicalDocument.description}
                  createdAt={pedagogicalDocument.createdAt}
                  createdBy={pedagogicalDocument.createdBy}
                  modifiedAt={pedagogicalDocument.modifiedAt}
                  modifiedBy={pedagogicalDocument.modifiedBy}
                  editing={editingId === pedagogicalDocument.id}
                  onStartEditing={() => setEditingId(pedagogicalDocument.id)}
                  onStopEditing={() => setEditingId(null)}
                />
              )
            )}
          </Tbody>
        </Table>
      ))}
    </>
  )
})

const InfoButtonWithMargin = styled(InfoButton)`
  margin-left: ${defaultMargins.xs};
`
