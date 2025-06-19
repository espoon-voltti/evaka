// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import type { MutableRefObject } from 'react'
import { useMemo } from 'react'
import React, { useContext, useRef, useState } from 'react'

import DateRange from 'lib-common/date-range'
import type { Action } from 'lib-common/generated/action'
import type {
  AssistanceAction,
  AssistanceActionOption,
  AssistanceActionResponse
} from 'lib-common/generated/api-types/assistanceaction'
import {
  assistanceActionOptionCategories,
  type AssistanceActionOptionCategory
} from 'lib-common/generated/api-types/assistanceaction'
import { useMutationResult } from 'lib-common/query'
import { scrollToRef } from 'lib-common/utils/scrolling'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { LabelLike } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/employee'

import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { isActiveDateRange } from '../../../utils/date'
import LabelValueList from '../../common/LabelValueList'
import Toolbar from '../../common/Toolbar'
import ToolbarAccordion from '../../common/ToolbarAccordion'
import { deleteAssistanceActionMutation } from '../queries'

import AssistanceActionForm from './AssistanceActionForm'
import { DeleteConfirmation } from './DeleteConfirmation'

export interface Props {
  assistanceAction: AssistanceAction
  permittedActions: Action.AssistanceAction[]
  assistanceActions: AssistanceActionResponse[]
  assistanceActionOptions: AssistanceActionOption[]
  refSectionTop: MutableRefObject<HTMLElement | null>
}

export default React.memo(function AssistanceActionRow({
  assistanceAction,
  permittedActions,
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
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const refForm = useRef(null)

  const { mutateAsync: deleteAssistanceAction } = useMutationResult(
    deleteAssistanceActionMutation
  )

  const optionsByCategory = useMemo(
    () =>
      assistanceActionOptionCategories.reduce(
        (acc, category) => ({
          ...acc,
          [category]: sortBy(
            assistanceActionOptions.filter(
              (o) =>
                o.category === category &&
                assistanceAction.actions.includes(o.value)
            ),
            [(o) => o.displayOrder, (o) => o.nameFi]
          )
        }),
        {} as Record<AssistanceActionOptionCategory, AssistanceActionOption[]>
      ),
    [assistanceActionOptions, assistanceAction.actions]
  )

  return (
    <div>
      {uiMode === `remove-assistance-action-${assistanceAction.id}` && (
        <DeleteConfirmation
          title={i18n.childInformation.assistanceAction.removeConfirmation}
          range={
            new DateRange(assistanceAction.startDate, assistanceAction.endDate)
          }
          onSubmit={() =>
            deleteAssistanceAction({
              id: assistanceAction.id,
              childId: assistanceAction.childId
            })
          }
        />
      )}

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
                  <FixedSpaceColumn>
                    {assistanceActionOptionCategories.map((category) =>
                      optionsByCategory[category].length > 0 ? (
                        <FixedSpaceColumn spacing="zero" key={category}>
                          <LabelLike>
                            {
                              i18n.childInformation.assistanceAction.fields
                                .actionsByCategory[category]
                            }
                          </LabelLike>
                          <UnorderedList>
                            {optionsByCategory[category].map((option) => (
                              <li key={option.value}>{option.nameFi}</li>
                            ))}
                          </UnorderedList>
                        </FixedSpaceColumn>
                      ) : null
                    )}

                    {featureFlags.assistanceActionOther &&
                      assistanceAction.otherAction !== '' && (
                        <FixedSpaceColumn spacing="zero">
                          <LabelLike>
                            {
                              i18n.childInformation.assistanceAction.fields
                                .actionsByCategory.OTHER
                            }
                          </LabelLike>
                          <UnorderedList>
                            <li>
                              {
                                i18n.childInformation.assistanceAction.fields
                                  .actionTypes.OTHER
                              }
                              : {assistanceAction.otherAction}
                            </li>
                          </UnorderedList>
                        </FixedSpaceColumn>
                      )}
                  </FixedSpaceColumn>
                )
              }
            ]}
          />
        )}
      </ToolbarAccordion>
    </div>
  )
})
