<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="warning-box-container" :class="{ bold: bold }">
    <font-awesome-icon
      :icon="['fas', 'exclamation-circle']"
    ></font-awesome-icon>
      <slot />
  </div>
</template>

<script>
  export default {
    name: 'c-warning-box',
    props: {
      bold: {
        type: Boolean,
        default: false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .warning-box-container {
    border: 1px solid $orange;
    border-radius: 2px;
    display: flex;
    padding: 1.25rem 1rem;

    svg {
      color: $orange;
      height: 1.25rem;
      margin-top: 0.25rem;
      margin-right: 1rem;
      width: auto;
    }

    &.bold {
      font-weight: 600;
      color: $orange;
      display: inline-block;
      padding-top: 0.2rem;
      padding-bottom: 0.2rem;
    }
  }

</style>
