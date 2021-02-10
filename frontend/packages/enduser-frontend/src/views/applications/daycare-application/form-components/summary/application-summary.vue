<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="application-summary-wrapper">
    <c-title :size="2" :isGreyDark="true">{{
      $t(`form.summary.${type.toString().toLowerCase()}-check-title`)
    }}</c-title>
    <p
      class="check-application-information"
      v-html="$t('form.summary.send-information')"
    ></p>

    <div class="application-attachments-info" v-if="attachmentsEnabled" v-show="form.urgent && urgentFiles.length === 0">
      <span class="float-left">
        <font-awesome-icon
          :icon="['fas', 'info-circle']"
          size="1x"
        ></font-awesome-icon>
      </span>
      <div class="application-attachments-info-content" >
        <p v-html="$t('form.summary.attachments-info.headline')"></p>
        <ul>
          <li>{{ $t('form.daycare-application.service.expedited.label') }}</li>
          <li>{{ $t('form.daycare-application.service.extended.label') }}</li>
        </ul>
        <a @click="closeSummary">{{ $t('form.summary.attachments-info.go-back-1') }}</a> <span>{{ $t('form.summary.attachments-info.go-back-2') }}</span>
      </div>
    </div>

    <div class="application-date-details">
      <span class="application-created">
        <span class="strong"
          >{{ $t('form.summary.application-created') }}:</span
        >
        {{ form.createdDate | date }}
      </span>
      <span class="application-modified">
        <span class="strong"
          >{{ $t('form.summary.application-last-updated') }}:</span
        >
        {{ form.modifiedDate ? form.modifiedDate : form.createdDate | date }}
      </span>
    </div>
    <div class="application-summary">
      <daycare-summary :applicationForm="form" />
    </div>
    <div class="summary-checkbox-wrapper">
      <label
        class="summary-checkbox"
        :class="{ active: summaryChecked }"
        tabindex="0"
        @keyup.enter.self="toggleCheckbox"
      >
        <span class="inner-wrapper">
          <span class="tick">
            <font-awesome-icon :icon="['far', 'check']"></font-awesome-icon>
          </span>
          <input
            type="checkbox"
            dataClass="summary-check"
            :checked="summaryChecked"
            @click="summaryCheckedChanged"
            ref="checkbox"
          />
        </span>
        {{ $t('form.summary.summary-checked') }}
      </label>
    </div>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'
  import { config } from '@/config'
  import DaycareSummary from '@/views/applications/daycare-application/form-components/summary/application-daycare-summary'

  export default {
    props: {
      summaryChecked: Boolean,
      form: Object
    },
    components: {
      DaycareSummary
    },
    computed: {
      ...mapGetters(['urgentFiles']),
      type() {
        return this.form.type.value
      },
      attachmentsEnabled() {
        return config.feature.attachments
      }
    },
    methods: {
      toggleCheckbox() {
        const el = this.$refs.checkbox
        el.click()
      },
      summaryCheckedChanged(event) {
        this.$emit('summaryCheckedChanged', event.target.checked)
      },
      closeSummary() {
        this.$emit('closeSummary')
      }
    }
  }
</script>

<style lang="scss" scoped>
  .check-application-information {
    color: $black;
    margin-bottom: 2rem;
  }

  .application-summary {
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

  .summary-checkbox-wrapper {
    padding: 0.5rem 2rem 1.5rem;
  }

  .summary-checkbox {
    position: relative;
    height: 40px;
    padding-left: 64px;
    display: flex;
    align-items: center;
    cursor: pointer;

    .inner-wrapper {
      display: inline-block;
      position: absolute;
      left: 0;
      top: 0;

      content: '';
      margin-right: 0.325rem;
      height: 40px;
      width: 40px;
      line-height: 56px;
      text-align: center;
      vertical-align: middle;
      font-size: 1.25rem;
      transition: all 0.2s ease;
      border: 1px solid rgba(gray, 0.8);
    }

    .tick {
      position: absolute;
      opacity: 0;
      top: 0;
      left: 50%;
      transform: translateX(-50%);
      line-height: 38px;
      font-size: 1.75rem;
      color: rgba(gray, 0.8);
    }

    &:not(.active):hover {
      &:after {
        opacity: 1;
      }
    }

    &.active {
      .inner-wrapper {
        border-color: $blue;
        color: $blue;
        background: $blue;

        .tick {
          color: white;
          opacity: 1;

          border-color: $blue;
        }
      }
    }

    &:focus:before {
      box-shadow: 0 0 0 2px #00d1b2;
      border-color: white;
    }

    input {
      position: absolute;
      visibility: hidden;
      -webkit-appearance: none;
    }

    &.disabled {
      opacity: 0.35;
      cursor: not-allowed;

      &:after {
        display: none;
      }

      &:focus:before {
        box-shadow: none;
        border-color: rgba(gray, 0.8);
      }
    }
  }

  .application-attachments-info {
    border: 1px solid $soft-blue;
    padding: 1rem;
    margin: 1rem 0;

    & ul {
      list-style-type: circle;
      list-style-position: inside;
    }

    & li {
      margin-left: 1rem;
    }
  }

  .application-attachments-info-content {
    padding: 0 2rem;
  }

  .float-left {
    float: left;
  }
</style>
