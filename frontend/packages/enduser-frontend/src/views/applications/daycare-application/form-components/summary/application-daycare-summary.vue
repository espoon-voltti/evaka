<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="club-summary">
    <!-- Palvelun tarve -->
    <c-summary-section :title="$t(`form.${type}-application.service.title`)">
      <!-- Palvelun tarpeen alkaminen -->
      <c-summary-subsection
        :icon="['far', 'calendar-alt']"
        :title="$t(`form.${type}-application.service.section-title`)"
      >
        <!-- DISABLE FOR NOW
          <c-summary-field :value="applicationForm.moveToAnotherUnit" :label="$t('form.daycare-application.requestAnotherUnit.label')"/>
        -->

        <c-summary-field
          :value="applicationForm.urgent"
          :label="$t('form.daycare-application.service.expedited.label')"
          v-if="!isPreschool"
        />

        <!-- Liitteet -->
        <div
          class="attachment-list-wrapper"
          v-if="attachmentsEnabled"
          v-show="applicationForm.urgent"
        >
          <p class="strong">
            {{ $t('form.daycare-application.service.attachments-label') }}
          </p>
          <p v-if="uploadedUrgentFiles.length === 0">
            {{ $t('form.daycare-application.service.no-attachments') }}
          </p>
          <ul v-else>
            <li v-for="file in uploadedUrgentFiles" v-bind:key="file.id">
              <span class="attachment-icon">
                <font-awesome-icon :icon="['fal', 'file']"></font-awesome-icon>
              </span>
              <a
                :href="`/api/application/attachments/${file.id}/download`"
                target="_blank"
                rel="noreferrer"
                >{{ file.name }}</a
              >
            </li>
          </ul>
        </div>

        <div class="columns is-gapless">
          <div class="column">
            <div class="strong">
              {{ $t(`form.${type}-application.service.startDate.label`) }}
            </div>
            <div>{{ preferredStartDate | date }}</div>
          </div>
        </div>
      </c-summary-subsection>

      <hr class="divider" />

      <!-- Päivittäinen palvelun tarve -->
      <c-summary-subsection
        :title="$t(`form.${type}-application.service.dailyTitle`)"
      >
        <div class="columns is-gapless" v-if="isPreschool">
          <div class="column">
            <c-summary-field
              :value="applicationForm.connectedDaycare"
              :label="
                $t('form.preschool-application.service.connectedDaycare.label')
              "
            />
          </div>
        </div>
        <div class="columns is-gapless" v-if="showDailyTime">
          <div class="column">
            <div class="strong">
              {{ $t('form.daycare-application.service.dailyTime.label') }}:
            </div>
            <div>{{ dailyTime }}</div>
          </div>
        </div>

        <c-summary-field
          :value="applicationForm.partTime"
          :label="$t('form.daycare-application.service.dailyTime.partTime')"
          v-if="showDailyTime"
        />

        <c-summary-field
          :value="applicationForm.extendedCare"
          :label="$t('form.daycare-application.service.extended.label')"
          v-if="showDailyTime"
        />
      </c-summary-subsection>

      <hr class="divider" />

      <!-- Tuen tarve -->
      <c-summary-subsection
        :icon="['far', 'hands']"
        :title="$t(`form.${type}-application.service.clubCare.section-title`)"
      >
        <c-summary-field
          v-if="isPreschool && !currentLocaleIsSwedish"
          :value="applicationForm.careDetails.preparatory"
          :label="
            $t('form.preschool-application.service.clubCare.preparatory.label')
          "
        />
        <c-summary-field
          :value="applicationForm.careDetails.assistanceNeeded"
          :label="$t('form.daycare-application.service.clubCare.label')"
        />

        <div
          class="columns"
          v-if="applicationForm.careDetails.assistanceNeeded"
        >
          <c-summary-field
            class="is-4 column"
            :label="''"
            :value="applicationForm.careDetails.assistanceDescription"
          ></c-summary-field>
        </div>
      </c-summary-subsection>
    </c-summary-section>

    <hr class="column-divider" />

    <!-- Hakutoive -->
    <c-summary-section
      :title="$t('form.daycare-application.preferredUnits.title')"
    >
      <c-summary-subsection
        :icon="['fal', 'users']"
        :title="$t(`form.${type}-application.siblingBasis.title`)"
      >
        <c-summary-field
          :value="applicationForm.apply.siblingBasis"
          :label="
            $t(`form.${type}-application.preferredUnits.siblings.select.label`)
          "
        />
        <div class="columns" v-if="applicationForm.apply.siblingBasis">
          <c-summary-field
            class="is-4 column"
            :label="
              $t('form.daycare-application.preferredUnits.siblings.input.label')
            "
            :value="applicationForm.apply.siblingName"
          ></c-summary-field>
          <c-summary-field
            class="is-4 column"
            :label="
              $t(
                'form.daycare-application.preferredUnits.siblings.input.ssn-label'
              )
            "
            :value="applicationForm.apply.siblingSsn"
          ></c-summary-field>
        </div>
      </c-summary-subsection>

      <hr class="divider" />

      <c-summary-subsection
        class="preferred-units"
        :icon="['fal', 'map-marker-alt']"
        :title="$t('form.daycare-application.preferredUnits.title')"
      >
        <p class="strong">
          {{ $t('form.summary.apply-section.preferred-locations') }}
        </p>

        <div class="columns">
          <div class="selected-units-list column is-6">
            <c-unit-list-item
              v-for="(daycareId, index) in preferredUnits"
              :key="daycareId"
              :removable="false"
              :unitId="daycareId"
              :order="index + 1"
            ></c-unit-list-item>
          </div>
        </div>
      </c-summary-subsection>
    </c-summary-section>

    <hr class="column-divider" />

    <!-- Henkilötiedot -->
    <c-summary-section
      :title="$t('form.daycare-application.personalInfo.title')"
    >
      <!-- Lapsen tiedot -->
      <c-summary-subsection
        :icon="['fal', 'child']"
        :title="$t('form.summary.child-section.title')"
      >
        <div class="columns">
          <c-summary-field
            class="is-4 column"
            :label="$t('form.summary.child-section.full-name')"
            :value="childFullName"
          ></c-summary-field>

          <c-summary-field
            class="is-4 column"
            :label="$t('form.summary.child-section.identity-number')"
            :value="applicationForm.child.socialSecurityNumber"
          ></c-summary-field>
        </div>

        <div class="columns">
          <c-summary-field
            class="column"
            :class="{
              'corrected-address': applicationForm.child.hasCorrectingAddress
            }"
            :label="$t('form.summary.child-section.address')"
            :value="childAddress"
          ></c-summary-field>
        </div>

        <div class="summary-data correcting-address">
          <c-summary-field
            :value="applicationForm.child.hasCorrectingAddress === true"
            :label="$t('form.persons.new-addr')"
          />

          <div
            class="columns summary-data-field"
            v-if="applicationForm.child.hasCorrectingAddress"
          >
            <div class="is-4 column">
              <p class="strong">
                {{ $t('form.summary.child-section.address') }}:
              </p>
              {{ childCorrectingAddress }}
            </div>
          </div>
        </div>

        <!--
        <div class="columns">
          <c-summary-field
            class="is-4 column"
            :label="$t('form.summary.child-section.nationality')"
            :value="mapCountry"
          >
          </c-summary-field>

          <c-summary-field
            class="is-4 column"
            :label="$t('form.summary.child-section.language')"
            :value="mapLanguage"
          >
          </c-summary-field>
        </div>
        -->
      </c-summary-subsection>

      <hr class="divider" />

      <!-- Huoltajan tiedot -->
      <c-summary-subsection
        :icon="['far', 'user']"
        :title="$t('form.summary.guardian-section.title1')"
      >
        <div class="columns">
          <c-summary-field
            class="is-4 column"
            :label="$t('form.summary.guardian-section.full-name')"
            :value="guardianFullName"
          ></c-summary-field>

          <c-summary-field
            class="is-4 column"
            :label="$t('form.summary.guardian-section.identity-number')"
            :value="applicationForm.guardian.socialSecurityNumber"
          ></c-summary-field>
        </div>

        <div class="columns">
          <c-summary-field
            class="column"
            :class="{
              'corrected-address': applicationForm.guardian.hasCorrectingAddress
            }"
            :label="$t('form.summary.guardian-section.address')"
            :value="guardianAddress"
          ></c-summary-field>
        </div>

        <div class="summary-data correcting-address">
          <c-summary-field
            :value="applicationForm.guardian.hasCorrectingAddress === true"
            :label="$t('form.persons.new-addr')"
          />
          <div
            class="columns summary-data-field"
            v-if="applicationForm.guardian.hasCorrectingAddress"
          >
            <div class="is-4 column">
              <p class="strong">
                {{ $t('form.summary.child-section.address') }}:
              </p>
              {{ guardianCorrectingAddress }}
            </div>
          </div>
        </div>

        <div class="columns">
          <c-summary-field
            class="is-4 column"
            :label="$t('general.input.tel')"
            :value="applicationForm.guardian.phoneNumber"
          ></c-summary-field>

          <c-summary-field
            class="is-4 column"
            :label="$t('general.input.email')"
            :value="applicationForm.guardian.email"
          ></c-summary-field>
        </div>
      </c-summary-subsection>

      <hr class="column-divider" />

      <c-summary-subsection
        :icon="['far', 'user']"
        :title="$t('form.summary.guardian-section.title2')"
      >
        <c-summary-field
          :value="applicationForm.hasOtherVtjGuardian"
          :label="$t(`form.persons.guardian2.other-guardian-${type}`)"
        />
        <c-summary-field
          :value="
            applicationForm.hasOtherVtjGuardian &&
            !applicationForm.otherVtjGuardianHasSameAddress
          "
          :label="$t(`form.persons.guardian2.separate-address-daycare`)"
        />

        <c-summary-field
          v-if="
            applicationForm.hasOtherVtjGuardian &&
            !applicationForm.otherVtjGuardianHasSameAddress
          "
          :value="
            $t(
              `form.persons.guardian2.agreement-status.${
                applicationForm.otherGuardianAgreementStatus || 'NOT_SET'
              }`
            )
          "
          :label="$t(`form.persons.guardian2.agreement-status.title`)"
        />

        <div v-if="applicationForm.hasSecondGuardian">
          <div class="columns">
            <c-summary-field
              class="is-4 column"
              :label="$t('form.summary.guardian-section.full-name')"
              :value="guardian2FullName"
            ></c-summary-field>

            <c-summary-field
              class="is-4 column"
              :label="$t('form.summary.guardian-section.identity-number')"
              :value="applicationForm.guardian2.socialSecurityNumber"
            ></c-summary-field>
          </div>
          <div class="columns">
            <c-summary-field
              class="is-4 column"
              :label="$t('general.input.tel')"
              :value="applicationForm.guardian2.phoneNumber"
            ></c-summary-field>
            <c-summary-field
              class="is-4 column"
              :label="$t('general.input.email')"
              :value="applicationForm.guardian2.email"
            ></c-summary-field>
          </div>

          <div
            class="columns"
            v-if="
              applicationForm.hasOtherVtjGuardian &&
              !otherVtjGuardianHasSameAddress
            "
          >
            <c-summary-field
              class="is-4 column"
              :label="$t('general.input.tel')"
              :value="applicationForm.guardian2.phoneNumber"
            ></c-summary-field>

            <c-summary-field
              class="is-4 column"
              :label="$t('general.input.email')"
              :value="applicationForm.guardian2.email"
            ></c-summary-field>
          </div>
        </div>
      </c-summary-subsection>

      <hr class="column-divider" v-if="showDailyTime" />

      <!-- Muut henkilöt -->
      <c-summary-subsection
        :icon="['fal', 'users']"
        :title="$t('form.persons.other-adults.title')"
        v-if="showDailyTime"
      >
        <c-summary-field
          :value="applicationForm.hasOtherAdults"
          :label="$t('form.persons.other-adults.add-other-adult-daycare')"
        />

        <div v-if="applicationForm.hasOtherAdults">
          <div
            v-for="(adult, index) in applicationForm.otherAdults"
            :key="index"
            class="columns"
          >
            <c-summary-field
              class="is-4 column"
              :label="$t('form.summary.guardian-section.person-name')"
              :value="`${adult.firstName} ${adult.lastName}`"
            ></c-summary-field>
            <c-summary-field
              class="is-4 column"
              :label="$t('form.summary.guardian-section.identity-number')"
              :value="adult.socialSecurityNumber"
            ></c-summary-field>
          </div>
        </div>
      </c-summary-subsection>

      <hr class="column-divider" v-if="showDailyTime" />

      <!-- Muut lapset -->
      <c-summary-subsection
        :icon="['fal', 'users']"
        :title="$t('form.persons.other-children-section.title')"
        v-if="showDailyTime"
      >
        <c-summary-field
          :value="applicationForm.hasOtherChildren"
          :label="$t('form.persons.other-children-section.label-daycare')"
        />

        <div v-if="applicationForm.hasOtherChildren">
          <div
            v-for="(child, index) in applicationForm.otherChildren"
            :key="index"
            class="columns"
          >
            <c-summary-field
              class="is-4 column"
              :label="$t('form.summary.other-children.child-name')"
              :value="`${child.firstName} ${child.lastName}`"
            ></c-summary-field>
            <c-summary-field
              class="is-4 column"
              :label="$t('form.summary.guardian-section.identity-number')"
              :value="child.socialSecurityNumber"
            ></c-summary-field>
          </div>
        </div>
      </c-summary-subsection>
    </c-summary-section>

    <hr class="column-divider" />

    <c-summary-section :title="$t('form.daycare-application.payment.title')">
      <c-summary-field
        :value="applicationForm.maxFeeAccepted"
        :label="$t('form.daycare-application.payment.checkbox')"
      />
    </c-summary-section>

    <hr class="column-divider" />

    <!-- Muut lisätiedot -->
    <c-summary-section :title="$t('form.daycare-application.additional.title')">
      <c-summary-subsection
        :title="$t('form.daycare-application.additional.title')"
      >
        <div class="columns">
          <c-summary-field
            class="is-4 column"
            :label="''"
            :value="applicationForm.additionalDetails.otherInfo"
          ></c-summary-field>
        </div>
      </c-summary-subsection>

      <hr class="column-divider" />

      <c-summary-subsection
        :title="$t('form.daycare-application.additional.special-diet')"
      >
        <div class="columns">
          <c-summary-field
            class="is-4 column"
            :label="''"
            :value="applicationForm.additionalDetails.dietType"
          ></c-summary-field>
        </div>
      </c-summary-subsection>

      <hr class="column-divider" />

      <c-summary-subsection
        :title="$t('form.daycare-application.additional.allergies')"
      >
        <div class="columns">
          <c-summary-field
            class="is-4 column"
            :label="''"
            :value="applicationForm.additionalDetails.allergyType"
          ></c-summary-field>
        </div>
      </c-summary-subsection>
    </c-summary-section>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'
  import { config } from '@evaka/enduser-frontend/src/config'
  import _ from 'lodash'
  import { formatDate } from '@/utils/date-utils'
  import { APPLICATION_TYPE, LANGUAGES } from '@/constants.ts'

  export default {
    props: {
      applicationForm: Object,
      summaryChecked: Boolean
    },
    methods: {
      summaryCheckedChanged(value) {
        this.$emit('summaryCheckedChanged', value)
      }
    },
    computed: {
      ...mapGetters(['countries', 'languages', 'uploadedUrgentFiles']),
      type() {
        return this.applicationForm.type.value
      },
      attachmentsEnabled() {
        return config.feature.attachments
      },
      currentLocaleIsSwedish() {
        return this.$i18n.locale.toLowerCase() === LANGUAGES.SV
      },
      showDailyTime() {
        return (
          (this.isPreschool && this.applicationForm.connectedDaycare) ||
          !this.isPreschool
        )
      },
      isPreschool() {
        return (
          this.applicationForm.type.value === APPLICATION_TYPE.PRESCHOOL.value
        )
      },
      preferredStartDate() {
        return formatDate(this.applicationForm.preferredStartDate)
      },
      dailyTime() {
        return `${this.applicationForm.serviceStart} - ${this.applicationForm.serviceEnd}`
      },
      applicationType() {
        return this.applicationForm.type.label
      },
      childFullName() {
        return `${this.applicationForm.child.firstName} ${this.applicationForm.child.lastName}`
      },
      childAddress() {
        if (this.applicationForm.child.address.street) {
          return (
            this.applicationForm.child.address.street +
            ', ' +
            this.applicationForm.child.address.postalCode +
            ' ' +
            this.applicationForm.child.address.city
          )
        } else {
          return ''
        }
      },
      childCorrectingAddress() {
        return (
          this.applicationForm.child.correctingAddress.street +
          ', ' +
          this.applicationForm.child.correctingAddress.postalCode +
          ' ' +
          this.applicationForm.child.correctingAddress.city
        )
      },
      mapCountry() {
        if (this.applicationForm.child) {
          const country = this.countries.filter((c) =>
            this.applicationForm.child.nationality.includes(c.name)
          )
          if (country.length) {
            return country[0].value
          }
        }
      },
      mapLanguage() {
        if (this.applicationForm.child) {
          const language = this.languages.filter((l) =>
            _.includes(this.applicationForm.child.language, l.name)
          )
          if (language.length) {
            return language[0].value
          }
        }
      },
      guardianFullName() {
        return `${this.applicationForm.guardian.firstName} ${this.applicationForm.guardian.lastName}`
      },
      guardianAddress() {
        if (this.applicationForm.guardian.address.street) {
          return `${this.applicationForm.guardian.address.street},  ${this.applicationForm.guardian.address.postalCode}  ${this.applicationForm.guardian.address.city}`
        } else {
          return ''
        }
      },
      guardianCorrectingAddress() {
        return `${this.applicationForm.guardian.correctingAddress.street}, ${this.applicationForm.guardian.correctingAddress.postalCode} ${this.applicationForm.guardian.correctingAddress.city}`
      },
      guardian2FullName() {
        return `${this.applicationForm.guardian2.firstName} ${this.applicationForm.guardian2.lastName}`
      },
      guardian2Address() {
        if (this.applicationForm.guardian2.address.street) {
          return `${this.applicationForm.guardian2.address.street},  ${this.applicationForm.guardian2.address.postalCode}  ${this.applicationForm.guardian2.address.city}`
        } else {
          return ''
        }
      },
      guardian2CorrectingAddress() {
        return `${this.applicationForm.guardian2.correctingAddress.street}, ${this.applicationForm.guardian2.correctingAddress.postalCode} ${this.applicationForm.guardian2.correctingAddress.city}`
      },
      preferredUnits() {
        return this.applicationForm.apply.preferredUnits
      }
    }
  }
