import { faPen, faQuestion, faTrash } from 'Icons'
import React, { useCallback, useState } from 'react'

import { Success } from 'lib-common/api'
import { ClubTerm, PreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { useQueryResult } from 'lib-common/query'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { preschoolTermsQuery } from './queries'

export default React.memo(function TermsSection() {
  const { i18n } = useTranslation()

  const preschoolTerms = useQueryResult(preschoolTermsQuery())

  const [preschoolTermToDelete, setPreschoolTermToDelete] =
    useState<PreschoolTerm>()

  const [clubTermToDelete, setClubTermToDelete] = useState<ClubTerm>()

  // TODO: Add functionality to delete preschool or club term
  const onDeleteTerm = useCallback(() => {
    if (preschoolTermToDelete) {
      return Promise.resolve(Success.of())
    } else if (clubTermToDelete) {
      return Promise.resolve(Success.of())
    } else {
      return Promise.reject()
    }
  }, [preschoolTermToDelete])

  const onDeleteTermReject = useCallback(() => {
    setPreschoolTermToDelete(undefined)
    setClubTermToDelete(undefined)
  }, [setPreschoolTermToDelete, setClubTermToDelete])

  const onDeleteTermSuccess = useCallback(() => {
    setPreschoolTermToDelete(undefined)
    setClubTermToDelete(undefined)
  }, [setPreschoolTermToDelete, setClubTermToDelete])

  return (
    <>
      <H2>{i18n.titles.preschoolTerms}</H2>

      {renderResult(preschoolTerms, (preschoolTerms) => (
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.terms.finnishPreschool}</Th>
              <Th>{i18n.terms.extendedPeriodStarts}</Th>
              <Th>{i18n.terms.applicationPeriodStarts}</Th>
              <Th>{i18n.terms.termBreaks}</Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {preschoolTerms
              .sort((a, b) =>
                b.finnishPreschool.start.compareTo(a.finnishPreschool.start)
              )
              .map((row, i) => (
                <Tr key={i} data-qa="preschool-term-row">
                  <Td>{row.finnishPreschool.format('dd.MM.yyyy')}</Td>
                  <Td>{row.extendedTerm.start.format('dd.MM.yyyy')}</Td>
                  <Td>{row.applicationPeriod.start.format('dd.MM.yyyy')}</Td>
                  <Td>
                    <ul>
                      {row.termBreaks.map((termBreak) => (
                        <li key={termBreak.start.format('dd.MM.yyyy')}>
                          {termBreak.formatCompact()}
                        </li>
                      ))}
                    </ul>
                  </Td>
                  <Td>
                    <FixedSpaceRow spacing="s">
                      <IconButton
                        icon={faPen}
                        data-qa="btn-edit"
                        //onClick={() => navigate('...')} TODO: Add functionality to navigate and edit term row
                        aria-label={i18n.common.edit}
                      />
                      <IconButton
                        icon={faTrash}
                        data-qa="btn-delete"
                        onClick={() => setPreschoolTermToDelete(row)}
                        aria-label={i18n.common.remove}
                      />
                    </FixedSpaceRow>
                  </Td>
                </Tr>
              ))}
          </Tbody>
        </Table>
      ))}

      {!!preschoolTermToDelete && (
        <AsyncFormModal
          type="warning"
          title={i18n.terms.confirmDelete}
          icon={faQuestion}
          rejectAction={onDeleteTermReject}
          rejectLabel={i18n.common.cancel}
          resolveAction={onDeleteTerm}
          resolveLabel={i18n.common.remove}
          onSuccess={onDeleteTermSuccess}
        />
      )}
    </>
  )
})
