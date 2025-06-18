// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React from 'react'

import type { ProcessMetadata } from 'lib-common/generated/api-types/process'
import type { DocumentMetadata } from 'lib-common/generated/api-types/process'
import { faArrowDownToLine } from 'lib-icons'

import UnorderedList from '../atoms/UnorderedList'
import { Button } from '../atoms/buttons/Button'
import { useTranslations } from '../i18n'
import { FixedSpaceColumn, FixedSpaceRow } from '../layout/flex-helpers'
import { H3 } from '../typography'
import { Gap } from '../white-space'

import LabelValueList from './LabelValueList'

const DocumentMetadata = React.memo(function DocumentMetadata({
  document
}: {
  document: DocumentMetadata
}) {
  const i18n = useTranslations()

  return (
    <div>
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.metadata.name,
            value: (
              <FixedSpaceRow>
                <span>{document.name}</span>
                {document.downloadPath !== null && (
                  <Button
                    appearance="inline"
                    icon={faArrowDownToLine}
                    text={i18n.metadata.downloadPdf}
                    onClick={() => {
                      window.open(
                        `/api${document.downloadPath}`,
                        '_blank',
                        'noopener,noreferrer'
                      )
                    }}
                  />
                )}
              </FixedSpaceRow>
            )
          },
          {
            label: i18n.metadata.documentId,
            value: document.documentId
          },
          {
            label: i18n.metadata.createdAt,
            value: document.createdAt?.format() ?? '-'
          },
          {
            label: i18n.metadata.createdBy,
            value: document.createdBy
              ? `${document.createdBy.name} (${i18n.common.userTypes[document.createdBy.type]}) `
              : '-'
          },
          ...(document.receivedBy
            ? [
                {
                  label: i18n.metadata.receivedBy.label,
                  value: i18n.metadata.receivedBy[document.receivedBy]
                }
              ]
            : []),
          ...(document.sfiDeliveries.length > 0
            ? [
                {
                  label: i18n.metadata.sfiDelivery.label,
                  value: (
                    <UnorderedList>
                      {orderBy(
                        document.sfiDeliveries,
                        (d) => d.recipientName
                      ).map((delivery, i) => (
                        <li key={i}>
                          {delivery.recipientName} -{' '}
                          {i18n.metadata.sfiDelivery.method[delivery.method]} (
                          {delivery.time.format()})
                        </li>
                      ))}
                    </UnorderedList>
                  )
                }
              ]
            : []),
          {
            label: i18n.metadata.confidentiality,
            value:
              document.confidential === true
                ? i18n.metadata.confidential
                : document.confidential === false
                  ? i18n.metadata.public
                  : i18n.metadata.notSet
          },
          ...(document.confidential && document.confidentiality
            ? [
                {
                  label: i18n.metadata.confidentialityDuration,
                  value: `${document.confidentiality.durationYears} ${i18n.metadata.years}`
                },
                {
                  label: i18n.metadata.confidentialityBasis,
                  value: document.confidentiality.basis
                }
              ]
            : [])
        ]}
      />
    </div>
  )
})

export const Metadatas = React.memo(function Metadatas({
  metadata
}: {
  metadata: ProcessMetadata | null
}) {
  const i18n = useTranslations()
  return metadata === null ? (
    <>
      <Gap />
      <div data-qa="metadata-not-found">{i18n.metadata.notFound}</div>
    </>
  ) : (
    <div>
      <Gap />
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.metadata.processNumber,
            value: metadata.process.processNumber,
            dataQa: 'process-number-field'
          },
          ...(metadata.processName
            ? [
                {
                  label: i18n.metadata.processName,
                  value: metadata.processName
                }
              ]
            : []),
          {
            label: i18n.metadata.organization,
            value: metadata.process.organization
          },
          ...(metadata.process.archiveDurationMonths !== null
            ? [
                {
                  label: i18n.metadata.archiveDurationMonths,
                  value: `${metadata.process.archiveDurationMonths} ${i18n.metadata.monthsUnit}`
                }
              ]
            : [])
        ]}
      />
      <Gap />
      <H3>{i18n.metadata.primaryDocument}</H3>
      <DocumentMetadata document={metadata.primaryDocument} />
      {metadata.secondaryDocuments.length > 0 && (
        <>
          <Gap />
          <H3>{i18n.metadata.secondaryDocuments}</H3>
          <FixedSpaceColumn>
            {metadata.secondaryDocuments.map((doc, i) => (
              <DocumentMetadata key={i} document={doc} />
            ))}
          </FixedSpaceColumn>
        </>
      )}
      {metadata.process.history.length > 0 && (
        <>
          <Gap />
          <H3>{i18n.metadata.history}</H3>
          <ul>
            {metadata.process.history.map((row) => (
              <li key={row.rowIndex}>
                {row.enteredAt.format()}: {i18n.metadata.states[row.state]},{' '}
                {row.enteredBy.name} (
                {i18n.common.userTypes[row.enteredBy.type]})
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  )
})
