// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { formatInTimeZone } from 'date-fns-tz'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { useForm } from 'lib-common/form/hooks'
import {
  ChildDocumentDetails,
  DocumentContent
} from 'lib-common/generated/api-types/document'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useDebounce } from 'lib-common/utils/useDebounce'
import Button from 'lib-components/atoms/buttons/Button'
import Spinner from 'lib-components/atoms/state/Spinner'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H1, H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import {
  childDocumentQuery,
  publishChildDocumentMutation,
  updateChildDocumentContentMutation
} from '../child-information/queries'
import DocumentView from '../document-templates/DocumentView'
import {
  documentForm,
  getDocumentFormInitialState
} from '../document-templates/documents'

const ActionBar = styled.div`
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: white;
  margin-top: ${defaultMargins.L};
  padding: ${defaultMargins.s} 0;
`

const ChildDocumentEditorView = React.memo(function ChildDocumentEditorView({
  document
}: {
  document: ChildDocumentDetails
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const bind = useForm(
    documentForm,
    () =>
      getDocumentFormInitialState(document.template.content, document.content),
    i18n.validationErrors
  )
  const [preview, setPreview] = useState(document.published)
  const [lastSaved, setLastSaved] = useState(HelsinkiDateTime.now())
  const [lastSavedContent, setLastSavedContent] = useState(document.content)
  const { mutateAsync: updateChildDocumentContent } = useMutationResult(
    updateChildDocumentContentMutation
  )
  const { mutateAsync: publishChildDocument } = useMutationResult(
    publishChildDocumentMutation
  )

  const save = useCallback(
    async (content: DocumentContent) => {
      const result = await updateChildDocumentContent({
        id: document.id,
        content
      })
      if (result.isSuccess) {
        setLastSaved(HelsinkiDateTime.now())
        setLastSavedContent(content)
      }
    },
    [updateChildDocumentContent, document.id]
  )

  const saved = useMemo(
    () => bind.isValid() && lastSavedContent === bind.value(),
    [bind, lastSavedContent]
  )

  const debouncedValidContent = useDebounce(
    bind.isValid() ? bind.value() : null,
    1000
  )

  useEffect(() => {
    if (debouncedValidContent !== null) {
      void save(debouncedValidContent)
    }
  }, [debouncedValidContent, save])

  const goBack = () => navigate(`/child-information/${document.child.id}`)

  const publishAndGoBack = async () => {
    const result = await publishChildDocument({
      documentId: document.id,
      childId: document.child.id
    })
    if (result.isSuccess) {
      goBack()
    }
  }

  return (
    <div>
      <Container>
        <ContentArea opaque>
          <FixedSpaceRow justifyContent="space-between" alignItems="center">
            <FixedSpaceColumn>
              <H1 noMargin>{document.template.name}</H1>
              <H2 noMargin>
                {document.child.firstName} {document.child.lastName} (
                {document.child.dateOfBirth?.format()})
              </H2>
            </FixedSpaceColumn>
            <FixedSpaceColumn
              spacing="xxs"
              justifyContent="start"
              alignItems="flex-end"
            >
              {document.template.confidential && (
                <strong>
                  {i18n.documentTemplates.templateEditor.confidential}
                </strong>
              )}
              {!!document.template.legalBasis && (
                <span>{document.template.legalBasis}</span>
              )}
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <Gap size="XXL" />
          <DocumentView bind={bind} readOnly={preview} />
        </ContentArea>
      </Container>

      <ActionBar>
        <Container>
          <FixedSpaceRow justifyContent="space-between" alignItems="center">
            <FixedSpaceRow alignItems="center">
              {preview || document.published ? (
                <Button text={i18n.common.goBack} onClick={goBack} />
              ) : (
                <Button
                  text={i18n.common.goBack}
                  onClick={() => save(bind.value()).then(goBack)}
                />
              )}
              {preview && !document.published && (
                <Button
                  text={i18n.common.edit}
                  onClick={() => setPreview(false)}
                />
              )}
              {!preview && (
                <FixedSpaceRow alignItems="center" spacing="xs">
                  <span>
                    {i18n.common.saved}{' '}
                    {formatInTimeZone(
                      lastSaved.timestamp,
                      'Europe/Helsinki',
                      'HH:mm:ss'
                    )}
                  </span>
                  {!saved && <Spinner size={defaultMargins.m} />}
                </FixedSpaceRow>
              )}
            </FixedSpaceRow>
            {!preview && (
              <Button
                text={i18n.childInformation.childDocuments.editor.preview}
                primary
                onClick={() => setPreview(true)}
                disabled={!saved}
              />
            )}
            {preview && !document.published && (
              <Button
                text={i18n.childInformation.childDocuments.editor.publish}
                primary
                onClick={publishAndGoBack}
              />
            )}
          </FixedSpaceRow>
        </Container>
      </ActionBar>
    </div>
  )
})

export default React.memo(function ChildDocumentEditor() {
  const { documentId } = useNonNullableParams()
  const documentResult = useQueryResult(childDocumentQuery(documentId))

  return renderResult(documentResult, (document) => (
    <ChildDocumentEditorView document={document} />
  ))
})
