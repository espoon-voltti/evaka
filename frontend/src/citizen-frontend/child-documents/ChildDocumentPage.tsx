// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect } from 'react'
import styled from 'styled-components'
import { useLocation, useSearchParams } from 'wouter'

import { useBoolean, useForm } from 'lib-common/form/hooks'
import type { ChildDocumentCitizenDetails } from 'lib-common/generated/api-types/document'
import type { ChildDocumentId } from 'lib-common/generated/api-types/shared'
import { useMutation, useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import { NotificationsContext } from 'lib-components/Notifications'
import { Button } from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { desktopMinPx, tabletMin } from 'lib-components/breakpoints'
import { ChildDocumentStateChip } from 'lib-components/document-templates/ChildDocumentStateChip'
import DocumentView from 'lib-components/document-templates/DocumentView'
import {
  documentForm,
  getDocumentCategory,
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
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowDownToLine, faCheck, faPrint } from 'lib-icons'

import Footer from '../Footer'
import { renderResult } from '../async-rendering'
import { downloadChildDocument } from '../generated/api-clients/document'
import { useTranslation } from '../localization'
import useTitle from '../useTitle'

import {
  childDocumentDetailsQuery,
  childDocumentReadMutation,
  updateChildDocumentMutation
} from './queries'

const TopButtonRow = styled(FixedSpaceRow)`
  @media (max-width: 1215px) {
    margin-right: ${defaultMargins.s};
  }

  @media print {
    display: none;
  }
`

const StickyContainer = styled(Container)`
  position: sticky;
  bottom: 0;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  box-shadow: 0 -2px 4px 0 rgba(0, 0, 0, 0.15);
  padding: ${defaultMargins.s} 0;

  @media (max-width: ${desktopMinPx - 1}px) {
    box-shadow: none;
    position: static;
  }

  > * {
    margin-right: ${defaultMargins.m};

    @media (min-width: ${tabletMin}) {
      margin: ${defaultMargins.s} ${defaultMargins.m} ${defaultMargins.s} 0;
    }

    @media (min-width: 1216px) {
      margin-left: ${defaultMargins.m};
    }
  }
`

export default React.memo(function ChildDocumentPage() {
  const id = useIdRouteParam<ChildDocumentId>('id')

  const childDocument = useQueryResult(
    childDocumentDetailsQuery({ documentId: id })
  )
  const i18n = useTranslation()

  return (
    <>
      <Content>
        <Gap size="s" />
        <TopButtonRow justifyContent="space-between">
          <ReturnButton label={i18n.common.return} />
          <FixedSpaceRow>
            <Button
              appearance="inline"
              onClick={() => window.print()}
              icon={faPrint}
              text={i18n.common.print}
            />
            {childDocument.map((d) => d.downloadable).getOrElse(false) && (
              <Button
                appearance="inline"
                icon={faArrowDownToLine}
                text={i18n.common.download}
                onClick={() => {
                  window.open(
                    downloadChildDocument({ documentId: id }).url.toString(),
                    '_blank',
                    'noopener,noreferrer'
                  )
                }}
              />
            )}
          </FixedSpaceRow>
        </TopButtonRow>
        <Gap size="s" />

        {renderResult(childDocument, (document) => (
          <ChildDocumentView document={document} />
        ))}

        <Gap size="s" />
      </Content>
      <Footer />
    </>
  )
})

const ChildDocumentView = React.memo(function ChildDocumentView({
  document
}: {
  document: ChildDocumentCitizenDetails
}) {
  const i18n = useTranslation()
  useTitle(i18n, document.template.name)

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

  const [searchParams] = useSearchParams()

  const initialReadOnly = searchParams.get('readOnly') !== 'false'
  const [readOnly, { on: readOnlyOn, off: readOnlyOff }] =
    useBoolean(initialReadOnly)

  const { addNotification } = useContext(NotificationsContext)
  const [, navigate] = useLocation()
  const onSuccess = useCallback(() => {
    const returnTo = searchParams.get('returnTo')
    if (returnTo === 'calendar') {
      navigate('/calendar')
      addNotification({
        icon: faCheck,
        iconColor: colors.status.success,
        children: i18n.children.childDocuments.success,
        dataQa: `toast-child-document-${document.id}-success`
      })
    }
  }, [
    addNotification,
    document.id,
    i18n.children.childDocuments.success,
    navigate,
    searchParams
  ])

  return (
    <>
      <Container>
        <ContentArea opaque>
          <FixedSpaceRow justifyContent="space-between">
            <FixedSpaceColumn>
              <H1 noMargin>{document.template.name}</H1>
              <H2 noMargin>
                <PersonName person={document.child} format="First Last" />
                {document.child.dateOfBirth
                  ? ` (${document.child.dateOfBirth.format()})`
                  : ''}
              </H2>
            </FixedSpaceColumn>
            <FixedSpaceColumn spacing="xs" alignItems="flex-end">
              {document.decision && (
                <>
                  <div>
                    {i18n.children.childDocuments.decisionNumber}{' '}
                    {document.decision.decisionNumber}
                  </div>
                  <Gap size="xs" />
                </>
              )}
              <ChildDocumentStateChip
                status={document.decision?.status ?? document.status}
              />
              {document.template.confidentiality !== null && (
                <Label>{i18n.children.childDocuments.confidential}</Label>
              )}
              <span>{document.template.legalBasis}</span>
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <Gap />
          <DocumentView bind={bind} readOnly={readOnly} />
          {getDocumentCategory(document.template.type) === 'external' &&
            document.status === 'COMPLETED' && (
              <InfoBox message={i18n.children.childDocuments.sentInfo} />
            )}
        </ContentArea>
      </Container>
      <Gap size="m" />
      <StickyContainer>
        <FixedSpaceRow
          justifyContent="space-between"
          alignItems="center"
          gap={defaultMargins.m}
        >
          <ReturnButton label={i18n.common.return} data-qa="return-button" />
          {document.status === 'CITIZEN_DRAFT' && (
            <>
              {readOnly && (
                <Button
                  text={i18n.common.edit}
                  onClick={readOnlyOff}
                  data-qa="edit-button"
                />
              )}
              <AlignRight>
                {readOnly ? (
                  <ConfirmedMutation
                    mutation={updateChildDocumentMutation}
                    onClick={() => ({
                      documentId: document.id,
                      body: {
                        status: 'COMPLETED' as const,
                        content: bind.value()
                      }
                    })}
                    confirmationTitle={
                      i18n.children.childDocuments.sendingConfirmationTitle
                    }
                    confirmationText={
                      i18n.children.childDocuments.sendingConfirmationText
                    }
                    onSuccess={onSuccess}
                    buttonText={i18n.children.childDocuments.send}
                    primary
                    disabled={!bind.isValid()}
                    data-qa="send-button"
                  />
                ) : (
                  <Button
                    text={i18n.children.childDocuments.preview}
                    onClick={readOnlyOn}
                    primary
                    data-qa="preview-button"
                  />
                )}
              </AlignRight>
            </>
          )}
        </FixedSpaceRow>
      </StickyContainer>
    </>
  )
})
