// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import Title from '@evaka/lib-components/src/atoms/Title'
import {
  Th,
  Tr,
  Td,
  Thead,
  Tbody
} from '@evaka/lib-components/src/layout/Table'
import InfoModal from '@evaka/lib-components/src/molecules/modals/InfoModal'
import { useTranslation } from '~state/i18n'
import { Link } from 'react-router-dom'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { DuplicatePeopleReportRow } from '~types/reports'
import { getDuplicatePeopleReport } from '~api/reports'
import ReturnButton from '@evaka/lib-components/src/atoms/buttons/ReturnButton'
import styled from 'styled-components'
import { faQuestion } from '@evaka/lib-icons'
import { deletePerson, mergePeople } from '~api/person'
import { UIContext } from '~state/ui'
import { UUID } from '~types'
import { CHILD_AGE } from '~constants'
import LocalDate from '@evaka/lib-common/src/local-date'
import { TableScrollable } from 'components/reports/common'

interface RowProps {
  odd: boolean
}

const StyledRow = styled(Tr)<RowProps>`
  ${(props) => (props.odd ? `background: #F1F7FF;` : '')}
`
const NoWrapTd = styled(Td)`
  white-space: nowrap;
`

interface Selection {
  group: number
  row: number
}

const hasReferences = (row: DuplicatePeopleReportRow) =>
  !(
    row.applicationsGuardian == 0 &&
    row.feeDecisionsHead == 0 &&
    row.feeDecisionsPartner == 0 &&
    row.fridgeChildren == 0 &&
    row.fridgePartners == 0 &&
    row.incomes == 0 &&
    row.invoices == 0 &&
    row.absences == 0 &&
    row.applicationsChild == 0 &&
    row.assistanceNeeds == 0 &&
    row.assistanceActions == 0 &&
    row.backups == 0 &&
    row.feeAlterations == 0 &&
    row.feeDecisionParts == 0 &&
    row.fridgeParents == 0 &&
    row.invoiceRows == 0 &&
    row.placements == 0 &&
    row.serviceNeeds == 0
  )

const isChild = (dateOfBirth: LocalDate) => {
  const age = LocalDate.today().differenceInYears(dateOfBirth)
  return age < CHILD_AGE
}

function DuplicatePeople() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<DuplicatePeopleReportRow[]>>(
    Loading.of()
  )
  const [duplicate, setDuplicate] = useState<Selection | null>(null)
  const [master, setMaster] = useState<Selection | null>(null)
  const [deleteId, setDeleteId] = useState<UUID | null>(null)
  const { setErrorMessage } = useContext(UIContext)

  const loadData = () => {
    setRows(Loading.of())
    void getDuplicatePeopleReport().then(setRows)
  }

  useEffect(() => {
    loadData()
  }, [])

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.duplicatePeople.title}</Title>
        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>Henkilön nimi</Th>
                  <Th>Hetu</Th>
                  <Th>Synt.aika</Th>
                  <Th>Katuosoite</Th>
                  <Th />
                  <Th />

                  <Th>Hakemuksia (huoltajana)</Th>
                  <Th>Jääkaappi- puolisoja</Th>
                  <Th>Jääkaappi- lapsia</Th>
                  <Th>Tulo- tietoja</Th>
                  <Th>Maksu- päätöksiä (päämies)</Th>
                  <Th>Maksu- päätöksiä (puoliso)</Th>
                  <Th>Laskuja</Th>

                  <Th>Hakemuksia (lapsena)</Th>
                  <Th>Sijoituksia</Th>
                  <Th>Päämiehiä</Th>
                  <Th>Palvelun tarpeita</Th>
                  <Th>Tuen tarpeita</Th>
                  <Th>Tuki- toimia</Th>
                  <Th>Maksu- muutoksia</Th>
                  <Th>Poissa- oloja</Th>
                  <Th>Vara- sijoituksia</Th>
                  <Th>Maksu- päätös- osia</Th>
                  <Th>Lasku- rivejä</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.value.map((row: DuplicatePeopleReportRow) => (
                  <StyledRow key={row.id} odd={row.groupIndex % 2 != 0}>
                    <NoWrapTd>
                      <Link
                        to={
                          isChild(row.dateOfBirth)
                            ? `/child-information/${row.id}`
                            : `/profile/${row.id}`
                        }
                      >
                        {row.lastName} {row.firstName}
                      </Link>
                    </NoWrapTd>
                    <NoWrapTd>{row.socialSecurityNumber}</NoWrapTd>
                    <NoWrapTd>{row.dateOfBirth.format()}</NoWrapTd>
                    <NoWrapTd>{row.streetAddress}</NoWrapTd>
                    <NoWrapTd>
                      {duplicate ? (
                        duplicate.group == row.groupIndex ? (
                          duplicate.row == row.duplicateNumber ? (
                            <Button
                              className="inline"
                              onClick={() => setDuplicate(null)}
                              text={i18n.common.cancel}
                            />
                          ) : (
                            <Button
                              className="inline"
                              onClick={() =>
                                setMaster({
                                  group: row.groupIndex,
                                  row: row.duplicateNumber
                                })
                              }
                              text={i18n.reports.duplicatePeople.moveTo}
                            />
                          )
                        ) : null
                      ) : (
                        <Button
                          className="inline"
                          disabled={!hasReferences(row)}
                          onClick={() =>
                            setDuplicate({
                              group: row.groupIndex,
                              row: row.duplicateNumber
                            })
                          }
                          text={i18n.reports.duplicatePeople.moveFrom}
                        />
                      )}
                    </NoWrapTd>
                    <NoWrapTd>
                      <Button
                        className="inline"
                        disabled={duplicate != null || hasReferences(row)}
                        onClick={() => setDeleteId(row.id)}
                        text={i18n.common.remove}
                      />
                    </NoWrapTd>
                    <NoWrapTd>{row.applicationsGuardian}</NoWrapTd>
                    <NoWrapTd>{row.fridgePartners}</NoWrapTd>
                    <NoWrapTd>{row.fridgeChildren}</NoWrapTd>
                    <NoWrapTd>{row.incomes}</NoWrapTd>
                    <NoWrapTd>{row.feeDecisionsHead}</NoWrapTd>
                    <NoWrapTd>{row.feeDecisionsPartner}</NoWrapTd>
                    <NoWrapTd>{row.invoices}</NoWrapTd>

                    <NoWrapTd>{row.applicationsChild}</NoWrapTd>
                    <NoWrapTd>{row.placements}</NoWrapTd>
                    <NoWrapTd>{row.fridgeParents}</NoWrapTd>
                    <NoWrapTd>{row.serviceNeeds}</NoWrapTd>
                    <NoWrapTd>{row.assistanceNeeds}</NoWrapTd>
                    <NoWrapTd>{row.assistanceActions}</NoWrapTd>
                    <NoWrapTd>{row.feeAlterations}</NoWrapTd>
                    <NoWrapTd>{row.absences}</NoWrapTd>
                    <NoWrapTd>{row.backups}</NoWrapTd>
                    <NoWrapTd>{row.feeDecisionParts}</NoWrapTd>
                    <NoWrapTd>{row.invoiceRows}</NoWrapTd>
                  </StyledRow>
                ))}
              </Tbody>
            </TableScrollable>
            {master && duplicate && (
              <InfoModal
                iconColour={'orange'}
                title={i18n.reports.duplicatePeople.confirmMoveTitle}
                resolveLabel={i18n.common.confirm}
                rejectLabel={i18n.common.cancel}
                icon={faQuestion}
                reject={() => {
                  setMaster(null)
                  setDuplicate(null)
                }}
                resolve={() => {
                  const masterId = rows.value.find(
                    (row) =>
                      row.groupIndex == master.group &&
                      row.duplicateNumber == master.row
                  )?.id
                  const duplicateId = rows.value.find(
                    (row) =>
                      row.groupIndex == duplicate.group &&
                      row.duplicateNumber == duplicate.row
                  )?.id

                  setMaster(null)
                  setDuplicate(null)

                  if (masterId && duplicateId) {
                    void mergePeople(masterId, duplicateId).then((res) => {
                      if (res.isFailure) {
                        setErrorMessage({
                          type: 'error',
                          title: i18n.reports.duplicatePeople.errorTitle,
                          text: i18n.reports.duplicatePeople.errorText
                        })
                      }
                      loadData()
                    })
                  }
                }}
              />
            )}
            {deleteId && (
              <InfoModal
                iconColour={'orange'}
                title={i18n.reports.duplicatePeople.confirmDeleteTitle}
                resolveLabel={i18n.common.remove}
                rejectLabel={i18n.common.cancel}
                icon={faQuestion}
                reject={() => {
                  setDeleteId(null)
                }}
                resolve={() => {
                  void deletePerson(deleteId).then(loadData)
                  setDeleteId(null)
                }}
              />
            )}
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default DuplicatePeople
