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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.request.exception.*;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.demo.model.ObjectModelSerDes;
import org.eclipse.leshan.server.demo.servlet.json.JacksonLwM2mNodeDeserializer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonLwM2mNodeSerializer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonRegistrationSerializer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonResponseSerializer;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
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
public class ParkingspotServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ParkingspotServlet.class);
    private static final long serialVersionUID = 1L;
    private final LeshanServer server;
    private final ObjectMapper mapper;
    private HashMap<String, HashMap<String, String>> parkinglots = new HashMap<>();

    public ParkingspotServlet(LeshanServer server) {
        this.server = server;
        server.getObservationService().addListener(this.observationListener);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String json = new JSONArray(this.parkinglots.values().toArray()).toString();
        resp.setContentType("application/json");
        resp.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void newObservation(Observation observation, Registration registration) {
            parkinglots.put(observation.getRegistrationId(), new HashMap<>());
        }

        @Override
        public void cancelled(Observation observation) {
        }

        @Override
        public void onResponse(SingleObservation observation, Registration registration, ObserveResponse response) {
            String id = observation.getRegistrationId() + observation.getPath().toString();

            LOG.info(observation.getRegistrationId());
            LOG.info(observation.getPath().toString());
            LOG.info("{}", response.getContent().getId());

            LwM2mNode resource = response.getContent();
            HashMap<String, String> parkinglot = parkinglots.get(observation.getRegistrationId());
            if (resource instanceof LwM2mSingleResource) {
                parkinglot.put(observation.getPath().toString(), ((LwM2mSingleResource) resource).getValue().toString());
                parkinglots.put(observation.getRegistrationId(), parkinglot);
            }
        }

        @Override
        public void onResponse(CompositeObservation observation, Registration registration, ObserveCompositeResponse response) {
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {

        }
    };
}
