<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <main>
    <section
      class="section container application-preview-wrapper"
      v-if="isLoaded"
    >
      <c-title :size="2" :isGreyDark="true">{{
        $t(`form.summary.${type.toString().toLowerCase()}-title`)
      }}</c-title>

      <div class="application-date-details">
        <span class="application-created">
          <span class="strong"
            >{{ $t('form.summary.application-created') }}:</span
          >
          {{ daycareForm.createdDate | date }}
        </span>
        <span class="application-modified">
          <span class="strong"
            >{{ $t('form.summary.application-last-updated') }}:</span
          >
          {{ daycareForm.modifiedDate | date }}
        </span>
      </div>
      <div class="application-preview">
        <application-summary
          v-if="isLoaded"
          :applicationForm="daycareForm"
        />
      </div>

      <div class="buttons has-text-centered">
        <button class="button  is-primary is-outlined" @click="goBack">
          {{ $t('general.back') }}
        </button>
      </div>
    </section>
  </main>
</template>

<script>
  import { mapGetters } from 'vuex'
  import ApplicationSummary from '@/views/applications/daycare-application/form-components/summary/application-daycare-summary'

  export default {
    data() {
      return {
        isLoaded: false
      }
    },
    components: {
      ApplicationSummary
    },
    computed: {
      ...mapGetters(['daycareForm']),
      id() {
        return this.$route.params.id
      },
      type() {
        return this.daycareForm.type.value
      }
    },
    methods: {
      async loadUnits() {
        // the value is in "UI format"
        const date = this.daycareForm.preferredStartDate
        const type = this.daycareForm.type.value === 'PRESCHOOL'
          ? this.daycareForm.careDetails.preparatory
            ? 'PREPARATORY'
            : 'PRESCHOOL'
          : 'DAYCARE'
        this.$store.dispatch('loadApplicationUnits', { type, date })
      },
      goBack() {
        return history.go(-1)
      }
    },
    async created() {
      await this.$store.dispatch('loadApplication', {
        type: 'DAYCARE',
        applicationId: this.id
      })
      this.isLoaded = true
      this.loadUnits()
    }
  }
</script>

<style lang="scss" scoped>
  .application-preview-wrapper {
    .title.is-2 {
      margin-bottom: 2rem;
    }
  }

  .application-preview {
    border: 1px solid #cecece;
    border-radius: 3px;
    margin-bottom: 2rem;

    strong {
      margin-right: 0.25rem;
    }
  }

  .application-date-details {
    margin-bottom: 0.5rem;
    font-size: 0.925rem;
    padding-left: 2.2rem;

    .application-created {
      margin-right: 2rem;
    }
  }
</style>
