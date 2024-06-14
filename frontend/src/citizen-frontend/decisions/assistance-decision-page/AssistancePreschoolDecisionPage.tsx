// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect } from 'react'

import Footer from 'citizen-frontend/Footer'
import { renderResult } from 'citizen-frontend/async-rendering'
import { useTranslation } from 'citizen-frontend/localization'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import AssistanceNeedPreschoolDecisionReadOnly from 'lib-components/assistance-need-decision/AssistanceNeedPreschoolDecisionReadOnly'
import LegacyInlineButton from 'lib-components/atoms/buttons/LegacyInlineButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Content from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import { translations } from 'lib-customizations/citizen'
import colors from 'lib-customizations/common'
import { faArrowDownToLine } from 'lib-icons'

import {
  assistanceNeedPreschoolDecisionQuery,
  markAssistanceNeedPreschoolDecisionAsReadMutation
} from './queries-preschool'

export default React.memo(function AssistanceNeedPreschoolDecisionPage() {
  const { id } = useRouteParams(['id'])

  const decision = useQueryResult(assistanceNeedPreschoolDecisionQuery({ id }))
  const i18n = useTranslation()

  const { mutate: markAssistanceNeedDecisionAsRead } = useMutationResult(
    markAssistanceNeedPreschoolDecisionAsReadMutation
  )

  useEffect(() => {
    markAssistanceNeedDecisionAsRead({ id })
  }, [id, markAssistanceNeedDecisionAsRead])

  return (
    <>
      <Content>
        <Gap size="s" />
        <FixedSpaceRow justifyContent="space-between">
          <ReturnButton label={i18n.common.return} />
          <FixedSpaceRow>
            <LegacyInlineButton
              icon={faArrowDownToLine}
              text={i18n.common.download}
              onClick={() => {
                window.open(
                  `/api/application/citizen/children/assistance-need-preschool-decisions/${id}/pdf`,
                  '_blank',
                  'noopener,noreferrer'
                )
              }}
              data-qa="assistance-need-decision-download-btn"
              disabled={!decision.getOrElse(undefined)?.hasDocument}
              color={colors.main.m1}
            />
            <Gap horizontal sizeOnMobile="xs" size="zero" />
          </FixedSpaceRow>
        </FixedSpaceRow>
        <Gap size="s" />

        {renderResult(decision, (decision) => (
          <AssistanceNeedPreschoolDecisionReadOnly
            decision={decision}
            texts={
              translations[decision.form.language === 'SV' ? 'sv' : 'fi']
                .decisions.assistancePreschoolDecisions
            }
          />
        ))}

        <Gap size="m" />
        <ReturnButton label={i18n.common.return} />
        <Gap size="s" />
      </Content>
      <Footer />
    </>
  )
})
