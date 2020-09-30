<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="disabled-label">
    <div class="disabled-label-content">
      <slot />
    </div>
  </div>
</template>

<script>
  export default {
    name: 'DisabledLabel'
  }
</script>

<style lang="scss" scoped>
  .disabled-label {
    &-content {
      padding: 4px 16px;
      border-radius: 2px;
      background-color: $grey-light;
      color: white;
      display: inline-block;
    }
  }
</style>
