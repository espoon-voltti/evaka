// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H1 } from 'lib-components/typography'
import React, { useEffect, useState } from 'react'
import { useHistory } from 'react-router'
import { Link } from 'react-router-dom'
import { Loading, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { Gap } from 'lib-components/white-space'
import { faCopy, faPen, faTrash } from 'lib-icons'
import { useTranslation } from '../../../state/i18n'
import {
  deleteVasuTemplate,
  getVasuTemplateSummaries,
  VasuTemplateSummary
} from './api'
import CopyTemplateModal from './CopyTemplateModal'
import CreateTemplateModal from './CreateOrEditTemplateModal'

export default React.memo(function VasuTemplatesPage() {
  const { i18n } = useTranslation()
  const t = i18n.vasuTemplates
  const h = useHistory()

  const [templates, setTemplates] = useState<Result<VasuTemplateSummary[]>>(
    Loading.of()
  )

  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [templateToCopy, setTemplateToCopy] = useState<VasuTemplateSummary>()
  const [templateToEdit, setTemplateToEdit] = useState<VasuTemplateSummary>()

  const loadTemplates = useRestApi(getVasuTemplateSummaries, setTemplates)
  useEffect(loadTemplates, [loadTemplates])

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        <H1>{t.title}</H1>

        {templates.isLoading && <SpinnerSegment />}
        {templates.isFailure && <ErrorSegment />}
        {templates.isSuccess && (
          <>
            <AddButtonRow
              onClick={() => setCreateModalOpen(true)}
              text={t.addNewTemplate}
            />
            <Table>
              <Thead>
                <Tr>
                  <Th>{t.name}</Th>
                  <Th>{t.valid}</Th>
                  <Th>{t.language}</Th>
                  <Th>{t.documentCount}</Th>
                  <Th />
                </Tr>
              </Thead>
              <Tbody>
                {templates.value.map((template) => (
                  <Tr key={template.id}>
                    <Td>
                      <Link to={`/vasu-templates/${template.id}`}>
                        {template.name}
                      </Link>
                    </Td>
                    <Td>{template.valid.format()}</Td>
                    <Td>{t.languages[template.language]}</Td>
                    <Td>{template.documentCount}</Td>
                    <Td>
                      <FixedSpaceRow spacing="s">
                        <IconButton
                          icon={faCopy}
                          onClick={() => setTemplateToCopy(template)}
                        />
                        <IconButton
                          icon={faPen}
                          onClick={() => setTemplateToEdit(template)}
                        />
                        <IconButton
                          icon={faTrash}
                          disabled={template.documentCount > 0}
                          onClick={() => {
                            void deleteVasuTemplate(template.id).then(() =>
                              loadTemplates()
                            )
                          }}
                        />
                      </FixedSpaceRow>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </>
        )}

        {(createModalOpen || templateToEdit) && (
          <CreateTemplateModal
            onSuccess={(id) => {
              if (createModalOpen) {
                h.push(`/vasu-templates/${id}`)
              } else {
                loadTemplates()
                setTemplateToEdit(undefined)
              }
            }}
            onCancel={() => {
              setCreateModalOpen(false)
              setTemplateToEdit(undefined)
            }}
            template={templateToEdit}
          />
        )}

        {templateToCopy && (
          <CopyTemplateModal
            template={templateToCopy}
            onSuccess={(id) => h.push(`/vasu-templates/${id}`)}
            onCancel={() => setTemplateToCopy(undefined)}
          />
        )}
      </ContentArea>
    </Container>
  )
})
