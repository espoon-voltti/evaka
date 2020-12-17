// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { MutableRefObject, useContext, useRef, useState } from 'react'
import { useTranslation } from '~/state/i18n'
import { AssistanceNeed } from '~types/child'
import { UIContext } from '~state/ui'
import InfoBall from '~components/common/InfoBall'
import AssistanceNeedForm from '~components/child-information/assistance-need/AssistanceNeedForm'
import { faQuestion } from '@evaka/lib-icons'
import ToolbarAccordion from '~components/common/ToolbarAccordion'
import { isActiveDateRange } from '~/utils/date'
import InfoModal from '~components/common/InfoModal'
import { formatDecimal } from '~components/utils'

import { formatParagraphs } from '~utils/html-utils'
import LabelValueList from '~components/common/LabelValueList'
import { ASSISTANCE_BASIS_LIST } from '~constants'
import Toolbar from 'components/shared/molecules/Toolbar'
import { scrollToRef } from 'utils'
import { removeAssistanceNeed } from 'api/child/assistance-needs'

export interface Props {
  assistanceNeed: AssistanceNeed
  onReload: () => undefined | void
  refSectionTop: MutableRefObject<HTMLElement | null>
}

function AssistanceNeedRow({ assistanceNeed, onReload, refSectionTop }: Props) {
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
      iconColour={'orange'}
      title={i18n.childInformation.assistanceNeed.removeConfirmation}
      text={`${
        i18n.common.period
      } ${assistanceNeed.startDate.format()} - ${assistanceNeed.endDate.format()}`}
      resolveLabel={i18n.common.remove}
      rejectLabel={i18n.common.cancel}
      icon={faQuestion}
      reject={() => clearUiMode()}
      resolve={() =>
        removeAssistanceNeed(assistanceNeed.id).then(() => {
          clearUiMode()
          onReload()
        })
      }
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
        toolbar={
          <Toolbar
            dateRange={assistanceNeed}
            onCopy={() => {
              toggleUiMode(`duplicate-assistance-need_${assistanceNeed.id}`)
              scrollToRef(refSectionTop)
            }}
            onEdit={() => {
              toggleUiMode(`edit-assistance-need-${assistanceNeed.id}`)
              setToggled(true)
              scrollToRef(refForm)
            }}
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
                  <span>
                    {formatDecimal(assistanceNeed.capacityFactor)}
                    <InfoBall
                      text={
                        i18n.childInformation.assistanceNeed.fields
                          .capacityFactorInfo
                      }
                      inline
                    />
                  </span>
                )
              },
              {
                label: i18n.childInformation.assistanceNeed.fields.description,
                value: formatParagraphs(assistanceNeed.description)
              },
              {
                label: i18n.childInformation.assistanceNeed.fields.bases,
                value: (
                  <ul>
                    {ASSISTANCE_BASIS_LIST.filter(
                      (basis) => basis != 'OTHER'
                    ).map(
                      (basis) =>
                        assistanceNeed.bases.has(basis) && (
                          <li key={basis}>
                            {
                              i18n.childInformation.assistanceNeed.fields
                                .basisTypes[basis]
                            }
                          </li>
                        )
                    )}
                    {assistanceNeed.bases.has('OTHER') && (
                      <li>
                        {
                          i18n.childInformation.assistanceNeed.fields.basisTypes
                            .OTHER
                        }
                        : {assistanceNeed.otherBasis}
                      </li>
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

export default AssistanceNeedRow
