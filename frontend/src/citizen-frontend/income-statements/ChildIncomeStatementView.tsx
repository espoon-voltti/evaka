// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import type {
  IncomeStatement,
  IncomeStatementAttachmentType
} from 'lib-common/generated/api-types/incomestatement'
import type { IncomeStatementId } from 'lib-common/generated/api-types/shared'
import {
  collectAttachmentIds,
  toIncomeStatementAttachments
} from 'lib-common/income-statements/attachments'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Main from 'lib-components/atoms/Main'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import InputField from 'lib-components/atoms/form/InputField'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'

import {
  CitizenAttachments,
  CitizenAttachmentsWithUpload
} from './IncomeStatementAttachments'
import { Row } from './IncomeStatementComponents'
import {
  incomeStatementQuery,
  updateSentIncomeStatementMutation
} from './queries'

export default React.memo(function ChildIncomeStatementView() {
  const incomeStatementId =
    useIdRouteParam<IncomeStatementId>('incomeStatementId')
  const t = useTranslation()
  const result = useQueryResult(incomeStatementQuery({ incomeStatementId }))

  return renderResult(result, (incomeStatement) => (
    <Container>
      <ReturnButton label={t.common.return} />
      <Main>
        <ContentArea opaque>
          <FixedSpaceRow spacing="L">
            <H1>{t.income.view.title}</H1>
          </FixedSpaceRow>
          <Row
            label={t.income.view.startDate}
            value={incomeStatement.startDate.format()}
            data-qa="start-date"
          />
          <Row
            label={t.income.view.feeBasis}
            value={t.income.view.statementTypes[incomeStatement.type]}
          />
          {incomeStatement.type === 'CHILD_INCOME' && (
            <ChildIncomeInfo incomeStatement={incomeStatement} />
          )}
        </ContentArea>
      </Main>
    </Container>
  ))
})

const ChildIncomeInfo = React.memo(function IncomeInfo({
  incomeStatement
}: {
  incomeStatement: IncomeStatement.ChildIncome
}) {
  const t = useTranslation()
  const navigate = useNavigate()

  const editable = incomeStatement.status !== 'HANDLED'

  const requiredAttachments = useMemo(
    (): Set<IncomeStatementAttachmentType> => new Set(['CHILD_INCOME']),
    []
  )

  const [otherInfo, setOtherInfo] = useState(() => incomeStatement.otherInfo)
  const [incomeStatementAttachments, setIncomeStatementAttachments] = useState(
    () => toIncomeStatementAttachments(incomeStatement.attachments)
  )

  return (
    <>
      <HorizontalLine />
      <Row
        label={t.income.view.otherInfo}
        value={editable ? '' : incomeStatement.otherInfo || '-'}
        data-qa={editable ? undefined : 'other-info'}
      />
      {editable && (
        <InputField
          value={otherInfo}
          onChange={setOtherInfo}
          placeholder={t.common.write}
          data-qa="other-info"
        />
      )}
      <HorizontalLine />
      {editable ? (
        <CitizenAttachmentsWithUpload
          incomeStatementId={incomeStatement.id}
          requiredAttachments={requiredAttachments}
          incomeStatementAttachments={incomeStatementAttachments}
          alwaysIncludeOther={false}
          onChange={setIncomeStatementAttachments}
        />
      ) : (
        <CitizenAttachments
          incomeStatementAttachments={incomeStatementAttachments}
        />
      )}
      {editable && (
        <>
          <Gap size="L" />
          <FixedSpaceRow justifyContent="flex-end">
            <Button
              text={t.common.cancel}
              onClick={() => navigate('/income')}
            />
            <MutateButton
              primary
              text={t.common.save}
              mutation={updateSentIncomeStatementMutation}
              onClick={() => ({
                incomeStatementId: incomeStatement.id,
                body: {
                  otherInfo,
                  attachmentIds: collectAttachmentIds(
                    incomeStatementAttachments
                  )
                }
              })}
              onSuccess={() => navigate('/income')}
              data-qa="save-btn"
            />
          </FixedSpaceRow>
        </>
      )}
    </>
  )
})
