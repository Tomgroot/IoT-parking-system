/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonLwM2mNodeDeserializer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonLwM2mNodeSerializer;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service HTTP REST API calls.
 */
public class ParkinglotServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ParkinglotServlet.class);
    private static final long serialVersionUID = 1L;
    private final LeshanServer server;
    private HashMap<String, HashMap<String, String>> parkingspots = new HashMap<>();
    private static ParkinglotServlet parkinglotServlet;
    private HashMap<String, Object> parkinglotProperties = new HashMap<>();
    private final ObjectMapper mapper;

    public ParkinglotServlet(LeshanServer server) {
        this.server = server;
        server.getObservationService().addListener(this.observationListener);
        server.getRegistrationService().addListener(this.registrationListener);
        parkinglotProperties.put("name", "P1");
        parkinglotProperties.put("capacity", 0);
        parkinglotProperties.put("reservations", 0);
        parkinglotProperties.put("vehicles", 0);
        parkinglotProperties.put("rate", 1);
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        module.addSerializer(LwM2mNode.class, new JacksonLwM2mNodeSerializer());
        module.addDeserializer(LwM2mNode.class, new JacksonLwM2mNodeDeserializer());
        mapper.registerModule(module);
        parkinglotServlet = this;
    }

    public static ParkinglotServlet getInstance() {
        return parkinglotServlet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        resp.setContentType("application/json");
        if (path != null && path[path.length - 1].equals("info")) {
            resp.getOutputStream().write(getParkinglotPropertiesJson().getBytes(StandardCharsets.UTF_8));
        } else {
            resp.getOutputStream().write(getParkingspotsJson().getBytes(StandardCharsets.UTF_8));
        }
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String content = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
        JSONObject json = new JSONObject(content);
        parkinglotProperties.put("rate", json.get("rate"));
        resp.setContentType("application/json");
        resp.getOutputStream().write(getParkinglotPropertiesJson().getBytes(StandardCharsets.UTF_8));
        resp.setStatus(HttpServletResponse.SC_OK);
        return;
    }

    public String getParkinglotPropertiesJson() {
        return new JSONObject(parkinglotProperties).toString();
    }

    public String getParkingspotsJson() {
        return new JSONArray(this.parkingspots.values().toArray()).toString();
    }

    public String getSingleParkingspotJson(String id) {
        return new JSONObject(this.parkingspots.get(id)).toString();
    }

    public void parkingspotNewRegistration(Registration registration) {
        HashMap<String, String> parkingspot = new HashMap<>();
        try {
            LwM2mNode spotId = server.send(registration, new ReadRequest(32800,0,32700)).getContent();
            if (spotId instanceof LwM2mSingleResource) {
                parkingspot.put("parkingspotId", ((LwM2mSingleResource) spotId).getValue().toString());
            }
            LwM2mNode spotState = server.send(registration, new ReadRequest(32800,0,32701)).getContent();
            if (spotId instanceof LwM2mSingleResource) {
                parkingspot.put("parkingspotState", ((LwM2mSingleResource) spotState).getValue().toString());
                occupyParkingSpot(registration, ((LwM2mSingleResource) spotState).getValue().toString(), true);
            }
            LwM2mNode vehiclecounterId = server.send(registration, new ReadRequest(32801,0,32700)).getContent();
            if (spotId instanceof LwM2mSingleResource) {
                parkingspot.put("vehiclecounterId", ((LwM2mSingleResource) vehiclecounterId).getValue().toString());
            }
            LwM2mNode vehicleCounter = server.send(registration, new ReadRequest(32801,0,32703)).getContent();
            if (spotId instanceof LwM2mSingleResource) {
                parkingspot.put("vehicleCounter", ((LwM2mSingleResource) vehicleCounter).getValue().toString());
            }
            LwM2mNode direction = server.send(registration, new ReadRequest(32801,0,32705)).getContent();
            if (spotId instanceof LwM2mSingleResource) {
                parkingspot.put("direction", ((LwM2mSingleResource) direction).getValue().toString());
            }
            LwM2mNode lotNameVehicleCounter = server.send(registration, new ReadRequest(32801,0,32706)).getContent();
            if (spotId instanceof LwM2mSingleResource) {
                parkingspot.put("lotNameVehicleCounter", ((LwM2mSingleResource) lotNameVehicleCounter).getValue().toString());
            }
            LwM2mNode spotVehicle = server.send(registration, new ReadRequest(32800,0,32702)).getContent();
            if (spotId instanceof LwM2mSingleResource) {
                parkingspot.put("vehicleId", ((LwM2mSingleResource) spotVehicle).getValue().toString());
            }
            LwM2mNode spotName = server.send(registration, new ReadRequest(32800,0,32706)).getContent();
            if (spotId instanceof LwM2mSingleResource) {
                parkingspot.put("lotName", ((LwM2mSingleResource) spotName).getValue().toString());
            }
            LwM2mNode textDisplay = server.send(registration, new ReadRequest(3341,0,5527)).getContent();
            if (spotId instanceof LwM2mSingleResource) {
                parkingspot.put("textdisplay", ((LwM2mSingleResource) textDisplay).getValue().toString());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        parkingspot.put("endpoint", registration.getEndpoint());
        parkingspots.put(registration.getId(), parkingspot);
        updateParkinglotProperties();
    }

    private void updateParkinglotProperties() {
        int capicity = 0;
        int reservations = 0;
        int vehicles = 0;
        for(HashMap.Entry<String, HashMap<String, String>> clients : parkingspots.entrySet()) {
            for(HashMap.Entry<String, String> entry : clients.getValue().entrySet()) {
                String key = entry.getKey();
                if (key.equals("parkingspotState")) {
                    String value = entry.getValue();
                    if (value.equals("Free")) {
                        capicity += 1;
                    } else if (value.equals("Reserved")) {
                        reservations += 1;
                    } else if (value.equals("Occupied")) {
                        vehicles += 1;
                    }
                }
            }
        }
        parkinglotProperties.put("capacity", capicity);
        parkinglotProperties.put("reservations", reservations);
        parkinglotProperties.put("vehicles", vehicles);
    }

    public void parkingspotOnResponse(SingleObservation observation, Registration registration, ObserveResponse response) {
        LwM2mNode resource = response.getContent();
        HashMap<String, String> parkinglot = parkingspots.get(registration.getId());
        String path = observation.getPath().toString();
        if (resource instanceof LwM2mSingleResource) {
            if (path.equals("/32800/0/32700")) {
                path = "parkingspotId";
            } else if (path.equals("/32801/0/32700")) {
                path = "vehiclecounterId";
            } else if (path.equals("/32800/0/32701")) {
                path = "parkingspotState";
                occupyParkingSpot(registration, ((LwM2mSingleResource) resource).getValue().toString(), true);
            } else if (path.equals("/3341/0/5527")) {
                path = "textdisplay";
            } else if (path.equals("/32800/0/32706")) {
                path = "lotName";
            } else if (path.equals("/32800/0/32702")) {
                path = "vehicleId";
            } else if (path.equals("/32801/0/32704")) {
                path = "lastPlate";
            } else if (path.equals("/32801/0/32705")) {
                path = "direction";
            } else if (path.equals("/32801/0/32703")) {
                path = "vehicleCounter";
            } else if (path.equals("/32801/0/32706")) {
                path = "lotNameVehicleCounter";
            } else if (path.equals("/3345/0/5703")) {
                if (Float.parseFloat(((LwM2mSingleResource) resource).getValue().toString()) > 0
                        && parkinglot.get("parkingspotState").equals("Free")) {
                    occupyParkingSpot(registration, "Occupied", false);
                } else if (Float.parseFloat(((LwM2mSingleResource) resource).getValue().toString()) < 0
                        && parkinglot.get("parkingspotState").equals("Occupied")) {
                    occupyParkingSpot(registration, "Free", false);
                }
            }
            if (path.equals("lastPlate") && parkinglot.get("direction").equals("1")
                    && ((LwM2mSingleResource) resource).getValue().toString().equals(parkinglot.get("vehicleId"))) {
                if (checkKenteken()) {
                    occupyParkingSpot(registration, "Occupied", false);
                }
            }
            parkinglot.put(path, ((LwM2mSingleResource) resource).getValue().toString());
            parkingspots.put(registration.getId(), parkinglot);
            updateParkinglotProperties();
        }
    }

    private boolean checkKenteken () {
        //TODO
        return true;
    }

    private void occupyParkingSpot(Registration registration, String state, boolean colorOnly) {
        String color = "green";
        if (state.equals("Occupied")) {
            color = "red";
        } else if (state.equals("Reserved")) {
            color = "orange";
        }

        LwM2mNode node;
        try {
            if (!colorOnly) {
                node = mapper.readValue("{\"id\":32701,\"kind\":\"singleResource\",\"value\":\"" + state + "\",\"type\":\"string\"}", LwM2mNode.class);
                WriteRequest requestState = new WriteRequest(WriteRequest.Mode.REPLACE, ContentFormat.fromName("TLV"), "/32800/0/32701", node);
                WriteResponse cResponseState = server.send(registration, requestState, 5000);
            }

            node = mapper.readValue("{\"id\":5527,\"kind\":\"singleResource\",\"value\":\""+color+"\",\"type\":\"string\"}", LwM2mNode.class);
            WriteRequest requestTextdisplay = new WriteRequest(WriteRequest.Mode.REPLACE, ContentFormat.fromName("TLV"), "/3341/0/5527", node);
            WriteResponse cResponseTextdisplay = server.send(registration, requestTextdisplay, 5000);
        } catch (JsonProcessingException | InterruptedException e) {
            throw new InvalidRequestException(e, "%s", e.getMessage());
        }
    }

    private final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void newObservation(Observation observation, Registration registration) {
            parkingspotNewRegistration(registration);
        }

        @Override
        public void cancelled(Observation observation) { }

        @Override
        public void onResponse(SingleObservation observation, Registration registration, ObserveResponse response) {
            parkingspotOnResponse(observation, registration, response);
        }

        @Override
        public void onResponse(CompositeObservation observation, Registration registration, ObserveCompositeResponse response) { }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) { }
    };

    final RegistrationListener registrationListener = new RegistrationListener() {

        public void registered(Registration registration, Registration previousReg,
                               Collection<Observation> previousObsersations) { }

        @Override
        public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) { }

        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                                 Registration newReg) {
            parkingspots.remove(registration.getId());
        }
    };
}
