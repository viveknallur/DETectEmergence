/*
 * Copyright 2014 Amal Elgammal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;
import java.io.FileReader;
import java.io.IOException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import org.ini4j.Ini;
import java.io.PrintWriter;

/**
 * Calculates the least noisy route- independent of a vehicle as the calculation is based on the
 * noise data linked to edges stored in Redis
 * <p>
 * @author Amal Elgammal
 */
public class LeastNoisyWeighting implements Weighting
{
    String currentCity;
    String sensorReading = "noise";
    Jedis jedis;

    public LeastNoisyWeighting()
    {

    }

    public LeastNoisyWeighting( String city ) throws JedisConnectionException, JedisDataException
    {
        //System.out.println("LeastNoiseWeighting instantiated with city parameter!");
        this.currentCity = city;
        //System.out.println("this.currentCity inside constructor = " + this.currentCity);
        String host="";
        String fileName = "./sensors-config-files/" + this.currentCity + ".config";
        System.out.println("fileName = " + fileName);
        try
        {
            Ini ini = new Ini(new FileReader(fileName));
            host = ini.get("ConnectionSettings").fetch("REDIS_URL");
            System.out.println("jedisHost = " + host);
        } catch (IOException e)
        {
            System.out.println("IOError: " + e.getMessage());
        }

        try
        {
            jedis = new Jedis(host);

        } catch (JedisConnectionException e)
        {
            System.out.println("JedisConnectionException: " + e.getMessage());
        } catch (JedisDataException e)
        {
            System.out.println("JedisDataException: " + e.getMessage());

        } catch (Exception e)
        {
            System.out.println("Error: " + e.getMessage());

        }

    }

    @Override
    public double getMinWeight( double noiseValue )
    {
        //TODO: Check if this needs to be updated with other routing algorithms
        return noiseValue;
    }

    @Override
    public double calcWeight( EdgeIteratorState edge, boolean reverse )
    {
        double noiseValue = getNoiseFromRedis(edge);
        return noiseValue;
    }

    double getNoiseFromRedis( EdgeIteratorState edge )
    {
        double noiseValue = 52;
        String ntime;

        String edgeName = edge.getName();

        if (edgeName.length() > 0)
        {
            if (edgeName.contains(","))
            {
                edgeName = edgeName.split(",")[0];
            }
            
            edgeName = this.currentCity + "_" + this.sensorReading + "_" + edgeName;
            System.out.println("edgeName = " + edgeName);
            if (jedis.exists(edgeName))
            {
                noiseValue = Double.parseDouble(jedis.hget(edgeName, "value"));
                System.out.println("noiseValue = " + noiseValue);
                ntime = jedis.hget(edgeName, "timestamp");
            }
        }

        //TODO: check what to do with time and how to amend the instruction list with noise readings and timestamp 
        return noiseValue;
    }

    @Override
    public String toString()
    {
        //TODO: check if we need to register it with the encodering manger or elsewhere
        return "LEAST_NOISY";
    }

}
