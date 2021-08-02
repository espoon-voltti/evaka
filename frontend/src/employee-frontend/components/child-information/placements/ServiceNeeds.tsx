// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import styled from 'styled-components'
import _ from 'lodash'
import { faPlus, faQuestion } from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H4 } from 'lib-components/typography'
import { useTranslation } from '../../../state/i18n'
import { ServiceNeed, Placement } from '../../../types/child'
import { ChildContext } from '../../../state'
import { deleteServiceNeed } from '../../../api/child/service-needs'
import { DateRange } from '../../../utils/date'
import { RequireRole } from '../../../utils/roles'
import ServiceNeedEditorRow from './service-needs/ServiceNeedEditorRow'
import ServiceNeedReadRow from './service-needs/ServiceNeedReadRow'
import MissingServiceNeedRow from './service-needs/MissingServiceNeedRow'
import FiniteDateRange from 'lib-common/finite-date-range'

interface Props {
  placement: Placement
  reload: () => void
}

function ServiceNeeds({ placement, reload }: Props) {
  const { serviceNeeds, type: placementType } = placement

  const { i18n } = useTranslation()
  const t = i18n.childInformation.placements.serviceNeeds

  const [creatingNew, setCreatingNew] = useState<boolean | LocalDate>(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const { serviceNeedOptions } = useContext(ChildContext)

  const gaps: DateRange[] = useMemo(
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

  if (
    serviceNeedOptions.isSuccess &&
    serviceNeedOptions.value.find(
      (opt) => opt.validPlacementType === placementType && !opt.defaultOption
    ) === undefined
  ) {
    // only default option exists so service needs are not relevant
    // and do not need to be rendered
    return null
  }

  const options = serviceNeedOptions.isSuccess
    ? serviceNeedOptions.value
        .filter(
          (opt) =>
            opt.validPlacementType === placementType && !opt.defaultOption
        )
        .map((opt) => ({
          label: opt.name,
          value: opt.id
        }))
    : []

  return (
    <div>
      <HeaderRow>
        <H4 noMargin>{t.title}</H4>
        <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR']}>
          <InlineButton
            onClick={() => setCreatingNew(true)}
            text={t.createNewBtn}
            icon={faPlus}
            disabled={creatingNew !== false || editingId !== null}
          />
        </RequireRole>
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
                shiftCare: false
              }}
              onSuccess={() => {
                setCreatingNew(false)
                reload()
              }}
              onCancel={() => setCreatingNew(false)}
            />
          )}

          {_.orderBy(rows, ['startDate'], ['desc']).map((sn) =>
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
                  shiftCare: false
                }}
                onSuccess={() => {
                  setCreatingNew(false)
                  reload()
                }}
                onCancel={() => setCreatingNew(false)}
              />
            ) : (
              <MissingServiceNeedRow
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

      {deletingId && (
        <InfoModal
          title={t.deleteServiceNeed.confirmTitle}
          iconColour={'orange'}
          icon={faQuestion}
          resolve={{
            action: () => deleteServiceNeed(deletingId).then(reload),
            label: t.deleteServiceNeed.btn
          }}
          reject={{
            action: () => setDeletingId(null),
            label: i18n.common.cancel
          }}
        />
      )}
    </div>
  )
}

type ServiceNeedOrGap = ServiceNeed | DateRange

const HeaderRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`

export default ServiceNeeds
