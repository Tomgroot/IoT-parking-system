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
      <template v-slot:item.actions="{ item }">
      <request-button
        @on-click="openWriteDialog"
        ref="Reserve"
        :title="'Reserve'"
        >W</request-button
      >
      <parkinglot-license-plate-dialog
        v-if="showDialog"
        v-model="showDialog"
        @write="write(item)"
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
  name: "Parkinglot",
  data: () => ({
    loading: true,
    dialog: false,
    parkingspots: [],
    headers: [
      { text: "Parking lot", value: "/32800/0/32700" },
      { text: "Status", value: "/32800/0/32701" },
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
        this.$refs.W.resetState();
      },
    }
  },
  methods: {
    openWriteDialog() {
      this.dialog = true;
    }
  },
  mounted() {
    this.sse = this.$sse
      .create({ url: "api/event" })
      .on("REGISTRATION", (park) => {
        this.parkingspots = this.parkingspots
          .filter((r) => park.endpoint !== r.endpoint)
          .concat(park);
      })
      .on("UPDATED", (msg) => {
        let park = msg.parkingspot;
        this.parkingspots = this.parkingspots
          .filter((r) => park.parkingspotId !== r.parkingspotId)
          .concat(park);
      })
      .on("DEREGISTRATION", (park) => {
        this.parkingspots = this.parkingspots.filter(
          (p) => park.parkingspotId !== p.parkingspotId
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
