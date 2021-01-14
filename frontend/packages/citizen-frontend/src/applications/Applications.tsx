import { useTranslation } from '~localization'
import React, { useEffect, useState } from 'react'
import { GuardianApplications } from '~applications/types'
import { client } from '~api-client'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'

export default React.memo(function Applications() {
  const t = useTranslation()
  const [guardianApplications, setGuardianApplications] = useState<
    GuardianApplications[]
  >()

  const getGuardianApplications = async (): Promise<GuardianApplications[]> => {
    const { data } = await client.get<JsonOf<GuardianApplications[]>>(
      '/citizen/applications/by-guardian'
    )
    return data.map(({ applicationSummaries, ...rest }) => ({
      ...rest,
      applicationSummaries: applicationSummaries.map((json) => ({
        ...json,
        sentDate: LocalDate.parseNullableIso(json.sentDate)
      }))
    }))
  }

  useEffect(() => {
    void getGuardianApplications()
      .then((applications) => {
        return applications
      })
      .then(setGuardianApplications)
  }, [setGuardianApplications])

  return (
    <div>
      <div>{t.applications.title}</div>
      <br />
      {guardianApplications?.map(
        (guardianApplications: GuardianApplications) => (
          <div key={guardianApplications.childName}>
            <span>
              Applications for {guardianApplications.childName}:
              {guardianApplications.applicationSummaries?.map(
                (applicationSummary) => (
                  <a
                    key={applicationSummary.applicationId}
                    href={`/citizen/applications/${applicationSummary.applicationId}`}
                  >
                    {applicationSummary.applicationId}{' '}
                  </a>
                )
              )}
            </span>
          </div>
        )
      )}
    </div>
  )
})
