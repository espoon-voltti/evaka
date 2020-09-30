<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div role="tablist">
    <div class="club-heading">
      <c-title :size="2">{{
        $t('form.club-heading')
      }}</c-title>
      <div class="form-guidelines" v-html="$t('form.club-guidelines')"></div>
    </div>
    <form-section
      :title="$t('form.care.title')"
      :selected="true"
      id="club-form-section-care"
    >
      <club-care
        :bday="getChildBirthday"
      />
    </form-section>

    <form-section :title="$t('form.apply.title')" id="club-form-section-clubs">
      <apply-to-club
        :isLoading="isLoading"
        :type="type"
      />
    </form-section>

    <form-section
      :title="$t('form.persons.title')"
      id="club-form-section-personal-details"
    >
      <personal-details :applicationType="applicationType" />
    </form-section>

    <form-section
      :title="$t('form.additional.title')"
      id="club-form-section-additional-details"
    >
      <additional-details :applicationType="applicationType" />
    </form-section>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'
  import { applicationTypeToDaycareTypes } from '@/constants'
  import FormSection from '@/views/applications/club-application/form-components/form-section.vue'
  import ClubCare from '@/views/applications/club-application/form-components/club-care.vue'
  import ApplyToClub from '@/views/applications/club-application/form-components/apply-to-club.vue'
  import PersonalDetails from '@/views/applications/club-application/form-components/personal-details.vue'
  import AdditionalDetails from '@/views/applications/club-application/form-components/additional-details.vue'

  export default {
    props: ['type', 'isLoading'],
    components: {
      FormSection,
      ClubCare,
      ApplyToClub,
      PersonalDetails,
      AdditionalDetails
    },
    computed: {
      ...mapGetters(['getChildBirthday']),
      applicationType() {
        return applicationTypeToDaycareTypes(this.type) || []
      }
    }
  }
</script>
