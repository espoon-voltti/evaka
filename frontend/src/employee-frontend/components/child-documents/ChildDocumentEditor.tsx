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
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H1, H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

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

  const debouncedContent = useDebounce(bind.value(), 1000)

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

  useEffect(() => {
    void save(debouncedContent)
  }, [debouncedContent, save])

  const dirty = useMemo(
    () => lastSavedContent !== bind.value(),
    [lastSavedContent, bind]
  )

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
          <H1>
            {document.child.firstName} {document.child.lastName} (
            {document.child.dateOfBirth?.format()})
          </H1>
          <H2>
            {i18n.documentTemplates.documentTypes[document.template.type]}
          </H2>
          <DocumentView bind={bind} readOnly={preview} />
        </ContentArea>
      </Container>

      <ActionBar>
        <Container>
          <FixedSpaceRow justifyContent="space-between" alignItems="center">
            <FixedSpaceRow alignItems="center">
              {preview || document.published ? (
                <Button text="Poistu" onClick={goBack} />
              ) : (
                <Button
                  text="Poistu ja tallenna luonnoksena"
                  onClick={() => save(bind.value()).then(goBack)}
                />
              )}
              {preview && !document.published && (
                <Button text="Muokkaa" onClick={() => setPreview(false)} />
              )}
              {!preview && (
                <FixedSpaceRow alignItems="center" spacing="xs">
                  <span>
                    Tallennettu{' '}
                    {formatInTimeZone(
                      lastSaved.timestamp,
                      'Europe/Helsinki',
                      'HH:mm:ss'
                    )}
                  </span>
                  {dirty && <Spinner size={defaultMargins.m} />}
                </FixedSpaceRow>
              )}
            </FixedSpaceRow>
            {!preview && (
              <Button
                text="Esikatsele"
                primary
                onClick={() => setPreview(true)}
                disabled={dirty}
              />
            )}
            {preview && !document.published && (
              <Button
                text="Julkaise huoltajalle"
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
