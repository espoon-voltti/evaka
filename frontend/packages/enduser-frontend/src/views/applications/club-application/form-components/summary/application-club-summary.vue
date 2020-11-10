<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="club-summary">
    <c-summary-section :title="$t('form.summary.care-section.title')">
      <c-summary-subsection
        :icon="['far', 'calendar-alt']"
        :title="$t('form.summary.care-section.care-term-title')"
      >
        <div class="columns is-gapless">
          <div class="column">
            <div class="strong">
              {{ $t('form.summary.care-section.club-term') }}
            </div>
            <div v-if="clubTerm">
              {{ $t('form.summary.term') }}
              {{ clubTerm.start | date }} - {{ clubTerm.end | date }}
            </div>
          </div>
        </div>

        <div class="columns is-gapless">
          <div class="column">
            <div class="strong">
              {{ $t('form.summary.care-section.start-date') }}
            </div>
            <div>{{ applicationForm.preferences.preferredStartDate }}</div>
          </div>
        </div>

        <c-summary-field
          :value="wasOnDaycare"
          :label="$t('form.summary.care-section.was-on-daycare')"
        />
        <c-summary-field
          :value="wasOnClubCare"
          :label="$t('form.summary.care-section.was-on-club-care')"
        />
      </c-summary-subsection>

      <hr class="divider" />

      <c-summary-subsection
        :icon="['far', 'hands']"
        :title="$t('form.summary.care-section.special-care-title')"
      >
        <c-summary-field
          :value="assistanceNeeded"
          :label="$t('form.summary.care-section.special-care')"
        />
        <p v-if="assistanceNeeded">{{ assistanceDescription }}</p>
      </c-summary-subsection>
    </c-summary-section>

    <hr class="divider" />

    <c-summary-section :title="$t('form.apply.title')">
      <c-summary-subsection
        :icon="['fal', 'users']"
        :title="$t('form.summary.apply-section.sibling-basis-title')"
      >
        <c-summary-field
          :value="isSiblingBasis"
          :label="$t('form.summary.apply-section.sibling-basis')"
        />
        <div class="columns is-gapless" v-if="isSiblingBasis">
          <div class="column">
            <div class="strong">
              {{ $t('form.summary.apply-section.sibling-name') }}
            </div>
            <div>{{ siblingName }}</div>
          </div>
          <div class="column">
            <div class="strong">
              {{ $t('form.summary.apply-section.sibling-ssn') }}
            </div>
            <div>{{ siblingSsn }}</div>
          </div>
        </div>
      </c-summary-subsection>

      <hr class="divider" />

      <c-summary-subsection
        :icon="['fal', 'map-marker-alt']"
        class="preferred-units"
        :title="$t('form.summary.apply-section.preferred-locations-title')"
      >
        <p class="strong">
          {{ $t('form.summary.apply-section.preferred-locations') }}
        </p>

        <div class="columns">
          <div class="selected-units-list column is-6">
            <c-unit-list-item
              v-for="(clubGroupId, index) in applicationForm.preferences
                .preferredUnits"
              :key="clubGroupId"
              :removable="false"
              :unitId="clubGroupId"
              :order="index + 1"
            >
            </c-unit-list-item>
          </div>
        </div>
      </c-summary-subsection>
    </c-summary-section>

    <hr class="divider" />

    <c-summary-section :title="$t('form.summary.persons-section.title')">
      <c-summary-subsection
        :icon="['fal', 'child']"
        :title="$t('form.summary.child-section.title')"
      >
        <div class="columns">
          <c-summary-field
            class="is-4 column"
            :label="$t('form.summary.child-section.full-name')"
            :value="childFullName"
          >
          </c-summary-field>

          <c-summary-field
            class="is-4 column"
            :label="$t('form.summary.child-section.identity-number')"
            :value="applicationForm.child.person.socialSecurityNumber"
          >
          </c-summary-field>
        </div>

        <div class="columns">
          <c-summary-field
            class="column"
            :class="{
              'corrected-address': applicationForm.child.futureAddress !== null
            }"
            :label="$t('form.summary.child-section.address')"
            :value="childAddress"
          >
          </c-summary-field>
        </div>

        <div class="summary-data correcting-address">
          <c-summary-field
            :value="applicationForm.child.futureAddress !== null"
            :label="$t('form.summary.persons-section.new-addr')"
          />

          <div
            class="columns summary-data-field"
            v-if="applicationForm.child.futureAddress !== null"
          >
            <div class="is-4 column">
              <p class="strong">{{ $t('general.input.moving-date') }}:</p>
              {{ applicationForm.child.futureAddress.movingDate }}
            </div>
            <div class="is-4 column">
              <p class="strong">
                {{ $t('form.summary.child-section.address') }}:
              </p>
              {{ childCorrectingAddress }}
            </div>
          </div>
        </div>

      </c-summary-subsection>

      <hr class="divider" />

      <c-summary-subsection
        :icon="['far', 'user']"
        :title="$t('form.summary.guardian-section.title1')"
      >
        <div class="columns">
          <c-summary-field
            class="is-4 column"
            :label="$t('form.summary.guardian-section.full-name')"
            :value="guardianFullName"
          >
          </c-summary-field>

          <c-summary-field
            class="is-4 column"
            :label="$t('form.summary.guardian-section.identity-number')"
            :value="applicationForm.guardian.person.socialSecurityNumber"
          >
          </c-summary-field>
        </div>

        <div class="columns">
          <c-summary-field
            class="column"
            :class="{
              'corrected-address': applicationForm.guardian.futureAddress !== null
            }"
            :label="$t('form.summary.guardian-section.address')"
            :value="guardianAddress"
          >
          </c-summary-field>
        </div>

        <div class="summary-data correcting-address">
          <c-summary-field
            :value="applicationForm.guardian.futureAddress !== null"
            :label="$t('form.summary.persons-section.new-addr')"
          />
          <div
            class="columns summary-data-field"
            v-if="applicationForm.guardian.futureAddress !== null"
          >
            <div class="is-4 column">
              <p class="strong">{{ $t('general.input.moving-date') }}:</p>
              {{ applicationForm.guardian.futureAddress.movingDate }}
            </div>
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
          >
          </c-summary-field>

          <c-summary-field
            class="is-4 column"
            :label="$t('general.input.email')"
            :value="applicationForm.guardian.email"
          >
          </c-summary-field>
        </div>
      </c-summary-subsection>
    </c-summary-section>

    <hr class="divider" />

    <c-summary-section :title="$t('form.additional.title')">
      <c-summary-subsection
        :icon="['fal', 'info']"
        :title="$t('form.additional.title')"
      >
        <div class="columns">
          <c-summary-field
            class="is-4 column"
            :label="''"
            :value="applicationForm.otherInfo"
          >
          </c-summary-field>
        </div>
      </c-summary-subsection>
    </c-summary-section>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'
  import _ from 'lodash'
  import form, {bind} from "@/mixins/form";

  export default {
    methods: {
      summaryCheckedChanged(value) {
        this.$emit('summaryCheckedChanged', value)
      }
    },
    mixins: [form],
    computed: {
      ...mapGetters(['countries', 'languages', 'clubTerms']),
      applicationType: bind('application', 'type'),
      applicationForm: bind('application', 'form'),
      childFullName() {
        if (this.applicationForm.child.person.firstName) {
          return (
            this.applicationForm.child.person.firstName +
            ' ' +
            this.applicationForm.child.person.lastName
          )
        }
      },
      childAddress() {
        if (this.applicationForm.child.address.street) {
          return (
            this.applicationForm.child.address.street +
            ', ' +
            this.applicationForm.child.address.postalCode +
            ' ' +
            this.applicationForm.child.address.postOffice
          )
        }
      },
      childCorrectingAddress() {
        if (this.applicationForm.child.futureAddress !== null) {
          return (
            this.applicationForm.child.futureAddress.street +
            ', ' +
            this.applicationForm.child.futureAddress.postalCode +
            ' ' +
            this.applicationForm.child.futureAddress.postOffice
          )
        }
      },
      mapCountry() {
        const country = this.countries.filter((c) =>
          this.applicationForm.child.nationality.includes(c.name)
        )
        if (country.length) {
          return country[0].value
        }
      },
      mapLanguage() {
        const language = this.languages.filter((l) =>
          _.includes(this.applicationForm.child.language, l.name)
        )
        if (language.length) {
          return language[0].value
        }
      },
      guardianFullName() {
        if (this.applicationForm.guardian.person.firstName) {
          return (
            this.applicationForm.guardian.person.firstName +
            ' ' +
            this.applicationForm.guardian.person.lastName
          )
        }
      },
      guardianAddress() {
        if (this.applicationForm.guardian.address.street) {
          return (
            this.applicationForm.guardian.address.street +
            ', ' +
            this.applicationForm.guardian.address.postalCode +
            ' ' +
            this.applicationForm.guardian.address.postOffice
          )
        }
      },
      guardianCorrectingAddress() {
        if (this.applicationForm.guardian.futureAddress !== null) {
          return (
            this.applicationForm.guardian.futureAddress.street +
            ', ' +
            this.applicationForm.guardian.futureAddress.postalCode +
            ' ' +
            this.applicationForm.guardian.futureAddress.postOffice
          )
        }
      },
      secondGuardianFullName() {
        if (this.applicationForm.secondGuardian !== null) {
          return (
            this.applicationForm.secondGuardian.person.firstName +
            ' ' +
            this.applicationForm.secondGuardian.person.lastName
          )
        }
      },
      secondGuardianAddress() {
        if (this.applicationForm.secondGuardian !== null && this.applicationForm.secondGuardian.address !== null) {
          return (
            this.applicationForm.secondGuardian.address.street +
            ', ' +
            this.applicationForm.secondGuardian.address.postalCode +
            ' ' +
            this.applicationForm.secondGuardian.address.postOffice
          )
        }
      },
      secondGuardianCorrectingAddress() {
        if (this.applicationForm.secondGuardian !== null && this.applicationForm.secondGuardian.futureAddress !== null) {
          return (
            this.applicationForm.secondGuardian.futureAddress.street +
            ', ' +
            this.applicationForm.secondGuardian.futureAddress.postalCode +
            ' ' +
            this.applicationForm.secondGuardian.futureAddress.postOffice
          )
        }
      },
      isSiblingBasis() {
        return this.applicationForm.preferences.siblingBasis !== null
      },
      siblingName() {
        return this.applicationForm.preferences.siblingBasis?.siblingName
      },
      siblingSsn() {
        return this.applicationForm.preferences.siblingBasis?.siblingSsn
      },
      assistanceNeeded() {
        return this.applicationForm.child.assistanceNeeded
      },
      assistanceDescription() {
        return this.applicationForm.child.assistanceDescription
      },
      wasOnDaycare() {
        return this.applicationForm.clubDetails.wasOnDaycare
      },
      wasOnClubCare() {
        return this.applicationForm.clubDetails.wasOnClubCare
      },
      clubTerm() {
        return this.clubTerms.find((t) => t.id === this.applicationForm.term)
      }
    }
  }
</script>

<style lang="scss" scoped>
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
    margin-bottom: 2rem;
    background: $white-bis;
  }
</style>
