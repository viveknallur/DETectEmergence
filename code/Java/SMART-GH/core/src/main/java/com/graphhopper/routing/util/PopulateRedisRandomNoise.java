/*
 * Copyright 2014 elgammaa.
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

import java.util.Random;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 *
 * @author elgammaa
 */
public class PopulateRedisRandomNoise
{

    public static void main( String[] strs ) throws Exception, JedisConnectionException, JedisDataException
    {

        SAXParserFactory parserFactor = SAXParserFactory.newInstance();
        SAXParser parser = parserFactor.newSAXParser();
        SAXHandler handler = new SAXHandler();
        parser.parse("../maps/output.xml", handler);
        System.out.println("Number of ways = " + handler.waysIDs.size());
        try
        {
            Jedis jedis = new Jedis("localhost");
            jedis.flushAll();

            for (int i = 0; i < handler.waysIDs.size(); i++)
            {
                String hashkey = handler.waysIDs.get(i);
                //System.out.println("hashkey = "+hashkey);
                hashkey = "dublin_noise_" + hashkey;

                Random noiseValue = new Random();
                double returnedNoise = noiseValue.nextInt(80);
                String noise = String.valueOf(returnedNoise);
                Random noiseTime = new Random();
                double returnedTime = noiseTime.nextInt(23);
                String ntime = String.valueOf(returnedTime) + "time";

                if (!jedis.exists(hashkey))
                {
                    jedis.hset(hashkey, "value", noise);

                } else
                {
                    String currentNoise = jedis.hget(hashkey, "value");
                    returnedNoise = (returnedNoise + Double.parseDouble(currentNoise)) / 2;
                    noise = String.valueOf(returnedNoise);
                    jedis.hset(hashkey, "value", noise);
                }

                jedis.hset(hashkey, "timestamp", ntime);

            }

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
}

class SAXHandler extends DefaultHandler
{
    ArrayList<String> waysIDs = new ArrayList<String>();
    String wayId = "";
    String wayName = "";
    String wayRef = "";
    int refFlag = 0;

    public SAXHandler()
    {
        super();
    }

    @Override
    public void startElement( String uri, String localName, String qName, Attributes attributes ) throws SAXException
    {

        if (qName.equalsIgnoreCase("way"))
        {
            wayId = attributes.getValue("id");
        } else if (qName.equalsIgnoreCase("tag"))
        {
            if (attributes.getValue("k").equalsIgnoreCase("name"))
            {
                wayName = attributes.getValue("v");

            } else if (attributes.getValue("k").equalsIgnoreCase("ref"))
            {
                wayRef = attributes.getValue("v");
                refFlag = 1;
            }
        }

    }

    @Override
    public void endElement( String uri, String localName, String qName ) throws SAXException
    {
        //exclude wayid and could be added (TODO) it as a set inside the hash; e.g. key: dublin_noise_Parnell_R111, fields:(noise, timestamp, wayIDs:Set)

        if (qName.equalsIgnoreCase("way"))
        {
            if (wayName.length() > 0)
            {
                String firstToken = wayName.split(" ")[0];

                if (firstToken.matches(".*\\d.*"))
                {
                    if (wayName.length() > firstToken.length())
                        wayName = wayName.substring(firstToken.length() + 1);
                }

                // if (refFlag == 1)
                //    waysIDs.add(wayName + "_" + wayRef);
                //else
                waysIDs.add(wayName);
            }
            refFlag = 0;

        }

    }

}
