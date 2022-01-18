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
  <div>
    <v-data-table
      dense
      v-if="!loading"
      :headers="headers"
      :items="parkingspots"
      item-key="endpoint"
      :items-per-page="10"
      class="elevation-0 fill-height ma-3"
      :search="search"
    >
      <template v-slot:item.status="{ item }">
        {{ item.status | `Free` }}
      </template>
      <template v-slot:item.actions="">
      <request-button
        @on-click="openWriteDialog"
        ref="reserve"
        :title="'Reserve'"
        >Reserve</request-button
      >
      <parkinglot-license-plate-dialog
        v-if="showDialog"
        v-model="showDialog"
        @reserve="reserve($event)"
      />
    </template>

      
    </v-data-table>
  </div>
</template>

<script>
import ParkinglotLicensePlateDialog from "../components/parkinglot/ParkinglotLicensePlateDialog.vue";
import RequestButton from "../components/RequestButton.vue";
export default {
  components: { ParkinglotLicensePlateDialog, RequestButton },
  useSSE: true,
  name: "Parkingspot",
  data: () => ({
    loading: true,
    dialog: false,
    parkingspots: [],
    headers: [
      { text: "Parking spot", value: "parkingspotId" },
      { text: "State", value: "parkingspotState" },
      { text: "Actions", value: "actions", sortable: false },
    ],
    search: "",
  }),
  computed: {
    showDialog: {
      get() {
        return this.dialog;
      },
      set(value) {
        this.dialog = value;
        this.$refs.reserve.resetState();
      },
    }
  },
  methods: {
    openWriteDialog() {
      this.dialog = true;
    },
    requestPath(endpoint, path) {
      return `api/clients/${encodeURIComponent(endpoint)}${path}`;
    },
    reserve(value) {
      let requestButton = this.$refs.reserve;
      console.log(value);
      this.axios
        .put("api/clients/DESKTOP-TOM/reserve?timeout=5&format=TLV", {"licenseplate": value})
        .then((response) => {
          console.log(response);
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
  },
  mounted() {
    this.sse = this.$sse
      .create({ url: "api/event" })
      .on("REGISTRATION", (park) => {
        this.parkingspots = this.parkingspots
          .filter((p) => p.endpoint !== p.endpoint)
          .concat(park);
      })
      .on("UPDATED", (park) => {
        this.parkingspots = this.parkingspots
          .filter((p) => p.endpoint !== park.endpoint)
          .concat(park);
      })
      .on("DEREGISTRATION", (park) => {
        this.parkingspots = this.parkingspots.filter(
          (p) => p.endpoint !== park.endpoint
        );
      })
      .on("SLEEPING", (park) => {
        for (var i = 0; i < this.parkingspots.length; i++) {
          if (this.parkingspots[i].endpoint === park.ep) {
            this.parkingspots[i].sleeping = true;
          }
        }
      })
      .on("error", (err) => {
        console.error("sse unexpected error", err);
      });
    this.sse.connect().catch((err) => {
      console.error("Failed to connect to server", err);
    });

    this.axios
      .get("api/parkingspots")
      .then(
        (response) => (
          (this.loading = false), (this.parkingspots = response.data)
        )
      );
  },
  beforeDestroy() {
    // close eventsource on destroy
    this.sse.disconnect();
  }
};
</script>
