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

import java.util.Collection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.reader.OSMNode;
import com.graphhopper.reader.OSMReader;
import com.graphhopper.reader.OSMTurnRelation;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMTurnRelation.TurnCostTableEntry;
import com.graphhopper.util.*;
import java.util.*;

/**
 * Abstract class which handles flag decoding and encoding. Every encoder should be registered to a
 * EncodingManager to be usable. If you want the full long to be stored you need to enable this in
 * the GraphHopperStorage.
 * <p/>
 * @author Peter Karich
 * @author Nop
 * @see EncodingManager
 */
public abstract class AbstractFlagEncoder implements FlagEncoder, TurnCostEncoder
{
    private final static Logger logger = LoggerFactory.getLogger(AbstractFlagEncoder.class);

    /* Edge Flag Encoder fields */
    private long nodeBitMask;
    private long wayBitMask;
    private long relBitMask;
    protected long forwardBit = 0;
    protected long backwardBit = 0;
    protected long directionBitMask = 0;
    protected long roundaboutBit = 0;
    protected EncodedDoubleValue speedEncoder;
    // bit to signal that way is accepted
    protected long acceptBit = 0;
    protected long ferryBit = 0;

    /* Turn Cost Flag Encoder fields */
    protected int maxCostsBits;
    protected long costsMask;

    protected long restrictionBit;
    protected long costShift;

    /* processing properties (to be initialized lazy when needed) */
    protected EdgeExplorer edgeOutExplorer;
    protected EdgeExplorer edgeInExplorer;

    /* restriction definitions where order is important */
    protected List<String> restrictions = new ArrayList<String>(5);
    protected HashSet<String> intendedValues = new HashSet<String>(5);
    protected HashSet<String> restrictedValues = new HashSet<String>(5);
    protected HashSet<String> ferries = new HashSet<String>(5);
    protected HashSet<String> oneways = new HashSet<String>(5);
    protected HashSet<String> acceptedRailways = new HashSet<String>(5);
    // http://wiki.openstreetmap.org/wiki/Mapfeatures#Barrier
    protected HashSet<String> absoluteBarriers = new HashSet<String>(5);
    protected HashSet<String> potentialBarriers = new HashSet<String>(5);
    // should potential barriers block when no access limits are given?
    protected boolean blockByDefault = true;
    protected boolean blockFords = true;
    protected int speedBits;
    protected double speedFactor;

    public AbstractFlagEncoder( int speedBits, double speedFactor )
    {
        this.speedBits = speedBits;
        this.speedFactor = speedFactor;
        oneways.add("yes");
        oneways.add("true");
        oneways.add("1");
        oneways.add("-1");

        ferries.add("shuttle_train");
        ferries.add("ferry");

        acceptedRailways.add("tram");
        acceptedRailways.add("abandoned");
        acceptedRailways.add("disused");
    }

    /**
     * Defines the bits for the node flags, which are currently used for barriers only.
     * <p>
     * @return incremented shift value pointing behind the last used bit
     */
    public int defineNodeBits( int index, int shift )
    {
        return shift;
    }

    /**
     * Defines bits used for edge flags used for access, speed etc.
     * <p/>
     * @param index
     * @param shift bit offset for the first bit used by this encoder
     * @return incremented shift value pointing behind the last used bit
     */
    public int defineWayBits( int index, int shift )
    {
        if (forwardBit != 0)
            throw new IllegalStateException("You must not register a FlagEncoder (" + toString() + ") twice!");

        // define the first 2 speedBits in flags for routing
        forwardBit = 1L << shift;
        backwardBit = 2L << shift;
        directionBitMask = 3L << shift;
        shift += 2;
        roundaboutBit = 1L << shift;
        shift++;

        // define internal flags for parsing
        index *= 2;
        acceptBit = 1L << index;
        ferryBit = 2L << index;

        return shift;
    }

    /**
     * Defines the bits which are used for relation flags.
     * <p>
     * @return incremented shift value pointing behind the last used bit
     */
    public int defineRelationBits( int index, int shift )
    {
        return shift;
    }

