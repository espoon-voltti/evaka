// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { UseQueryOptions } from '@tanstack/react-query'
import React, { useState } from 'react'

import type { ProcessMetadataResponse } from 'lib-common/generated/api-types/caseprocess'
import { useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { Metadatas } from 'lib-components/molecules/Metadatas'
import { faChevronDown, faChevronUp } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'

type ProcessMetadataQuery = UseQueryOptions<
  ProcessMetadataResponse,
  unknown,
  ProcessMetadataResponse,
  readonly unknown[]
>

export const MetadataSection = React.memo(function MetadataSection({
  query,
  'data-qa': dataQa
}: {
  query: ProcessMetadataQuery
  'data-qa': string
}) {
  const [sectionOpen, setSectionOpen] = useState(false)
  const i18n = useTranslation()
  return (
    <>
      <Button
        appearance="inline"
        icon={sectionOpen ? faChevronUp : faChevronDown}
        text={
          sectionOpen
            ? i18n.components.metadata.section.hide
            : i18n.components.metadata.section.show
        }
        onClick={() => setSectionOpen(!sectionOpen)}
        data-qa={`metadata-toggle-${dataQa}`}
      />
      {sectionOpen && <MetadataResultSection query={query} />}
    </>
  )
})

const MetadataResultSection = React.memo(function MetadataResultSection<
  Q extends ProcessMetadataQuery
>({ query }: { query: Q }) {
  const metadataResult = useQueryResult(query)
  return (
    <>
      {renderResult(metadataResult, ({ data }) => {
        return <Metadatas metadata={data} />
      })}
    </>
  )
})
