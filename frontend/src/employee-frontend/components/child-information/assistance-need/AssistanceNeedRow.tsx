// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { MutableRefObject, useContext, useRef, useState } from 'react'

import { Action } from 'lib-common/generated/action'
import { AssistanceBasisOption } from 'lib-common/generated/api-types/assistanceneed'
import { formatDecimal } from 'lib-common/utils/number'
import { scrollToRef } from 'lib-common/utils/scrolling'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faQuestion } from 'lib-icons'

import { removeAssistanceNeed } from '../../../api/child/assistance-needs'
import AssistanceNeedForm from '../../../components/child-information/assistance-need/AssistanceNeedForm'
import LabelValueList from '../../../components/common/LabelValueList'
import Toolbar from '../../../components/common/Toolbar'
import ToolbarAccordion from '../../../components/common/ToolbarAccordion'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { AssistanceNeed, AssistanceNeedResponse } from '../../../types/child'
import { isActiveDateRange } from '../../../utils/date'

export interface Props {
  assistanceNeed: AssistanceNeed
  permittedActions: Action.AssistanceNeed[]
  onReload: () => void
  assistanceNeeds: AssistanceNeedResponse[]
  assistanceBasisOptions: AssistanceBasisOption[]
  refSectionTop: MutableRefObject<HTMLElement | null>
}

export default React.memo(function AssistanceNeedRow({
  assistanceNeed,
  permittedActions,
  onReload,
  assistanceNeeds,
  assistanceBasisOptions,
  refSectionTop
}: Props) {
  const { i18n } = useTranslation()
  const expandedAtStart = isActiveDateRange(
    assistanceNeed.startDate,
    assistanceNeed.endDate
  )
  const [toggled, setToggled] = useState(expandedAtStart)
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const refForm = useRef(null)

  const renderDeleteConfirmation = () => (
    <InfoModal
      type="warning"
      title={i18n.childInformation.assistanceNeed.removeConfirmation}
      text={`${
        i18n.common.period
      } ${assistanceNeed.startDate.format()} - ${assistanceNeed.endDate.format()}`}
      icon={faQuestion}
      reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
      resolve={{
        action: () =>
          removeAssistanceNeed(assistanceNeed.id).then(() => {
            clearUiMode()
            onReload()
          }),
        label: i18n.common.remove
      }}
    />
  )

  return (
    <div>
      {uiMode === `remove-assistance-need-${assistanceNeed.id}` &&
        renderDeleteConfirmation()}

      <ToolbarAccordion
        title={`${
          i18n.childInformation.assistanceNeed.fields.dateRange
        } ${assistanceNeed.startDate.format()} - ${assistanceNeed.endDate.format()}`}
        onToggle={() => setToggled((prev) => !prev)}
        open={toggled}
        data-qa="assistance-need-row"
        toolbar={
          <Toolbar
            dateRange={assistanceNeed}
            onCopy={() => {
              toggleUiMode(`duplicate-assistance-need_${assistanceNeed.id}`)
              scrollToRef(refSectionTop)
            }}
            editable={permittedActions.includes('UPDATE')}
            onEdit={() => {
              toggleUiMode(`edit-assistance-need-${assistanceNeed.id}`)
              setToggled(true)
              scrollToRef(refForm)
            }}
            deletable={permittedActions.includes('DELETE')}
            onDelete={() =>
              toggleUiMode(`remove-assistance-need-${assistanceNeed.id}`)
            }
            disableAll={!!uiMode && uiMode.startsWith('edit-assistance-need')}
          />
        }
      >
        {uiMode === `edit-assistance-need-${assistanceNeed.id}` ? (
          <div ref={refForm}>
            <AssistanceNeedForm
              assistanceNeed={assistanceNeed}
              onReload={onReload}
              assistanceNeeds={assistanceNeeds}
              assistanceBasisOptions={assistanceBasisOptions}
            />
          </div>
        ) : (
          <LabelValueList
            spacing="large"
            contents={[
              {
                label: i18n.childInformation.assistanceNeed.fields.dateRange,
                value: `${assistanceNeed.startDate.format()} - ${assistanceNeed.endDate.format()}`
              },
              {
                label:
                  i18n.childInformation.assistanceNeed.fields.capacityFactor,
                value: (
                  <ExpandingInfo
                    info={
                      i18n.childInformation.assistanceNeed.fields
                        .capacityFactorInfo
                    }
                    ariaLabel=""
                    width="full"
                  >
                    <span data-qa="assistance-need-multiplier">
                      {formatDecimal(assistanceNeed.capacityFactor)}
                    </span>
                  </ExpandingInfo>
                )
              },
              {
                label: i18n.childInformation.assistanceNeed.fields.bases,
                value: (
                  <ul>
                    {assistanceBasisOptions.map(
                      (basis) =>
                        assistanceNeed.bases.has(basis.value) && (
                          <li key={basis.value}>{basis.nameFi}</li>
                        )
                    )}
                  </ul>
                )
              }
            ]}
          />
        )}
      </ToolbarAccordion>
    </div>
  )
})
