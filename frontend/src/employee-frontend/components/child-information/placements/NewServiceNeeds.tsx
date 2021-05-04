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
import { NewServiceNeed, Placement } from '../../../types/child'
import { ChildContext } from '../../../state'
import { deleteNewServiceNeed } from '../../../api/child/new-service-needs'
import { DateRange, getGaps } from '../../../utils/date'
import NewServiceNeedEditorRow from './new-service-needs/NewServiceNeedEditorRow'
import NewServiceNeedReadRow from './new-service-needs/NewServiceNeedReadRow'
import MissingServiceNeedRow from './new-service-needs/MissingServiceNeedRow'

interface Props {
  placement: Placement
  reload: () => void
}

function NewServiceNeeds({ placement, reload }: Props) {
  const { serviceNeeds, type: placementType } = placement

  const { i18n } = useTranslation()
  const t = i18n.childInformation.placements.serviceNeeds

  const [creatingNew, setCreatingNew] = useState<boolean | LocalDate>(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const { serviceNeedOptions } = useContext(ChildContext)

  const gaps = useMemo(() => getGaps(placement.serviceNeeds, placement), [
    placement
  ])

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
        .filter((opt) => opt.validPlacementType === placementType)
        .map((opt) => ({
          label: opt.name,
          value: opt.id
        }))
    : []

  return (
    <div>
      <HeaderRow>
        <H4 noMargin>{t.title}</H4>
        <InlineButton
          onClick={() => setCreatingNew(true)}
          text={t.createNewBtn}
          icon={faPlus}
          disabled={creatingNew !== false || editingId !== null}
        />
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
            <NewServiceNeedEditorRow
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
                <NewServiceNeedEditorRow
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
                <NewServiceNeedReadRow
                  key={sn.id}
                  serviceNeed={sn}
                  onEdit={() => setEditingId(sn.id)}
                  onDelete={() => setDeletingId(sn.id)}
                  disabled={creatingNew !== false || editingId !== null}
                />
              )
            ) : creatingNew instanceof LocalDate &&
              sn.startDate.isEqual(creatingNew) ? (
              <NewServiceNeedEditorRow
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
            action: () => deleteNewServiceNeed(deletingId).then(reload),
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

type ServiceNeedOrGap = NewServiceNeed | DateRange

const HeaderRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`

export default NewServiceNeeds
