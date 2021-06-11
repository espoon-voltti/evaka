// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Loader from 'lib-components/atoms/Loader'
import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { useRestApi } from '../../../lib-common/utils/useRestApi'
import { AddButtonRow } from '../../../lib-components/atoms/buttons/AddButton'
import { StaticChip } from '../../../lib-components/atoms/Chip'
import { CollapsibleContentArea } from '../../../lib-components/layout/Container'
import { Table, Tbody, Td, Tr } from '../../../lib-components/layout/Table'
import { H2 } from '../../../lib-components/typography'
import colors from '../../../lib-customizations/common'
import { getVasuDocumentSummaries } from '../../api/vasu'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { UUID } from '../../types'
import { VasuDocumentState, VasuDocumentSummary } from '../../types/vasu'
import { formatDate } from '../../utils/date'

const StateCell = styled(Td)`
  display: flex;
  justify-content: flex-end;
`

const chipColors: Record<VasuDocumentState, string> = {
  DRAFT: colors.accents.emerald,
  CREATED: colors.accents.violet,
  REVIEWED: colors.primary,
  CLOSED: colors.greyscale.medium
}

type ChipLabels = Record<VasuDocumentState, string>

interface StateChipProps {
  state: VasuDocumentState
  labels: ChipLabels
}

function StateChip({ labels, state }: StateChipProps) {
  return <StaticChip color={chipColors[state]}>{labels[state]}</StaticChip>
}

interface Props {
  id: UUID
  startOpen: boolean
}

const VasuAndLeops = React.memo(function VasuAndLeops({
  id,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { vasus, setVasus } = useContext(ChildContext)

  const [open, setOpen] = useState(startOpen)

  const loadVasus = useRestApi(getVasuDocumentSummaries, setVasus)
  useEffect(() => loadVasus(id), [id, loadVasus])

  const getDates = ({ modifiedAt, publishedAt }: VasuDocumentSummary): string =>
    [
      `${i18n.childInformation.vasu.modified}: ${formatDate(modifiedAt)}`,
      ...(publishedAt
        ? [
            `${i18n.childInformation.vasu.published}: ${formatDate(
              publishedAt
            )}`.toLowerCase()
          ]
        : [])
    ].join(', ')

  const renderSummaries = () =>
    vasus
      .map((value) =>
        value.map((vasu) => (
          <Tr key={vasu.id}>
            <Td>{vasu.name}</Td>
            <Td>{getDates(vasu)}</Td>
            <StateCell>
              <StateChip
                state={vasu.state}
                labels={i18n.childInformation.vasu.states}
              />
            </StateCell>
            <Td>{i18n.common.edit}</Td>
          </Tr>
        ))
      )
      .getOrElse(null)

  const initializeNewVasuDocument = () => {
    // TODO implementation
  }
  return (
    <div>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.vasu.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
      >
        <AddButtonRow
          onClick={initializeNewVasuDocument}
          disabled={true}
          text={i18n.childInformation.vasu.createNew}
        />
        <Table>
          <Tbody>{renderSummaries()}</Tbody>
        </Table>
        {vasus.isLoading && <Loader />}
        {vasus.isFailure && <div>{i18n.common.loadingFailed}</div>}
      </CollapsibleContentArea>
    </div>
  )
})

export default VasuAndLeops
