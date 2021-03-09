// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { MutableRefObject, useContext, useRef, useState } from 'react'
import { useTranslation } from '../../../state/i18n'
import { AssistanceAction } from '../../../types/child'
import { UIContext } from '../../../state/ui'
import AssistanceActionForm from '../../../components/child-information/assistance-action/AssistanceActionForm'
import { faQuestion } from '@evaka/lib-icons'
import ToolbarAccordion from '../../../components/common/ToolbarAccordion'
import { isActiveDateRange } from '../../../utils/date'
import InfoModal from '@evaka/lib-components/molecules/modals/InfoModal'

import LabelValueList from '../../../components/common/LabelValueList'
import {
  ASSISTANCE_ACTION_TYPE_LIST,
  ASSISTANCE_MEASURE_LIST
} from '../../../constants'
import Toolbar from '../../../components/common/Toolbar'
import { scrollToRef } from '../../../utils'
import { removeAssistanceAction } from '../../../api/child/assistance-actions'

export interface Props {
  assistanceAction: AssistanceAction
  onReload: () => undefined | void
  refSectionTop: MutableRefObject<HTMLElement | null>
}

function AssistanceActionRow({
  assistanceAction,
  onReload,
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
      iconColour={'orange'}
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
            onEdit={() => {
              toggleUiMode(`edit-assistance-action-${assistanceAction.id}`)
              setToggled(true)
              scrollToRef(refForm)
            }}
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
                    {ASSISTANCE_ACTION_TYPE_LIST.filter(
                      (action) => action != 'OTHER'
                    ).map(
                      (action) =>
                        assistanceAction.actions.has(action) && (
                          <li key={action}>
                            {
                              i18n.childInformation.assistanceAction.fields
                                .actionTypes[action]
                            }
                          </li>
                        )
                    )}
                    {assistanceAction.actions.has('OTHER') && (
                      <li>
                        {
                          i18n.childInformation.assistanceAction.fields
                            .actionTypes.OTHER
                        }
                        : {assistanceAction.otherAction}
                      </li>
                    )}
                  </ul>
                )
              },
              {
                label: i18n.childInformation.assistanceAction.fields.measures,
                value: (
                  <ul>
                    {ASSISTANCE_MEASURE_LIST.map(
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
}

export default AssistanceActionRow
