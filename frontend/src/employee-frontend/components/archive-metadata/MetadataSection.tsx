// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import type { ProcessMetadataResponse } from 'lib-common/generated/api-types/caseprocess'
import { CollapsibleContentArea as Collapsible } from 'lib-components/layout/Container'
import { Metadatas } from 'lib-components/molecules/Metadatas'
import { H2 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

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
        return <Metadatas metadata={metadata} />
      })}
    </CollapsibleContentArea>
  )
})

const CollapsibleContentArea = styled(Collapsible)`
  @media print {
    display: none;
  }
`
