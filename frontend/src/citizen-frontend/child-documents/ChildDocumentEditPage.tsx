// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { useQueryClient } from '@tanstack/react-query'
import isEqual from 'lodash/isEqual'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import Footer from 'citizen-frontend/Footer'
import { renderResult } from 'citizen-frontend/async-rendering'
import { useTranslation } from 'citizen-frontend/localization'
import { useForm } from 'lib-common/form/hooks'
import {
  ChildDocumentCitizenDetails,
  DocumentContent
} from 'lib-common/generated/api-types/document'
import { ChildDocumentId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { Button } from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Spinner from 'lib-components/atoms/state/Spinner'
import { ChildDocumentStateChip } from 'lib-components/document-templates/ChildDocumentStateChip'
import DocumentView from 'lib-components/document-templates/DocumentView'
import {
  documentForm,
  getDocumentFormInitialState
} from 'lib-components/document-templates/documents'
import Content, {
  Container,
  ContentArea
} from 'lib-components/layout/Container'
import {
  AlignRight,
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faExclamationTriangle } from 'lib-icons'

import {
  childDocumentDetailsQuery,
  updateChildDocumentContentMutation
} from './queries'

export default React.memo(function ChildDocumentEditPage() {
  const id = useIdRouteParam<ChildDocumentId>('id')

  const childDocument = useQueryResult(
    childDocumentDetailsQuery({ documentId: id })
  )
  const i18n = useTranslation()

  return (
    <>
      <Content>
        <Gap size="s" />
        <ReturnButton label={i18n.common.return} />
        <Gap size="s" />

        {renderResult(childDocument, (document) => (
          <ChildDocumentEdit document={document} />
        ))}

        <Gap size="s" />
      </Content>
      <Footer />
    </>
  )
})

const ChildDocumentEdit = React.memo(function ChildDocumentEdit({
  document
}: {
  document: ChildDocumentCitizenDetails
}) {
  const i18n = useTranslation()

  const bind = useForm(
    documentForm,
    () =>
      getDocumentFormInitialState(document.template.content, document.content),
    i18n.validationErrors
  )

  const [lastSaved, setLastSaved] = useState(HelsinkiDateTime.now())
  const [lastSavedContent, setLastSavedContent] = useState(document.content)
  const [error, setError] = useState<'other' | null>(null)

  const { mutateAsync: updateChildDocumentContent, isPending: submitting } =
    useMutationResult(updateChildDocumentContentMutation)

  // invalidate cached document on unmount
  const queryClient = useQueryClient()
  useEffect(
    () => () => {
      void queryClient.invalidateQueries({
        queryKey: childDocumentDetailsQuery({ documentId: document.id })
          .queryKey,
        type: 'all'
      })
    },
    [queryClient, document.id]
  )

  const save = useCallback(
    async (content: DocumentContent) => {
      const result = await updateChildDocumentContent({
        documentId: document.id,
        body: content
      })
      if (result.isSuccess) {
        setLastSaved(HelsinkiDateTime.now())
        setLastSavedContent(content)
      } else {
        if (result.isFailure) {
          setError('other')
        }
      }
    },
    [updateChildDocumentContent, document.id]
  )

  const saved = useMemo(
    () => bind.isValid() && isEqual(lastSavedContent, bind.value()),
    [bind, lastSavedContent]
  )

  const debouncedValidContent = useDebounce(
    bind.isValid() ? bind.value() : null,
    1000
  )

  useEffect(() => {
    if (
      error === null &&
      debouncedValidContent !== null &&
      !isEqual(lastSavedContent, debouncedValidContent) &&
      !submitting
    ) {
      void save(debouncedValidContent)
    }
  }, [error, debouncedValidContent, lastSavedContent, save, submitting])

  useEffect(() => {
    if (error === 'other') {
      const handle = setTimeout(() => setError(null), 5000)
      return () => clearTimeout(handle)
    }
    return undefined
  }, [error])

  const navigate = useNavigate()

  return (
    <>
      <Container>
        <ContentArea opaque>
          <FixedSpaceRow justifyContent="space-between">
            <FixedSpaceColumn>
              <H1 noMargin>{document.template.name}</H1>
              <H2 noMargin translate="no">
                {document.child.firstName} {document.child.lastName}
                {document.child.dateOfBirth
                  ? ` (${document.child.dateOfBirth.format()})`
                  : ''}
              </H2>
            </FixedSpaceColumn>
            <FixedSpaceColumn spacing="xs">
              <ChildDocumentStateChip status={document.status} />
              {document.template.confidentiality !== null && (
                <Label>{i18n.children.childDocuments.confidential}</Label>
              )}
              <span>{document.template.legalBasis}</span>
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <Gap />
          <DocumentView bind={bind} readOnly={false} />
        </ContentArea>
      </Container>
      <Gap size="m" />
      <FixedSpaceRow
        justifyContent="space-between"
        alignItems="center"
        gap={defaultMargins.m}
      >
        <ReturnButton label={i18n.common.return} />
        <span>
          {i18n.common.saveSuccess} {lastSaved.toLocalTime().format('HH:mm:ss')}
        </span>
        {!saved && <Spinner size={defaultMargins.m} data-qa="saving-spinner" />}
        {error === 'other' && (
          <span>
            <FontAwesomeIcon
              icon={faExclamationTriangle}
              color={colors.status.warning}
            />{' '}
            {i18n.children.childDocuments.error.other}
          </span>
        )}
        <AlignRight>
          <Button
            text={i18n.children.childDocuments.preview}
            onClick={() =>
              navigate(`/child-documents/${document.id}`, { replace: true })
            }
            primary
            data-qa="preview-button"
          />
        </AlignRight>
      </FixedSpaceRow>
    </>
  )
})
