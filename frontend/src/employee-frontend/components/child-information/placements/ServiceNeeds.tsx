// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { Action } from 'lib-common/generated/action'
import { DaycarePlacementWithDetails } from 'lib-common/generated/api-types/placement'
import {
  ServiceNeed,
  ServiceNeedOption
} from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H4 } from 'lib-components/typography'
import { faPlus, faQuestion } from 'lib-icons'

import { deleteServiceNeed } from '../../../generated/api-clients/serviceneed'
import { useTranslation } from '../../../state/i18n'
import { DateRange } from '../../../utils/date'

import MissingServiceNeedRow from './service-needs/MissingServiceNeedRow'
import ServiceNeedEditorRow from './service-needs/ServiceNeedEditorRow'
import ServiceNeedReadRow from './service-needs/ServiceNeedReadRow'

const deleteServiceNeedResult = wrapResult(deleteServiceNeed)

interface Props {
  placement: DaycarePlacementWithDetails
  permittedPlacementActions: Action.Placement[]
  permittedServiceNeedActions: Record<UUID, Action.ServiceNeed[]>
  reload: () => void
  serviceNeedOptions: ServiceNeedOption[]
}

export default React.memo(function ServiceNeeds({
  placement,
  permittedPlacementActions,
  permittedServiceNeedActions,
  reload,
  serviceNeedOptions
}: Props) {
  const { serviceNeeds, type: placementType } = placement

  const { i18n } = useTranslation()
  const t = i18n.childInformation.placements.serviceNeeds

  const [creatingNew, setCreatingNew] = useState<boolean | LocalDate>(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const gaps = useMemo(
    () =>
      new FiniteDateRange(placement.startDate, placement.endDate)
        .getGaps(
          placement.serviceNeeds.map(
            (sn) => new FiniteDateRange(sn.startDate, sn.endDate)
          )
        )
        .map((gap) => ({ startDate: gap.start, endDate: gap.end })),
    [placement]
  )

  const rows: ServiceNeedOrGap[] = [...placement.serviceNeeds, ...gaps]

  const options = serviceNeedOptions.filter(
    (option) =>
      option.validPlacementType === placementType && !option.defaultOption
  )

  const placementHasNonDefaultServiceNeedOptions = serviceNeedOptions.some(
    (opt) => opt.validPlacementType === placementType && !opt.defaultOption
  )

  const createAllowed = permittedPlacementActions.includes(
    'CREATE_SERVICE_NEED'
  )

  // if only default option exists service needs are not relevant and do not need to be rendered
  return placementHasNonDefaultServiceNeedOptions ? (
    <div>
      <HeaderRow>
        <H4 noMargin>{t.title}</H4>
        {createAllowed && (
          <InlineButton
            onClick={() => setCreatingNew(true)}
            text={t.createNewBtn}
            icon={faPlus}
            disabled={creatingNew !== false || editingId !== null}
          />
        )}
      </HeaderRow>

      <Table>
        <Thead>
          <Tr>
            <Th>{t.period}</Th>
            <Th>{t.description}</Th>
            <Th>{t.shiftCare}</Th>
            <Th>{t.confirmed}</Th>
            <Th />
          </Tr>
        </Thead>
        <Tbody>
          {creatingNew === true && (
            <ServiceNeedEditorRow
              placement={placement}
              options={options}
              initialForm={{
                startDate: serviceNeeds.length
                  ? undefined
                  : placement.startDate,
                endDate: placement.endDate,
                optionId: undefined,
                shiftCare: 'NONE'
              }}
              onSuccess={() => {
                setCreatingNew(false)
                reload()
              }}
              onCancel={() => setCreatingNew(false)}
            />
          )}

          {orderBy(rows, ['startDate'], ['desc']).map((sn) =>
            'id' in sn ? (
              editingId === sn.id ? (
                <ServiceNeedEditorRow
                  key={sn.id}
                  placement={placement}
                  options={options}
                  initialForm={{
                    startDate: sn.startDate,
                    endDate: sn.endDate,
                    optionId: sn.option.id,
                    shiftCare: sn.shiftCare
                  }}
                  onSuccess={() => {
                    setEditingId(null)
                    reload()
                  }}
                  onCancel={() => setEditingId(null)}
                  editingId={editingId}
                />
              ) : (
                <ServiceNeedReadRow
                  key={sn.id}
                  serviceNeed={sn}
                  permittedActions={permittedServiceNeedActions[sn.id] ?? []}
                  onEdit={() => setEditingId(sn.id)}
                  onDelete={() => setDeletingId(sn.id)}
                  disabled={creatingNew !== false || editingId !== null}
                />
              )
            ) : creatingNew instanceof LocalDate &&
              sn.startDate.isEqual(creatingNew) ? (
              <ServiceNeedEditorRow
                key={sn.startDate.toJSON()}
                placement={placement}
                options={options}
                initialForm={{
                  startDate: sn.startDate,
                  endDate: sn.endDate,
                  optionId: undefined,
                  shiftCare: 'NONE'
                }}
                onSuccess={() => {
                  setCreatingNew(false)
                  reload()
                }}
                onCancel={() => setCreatingNew(false)}
              />
            ) : (
              <MissingServiceNeedRow
                createAllowed={createAllowed}
                key={sn.startDate.toJSON()}
                startDate={sn.startDate}
                endDate={sn.endDate}
                onEdit={() => setCreatingNew(sn.startDate)}
                disabled={creatingNew !== false || editingId !== null}
              />
            )
          )}
        </Tbody>
      </Table>

      {!!deletingId && (
        <InfoModal
          title={t.deleteServiceNeed.confirmTitle}
          type="warning"
          icon={faQuestion}
          resolve={{
            action: () =>
              deleteServiceNeedResult({ id: deletingId })
                .then(reload)
                .finally(() => setDeletingId(null)),
            label: t.deleteServiceNeed.btn
          }}
          reject={{
            action: () => setDeletingId(null),
            label: i18n.common.cancel
          }}
        />
      )}
    </div>
  ) : null
})

type ServiceNeedOrGap = ServiceNeed | DateRange

const HeaderRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`
