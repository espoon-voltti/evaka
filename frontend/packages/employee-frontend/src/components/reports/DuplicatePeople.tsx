// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import {
  Button,
  Container,
  ContentArea,
  Loader,
  Table,
  Title
} from '~components/shared/alpha'
import InfoModal from '~components/common/InfoModal'
import { useTranslation } from '~state/i18n'
import { Link } from 'react-router-dom'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { DuplicatePeopleReportRow } from '~types/reports'
import { getDuplicatePeopleReport } from '~api/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import styled from 'styled-components'
import { faQuestion } from '@evaka/icons'
import { deletePerson, mergePeople } from '~api/person'
import { UIContext } from '~state/ui'
import { UUID } from '~types'
import { CHILD_AGE } from '~constants'
import LocalDate from '@evaka/lib-common/src/local-date'
import { TableScrollable } from 'components/reports/common'

interface RowProps {
  odd: boolean
}

const StyledRow = styled(Table.Row)<RowProps>`
  ${(props) => (props.odd ? `background: #F1F7FF;` : '')}
`
const NoWrapTd = styled(Table.Td)`
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
    Loading()
  )
  const [duplicate, setDuplicate] = useState<Selection | null>(null)
  const [master, setMaster] = useState<Selection | null>(null)
  const [deleteId, setDeleteId] = useState<UUID | null>(null)
  const { setErrorMessage } = useContext(UIContext)

  const loadData = () => {
    setRows(Loading())
    void getDuplicatePeopleReport().then(setRows)
  }

  useEffect(() => {
    loadData()
  }, [])

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.duplicatePeople.title}</Title>
        {isLoading(rows) && <Loader />}
        {isFailure(rows) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(rows) && (
          <>
            <TableScrollable>
              <Table.Head>
                <Table.Row>
                  <Table.Th>Henkilön nimi</Table.Th>
                  <Table.Th>Hetu</Table.Th>
                  <Table.Th>Synt.aika</Table.Th>
                  <Table.Th>Katuosoite</Table.Th>
                  <Table.Th />
                  <Table.Th />

                  <Table.Th>Hakemuksia (huoltajana)</Table.Th>
                  <Table.Th>Jääkaappi- puolisoja</Table.Th>
                  <Table.Th>Jääkaappi- lapsia</Table.Th>
                  <Table.Th>Tulo- tietoja</Table.Th>
                  <Table.Th>Maksu- päätöksiä (päämies)</Table.Th>
                  <Table.Th>Maksu- päätöksiä (puoliso)</Table.Th>
                  <Table.Th>Laskuja</Table.Th>

                  <Table.Th>Hakemuksia (lapsena)</Table.Th>
                  <Table.Th>Sijoituksia</Table.Th>
                  <Table.Th>Päämiehiä</Table.Th>
                  <Table.Th>Palvelun tarpeita</Table.Th>
                  <Table.Th>Tuen tarpeita</Table.Th>
                  <Table.Th>Tuki- toimia</Table.Th>
                  <Table.Th>Maksu- muutoksia</Table.Th>
                  <Table.Th>Poissa- oloja</Table.Th>
                  <Table.Th>Vara- sijoituksia</Table.Th>
                  <Table.Th>Maksu- päätös- osia</Table.Th>
                  <Table.Th>Lasku- rivejä</Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {rows.data.map((row: DuplicatePeopleReportRow) => (
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
                              plain
                              className="inline"
                              onClick={() => setDuplicate(null)}
                            >
                              {i18n.common.cancel}
                            </Button>
                          ) : (
                            <Button
                              plain
                              className="inline"
                              onClick={() =>
                                setMaster({
                                  group: row.groupIndex,
                                  row: row.duplicateNumber
                                })
                              }
                            >
                              {i18n.reports.duplicatePeople.moveTo}
                            </Button>
                          )
                        ) : null
                      ) : (
                        <Button
                          plain
                          className="inline"
                          disabled={!hasReferences(row)}
                          onClick={() =>
                            setDuplicate({
                              group: row.groupIndex,
                              row: row.duplicateNumber
                            })
                          }
                        >
                          {i18n.reports.duplicatePeople.moveFrom}
                        </Button>
                      )}
                    </NoWrapTd>
                    <NoWrapTd>
                      <Button
                        plain
                        className="inline"
                        disabled={duplicate != null || hasReferences(row)}
                        onClick={() => setDeleteId(row.id)}
                      >
                        {i18n.common.remove}
                      </Button>
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
              </Table.Body>
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
                  const masterId = rows.data.find(
                    (row) =>
                      row.groupIndex == master.group &&
                      row.duplicateNumber == master.row
                  )?.id
                  const duplicateId = rows.data.find(
                    (row) =>
                      row.groupIndex == duplicate.group &&
                      row.duplicateNumber == duplicate.row
                  )?.id

                  setMaster(null)
                  setDuplicate(null)

                  if (masterId && duplicateId) {
                    void mergePeople(masterId, duplicateId).then((res) => {
                      if (isFailure(res)) {
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
