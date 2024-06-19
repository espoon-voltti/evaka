// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import Container, { ContentArea } from 'lib-components/layout/Container'
import FileUpload from 'lib-components/molecules/FileUpload'
import { H1, P } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'

import { deletePlacementFile, uploadPlacementFile } from './api'

export default React.memo(function PlacementToolPage() {
  const { i18n } = useTranslation()

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.placementTool.title}</H1>
        <P>{i18n.placementTool.description}</P>
        <FileUpload
          files={[]}
          onUpload={uploadPlacementFile}
          onDelete={deletePlacementFile}
          getDownloadUrl={() => ''}
          allowedFileTypes={['csv']}
        />
      </ContentArea>
    </Container>
  )
})