</script>

<style lang="scss" scoped>
  .attachment-list-wrapper {
    margin-bottom: 1rem;

    & .attachment-icon {
      margin-right: 1rem;
    }
  }

  .club-summary-title {
    margin-top: 1.5rem;
  }

  .summary-section {
    margin-bottom: 1rem;
    padding: 0.5rem 2rem;

    @include onMobile() {
      padding: 0 1rem;
    }

    .is-2 {
      margin-bottom: 2rem;
    }

    .divider {
      margin: 2rem 0;
    }
  }

  .summary-data-field {
    margin-bottom: 1rem;
    display: flex;
    flex-direction: row;
    align-items: center;

    @include onMobile() {
      align-items: flex-start;
    }
  }

  .correcting-address {
    padding: 1.5rem 2.5rem;
    background-color: whitesmoke;
    margin-bottom: 2rem;
  }

  .is-info {
    strong {
      color: white;
    }
  }

  .is-warning {
    color: #555;
    strong {
      color: #555;
    }
  }

  .summary-secondary-title {
    display: inline-block;
    padding-bottom: 0.25rem;
    color: #666;
    border-bottom: 1px solid #ddd;
  }

  #secondGuardianHasAgreed {
    font-weight: 600;
    padding: 0 0 2rem 4rem;
  }
</style>
