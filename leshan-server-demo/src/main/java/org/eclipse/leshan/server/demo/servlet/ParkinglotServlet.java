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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.server.californium.LeshanServer;
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

    public ParkinglotServlet(LeshanServer server) {
        this.server = server;
        server.getObservationService().addListener(this.observationListener);
        server.getRegistrationService().addListener(this.registrationListener);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);
        this.mapper = mapper;
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
        resp.setContentType("application/json");
        resp.getOutputStream().write(getParkingspotsJson().getBytes(StandardCharsets.UTF_8));
        resp.setStatus(HttpServletResponse.SC_OK);
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
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        parkingspot.put("endpoint", registration.getEndpoint());
        parkingspots.put(registration.getId(), parkingspot);
    }

    public void parkingspotOnResponse(SingleObservation observation, Registration registration, ObserveResponse response) {
        LwM2mNode resource = response.getContent();
        LOG.info(registration.getId());
        HashMap<String, String> parkinglot = parkingspots.get(registration.getId());
        String path = observation.getPath().toString();
        if (resource instanceof LwM2mSingleResource) {
            if (path.equals("/32800/0/32700")) {
                path = "parkingspotId";
            } else if (path.equals("/32800/0/32701")) {
                path = "parkingspotState";
            }
            parkinglot.put(path, ((LwM2mSingleResource) resource).getValue().toString());
            parkingspots.put(registration.getId(), parkinglot);
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
