import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { orderBy } from 'lodash'
import { faChild } from 'lib-icons'
import { Loading, Result } from 'lib-common/api'
import { formatDate } from 'lib-common/date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { useTranslation } from 'employee-frontend/state/i18n'
import { VoucherValueDecisionSummary } from 'employee-frontend/types/invoicing'
import { getPersonVoucherValueDecisions } from 'employee-frontend/api/invoicing'
import { formatCents } from 'employee-frontend/utils/money'

interface Props {
  id: string
  open: boolean
}

export default React.memo(function PersonVoucherValueDecisions({
  id,
  open
}: Props) {
  const { i18n } = useTranslation()

  const [voucherValueDecisions, setVoucherValueDecisions] = useState<
    Result<VoucherValueDecisionSummary[]>
  >(Loading.of())

  const loadDecisions = useRestApi(
    getPersonVoucherValueDecisions,
    setVoucherValueDecisions
  )
  useEffect(() => loadDecisions(id), [loadDecisions, id])

  return (
    <CollapsibleSection
      icon={faChild}
      title={i18n.personProfile.voucherValueDecisions}
      data-qa="person-voucher-value-decisions-collapsible"
      startCollapsed={!open}
    >
      {voucherValueDecisions.mapAll({
        loading() {
          return <Loader />
        },
        failure() {
          return <div>{i18n.common.loadingFailed}</div>
        },
        success(data) {
          return (
            <Table data-qa="table-of-voucher-value-decisions">
              <Thead>
                <Tr>
                  <Th>{i18n.valueDecisions.table.validity}</Th>
                  <Th>{i18n.valueDecisions.table.number}</Th>
                  <Th>{i18n.valueDecisions.table.totalValue}</Th>
                  <Th>{i18n.valueDecisions.table.totalCoPayment}</Th>
                  <Th>{i18n.valueDecisions.table.createdAt}</Th>
                  <Th>{i18n.valueDecisions.table.sentAt}</Th>
                  <Th>{i18n.valueDecisions.table.status}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {orderBy(data, ['createdAt'], ['desc']).map((decision) => {
                  return (
                    <Tr
                      key={`${decision.id}`}
                      data-qa="table-voucher-value-decision-row"
                    >
                      <Td>
                        {decision.child.firstName} {decision.child.lastName}
                        <br />
                        <Link to={`/finance/value-decisions/${decision.id}`}>
                          {`${decision.validFrom.format()} - ${
                            decision.validTo?.format() ?? ''
                          }`}
                        </Link>
                      </Td>
                      <Td>{decision.decisionNumber}</Td>
                      <Td>{formatCents(decision.voucherValue)}</Td>
                      <Td>{formatCents(decision.finalCoPayment)}</Td>
                      <Td>{formatDate(decision.created)}</Td>
                      <Td>{formatDate(decision.sentAt)}</Td>
                      <Td>{i18n.valueDecision.status[decision.status]}</Td>
                    </Tr>
                  )
                })}
              </Tbody>
            </Table>
          )
        }
      })}
    </CollapsibleSection>
  )
})
