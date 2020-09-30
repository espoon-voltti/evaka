<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <section class="additional-info">
    <div class="columns" v-if="isDaycareType">
      <div class="is-half column">
        <h4 class="title is-4 section-title">
          {{ $t('form.additional.title') }}
        </h4>

        <div class="allergies info-block">
          <label class="label">
            {{ $t('form.additional.child.allergies-label') }}
            <c-instructions
              :instruction="$t('form.additional.child.allergies-info')"
            ></c-instructions>
            <textarea
              name="allergyType"
              v-model="allergyType"
              :placeholder="$t('form.additional.child.allergies-placeholder')"
            ></textarea>
          </label>
        </div>

        <div class="special-diet info-block">
          <label class="label">
            {{ $t('form.additional.child.diet-label') }}
            <c-instructions
              :instruction="$t('form.additional.child.diet-info')"
            ></c-instructions>
            <textarea
              name="dietType"
              v-model="dietType"
              :placeholder="$t('form.additional.child.diet-placeholder')"
            ></textarea>
          </label>
        </div>
      </div>

      <div class="column is-half">
        <h4 class="title is-4 section-title">
          {{ $t('form.additional.other.title') }}:
        </h4>
        <div class="other-info info-block">
          <label class="label">
            {{ $t('form.additional.other.application-details-label') }}:
            <textarea
              name="otherInfo"
              v-model="otherInfo"
              :placeholder="
                $t('form.additional.other.application-details-placeholder')
              "
            ></textarea>
          </label>
        </div>
      </div>
    </div>

    <div class="columns" v-else>
      <div class="column">
        <c-section-title :icon="['fal', 'info']">
          {{ $t('form.additional.other.application-details-label') }}
        </c-section-title>
        
        <div class="other-info info-block">
          <textarea
            name="otherInfo"
            v-model="otherInfo"
            :placeholder="
              $t('form.additional.other.application-details-placeholder')
            "
            :aria-label="$t('form.additional.other.application-details-label')"
          ></textarea>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
  import form, { bind } from '@/mixins/form'

  export default {
    mixins: [form],
    props: ['applicationType'],
    computed: {
      allergyType: bind('application', 'additionalDetails.allergyType'),
      dietType: bind('application', 'additionalDetails.dietType'),
      otherInfo: bind('application', 'additionalDetails.otherInfo'),
      isDaycareType() {
        if (this.applicationType) {
          return this.applicationType[0] === 'DAYCARE'
        }
      }
    }
  }
</script>

<style lang="scss" scoped>
  .info-block {
    padding-bottom: 1rem;
    padding-right: 2rem;
  }

  .other-info {
    textarea {
      min-height: 15.25rem;
      margin-top: 0.75rem;
    }
  }
</style>
