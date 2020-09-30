<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div>
    <div v-if="typeof value === 'boolean'" class="summary-toggle">
      <span class="yes-tag" v-if="value">
        <font-awesome-icon :icon="['far', 'check']"></font-awesome-icon>
        {{ $t('general.yes') }}
      </span>
      <span class="no-tag" v-else>
        <font-awesome-icon :icon="['fal', 'times']"></font-awesome-icon>
        {{ $t('general.no') }}
      </span>
      <span :class="{ 'text-muted': !value }">{{ label }}</span>
    </div>
    <div v-else class="summary-field">
      <span class="summary-title strong">
        <span class="has-underline">
          {{ label }}<span v-if="label">:</span>
        </span>
      </span>
      <span class="summary-value" v-if="value">
        {{ value }}
      </span>
      <span class="empty-summary" v-else>
        {{ $t('form.summary.info-missing') }}
      </span>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      label: String,
      value: [String, Boolean]
    }
  }
</script>

<style lang="scss">
  .summary-title {
    display: block;

    .has-underline {
      display: inline-block;
      padding-bottom: 0;
      margin-bottom: 0.325rem;
    }
  }

  .summary-value {
    display: inline-block;
  }

  .empty-summary {
    font-size: 0.925rem;
    color: #999;
    font-style: italic;
  }

  .summary-toggle {
    margin-bottom: 1rem;
    display: flex;
    flex-direction: row;
    align-items: center;

    @include onMobile() {
      align-items: flex-start;
    }
  }

  .yes-tag,
  .no-tag {
    display: inline-flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    position: relative;
    min-width: 3rem;
    margin-right: 0.5rem;
    background-color: darkgrey;
    color: white;
    text-transform: uppercase;
    font-weight: 600;
    padding: 0 8px;

    .no-tag {
      opacity: 0.5;
    }

    svg {
      margin-right: 0.5em;
    }
  }

  .yes-tag {
    background-color: $soft-blue;
  }

  .text-muted {
    opacity: 0.5;
  }
</style>
