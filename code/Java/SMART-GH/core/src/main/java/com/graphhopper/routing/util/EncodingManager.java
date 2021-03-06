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
package com.graphhopper.routing.util;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.graphhopper.reader.OSMNode;
import com.graphhopper.reader.OSMReader;
import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMTurnRelation;
import com.graphhopper.reader.OSMTurnRelation.TurnCostTableEntry;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;
import java.util.*;

/**
 * Manager class to register encoder, assign their flag values and check objects with all encoders
 * during parsing.
 * <p/>
 * @author Peter Karich
 * @author Nop
 */
public class EncodingManager
{
    public static final String CAR = "car";
    public static final String BIKE = "bike";
    public static final String BIKE2 = "bike2";
    public static final String RACINGBIKE = "racingbike";
    public static final String MOUNTAINBIKE = "mtb";
    public static final String FOOT = "foot";
    private static final Map<String, String> defaultEdgeFlagEncoders = new HashMap<String, String>();
    private static final Map<String, String> defaultTurnFlagEncoders = new HashMap<String, String>();

    static
    {
        defaultEdgeFlagEncoders.put(CAR, CarFlagEncoder.class.getName());
        defaultEdgeFlagEncoders.put(BIKE, BikeFlagEncoder.class.getName());
        defaultEdgeFlagEncoders.put(BIKE2, Bike2WeightFlagEncoder.class.getName());
        defaultEdgeFlagEncoders.put(RACINGBIKE, RacingBikeFlagEncoder.class.getName());
        defaultEdgeFlagEncoders.put(MOUNTAINBIKE, MountainBikeFlagEncoder.class.getName());
        defaultEdgeFlagEncoders.put(FOOT, FootFlagEncoder.class.getName());
    }

    private final List<AbstractFlagEncoder> edgeEncoders = new ArrayList<AbstractFlagEncoder>();
    private int edgeEncoderNextBit = 0;

    private int nextWayBit = 0;
    private int nextNodeBit = 0;
    private int nextRelBit = 0;
    private int nextTurnBit = 0;
    private final int bytesForFlags;
    private final int maxTurnFlagsBits;
    private final int maxTurnCost;
    private boolean enableInstructions = true;

    /**
     * Instantiate manager with the given list of encoders. The manager knows the default encoders:
     * CAR, FOOT and BIKE (ignoring the case). Custom encoders can be specified by giving a full
     * class name e.g. "car:com.graphhopper.myproject.MyCarEncoder"
     * <p/>
     * @param flagEncodersStr comma delimited list of encoders. The order does not matter.
     */
    public EncodingManager( String flagEncodersStr )
    {
        this(flagEncodersStr, 4);
    }

    public EncodingManager( String flagEncodersStr, int bytesForFlags )
    {
        this(flagEncodersStr, bytesForFlags, 0);
    }

    public EncodingManager( String flagEncodersStr, int bytesForFlags, int maxTurnCost )
    {
        this(Arrays.asList(readFromEncoderString(defaultEdgeFlagEncoders, flagEncodersStr).toArray(new FlagEncoder[0])), bytesForFlags, maxTurnCost);
    }

    /**
     * Instantiate manager with the given list of encoders.
     * <p/>
     * @param flagEncoders comma delimited list of encoders. The order does not matter.
     */
    public EncodingManager( FlagEncoder... flagEncoders )
    {
        this(Arrays.asList(flagEncoders));
    }

    /**
     * Instantiate manager with the given list of encoders.
     * <p/>
     * @param flagEncoders comma delimited list of encoders. The order does not matter.
     */
    public EncodingManager( List<? extends FlagEncoder> flagEncoders )
    {
        // we don't need turn costs yet, but only restrictions => maxTurnCost = 0
        this(flagEncoders, 4, 0);
    }

    public EncodingManager( List<? extends FlagEncoder> flagEncoders, int bytesForFlags, int maxTurnCost )
    {
        if (bytesForFlags != 4 && bytesForFlags != 8)
            throw new IllegalStateException("For 'flags' currently only 4 or 8 bytes supported");

        this.maxTurnCost = maxTurnCost;
        this.bytesForFlags = bytesForFlags * 8;
        this.maxTurnFlagsBits = bytesForFlags * 8;

        Collections.sort(flagEncoders, new Comparator<FlagEncoder>()
        {
            @Override
            public int compare( FlagEncoder o1, FlagEncoder o2 )
            {
                return o1.getClass().toString().compareTo(o2.getClass().toString());
            }
        });
        for (FlagEncoder flagEncoder : flagEncoders)
        {
            registerEncoder((AbstractFlagEncoder) flagEncoder);
        }
    }

