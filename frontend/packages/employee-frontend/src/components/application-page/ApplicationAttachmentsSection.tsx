// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'

import { faFilePdf, faGavel } from 'icon-set'
import { Label } from '~components/shared/Typography'
import CollapsibleSection from '~components/shared/molecules/CollapsibleSection'
import ListGrid from '~components/shared/layout/ListGrid'
import { useTranslation } from '~state/i18n'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'components/shared/layout/flex-helpers'
import { Attachment } from '~types/application'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

type Props = {
  attachments: Attachment[]
}

export default React.memo(function ApplicationAttachmentsSection({
  attachments
}: Props) {
  const { i18n } = useTranslation()

  return (
    <CollapsibleSection
      title={i18n.application.attachments.title}
      icon={faGavel}
    >
      {attachments.length === 0 ? (
        <span>{i18n.application.attachments.none}</span>
      ) : (
        <FixedSpaceColumn spacing="XL">
          {attachments.map((attachment: Attachment) => (
            <ListGrid
              key={attachment.id}
              data-qa={`application-attachment-${attachment.id}`}
            >
              <Label>{i18n.application.attachments.name}</Label>

              <FixedSpaceRow spacing={'xs'} alignItems={'center'}>
                <FontAwesomeIcon icon={faFilePdf} />
                <Link to={`/attachments/${attachment.id}`}>
                  {attachment.name}
                </Link>
              </FixedSpaceRow>

              <Label>{i18n.application.attachments.contentType}</Label>
              <span>{attachment.contentType}</span>
            </ListGrid>
          ))}
        </FixedSpaceColumn>
      )}
    </CollapsibleSection>
  )
})