    /**
     * Defines the bits reserved for storing turn restriction and turn cost
     * <p>
     * @param shift bit offset for the first bit used by this encoder
     * @param numberCostsBits number of bits reserved for storing costs (range of values: [0,
     * 2^numberCostBits - 1] seconds )
     * @return incremented shift value pointing behind the last used bit
     */
    public int defineTurnBits( int index, int shift, int numberCostsBits )
    {
        this.maxCostsBits = numberCostsBits;

        int mask = 0;
        for (int i = 0; i < this.maxCostsBits; i++)
        {
            mask |= (1 << i);
        }
        this.costsMask = mask;

        restrictionBit = 1 << shift;
        costShift = shift + 1;
        return shift + maxCostsBits + 1;
    }

    /**
     * Analyze the properties of a relation and create the routing flags for the second read step.
     * In the pre-parsing step this method will be called to determine the useful relation tags.
     * <p/>
     */
    public abstract long handleRelationTags( OSMRelation relation, long oldRelationFlags );

    /**
     * Decide whether a way is routable for a given mode of travel. This skips some ways before
     * handleWayTags is called.
     * <p/>
     * @return the encoded value to indicate if this encoder allows travel or not.
     */
    public abstract long acceptWay( OSMWay way );

    /**
     * Analyze properties of a way and create the routing flags. This method is called in the second
     * parsing step.
     */
    public abstract long handleWayTags( OSMWay way, long allowed, long relationFlags );

    /**
     * Parse tags on nodes. Node tags can add to speed (like traffic_signals) where the value is
     * strict negative or blocks access (like a barrier), then the value is strict positive.This
     * method is called in the second parsing step.
     */
    public long handleNodeTags( OSMNode node )
    {
        // absolute barriers always block
        if (node.hasTag("barrier", absoluteBarriers))
            return directionBitMask;

        // movable barriers block if they are not marked as passable
        if (node.hasTag("barrier", potentialBarriers))
        {
            boolean locked = false;
            if (node.hasTag("locked", "yes"))
                locked = true;

            for (String res : restrictions)
            {
                if (!locked && node.hasTag(res, intendedValues))
                    return 0;
                
                if (node.hasTag(res, restrictedValues))
                    return directionBitMask;
            }

            if (blockByDefault)
                return directionBitMask;
        }

        if (blockFords
                && (node.hasTag("highway", "ford") || node.hasTag("ford"))
                && !node.hasTag(restrictions, intendedValues))
            return directionBitMask;

        return 0;
    }

    @Override
    public InstructionAnnotation getAnnotation( long flags, Translation tr )
    {
        return InstructionAnnotation.EMPTY;
    }

    /**
     * Swapping directions means swapping bits which are dependent on the direction of an edge like
     * the access bits. But also direction dependent speed values should be swapped too. Keep in
     * mind that this method is performance critical!
     */
    public long reverseFlags( long flags )
    {
        long dir = flags & directionBitMask;
        if (dir == directionBitMask || dir == 0)
            return flags;

        return flags ^ directionBitMask;
    }

    /**
     * Sets default flags with specified access.
     */
    public long flagsDefault( boolean forward, boolean backward )
    {
        long flags = speedEncoder.setDefaultValue(0);
        return setAccess(flags, forward, backward);
    }

    @Override
    public long setAccess( long flags, boolean forward, boolean backward )
    {
        return setBool(setBool(flags, K_BACKWARD, backward), K_FORWARD, forward);
    }

    @Override
    public long setSpeed( long flags, double speed )
    {
        if (speed < 0)
            throw new IllegalArgumentException("Speed cannot be negative: " + speed
                    + ", flags:" + BitUtil.LITTLE.toBitString(flags));

        if (speed > getMaxSpeed())
            speed = getMaxSpeed();
        return speedEncoder.setDoubleValue(flags, speed);
    }

    @Override
    public double getSpeed( long flags )
    {
        double speedVal = speedEncoder.getDoubleValue(flags);
        if (speedVal < 0)
            throw new IllegalStateException("Speed was negative!? " + speedVal);

        return speedVal;
    }

    @Override
    public long setReverseSpeed( long flags, double speed )
    {
        return setSpeed(flags, speed);
    }

    @Override
    public double getReverseSpeed( long flags )
    {
        return getSpeed(flags);
    }

    @Override
    public long setProperties( double speed, boolean forward, boolean backward )
    {
        return setAccess(setSpeed(0, speed), forward, backward);
    }

