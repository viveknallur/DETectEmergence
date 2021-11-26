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

import java.util.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import org.ini4j.Ini;
import java.io.PrintWriter;

/**
 *
 * @author elgammaa
 */
public class SmallTests
{
    public static void main( String[] strs ) throws Exception
    {
        String host = "";

        String fileName = "../sensors-config-files/" + "dublin" + ".config";

        ArrayList sensorsTxt = new ArrayList();

        try
        {
            /*PrintWriter writer = new PrintWriter("Where-is-that.txt", "UTF-8");
            writer.println("The first line");
            writer.println("The second line");
            writer.close();*/
            
            Ini ini = new Ini(new FileReader(fileName));
            System.out.println("host = " + ini.get("ConnectionSettings").fetch("REDIS_URL"));
           
            /*for (String key : ini.get("ConnectionSettings").keySet())
             {
                  System.out.println("key = " + key + ", and value = "+ ini.get("ConnectionSettings").fetch(key));
             }*/
        } catch (IOException e)
        {
            System.out.println(e.getMessage());
        }

    }

}
