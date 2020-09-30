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
            <div>{{ applicationForm.preferredStartDate }}</div>
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
              v-for="(clubGroupId, index) in applicationForm.apply
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
            :value="applicationForm.child.socialSecurityNumber"
          >
          </c-summary-field>
        </div>

        <div class="columns">
          <c-summary-field
            class="column"
            :class="{
              'corrected-address': applicationForm.child.hasCorrectingAddress
            }"
            :label="$t('form.summary.child-section.address')"
            :value="childAddress"
          >
          </c-summary-field>
        </div>

        <div class="summary-data correcting-address">
          <c-summary-field
            :value="applicationForm.child.hasCorrectingAddress"
            :label="$t('form.summary.persons-section.new-addr')"
          />

          <div
            class="columns summary-data-field"
            v-if="applicationForm.child.hasCorrectingAddress"
          >
            <div class="is-4 column">
              <p class="strong">{{ $t('general.input.moving-date') }}:</p>
              {{ applicationForm.child.childMovingDate }}
            </div>
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
            :value="applicationForm.guardian.socialSecurityNumber"
          >
          </c-summary-field>
        </div>

        <div class="columns">
          <c-summary-field
            class="column"
            :class="{
              'corrected-address': applicationForm.guardian.hasCorrectingAddress
            }"
            :label="$t('form.summary.guardian-section.address')"
            :value="guardianAddress"
          >
          </c-summary-field>
        </div>

        <div class="summary-data correcting-address">
          <c-summary-field
            :value="applicationForm.guardian.hasCorrectingAddress"
            :label="$t('form.summary.persons-section.new-addr')"
          />
          <div
            class="columns summary-data-field"
            v-if="applicationForm.guardian.hasCorrectingAddress"
          >
            <div class="is-4 column">
              <p class="strong">{{ $t('general.input.moving-date') }}:</p>
              {{ applicationForm.guardian.guardianMovingDate }}
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
            :value="applicationForm.additionalDetails.otherInfo"
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
      ...mapGetters(['countries', 'languages', 'clubTerms']),
      applicationType() {
        return this.applicationForm.type.label
      },
      childFullName() {
        if (this.applicationForm.child.firstName) {
          return (
            this.applicationForm.child.firstName +
            ' ' +
            this.applicationForm.child.lastName
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
            this.applicationForm.child.address.city
          )
        }
      },
      childCorrectingAddress() {
        if (this.applicationForm.child.hasCorrectingAddress) {
          return (
            this.applicationForm.child.correctingAddress.street +
            ', ' +
            this.applicationForm.child.correctingAddress.postalCode +
            ' ' +
            this.applicationForm.child.correctingAddress.city
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
        if (this.applicationForm.guardian.firstName) {
          return (
            this.applicationForm.guardian.firstName +
            ' ' +
            this.applicationForm.guardian.lastName
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
            this.applicationForm.guardian.address.city
          )
        }
      },
      guardianCorrectingAddress() {
        if (this.applicationForm.guardian.hasCorrectingAddress) {
          return (
            this.applicationForm.guardian.correctingAddress.street +
            ', ' +
            this.applicationForm.guardian.correctingAddress.postalCode +
            ' ' +
            this.applicationForm.guardian.correctingAddress.city
          )
        }
      },
      guardian2FullName() {
        if (this.applicationForm.guardian2.firstName) {
          return (
            this.applicationForm.guardian2.firstName +
            ' ' +
            this.applicationForm.guardian2.lastName
          )
        }
      },
      guardian2Address() {
        if (this.applicationForm.guardian2.address.street) {
          return (
            this.applicationForm.guardian2.address.street +
            ', ' +
            this.applicationForm.guardian2.address.postalCode +
            ' ' +
            this.applicationForm.guardian2.address.city
          )
        }
      },
      guardian2CorrectingAddress() {
        if (this.applicationForm.guardian2.hasCorrectingAddress) {
          return (
            this.applicationForm.guardian2.correctingAddress.street +
            ', ' +
            this.applicationForm.guardian2.correctingAddress.postalCode +
            ' ' +
            this.applicationForm.guardian2.correctingAddress.city
          )
        }
      },
      isSiblingBasis() {
        return this.applicationForm.apply.siblingBasis
      },
      siblingName() {
        return this.applicationForm.apply.siblingName
      },
      siblingSsn() {
        return this.applicationForm.apply.siblingSsn
      },
      assistanceNeeded() {
        return this.applicationForm.careDetails.assistanceNeeded
      },
      assistanceDescription() {
        return this.applicationForm.careDetails.assistanceDescription
      },
      wasOnDaycare() {
        return this.applicationForm.wasOnDaycare
      },
      wasOnClubCare() {
        return this.applicationForm.wasOnClubCare
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
