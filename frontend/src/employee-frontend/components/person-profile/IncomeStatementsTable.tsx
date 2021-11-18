// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import { IncomeStatement } from 'lib-common/api-types/incomeStatement'
import { useTranslation } from '../../state/i18n'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import React from 'react'
import { Link } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faFileAlt } from 'lib-icons'
import { formatDate } from 'lib-common/date'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Gap } from 'lib-components/white-space'
import { Dimmed } from 'lib-components/typography'

export default React.memo(function IncomeStatementsTable({
  personId,
  incomeStatements
}: {
  personId: UUID
  incomeStatements: IncomeStatement[]
}) {
  const i18n = useTranslation().i18n.personProfile.incomeStatement
  return incomeStatements.length === 0 ? (
    <div>{i18n.noIncomeStatements}</div>
  ) : (
    <Table>
      <Thead>
        <Tr>
          <Th>{i18n.incomeStatementHeading}</Th>
          <Th>{i18n.createdHeading}</Th>
          <Th>{i18n.handledHeading}</Th>
        </Tr>
      </Thead>
      <Tbody data-qa="income-statements">
        {incomeStatements.map((incomeStatement) => (
          <IncomeStatementRow
            key={incomeStatement.id}
            personId={personId}
            incomeStatement={incomeStatement}
          />
        ))}
      </Tbody>
    </Table>
  )
})

const IncomeStatementRow = React.memo(function IncomeStatementRow({
  personId,
  incomeStatement
}: {
  personId: UUID
  incomeStatement: IncomeStatement
}) {
  const { i18n } = useTranslation()

  return (
    <Tr key={incomeStatement.id} data-qa="income-statement-row">
      <Td verticalAlign="middle">
        <Link
          to={`/profile/${personId}/income-statement/${incomeStatement.id}`}
        >
          <FontAwesomeIcon icon={faFileAlt} />{' '}
          {incomeStatement.startDate.format()}
          {' - '}
          {incomeStatement.endDate?.format()}
        </Link>
      </Td>
      <Td verticalAlign="middle">{formatDate(incomeStatement.created)}</Td>
      <Td>
        <Checkbox
          data-qa="is-handled-checkbox"
          label={i18n.personProfile.incomeStatement.handled}
          hiddenLabel
          checked={incomeStatement.handled}
          disabled
        />
        {incomeStatement.handlerNote && (
          <>
            <Gap size={'xxs'} />
            <Dimmed>{incomeStatement.handlerNote}</Dimmed>
          </>
        )}
      </Td>
    </Tr>
  )
})
