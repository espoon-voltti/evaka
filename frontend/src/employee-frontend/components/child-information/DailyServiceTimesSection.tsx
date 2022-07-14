// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useState } from 'react'

import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H2, H4, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { getChildDailyServiceTimes } from '../../api/child/daily-service-times'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { DailyServiceTimesCreationForm } from './daily-service-times/DailyServiceTimesForms'
import DailyServiceTimesRow from './daily-service-times/DailyServiceTimesRow'

interface Props {
  id: UUID
  startOpen: boolean
}

export default React.memo(function DailyServiceTimesSection({
  id,
  startOpen
}: Props) {
  const { i18n } = useTranslation()

  const [open, setOpen] = useState(startOpen)

  const [apiData, loadData] = useApiState(
    () => getChildDailyServiceTimes(id),
    [id]
  )

  const [creationFormOpen, setCreationFormOpen] = useState(false)

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.childInformation.dailyServiceTimes.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="child-daily-service-times-collapsible"
    >
      <P>
        {i18n.childInformation.dailyServiceTimes.info}
        <br />
        {i18n.childInformation.dailyServiceTimes.info2}
      </P>

      <FixedSpaceRow justifyContent="flex-end">
        <AddButton
          flipped
          text={i18n.childInformation.dailyServiceTimes.create}
          onClick={() => {
            setCreationFormOpen(true)
          }}
          disabled={creationFormOpen}
          data-qa="create-daily-service-times"
        />
      </FixedSpaceRow>

      {creationFormOpen && (
        <>
          <HorizontalLine slim dashed />
          <H4>{i18n.childInformation.dailyServiceTimes.createNewTimes}</H4>
          <DailyServiceTimesCreationForm
            onClose={(shouldRefresh) => {
              setCreationFormOpen(false)

              if (shouldRefresh) {
                loadData()
              }
            }}
            childId={id}
          />
        </>
      )}

      <Gap size="m" />

      {renderResult(apiData, (dailyServiceTimesList) => (
        <Table>
          <Tbody>
            {orderBy(
              dailyServiceTimesList,
              ({ dailyServiceTimes: { times } }) => times.validityPeriod.start,
              ['desc']
            ).map(({ permittedActions, dailyServiceTimes: { id, times } }) => (
              <DailyServiceTimesRow
                key={id}
                times={times}
                permittedActions={permittedActions}
              />
            ))}
          </Tbody>
        </Table>
      ))}
    </CollapsibleContentArea>
  )
})