    public int getBytesForFlags()
    {
        return bytesForFlags / 8;
    }

    private static List<FlagEncoder> readFromEncoderString( Map<String, String> defaultEncoders, String encoderList )
    {
        String[] entries = encoderList.split(",");
        List<FlagEncoder> resultEncoders = new ArrayList<FlagEncoder>();

        for (String entry : entries)
        {
            entry = entry.trim();
            if (entry.isEmpty())
                continue;

            String className = null;
            int pos = entry.indexOf(":");
            if (pos > 0)
            {
                className = entry.substring(pos + 1);
            } else
            {
                className = defaultEncoders.get(entry.toLowerCase());
                if (className == null)
                    throw new IllegalArgumentException("Unknown encoder name " + entry);
            }

            try
            {
                @SuppressWarnings("unchecked")
                Class<FlagEncoder> cls = (Class<FlagEncoder>) Class.forName(className);
                resultEncoders.add((FlagEncoder) cls.getDeclaredConstructor().newInstance());
            } catch (Exception e)
            {
                throw new IllegalArgumentException("Cannot instantiate class " + className, e);
            }
        }
        return resultEncoders;
    }

    private static final String ERR = "Encoders are requesting more than %s bits of %s flags. ";
    private static final String WAY_ERR = "Decrease the number of vehicles or increase the flags to take long.";

    private void registerEncoder( AbstractFlagEncoder encoder )
    {
        int encoderCount = edgeEncoders.size();
        int usedBits = encoder.defineNodeBits(encoderCount, edgeEncoderNextBit);
        if (usedBits > bytesForFlags)
            throw new IllegalArgumentException(String.format(ERR, bytesForFlags, "node"));
        encoder.setNodeBitMask(usedBits - nextNodeBit, nextNodeBit);
        nextNodeBit = usedBits;

        usedBits = encoder.defineWayBits(encoderCount, nextWayBit);
        if (usedBits > bytesForFlags)
            throw new IllegalArgumentException(String.format(ERR, bytesForFlags, "way") + WAY_ERR);
        encoder.setWayBitMask(usedBits - nextWayBit, nextWayBit);
        nextWayBit = usedBits;

        usedBits = encoder.defineRelationBits(encoderCount, nextRelBit);
        if (usedBits > bytesForFlags)
            throw new IllegalArgumentException(String.format(ERR, bytesForFlags, "relation"));
        encoder.setRelBitMask(usedBits - nextRelBit, nextRelBit);
        nextRelBit = usedBits;

        edgeEncoderNextBit = usedBits;

        // turn flag bits are independent from edge encoder bits
        usedBits = encoder.defineTurnBits(encoderCount, nextTurnBit, determineRequiredBits(maxTurnCost));
        if (usedBits > maxTurnFlagsBits)
            throw new IllegalArgumentException(String.format(ERR, bytesForFlags, "turn"));
        nextTurnBit = usedBits;

        //everything okay, add encoder
        edgeEncoders.add(encoder);
    }

    /**
     * @return true if the specified encoder is found
     */
    public boolean supports( String encoder )
    {
        return getEncoder(encoder, false) != null;
    }

    public FlagEncoder getEncoder( String name )
    {
        return getEncoder(name, true);
    }

    private FlagEncoder getEncoder( String name, boolean throwExc )
    {
        for (AbstractFlagEncoder encoder : edgeEncoders)
        {
            if (name.equalsIgnoreCase(encoder.toString()))
                return encoder;
        }
        if (throwExc)
            throw new IllegalArgumentException("Encoder for " + name + " not found. Existing: " + toDetailsString());
        return null;
    }

    /**
     * Determine whether an osm way is a routable way for one of its encoders.
     */
    public long acceptWay( OSMWay way )
    {
        long includeWay = 0;
        for (AbstractFlagEncoder encoder : edgeEncoders)
        {
            includeWay |= encoder.acceptWay(way);
        }

        return includeWay;
    }

    public long handleRelationTags( OSMRelation relation, long oldRelationFlags )
    {
        long flags = 0;
        for (AbstractFlagEncoder encoder : edgeEncoders)
        {
            flags |= encoder.handleRelationTags(relation, oldRelationFlags);
        }

        return flags;
    }

    /**
     * Processes way properties of different kind to determine speed and direction. Properties are
     * directly encoded in 8 bytes.
     * <p/>
     * @param relationFlags The preprocessed relation flags is used to influence the way properties.
     * @return the encoded flags
     */
    public long handleWayTags( OSMWay way, long includeWay, long relationFlags )
    {
        long flags = 0;
        for (AbstractFlagEncoder encoder : edgeEncoders)
        {
            flags |= encoder.handleWayTags(way, includeWay, relationFlags & encoder.getRelBitMask());
        }

        return flags;
    }

