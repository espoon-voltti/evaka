// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faArrowDownToLine } from 'Icons'
import React from 'react'

import { Result } from 'lib-common/api'
import {
  Document,
  ProcessMetadataResponse
} from 'lib-common/generated/api-types/process'
import { Button } from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import LabelValueList from '../common/LabelValueList'

const DocumentMetadata = React.memo(function DocumentMetadata({
  document
}: {
  document: Document
}) {
  const { i18n } = useTranslation()

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
                        `/api/internal${document.downloadPath}`,
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
            label: i18n.metadata.createdAt,
            value: document.createdAt?.format() ?? '-'
          },
          {
            label: i18n.metadata.createdBy,
            value: document.createdBy
              ? `${document.createdBy.firstName} ${document.createdBy.lastName} ${document.createdBy.email ? `(${document.createdBy.email})` : ''} `
              : '-'
          },
          {
            label: i18n.metadata.confidentiality,
            value: document.confidential
              ? i18n.metadata.confidential
              : i18n.metadata.public
          }
        ]}
      />
    </div>
  )
})

export default React.memo(function MetadataSection({
  metadataResult
}: {
  metadataResult: Result<ProcessMetadataResponse>
}) {
  const { i18n } = useTranslation()

  return (
    <div>
      <H3>{i18n.metadata.title}</H3>
      {renderResult(metadataResult, ({ data: metadata }) => {
        if (metadata === null) return <div>{i18n.metadata.notFound}</div>
        return (
          <div>
            <LabelValueList
              spacing="small"
              contents={[
                {
                  label: i18n.metadata.processNumber,
                  value: metadata.process.processNumber
                },
                {
                  label: i18n.metadata.organization,
                  value: metadata.process.organization
                },
                {
                  label: i18n.metadata.archiveDurationMonths,
                  value: `${metadata.process.archiveDurationMonths} ${i18n.metadata.monthsUnit}`
                }
              ]}
            />
            <Gap />
            <H4>{i18n.metadata.documents}</H4>
            <DocumentMetadata document={metadata.primaryDocument} />
            <Gap />
            <H4>{i18n.metadata.history}</H4>
            <ul>
              {metadata.process.history.map((row) => (
                <li key={row.rowIndex}>
                  {row.enteredAt.format()}: {i18n.metadata.states[row.state]},{' '}
                  {row.enteredBy.name} (
                  {i18n.common.userTypes[row.enteredBy.type]})
                </li>
              ))}
            </ul>
          </div>
        )
      })}
    </div>
  )
})
