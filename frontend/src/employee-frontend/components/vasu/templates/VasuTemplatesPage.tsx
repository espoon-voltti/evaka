// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { H1 } from 'lib-components/typography'
import React, { useEffect, useState } from 'react'
import Container, {
  ContentArea
} from '../../../../lib-components/layout/Container'
import { Gap } from '../../../../lib-components/white-space'
import {
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from '../../../../lib-components/layout/Table'
import { useRestApi } from '../../../../lib-common/utils/useRestApi'
import { Loading, Result } from '../../../../lib-common/api'
import { SpinnerSegment } from '../../../../lib-components/atoms/state/Spinner'
import ErrorSegment from '../../../../lib-components/atoms/state/ErrorSegment'
import { useHistory } from 'react-router'
import { useTranslation } from '../../../state/i18n'
import { AddButtonRow } from '../../../../lib-components/atoms/buttons/AddButton'
import FiniteDateRange from '../../../../lib-common/finite-date-range'
import LocalDate from '../../../../lib-common/local-date'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import IconButton from '../../../../lib-components/atoms/buttons/IconButton'
import { faCopy, faPen, faTrash } from '../../../../lib-icons'
import {
  createVasuTemplate,
  deleteVasuTemplate,
  getVasuTemplateSummaries,
  VasuTemplateSummary
} from './api'

export default React.memo(function VasuTemplatesPage() {
  const { i18n } = useTranslation()
  const t = i18n.vasuTemplates
  const h = useHistory()

  const [templates, setTemplates] = useState<Result<VasuTemplateSummary[]>>(
    Loading.of()
  )

  const loadSummaries = useRestApi(getVasuTemplateSummaries, setTemplates)
  useEffect(loadSummaries, [loadSummaries])

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
              onClick={() => {
                void createVasuTemplate(
                  'Nimetön varhaiskasvatussuunnitelma',
                  new FiniteDateRange(
                    LocalDate.today(),
                    LocalDate.today().addYears(1)
                  )
                ).then(() => loadSummaries())
              }}
              text={'Lisää uusi vasu-pohja'}
            />
            <Table>
              <Thead>
                <Tr>
                  <Th>{t.name}</Th>
                  <Th>{t.valid}</Th>
                  <Th>{t.documentCount}</Th>
                  <Th />
                </Tr>
              </Thead>
              <Tbody>
                {templates.value.map((template) => (
                  <Tr key={template.id}>
                    <Td>{template.name}</Td>
                    <Td>{template.valid.format()}</Td>
                    <Td>{template.documentCount}</Td>
                    <Td>
                      <FixedSpaceRow spacing="s">
                        <IconButton icon={faCopy} disabled={true} />
                        <IconButton
                          icon={faPen}
                          onClick={() =>
                            h.push(`/vasu-templates/${template.id}`)
                          }
                        />
                        <IconButton
                          icon={faTrash}
                          disabled={template.documentCount > 0}
                          onClick={() => {
                            void deleteVasuTemplate(template.id).then(
                              loadSummaries
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
      </ContentArea>
    </Container>
  )
})