    public int getVehicleCount()
    {
        return edgeEncoders.size();
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        for (AbstractFlagEncoder encoder : edgeEncoders)
        {
            if (str.length() > 0)
                str.append(",");

            str.append(encoder.toString());
        }

        return str.toString();
    }

    public String toDetailsString()
    {
        StringBuilder str = new StringBuilder();
        for (AbstractFlagEncoder encoder : edgeEncoders)
        {
            if (str.length() > 0)
                str.append(",");

            str.append(encoder.toString());
            str.append(":");
            str.append(encoder.getClass().getName());
        }

        return str.toString();
    }

    public FlagEncoder getSingle()
    {
        if (getVehicleCount() > 1)
            throw new IllegalStateException("Multiple encoders are active. cannot return one:" + toString());

        if (getVehicleCount() == 0)
            throw new IllegalStateException("No encoder is active!");

        return edgeEncoders.get(0);
    }

    public long flagsDefault( boolean forward, boolean backward )
    {
        long flags = 0;
        for (AbstractFlagEncoder encoder : edgeEncoders)
        {
            flags |= encoder.flagsDefault(forward, backward);
        }
        return flags;
    }

    /**
     * Reverse flags, to do so all encoders are called.
     */
    public long reverseFlags( long flags )
    {
        // performance critical
        int len = edgeEncoders.size();
        for (int i = 0; i < len; i++)
        {
            flags = edgeEncoders.get(i).reverseFlags(flags);
        }
        return flags;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 53 * hash + (this.edgeEncoders != null ? this.edgeEncoders.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        final EncodingManager other = (EncodingManager) obj;
        if (this.edgeEncoders != other.edgeEncoders && (this.edgeEncoders == null || !this.edgeEncoders.equals(other.edgeEncoders)))
        {
            return false;
        }
        return true;
    }

    /**
     * Analyze tags on osm node. Store node tags (barriers etc) for later usage while parsing way.
     */
    public long handleNodeTags( OSMNode node )
    {
        long flags = 0;
        for (AbstractFlagEncoder encoder : edgeEncoders)
        {
            flags |= encoder.handleNodeTags(node);
        }

        return flags;
    }

    private static int determineRequiredBits( int value )
    {
        int numberOfBits = 0;
        while (value > 0)
        {
            value = value >> 1;
            numberOfBits++;
        }
        return numberOfBits;
    }

    public Collection<TurnCostTableEntry> analyzeTurnRelation( OSMTurnRelation turnRelation, OSMReader osmReader )
    {
        TLongObjectMap<TurnCostTableEntry> entries = new TLongObjectHashMap<OSMTurnRelation.TurnCostTableEntry>();

        int encoderCount = edgeEncoders.size();
        for (int i = 0; i < encoderCount; i++)
        {
            AbstractFlagEncoder encoder = edgeEncoders.get(i);
            for (TurnCostTableEntry entry : encoder.analyzeTurnRelation(turnRelation, osmReader))
            {
                TurnCostTableEntry oldEntry = entries.get(entry.getItemId());
                if (oldEntry != null)
                {
                    // merging different encoders
                    oldEntry.flags |= entry.flags;
                } else
                {
                    entries.put(entry.getItemId(), entry);
                }
            }
        }

        return entries.valueCollection();
    }

    public EncodingManager setEnableInstructions( boolean enableInstructions )
    {
        this.enableInstructions = enableInstructions;
        return this;
    }

    public void applyWayTags( OSMWay way, EdgeIteratorState edge )
    {
        // storing the road name does not yet depend on the flagEncoder so manage it directly
        if (enableInstructions)
        {
            // String wayInfo = carFlagEncoder.getWayInfo(way);
            // http://wiki.openstreetmap.org/wiki/Key:name
            String name = fixWayName(way.getTag("name"));
            // http://wiki.openstreetmap.org/wiki/Key:ref
            String osmId = fixWayName(way.getTag("id"));
            String refName = fixWayName(way.getTag("ref"));
            if (!Helper.isEmpty(refName))
            {
                if (Helper.isEmpty(name))
                    name = refName;// + ", " + osmId;
                else
                    name += ", " + refName;// + ", " + osmId;
            }

            edge.setName(name);
        }

        for (AbstractFlagEncoder encoder : edgeEncoders)
        {
            encoder.applyWayTags(way, edge);
        }
    }

    static String fixWayName( String str )
    {
        if (str == null)
            return "";
        return str.replaceAll(";[ ]*", ", ");
    }
}
