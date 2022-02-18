// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { MutableRefObject, useContext, useRef, useState } from 'react'

import {
  AssistanceAction,
  AssistanceActionResponse
} from 'employee-frontend/types/child'
import { Action } from 'lib-common/generated/action'
import { AssistanceActionOption } from 'lib-common/generated/api-types/assistanceaction'
import { scrollToRef } from 'lib-common/utils/scrolling'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { assistanceMeasures, featureFlags } from 'lib-customizations/employee'
import { faQuestion } from 'lib-icons'

import { removeAssistanceAction } from '../../../api/child/assistance-actions'
import AssistanceActionForm from '../../../components/child-information/assistance-action/AssistanceActionForm'
import LabelValueList from '../../../components/common/LabelValueList'
import Toolbar from '../../../components/common/Toolbar'
import ToolbarAccordion from '../../../components/common/ToolbarAccordion'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { isActiveDateRange } from '../../../utils/date'

export interface Props {
  assistanceAction: AssistanceAction
  permittedActions: Action.AssistanceAction[]
  onReload: () => undefined | void
  assistanceActions: AssistanceActionResponse[]
  assistanceActionOptions: AssistanceActionOption[]
  refSectionTop: MutableRefObject<HTMLElement | null>
}

export default React.memo(function AssistanceActionRow({
  assistanceAction,
  permittedActions,
  onReload,
  assistanceActions,
  assistanceActionOptions,
  refSectionTop
}: Props) {
  const { i18n } = useTranslation()
  const expandedAtStart = isActiveDateRange(
    assistanceAction.startDate,
    assistanceAction.endDate
  )
  const [toggled, setToggled] = useState(expandedAtStart)
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const refForm = useRef(null)

  const renderDeleteConfirmation = () => (
    <InfoModal
      type="warning"
      title={i18n.childInformation.assistanceAction.removeConfirmation}
      text={`${
        i18n.common.period
      } ${assistanceAction.startDate.format()} - ${assistanceAction.endDate.format()}`}
      icon={faQuestion}
      reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
      resolve={{
        action: () =>
          removeAssistanceAction(assistanceAction.id).then(() => {
            clearUiMode()
            onReload()
          }),
        label: i18n.common.remove
      }}
    />
  )

  return (
    <div>
      {uiMode === `remove-assistance-action-${assistanceAction.id}` &&
        renderDeleteConfirmation()}

      <ToolbarAccordion
        title={`${
          i18n.childInformation.assistanceAction.fields.dateRange
        } ${assistanceAction.startDate.format()} - ${assistanceAction.endDate.format()}`}
        onToggle={() => setToggled((prev) => !prev)}
        open={toggled}
        toolbar={
          <Toolbar
            dateRange={assistanceAction}
            onCopy={() => {
              toggleUiMode(`duplicate-assistance-action_${assistanceAction.id}`)
              scrollToRef(refSectionTop)
            }}
            editable={permittedActions.includes('UPDATE')}
            onEdit={() => {
              toggleUiMode(`edit-assistance-action-${assistanceAction.id}`)
              setToggled(true)
              scrollToRef(refForm)
            }}
            deletable={permittedActions.includes('DELETE')}
            onDelete={() =>
              toggleUiMode(`remove-assistance-action-${assistanceAction.id}`)
            }
            disableAll={!!uiMode && uiMode.startsWith('edit-assistance-action')}
          />
        }
      >
        {uiMode === `edit-assistance-action-${assistanceAction.id}` ? (
          <div ref={refForm}>
            <AssistanceActionForm
              assistanceAction={assistanceAction}
              onReload={onReload}
              assistanceActions={assistanceActions}
              assistanceActionOptions={assistanceActionOptions}
            />
          </div>
        ) : (
          <LabelValueList
            spacing="large"
            contents={[
              {
                label: i18n.childInformation.assistanceAction.fields.dateRange,
                value: `${assistanceAction.startDate.format()} - ${assistanceAction.endDate.format()}`
              },
              {
                label: i18n.childInformation.assistanceAction.fields.actions,
                value: (
                  <ul>
                    {assistanceActionOptions.map(
                      (option) =>
                        assistanceAction.actions.has(option.value) && (
                          <li key={option.value}>{option.nameFi}</li>
                        )
                    )}
                    {featureFlags.assistanceActionOtherEnabled &&
                    assistanceAction.otherAction !== '' ? (
                      <li>
                        {
                          i18n.childInformation.assistanceAction.fields
                            .actionTypes.OTHER
                        }
                        : {assistanceAction.otherAction}
                      </li>
                    ) : null}
                  </ul>
                )
              },
              assistanceMeasures.length > 0 && {
                label: i18n.childInformation.assistanceAction.fields.measures,
                value: (
                  <ul>
                    {assistanceMeasures.map(
                      (measure) =>
                        assistanceAction.measures.has(measure) && (
                          <li key={measure}>
                            {
                              i18n.childInformation.assistanceAction.fields
                                .measureTypes[measure]
                            }
                          </li>
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
