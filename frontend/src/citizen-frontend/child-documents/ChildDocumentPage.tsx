// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect } from 'react'
import styled from 'styled-components'

import Footer from 'citizen-frontend/Footer'
import { renderResult } from 'citizen-frontend/async-rendering'
import { useTranslation } from 'citizen-frontend/localization'
import { useForm } from 'lib-common/form/hooks'
import {
  ChildDocumentCitizenDetails,
  ChildDocumentDetails
} from 'lib-common/generated/api-types/document'
import { useMutation, useQueryResult } from 'lib-common/query'
import useRequiredParams from 'lib-common/useRequiredParams'
import DownloadButton from 'lib-components/atoms/buttons/DownloadButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { tabletMin } from 'lib-components/breakpoints'
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
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { childDocumentDetailsQuery, childDocumentReadMutation } from './queries'

const TopButtonRow = styled(FixedSpaceRow)`
  @media (max-width: ${tabletMin}) {
    margin-right: ${defaultMargins.s};
  }
`

export default React.memo(function ChildDocumentPage() {
  const { id } = useRequiredParams('id')

  const decision = useQueryResult(childDocumentDetailsQuery({ documentId: id }))
  const i18n = useTranslation()

  return (
    <>
      <Content>
        <Gap size="s" />
        <TopButtonRow justifyContent="space-between">
          <ReturnButton label={i18n.common.return} />
          <DownloadButton label={i18n.common.download} />
        </TopButtonRow>
        <Gap size="s" />

        {renderResult(decision, (document) => (
          <ChildDocumentView document={document} />
        ))}

        <Gap size="m" />
        <ReturnButton label={i18n.common.return} />
        <Gap size="s" />
      </Content>
      <Footer />
    </>
  )
})

const ChildDocumentView = React.memo(function ChildDocumentView({
  document
}: {
  document: ChildDocumentDetails | ChildDocumentCitizenDetails
}) {
  const i18n = useTranslation()

  const { mutateAsync: markRead } = useMutation(childDocumentReadMutation)
  useEffect(() => {
    void markRead({ documentId: document.id })
  }, [markRead, document.id])

  const bind = useForm(
    documentForm,
    () =>
      getDocumentFormInitialState(document.template.content, document.content),
    i18n.validationErrors
  )

  return (
    <Container>
      <ContentArea opaque>
        <FixedSpaceRow justifyContent="space-between">
          <FixedSpaceColumn>
            <H1 noMargin>{document.template.name}</H1>
            <H2 noMargin>
              {document.child.firstName} {document.child.lastName}
              {document.child.dateOfBirth
                ? ` (${document.child.dateOfBirth.format()})`
                : ''}
            </H2>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xs">
            <ChildDocumentStateChip status={document.status} />
            {document.template.confidential && (
              <Label>{i18n.children.vasu.confidential}</Label>
            )}
            <span>{document.template.legalBasis}</span>
          </FixedSpaceColumn>
        </FixedSpaceRow>
        <Gap />
        <DocumentView bind={bind} readOnly />
      </ContentArea>
    </Container>
  )
})
