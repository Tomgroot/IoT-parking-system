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
    <v-card-title>Name: {{info.name}}</v-card-title>
      <v-card-subtitle>
        Capacity: {{info.capacity}}<br>
        Nr of reservations: {{info.reservations}}<br>
        Vehicles in lot: {{info.vehicles}}<br>
        Rate p/h: {{info.rate}}<br>
      <request-button
        @on-click="openRateDialog"
        ref="rate"
        :title="'Rate'"
        >Change rate</request-button
      >
      <parkinglot-rate-dialog
        v-if="showRateDialog"
        v-model="showRateDialog"
        @changeRate="changeRate($event)"
      />
      </v-card-subtitle>
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
      <template v-slot:item.parkingspotState="{ item }">
        {{ item.parkingspotState }}
      </template>
      <template v-slot:item.textdisplay="{ item }">
        <div :style="{'background-color':item.textdisplay, 'color':item.textdisplay}">.</div>
      </template>
      <template v-slot:item.actions="{ item }">
      <request-button
        @on-click="openWriteDialog"
        ref="reserve"
        v-if="item.parkingspotState == `Free`"
        :title="'Reserve'"
        >Reserve</request-button
      >
      <parkinglot-license-plate-dialog
        v-if="showDialog"
        v-model="showDialog"
        @reserve="reserve($event, item)"
      />
    </template>

      
    </v-data-table>


    <v-data-table
      dense
      v-if="!loading"
      :headers="vehiclecounterHeaders"
      :items="vehiclecounters"
      item-key="endpoint"
      :items-per-page="10"
      class="elevation-0 fill-height ma-3"
      :search="search"
    >
      <template v-slot:item.lastPlate="{ item }">
        {{ item.lastPlate }}
      </template>
           
    </v-data-table>
  </div>
</template>

<script>
import ParkinglotLicensePlateDialog from "../components/parkinglot/ParkinglotLicensePlateDialog.vue";
import ParkinglotRateDialog from "../components/parkinglot/ParkinglotRateDialog.vue";
import RequestButton from "../components/RequestButton.vue";
export default {
  components: { ParkinglotLicensePlateDialog, RequestButton, ParkinglotRateDialog },
  useSSE: true,
  name: "Parkingspot",
  data: () => ({
    loading: true,
    dialog: false,
    rateDialog: false,
    info: {},
    parkingspots: [],
    vehiclecounters: [],
    headers: [
      { text: "Parking spot ID", value: "parkingspotId" },
      { text: "State", value: "parkingspotState" },
      { text: "Vehicle ID", value: "vehicleId" },
      { text: "Lot name", value: "lotName" },
      { text: "", value: "textdisplay", sortable: false  },
      { text: "Actions", value: "actions", sortable: false },
    ],
    vehiclecounterHeaders: [
      { text: "Vehicle counter Id", value: "vehiclecounterId" },
      { text: "Vehicle counter", value: "vehicleCounter" },
      { text: "Last plate", value: "lastPlate" },
      { text: "Direction", value: "direction" },
      { text: "Lot name", value: "lotNameVehicleCounter" },
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
    },
    showRateDialog: {
      get() {
        return this.rateDialog;
      },
      set(value) {
        this.rateDialog = value;
        this.$refs.rate.resetState();
      },
    }
  },
  methods: {
    openWriteDialog() {
      this.dialog = true;
    },
    openRateDialog() {
      this.rateDialog = true;
    },
    requestPath(endpoint, path) {
      return `api/clients/${encodeURIComponent(endpoint)}${path}`;
    },
    reserve(value, item) {
      let requestButton = this.$refs.reserve;
      this.axios
        .put("api/clients/"+item.endpoint+"/reserve?timeout=5&format=TLV", {"licenseplate": value})
        .then((response) => {
          console.log(response);
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    changeRate(value) {
      let requestButton = this.$refs.rate;
      this.axios
        .put("api/parkingspots", {"rate": value})
        .then((response) => {
          this.info = response.data;
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
        this.vehiclecounters = this.vehiclecounters
          .filter((p) => p.endpoint !== park.endpoint && p.vehiclecounterId !== "")
          .concat(park);

        this.parkingspots = this.parkingspots
          .filter((p) => p.endpoint !== park.endpoint && p.parkingspotId !== "")
          .concat(park);
      })
      .on("UPDATED", (park) => {
        this.vehiclecounters = this.vehiclecounters
          .filter((p) => p.endpoint !== park.endpoint && p.vehiclecounterId !== "")
          .concat(park);

        this.parkingspots = this.parkingspots
          .filter((p) => p.endpoint !== park.endpoint && p.parkingspotId !== "")
          .concat(park);
      })
      .on("DEREGISTRATION", (park) => {
        this.parkingspots = this.parkingspots.filter(
          (p) => p.endpoint !== park.endpoint
        );
        this.vehiclecounters = this.vehiclecounters.filter(
          (p) => p.endpoint !== park.endpoint
        );
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
        (response) => {
          this.loading = false; 
          this.parkingspots = response.data.filter((p) => p.parkingspotId !== "")
          this.vehiclecounters = response.data.filter((p) => p.vehiclecounterId !== "")
        }
      );
    
    this.axios
      .get("api/parkingspots/info")
      .then(
        (response) => {
          this.loading = false; 
          this.info = response.data;
        }
      );
  },
  beforeDestroy() {
    // close eventsource on destroy
    this.sse.disconnect();
  }
};
</script>
