import React, { useEffect, useState } from 'react'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { useTranslation } from '../localization'
import { Gap } from 'lib-components/white-space'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
import { Loading, Result } from 'lib-common/api'
import { getPedagogicalDocuments } from './api'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import LocalDate from 'lib-common/local-date'
import { PedagogicalDocument } from 'lib-common/generated/api-types/pedagogicaldocument'

export default function PedagogicalDocuments() {
  const t = useTranslation()
  const [pedagogicalDocuments, setPedagogicalDocuments] = useState<
    Result<PedagogicalDocument[]>
  >(Loading.of())

  const loadData = useRestApi(getPedagogicalDocuments, setPedagogicalDocuments)

  useEffect(() => loadData(), [loadData])

  return (
    <>
      <Container>
        <Gap size="s" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.pedagogicalDocuments.title}</H1>
          <p>{t.pedagogicalDocuments.description}</p>
          {pedagogicalDocuments.mapAll({
            loading() {
              return <SpinnerSegment />
            },
            failure() {
              return <ErrorSegment />
            },
            success(items) {
              return (
                items.length > 0 && <PedagogicalDocumentsTable items={items} />
              )
            }
          })}
        </ContentArea>
      </Container>
    </>
  )
}

function PedagogicalDocumentsTable({
  items
}: {
  items: PedagogicalDocument[]
}) {
  return (
    <Table>
      <Thead>
        <Tr>
          <Th></Th>
          <Th></Th>
          <Th></Th>
          <Th />
        </Tr>
      </Thead>
      <Tbody>
        {items.map((item) => (
          <Tr key={item.id}>
            <Td>{LocalDate.fromSystemTzDate(item.created).format()}</Td>
            <Td>{item.attachment?.name}</Td>
            <Td>{item.description}</Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
}
