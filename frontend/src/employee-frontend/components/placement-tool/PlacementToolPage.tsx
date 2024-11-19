// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import { useQueryResult } from 'lib-common/query'
import Container, { ContentArea } from 'lib-components/layout/Container'
import FileUpload from 'lib-components/molecules/FileUpload'
import { H1, P } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import WarningLabel from '../common/WarningLabel'

import { deletePlacementFile, uploadPlacementFile } from './api'
import { nextPreschoolTermQuery } from './queries'

export default React.memo(function PlacementToolPage() {
  const { i18n } = useTranslation()
  const preschoolTermResult = useQueryResult(nextPreschoolTermQuery())

  const nextPreschoolTerm = useMemo(
    () =>
      preschoolTermResult.map((r) => {
        return r.length > 0 ? r[0] : null
      }),
    [preschoolTermResult]
  )

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.placementTool.title}</H1>
        {renderResult(nextPreschoolTerm, (term) => (
          <>
            <P>{i18n.placementTool.description}</P>
            {term ? (
              <>
                <P>{`${i18n.placementTool.preschoolTermNotification} ${term.finnishPreschool.format()}`}</P>
                <FileUpload
                  disabled={term === null}
                  files={[]}
                  onUpload={uploadPlacementFile}
                  onDelete={deletePlacementFile}
                  getDownloadUrl={() => ''}
                  allowedFileTypes={['csv']}
                />
              </>
            ) : (
              <WarningLabel text={i18n.placementTool.preschoolTermWarning} />
            )}
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