    @Override
    public double getMaxSpeed()
    {
        return speedEncoder.getMaxValue();
    }

    /**
     * @return -1 if no maxspeed found
     */
    protected double getMaxSpeed( OSMWay way )
    {
        double maxSpeed = parseSpeed(way.getTag("maxspeed"));
        double fwdSpeed = parseSpeed(way.getTag("maxspeed:forward"));
        if (fwdSpeed >= 0 && (maxSpeed < 0 || fwdSpeed < maxSpeed))
            maxSpeed = fwdSpeed;

        double backSpeed = parseSpeed(way.getTag("maxspeed:backward"));
        if (backSpeed >= 0 && (maxSpeed < 0 || backSpeed < maxSpeed))
            maxSpeed = backSpeed;

        return maxSpeed;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 61 * hash + (int) this.directionBitMask;
        hash = 61 * hash + this.toString().hashCode();
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if (obj == null)
            return false;

        // only rely on the string
        //        if (getClass() != obj.getClass())
        //            return false;
        final AbstractFlagEncoder other = (AbstractFlagEncoder) obj;
        if (this.directionBitMask != other.directionBitMask)
            return false;

        return this.toString().equals(other.toString());
    }

    /**
     * @return the speed in km/h
     */
    protected static double parseSpeed( String str )
    {
        if (Helper.isEmpty(str))
            return -1;

        try
        {
            int val;
            // see https://en.wikipedia.org/wiki/Knot_%28unit%29#Definitions
            int mpInteger = str.indexOf("mp");
            if (mpInteger > 0)
            {
                str = str.substring(0, mpInteger).trim();
                val = Integer.parseInt(str);
                return val * DistanceCalcEarth.KM_MILE;
            }

            int knotInteger = str.indexOf("knots");
            if (knotInteger > 0)
            {
                str = str.substring(0, knotInteger).trim();
                val = Integer.parseInt(str);
                return val * 1.852;
            }

            int kmInteger = str.indexOf("km");
            if (kmInteger > 0)
            {
                str = str.substring(0, kmInteger).trim();
            } else
            {
                kmInteger = str.indexOf("kph");
                if (kmInteger > 0)
                {
                    str = str.substring(0, kmInteger).trim();
                }
            }

            return Integer.parseInt(str);
        } catch (Exception ex)
        {
            return -1;
        }
    }

    /**
     * This method parses a string ala "00:00" (hours and minutes) or "0:00:00" (days, hours and
     * minutes).
     * <p/>
     * @return duration value in minutes
     */
    protected static int parseDuration( String str )
    {
        if (str == null)
            return 0;

        try
        {
            // for now ignore this special duration notation
            // because P1M != PT1M but there are wrong edits in OSM! e.g. http://www.openstreetmap.org/way/24791405
            // http://wiki.openstreetmap.org/wiki/Key:duration
            if (str.startsWith("P"))
                return 0;

            int index = str.indexOf(":");
            if (index > 0)
            {
                String hourStr = str.substring(0, index);
                String minStr = str.substring(index + 1);
                index = minStr.indexOf(":");
                int minutes = 0;
                if (index > 0)
                {
                    // string contains hours too
                    String dayStr = hourStr;
                    hourStr = minStr.substring(0, index);
                    minStr = minStr.substring(index + 1);
                    minutes = Integer.parseInt(dayStr) * 60 * 24;
                }

                minutes += Integer.parseInt(hourStr) * 60;
                minutes += Integer.parseInt(minStr);
                return minutes;
            } else
            {
                return Integer.parseInt(str);
            }
        } catch (Exception ex)
        {
            logger.warn("Cannot parse " + str + " using 0 minutes");
        }
        return 0;
    }

    /**
     * Second parsing step. Invoked after splitting the edges. Currently used to offer a hook to
     * calculate precise speed values based on elevation data stored in the specified edge.
     */
    public void applyWayTags( OSMWay way, EdgeIteratorState edge )
    {
    }

