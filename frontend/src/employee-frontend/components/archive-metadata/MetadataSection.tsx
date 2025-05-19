// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import type { ProcessMetadataResponse } from 'lib-common/generated/api-types/process'
import { DocumentMetadata } from 'lib-common/generated/api-types/process'
import { Button } from 'lib-components/atoms/buttons/Button'
import { CollapsibleContentArea as Collapsible } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faArrowDownToLine } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import LabelValueList from '../common/LabelValueList'

const DocumentMetadata = React.memo(function DocumentMetadata({
  document
}: {
  document: DocumentMetadata
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

export default React.memo(function MetadataSection({
  metadataResult
}: {
  metadataResult: Result<ProcessMetadataResponse>
}) {
  const { i18n } = useTranslation()
  const [sectionOpen, { toggle: toggleOpen }] = useBoolean(false)

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.metadata.title}</H2>}
      open={sectionOpen}
      toggleOpen={toggleOpen}
      opaque
    >
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
                {
                  label: i18n.metadata.archiveDurationMonths,
                  value: `${metadata.process.archiveDurationMonths} ${i18n.metadata.monthsUnit}`
                }
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
          </div>
        )
      })}
    </CollapsibleContentArea>
  )
})

const CollapsibleContentArea = styled(Collapsible)`
  @media print {
    display: none;
  }
`
