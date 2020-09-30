<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="summary-data">
    <c-title :size="4" class="section-title">
      <font-awesome-icon :icon="icon" v-if="icon"></font-awesome-icon>
      {{ title }}
    </c-title>
    <slot />
  </div>
</template>

<script>
  export default {
    name: 'summary-subsection',
    props: {
      title: String,
      icon: Array
    }
  }
</script>

<style lang="scss" scoped></style>
