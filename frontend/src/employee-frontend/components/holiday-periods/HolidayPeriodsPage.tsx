// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useCallback, useState } from 'react'
import { useHistory } from 'react-router'

import { Translatable } from 'lib-common/generated/api-types/shared'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H1, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { deleteHolidayPeriod, getHolidayPeriods } from './api'

const translatableKeys: (keyof Translatable)[] = ['fi', 'sv', 'en']

export default React.memo(function HolidayPeriodsPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const [holidayPeriods, refresh] = useApiState(getHolidayPeriods, [])

  const [periodToDelete, setPeriodToDelete] = useState<UUID>()
  const onDelete = useCallback(
    () =>
      periodToDelete ? deleteHolidayPeriod(periodToDelete) : Promise.reject(),
    [periodToDelete]
  )

  const navigateToNew = useCallback(() => {
    history.push('/holiday-periods/new')
  }, [history])

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.titles.holidayPeriods}</H1>

        <AddButtonRow
          onClick={navigateToNew}
          text={i18n.common.addNew}
          data-qa="add-button"
        />
        {renderResult(holidayPeriods, (data) => (
          <Table>
            <Thead>
              <Tr>
                <Th>{i18n.holidayPeriods.period}</Th>
                <Th>{i18n.holidayPeriods.reservationDeadline}</Th>
                <Th>{i18n.holidayPeriods.showReservationBannerFrom}</Th>
                <Th>{i18n.holidayPeriods.description}</Th>
                <Th>{i18n.holidayPeriods.descriptionLink}</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {data.map((holiday) => (
                <Tr key={holiday.id} data-qa="holiday-period-row">
                  <Td data-qa="holiday-period">{holiday.period.format()}</Td>
                  <Td>{holiday.reservationDeadline.format()}</Td>
                  <Td>{holiday.showReservationBannerFrom.format()}</Td>
                  <Td data-qa="descriptions">
                    {translatableKeys.map(
                      (lang, i) =>
                        holiday.description[lang] && (
                          <Fragment key={`description-${lang}`}>
                            {i > 0 && <Gap />}
                            <P noMargin>{holiday.description[lang]}</P>
                          </Fragment>
                        )
                    )}
                  </Td>
                  <Td data-qa="links">
                    {translatableKeys.map(
                      (lang, i) =>
                        holiday.descriptionLink[lang] && (
                          <Fragment key={`link-${lang}`}>
                            {i > 0 && <Gap />}
                            <ExternalLink
                              text={holiday.descriptionLink[lang]}
                              href={holiday.descriptionLink[lang]}
                              newTab
                            />
                          </Fragment>
                        )
                    )}
                  </Td>
                  <Td>
                    <FixedSpaceRow spacing="s">
                      <IconButton
                        icon={faPen}
                        data-qa="btn-edit"
                        onClick={() =>
                          history.push(`/holiday-periods/${holiday.id}`)
                        }
                      />
                      <IconButton
                        icon={faTrash}
                        data-qa="btn-delete"
                        onClick={() => setPeriodToDelete(holiday.id)}
                      />
                    </FixedSpaceRow>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        ))}

        {periodToDelete && (
          <AsyncFormModal
            type="warning"
            title={i18n.holidayPeriods.confirmDelete}
            icon={faQuestion}
            rejectAction={() => setPeriodToDelete(undefined)}
            rejectLabel={i18n.common.cancel}
            resolveAction={onDelete}
            resolveLabel={i18n.common.remove}
            onSuccess={() => {
              setPeriodToDelete(undefined)
              refresh()
            }}
          />
        )}
      </ContentArea>
    </Container>
  )
})
