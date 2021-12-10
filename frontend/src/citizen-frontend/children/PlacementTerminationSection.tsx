// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChildPlacement } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label, P } from 'lib-components/typography'
import React, { useCallback, useState } from 'react'
import { renderResult } from '../async-rendering'
import { useLang, useTranslation } from '../localization'
import { getPlacements } from './api'

interface PlacementTerminationProps {
  childId: UUID
}

export default React.memo(function PlacementTerminationSection({
  childId
}: PlacementTerminationProps) {
  const [lang] = useLang()
  const t = useTranslation()
  const [placementsResponse, refreshPlacements] = useApiState(
    () => getPlacements(childId),
    [childId]
  )

  const getPlacementLabel = (p: ChildPlacement) =>
    [
      t.placement.type[p.placementType],
      p.placementUnitName,
      t.children.placementTermination.until(p.placementEndDate.format())
    ].join(', ')

  const [terminationDate, setTerminationDate] = useState('')
  const [selectedPlacements, setSelectedPlacements] = useState<
    ChildPlacement[]
  >([])

  const isValidDate = useCallback(
    (date: LocalDate): boolean =>
      date.isEqualOrAfter(LocalDate.today()) &&
      selectedPlacements.every((p) => p.placementEndDate.isAfter(date)),
    [selectedPlacements]
  )

  // TODO terminate api call
  const onSubmit = useCallback(() => Promise.reject(), [])

  const onSuccess = useCallback(() => {
    setSelectedPlacements([])
    refreshPlacements()
  }, [refreshPlacements])

  const isValid = true // TODO validate form

  return (
    <CollapsibleSection
      title={t.children.placementTermination.title}
      startCollapsed
      fitted
    >
      {renderResult(placementsResponse, (placements) => (
        <FixedSpaceColumn>
          <P>{t.children.placementTermination.description}</P>
          <div>
            <Label>{t.children.placementTermination.choosePlacement}</Label>
            {placements.map((p) => (
              <Checkbox
                key={p.placementId}
                label={getPlacementLabel(p)}
                checked={
                  !!selectedPlacements.find(
                    (p2) => p2.placementId === p.placementId
                  )
                }
                onChange={(checked) =>
                  setSelectedPlacements((prev) =>
                    checked
                      ? [...prev, p]
                      : prev.filter((p2) => p2.placementId !== p.placementId)
                  )
                }
              />
            ))}
          </div>
          <div>
            <ExpandingInfo
              info={t.children.placementTermination.lastDayInfo}
              ariaLabel={t.common.openExpandingInfo}
            >
              <Label>{t.children.placementTermination.lastDayOfPresence}</Label>
            </ExpandingInfo>
            <DatePicker
              hideErrorsBeforeTouched
              required
              locale={lang}
              isValidDate={isValidDate}
              date={terminationDate}
              onChange={setTerminationDate}
              openAbove
            />
          </div>

          <AsyncButton
            primary
            text={t.children.placementTermination.terminate}
            disabled={!isValid}
            onClick={onSubmit}
            onSuccess={onSuccess}
          />
        </FixedSpaceColumn>
      ))}
    </CollapsibleSection>
  )
})
