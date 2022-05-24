// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import { useApiState } from 'lib-common/utils/useRestApi'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import {
  CollapsibleContentArea,
  ContentArea
} from 'lib-components/layout/Container'
import { H2, H3 } from 'lib-components/typography'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'

import { getGuardianChildVasuSummaries } from './vasu/api'
import { VasuStateChip } from './vasu/components/VasuStateChip'

const VasuTable = styled.table`
  width: 100%;
  padding-left 16px;
`

const VasuTr = styled.tr``

const VasuTd = styled.td`
  padding-bottom: 16px;
`

const StateCell = styled(VasuTd)`
  display: flex;
  justify-content: flex-start;
`

export default React.memo(function CitizenVasuAndLeops() {
  const i18n = useTranslation()
  const [open, setOpen] = useState(true)
  const [vasus] = useApiState(() => getGuardianChildVasuSummaries(), [])
  const navigate = useNavigate()

  return (
    <>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.vasu.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="vasu-and-leops-collapsible"
      >
        {renderResult(vasus, (vasus) => (
          <ContentArea opaque paddingVertical="s" paddingHorizontal="zero">
            {vasus.map(
              (summary) =>
                summary.vasuDocumentsSummary.length > 0 && (
                  <div key={summary.child.id}>
                    <H3>{`${summary.child.firstName} ${summary.child.lastName}`}</H3>
                    <VasuTable key={summary.child.id}>
                      <tbody>
                        {summary.vasuDocumentsSummary.map((vasu) => (
                          <VasuTr key={vasu.id} data-qa={`vasu-${vasu.id}`}>
                            <VasuTd>
                              <InlineButton
                                onClick={() => navigate(`/vasu/${vasu.id}`)}
                                text={vasu.name}
                                data-qa="vasu-link"
                              />
                            </VasuTd>
                            <StateCell data-qa={`state-chip-${vasu.id}`}>
                              <VasuStateChip
                                state={vasu.documentState}
                                labels={i18n.vasu.states}
                              />
                            </StateCell>
                            <VasuTd data-qa={`published-at-${vasu.id}`}>
                              {vasu.publishedAt
                                ? LocalDate.fromSystemTzDate(
                                    vasu.publishedAt
                                  ).format()
                                : ''}
                            </VasuTd>
                          </VasuTr>
                        ))}
                      </tbody>
                    </VasuTable>
                  </div>
                )
            )}
          </ContentArea>
        ))}
      </CollapsibleContentArea>
    </>
  )
})
