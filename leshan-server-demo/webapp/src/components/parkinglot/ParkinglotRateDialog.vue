<!-----------------------------------------------------------------------------
 * Copyright (c) 2021 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
  ----------------------------------------------------------------------------->
<template>
  <v-dialog
    v-model="show"
    hide-overlay
    fullscreen
    transition="dialog-bottom-transition"
  >
    <v-card>
      <v-card-title class="headline grey lighten-2">
        Change rate parking lot
      </v-card-title>
      <v-card-text>
        <v-form ref="form" @submit.prevent="changeRate">
          <v-text-field
            :label="`Rate`"
            type="number"
            @input="input($event)"
          />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn text @click="changeRate">
          Change rate
        </v-btn>
        <v-btn text @click="show = false">
          Cancel
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
// import SingleValueInput from '../values/input/SingleValueInput.vue';

/**
 * A Dialog to Write a Single or Multi instance Resource.
 */
export default {
  components: { },
  props: {
    value: Boolean
  },
  data() {
    return {
      resourceValue: null,
    };
  },
  computed: {
    show: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit("input", value);
      },
    },
  },
  watch: {
    value(v) {
      if (v) this.resourceValue = null;
    },
  },
  methods: {
    input(event) {
      this.resourceValue = event; 
    },
    changeRate() {
      this.show = false;
      this.$emit("changeRate", this.resourceValue);
    },
  },
};
</script>
