// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import type { ExportedDocumentTemplate } from 'lib-common/generated/api-types/document'
import type { JsonOf } from 'lib-common/json'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import BaseModal, {
  ModalButtons
} from 'lib-components/molecules/modals/BaseModal'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'

interface Props {
  onClose: () => void
  onContinue: (template: JsonOf<ExportedDocumentTemplate>) => void
}

const FileInput = styled.input`
  max-width: 100%;
`

export default React.memo(function TemplateImportModal({
  onClose,
  onContinue
}: Props) {
  const { i18n } = useTranslation()

  const [data, setData] = useState<
    JsonOf<ExportedDocumentTemplate> | undefined
  >(undefined)
  const onClickContinue = useCallback(
    () => data && onContinue(data),
    [data, onContinue]
  )

  return (
    <BaseModal
      data-qa="template-import-modal"
      close={onClose}
      closeLabel={i18n.common.close}
      title={i18n.documentTemplates.templatesPage.import}
    >
      <FileInput
        type="file"
        accept=".template.json"
        onChange={(e) => {
          setData(undefined)
          const file = e.target.files?.item(0)
          if (file) {
            void file.text().then((text) => {
              // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
              const json = JSON.parse(text)
              // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
              if ('name' in json && typeof json.name === 'string') {
                // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
                setData(json)
              }
            })
          }
        }}
      />
      <ModalButtons $justifyContent="center">
        <LegacyButton
          primary
          disabled={!data}
          text={i18n.common.continue}
          onClick={onClickContinue}
          data-qa="continue"
        />
        <Gap horizontal size="s" />
        <LegacyButton
          onClick={onClose}
          data-qa="cancel"
          text={i18n.common.cancel}
        />
      </ModalButtons>
    </BaseModal>
  )
})
