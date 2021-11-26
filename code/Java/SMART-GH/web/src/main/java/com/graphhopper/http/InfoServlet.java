/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.http;

import com.graphhopper.GraphHopper;
import com.graphhopper.storage.StorableProperties;
import com.graphhopper.util.Constants;
import com.graphhopper.util.Helper;
import com.graphhopper.util.shapes.BBox;
import java.io.IOException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import org.json.JSONObject;
import org.ini4j.Ini;
import java.io.FileReader;

/**
 * @author Peter Karich
 */
public class InfoServlet extends GHBaseServlet
{
    @Inject
    private GraphHopper hopper;

    @Override
    public void doGet( HttpServletRequest req, HttpServletResponse res ) throws ServletException, IOException
    {
        try
        {
            writeInfos(req, res);
        } catch (IllegalArgumentException ex)
        {
            writeError(res, SC_BAD_REQUEST, ex.getMessage());
        } catch (Exception ex)
        {
            logger.error("Error while executing request: " + req.getQueryString(), ex);
            writeError(res, SC_INTERNAL_SERVER_ERROR, "Problem occured:" + ex.getMessage());
        }
    }

    void writeInfos( HttpServletRequest req, HttpServletResponse res ) throws Exception
    {
        BBox bb = hopper.getGraph().getBounds();
        List<Double> list = new ArrayList<Double>(4);
        list.add(bb.minLon);
        list.add(bb.minLat);
        list.add(bb.maxLon);
        list.add(bb.maxLat);

        JSONObject json = new JSONObject();
        json.put("bbox", list);

        String[] vehicles = hopper.getGraph().getEncodingManager().toString().split(",");
        json.put("supported_vehicles", vehicles);
        JSONObject features = new JSONObject();
        for (String v : vehicles)
        {
            JSONObject perVehicleJson = new JSONObject();
            perVehicleJson.put("elevation", hopper.hasElevation());
            features.put(v, perVehicleJson);
        }
        json.put("features", features);

        json.put("version", Constants.VERSION);
        json.put("build_date", Constants.BUILD_DATE);

        StorableProperties props = hopper.getGraph().getProperties();
        json.put("import_date", props.get("osmreader.import.date"));

        if (!Helper.isEmpty(props.get("prepare.date")))
            json.put("prepare_date", props.get("prepare.date"));
        
        //@Amal Elgammal: Append sent json to also include available senser data with respect
        //to the opened map. Also map name and city name are sent.
        String osmFile = hopper.getOSMFile();
        //System.out.println("osmFile=" + osmFile);
        
        ArrayList sensorsTxt = new ArrayList();
        
        sensorsTxt = getAvailableSensors(osmFile);
        //logger.info("These weighting are also available for "+ osmFile +":" + sensorsTxt.toString());
        json.put("osmFile", osmFile);
        json.put("city", getCity(osmFile));
        json.put("sensors",sensorsTxt);
        
        writeJson(req, res, json);
        
       
    }
    
    ArrayList getAvailableSensors(String osmFile) throws IOException
    {
        //we assume that names of the osm files should be in this format <city><optional '-'><any optional string><.*>
        
        String cityName = getCity(osmFile);
        
        //sensors configuration files are named as cityname.config

        String fileName = "./sensors-config-files/"+cityName + ".config";

        ArrayList sensorsTxt = new ArrayList();
        try
        {
            Ini ini = new Ini(new FileReader(fileName));
          
            for(String key: ini.get("SensorsAvailable").keySet())
            {
                String sensorName = ini.get("SensorsAvailable").fetch(key);
                String text = ini.get(sensorName).fetch("text");
                sensorsTxt.add(text);
            }
           
        } catch (IOException e)
        {
            logger.error(e.getMessage());
        }
           return sensorsTxt;
    }
    
    String getCity(String osmFile)
    {
        int num = osmFile.split("/").length;
        String cityName = osmFile.split("/")[num-1];
        
        cityName = cityName.split("\\.")[0];
        if (cityName.contains("-"))
             cityName = cityName.split("-")[0];
        
        return cityName;
    }
}