    /**
     * Special handling for ferry ways.
     */
    protected long handleFerryTags( OSMWay way, double unknownSpeed, double shortTripsSpeed, double longTripsSpeed )
    {
        // to hours
        double durationInHours = parseDuration(way.getTag("duration")) / 60d;
        if (durationInHours > 0)
            try
            {
                Number estimatedLength = way.getTag("estimated_distance", null);
                if (estimatedLength != null)
                {
                    // to km
                    double val = estimatedLength.doubleValue() / 1000;
                    // If duration AND distance is available we can calculate the speed more precisely
                    // and set both speed to the same value. Factor 1.4 slower because of waiting time!
                    shortTripsSpeed = Math.round(val / durationInHours / 1.4);
                    if (shortTripsSpeed > getMaxSpeed())
                        shortTripsSpeed = getMaxSpeed();
                    longTripsSpeed = shortTripsSpeed;
                }
            } catch (Exception ex)
            {
            }

        if (durationInHours == 0)
        {
            // unknown speed -> put penalty on ferry transport
            return setSpeed(0, unknownSpeed);
        } else if (durationInHours > 1)
        {
            // lengthy ferries should be faster than short trip ferry
            return setSpeed(0, longTripsSpeed);
        } else
        {
            return setSpeed(0, shortTripsSpeed);
        }
    }

    void setWayBitMask( int usedBits, int shift )
    {
        wayBitMask = (1L << usedBits) - 1;
        wayBitMask <<= shift;
    }

    long getWayBitMask()
    {
        return wayBitMask;
    }

    void setRelBitMask( int usedBits, int shift )
    {
        relBitMask = (1L << usedBits) - 1;
        relBitMask <<= shift;
    }

    long getRelBitMask()
    {
        return relBitMask;
    }

    void setNodeBitMask( int usedBits, int shift )
    {
        nodeBitMask = (1L << usedBits) - 1;
        nodeBitMask <<= shift;
    }

    long getNodeBitMask()
    {
        return nodeBitMask;
    }

    @Override
    public boolean isTurnRestricted( long flag )
    {
        return (flag & restrictionBit) != 0;
    }

    @Override
    public int getTurnCosts( long flag )
    {
        long result = (flag >> costShift) & costsMask;
        if (result >= Math.pow(2, maxCostsBits) || result < 0)
        {
            throw new IllegalStateException("Wrong encoding of turn costs");
        }
        return Long.valueOf(result).intValue();
    }

    @Override
    public long getTurnFlags( boolean restricted, int costs )
    {
        costs = Math.min(costs, (int) (Math.pow(2, maxCostsBits) - 1));
        long encode = costs << costShift;
        if (restricted)
        {
            encode |= restrictionBit;
        }
        return encode;
    }

    public Collection<TurnCostTableEntry> analyzeTurnRelation( OSMTurnRelation turnRelation, OSMReader osmReader )
    {
        return Collections.emptyList();
    }

    protected boolean isFerry( long internalFlags )
    {
        return (internalFlags & ferryBit) != 0;
    }

    protected boolean isAccept( long internalFlags )
    {
        return (internalFlags & acceptBit) != 0;
    }

    @Override
    public long setBool( long flags, int key, boolean value )
    {
        switch (key)
        {
            case K_FORWARD:
                return value ? flags | forwardBit : flags & ~forwardBit;
            case K_BACKWARD:
                return value ? flags | backwardBit : flags & ~backwardBit;
            case K_ROUNDABOUT:
                return value ? flags | roundaboutBit : flags & ~roundaboutBit;
            default:
                throw new IllegalArgumentException("Unknown key " + key + " for boolean value");
        }
    }

    @Override
    public boolean isBool( long flags, int key )
    {
        switch (key)
        {
            case K_FORWARD:
                return (flags & forwardBit) != 0;
            case K_BACKWARD:
                return (flags & backwardBit) != 0;
            case K_ROUNDABOUT:
                return (flags & roundaboutBit) != 0;
            default:
                throw new IllegalArgumentException("Unknown key " + key + " for boolean value");
        }
    }

    @Override
    public long setLong( long flags, int key, long value )
    {
        throw new UnsupportedOperationException("Unknown key " + key + " for long value.");
    }

    @Override
    public long getLong( long flags, int key )
    {
        throw new UnsupportedOperationException("Unknown key " + key + " for long value.");
    }

    @Override
    public long setDouble( long flags, int key, double value )
    {
        throw new UnsupportedOperationException("Unknown key " + key + " for double value.");
    }

    @Override
    public double getDouble( long flags, int key )
    {
        throw new UnsupportedOperationException("Unknown key " + key + " for double value.");
    }
}
