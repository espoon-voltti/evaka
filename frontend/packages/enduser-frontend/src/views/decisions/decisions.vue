<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div>
    <router-view></router-view>
    <footer-component class="space-footer"></footer-component>
  </div>
</template>
<script>
  import FooterComponent from '@/components/common/footer-component'
  export default {
    components: { FooterComponent }
  }
</script>
