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
package com.graphhopper.routing;

import com.graphhopper.routing.util.TestAlgoCollector;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.PrinctonReader;
import com.graphhopper.reader.dem.SRTMProvider;
import com.graphhopper.routing.util.*;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.TestAlgoCollector.OneRun;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphBuilder;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.Helper;
import com.graphhopper.util.StopWatch;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Try algorithms, indices and graph storages with real data
 * <p/>
 * @author Peter Karich
 */
public class RoutingAlgorithmIT
{
    TestAlgoCollector testCollector;

    @Before
    public void setUp()
    {
        testCollector = new TestAlgoCollector("core integration tests");
    }

    List<OneRun> createMonacoCar()
    {
        List<OneRun> list = new ArrayList<OneRun>();
        list.add(new OneRun(43.730729, 7.42135, 43.727697, 7.419199, 2580, 110));
        list.add(new OneRun(43.727687, 7.418737, 43.74958, 7.436566, 3588, 170));
        list.add(new OneRun(43.728677, 7.41016, 43.739213, 7.4277, 2561, 133));
        list.add(new OneRun(43.733802, 7.413433, 43.739662, 7.424355, 2230, 137));
        list.add(new OneRun(43.730949, 7.412338, 43.739643, 7.424542, 2100, 116));
        list.add(new OneRun(43.727592, 7.419333, 43.727712, 7.419333, 0, 1));

        // same special cases where GPS-exact routing could have problems (same edge and neighbor edges)
        list.add(new OneRun(43.727592, 7.419333, 43.727712, 7.41934, 0, 1));
        // on the same edge and very release
        list.add(new OneRun(43.727592, 7.419333, 43.727712, 7.4193, 3, 2));
        // one way stuff
        list.add(new OneRun(43.729445, 7.415063, 43.728856, 7.41472, 103, 4));
        list.add(new OneRun(43.728856, 7.41472, 43.729445, 7.415063, 320, 11));
        return list;
    }

    @Test
    public void testMonaco()
    {
        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                createMonacoCar(), "CAR", true, "CAR", "shortest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testOneWayCircleBug()
    {
        // export from http://www.openstreetmap.org/export#map=19/51.37605/-0.53155
        List<OneRun> list = new ArrayList<OneRun>();
        // going the bit longer way out of the circle
        list.add(new OneRun(51.376197, -0.531576, 51.376509, -0.530863, 153, 18));
        // now exacle the opposite direction: going into the circle (shorter)
        list.add(new OneRun(51.376509, -0.530863, 51.376197, -0.531576, 75, 15));

        runAlgo(testCollector, "files/circle-bug.osm.gz", "target/circle-bug-gh",
                list, "CAR", true, "CAR", "shortest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testMoscow()
    {
        // extracted via ./graphhopper.sh extract "37.582641,55.805261,37.626929,55.824455"
        List<OneRun> list = new ArrayList<OneRun>();
        // choose perpendicular
        // http://localhost:8989/?point=55.818994%2C37.595354&point=55.819175%2C37.596931
        list.add(new OneRun(55.818994, 37.595354, 55.819175, 37.596931, 1052, 14));
        // should choose the closest road not the other one (opposite direction)
        // http://localhost:8989/?point=55.818898%2C37.59661&point=55.819066%2C37.596374
        list.add(new OneRun(55.818898, 37.59661, 55.819066, 37.596374, 24, 2));
        // respect one way!
        // http://localhost:8989/?point=55.819066%2C37.596374&point=55.818898%2C37.59661
        list.add(new OneRun(55.819066, 37.596374, 55.818898, 37.59661, 1114, 23));
        runAlgo(testCollector, "files/moscow.osm.gz", "target/moscow-gh",
                list, "CAR", true, "CAR", "fastest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testMonacoFastest()
    {
        List<OneRun> list = createMonacoCar();
        list.get(0).setLocs(1, 117);
        list.get(0).setDistance(1, 2584);
        list.get(3).setDistance(1, 2279);
        list.get(3).setLocs(1, 141);
        list.get(4).setDistance(1, 2149);
        list.get(4).setLocs(1, 120);
        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                list, "CAR", true, "CAR", "fastest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testMonacoMixed()
    {
        // Additional locations are inserted because of new crossings from foot to highway paths!
        // Distance is the same.
        List<OneRun> list = createMonacoCar();
        list.get(0).setLocs(1, 110);
        list.get(1).setLocs(1, 170);
        list.get(2).setLocs(1, 132);
        list.get(3).setLocs(1, 137);
        list.get(4).setLocs(1, 116);

        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                list, "CAR,FOOT", false, "CAR", "shortest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    List<OneRun> createMonacoFoot()
    {
        List<OneRun> list = new ArrayList<OneRun>();
        list.add(new OneRun(43.730729, 7.421288, 43.727697, 7.419199, 1566, 92));
        list.add(new OneRun(43.727687, 7.418737, 43.74958, 7.436566, 3438, 136));
        list.add(new OneRun(43.728677, 7.41016, 43.739213, 7.427806, 2085, 112));
        list.add(new OneRun(43.733802, 7.413433, 43.739662, 7.424355, 1425, 89));
        return list;
    }

    @Test
    public void testMonacoFoot()
    {
        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                createMonacoFoot(), "FOOT", true, "FOOT", "shortest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testMonacoFoot3D()
    {
        // most routes have same number of points as testMonaceFoot results but longer distance due to elevation difference
        List<OneRun> list = createMonacoFoot();
        list.get(0).setDistance(1, 1627);
        list.get(2).setDistance(1, 2258);
        list.get(3).setDistance(1, 1482);
        
        // or slightly longer tour with less nodes: list.get(1).setDistance(1, 3610);
        list.get(1).setDistance(1, 3595);
        list.get(1).setLocs(1, 149);               

        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                list, "FOOT", true, "FOOT", "shortest", true);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testMonacoBike3D_twoSpeedsPerEdge()
    {
        List<OneRun> list = new ArrayList<OneRun>();
        // 1. alternative: go over steps 'Rampe Major' => 1.7km vs. around 2.7km
        list.add(new OneRun(43.730864, 7.420771, 43.727687, 7.418737, 2710, 118));
        // 2.
        list.add(new OneRun(43.728499, 7.417907, 43.74958, 7.436566, 3777, 194));
        // 3.
        list.add(new OneRun(43.728677, 7.41016, 43.739213, 7.427806, 2776, 167));
        // 4.
        list.add(new OneRun(43.733802, 7.413433, 43.739662, 7.424355, 1544, 84));

        // try reverse direction
        // 1.
        list.add(new OneRun(43.727687, 7.418737, 43.730864, 7.420771, 2599, 115));
        list.add(new OneRun(43.74958, 7.436566, 43.728499, 7.417907, 4199, 165));
        list.add(new OneRun(43.739213, 7.427806, 43.728677, 7.41016, 3261, 177));
        // 4. avoid tunnel(s)!
        list.add(new OneRun(43.739662, 7.424355, 43.733802, 7.413433, 2452, 112));
        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                list, "BIKE2", true, "BIKE2", "fastest", true);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testMonacoBike()
    {
        List<OneRun> list = new ArrayList<OneRun>();
        list.add(new OneRun(43.730864, 7.420771, 43.727687, 7.418737, 1642, 87));
        list.add(new OneRun(43.727687, 7.418737, 43.74958, 7.436566, 3580, 168));
        list.add(new OneRun(43.728677, 7.41016, 43.739213, 7.427806, 2323, 121));
        list.add(new OneRun(43.733802, 7.413433, 43.739662, 7.424355, 1434, 89));
        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                list, "BIKE", true, "BIKE", "shortest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testMonacoMountainBike()
    {
        List<OneRun> list = new ArrayList<OneRun>();
        list.add(new OneRun(43.730864, 7.420771, 43.727687, 7.418737, 2322, 110));
        list.add(new OneRun(43.727687, 7.418737, 43.74958, 7.436566, 3613, 178));
        list.add(new OneRun(43.728677, 7.41016, 43.739213, 7.427806, 2331, 121));
        // hard to select between secondard and primary (both are AVOID for mtb)
        list.add(new OneRun(43.733802, 7.413433, 43.739662, 7.424355, 1459, 88));
        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                list, "MTB", true, "MTB", "fastest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());

        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                list, "MTB,RACINGBIKE", false, "MTB", "fastest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testMonacoRacingBike()
    {
        List<OneRun> list = new ArrayList<OneRun>();
        list.add(new OneRun(43.730864, 7.420771, 43.727687, 7.418737, 2594, 111));
        list.add(new OneRun(43.727687, 7.418737, 43.74958, 7.436566, 3588, 170));
        list.add(new OneRun(43.728677, 7.41016, 43.739213, 7.427806, 2572, 135));
        list.add(new OneRun(43.733802, 7.413433, 43.739662, 7.424355, 1490, 84));
        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                list, "RACINGBIKE", true, "RACINGBIKE", "fastest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());

        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                list, "BIKE,RACINGBIKE", false, "RACINGBIKE", "fastest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testKremsBikeRelation()
    {
        List<OneRun> list = new ArrayList<OneRun>();
        list.add(new OneRun(48.409523, 15.602394, 48.375466, 15.72916, 12491, 159));
        list.add(new OneRun(48.410061, 15.63951, 48.411386, 15.604899, 3113, 87));
        list.add(new OneRun(48.412294, 15.62007, 48.398306, 15.609667, 3965, 94));

        runAlgo(testCollector, "files/krems.osm.gz", "target/krems-gh",
                list, "BIKE", true, "BIKE", "fastest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());

        runAlgo(testCollector, "files/krems.osm.gz", "target/krems-gh",
                list, "CAR,BIKE", false, "BIKE", "fastest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testKremsMountainBikeRelation()
    {
        List<OneRun> list = new ArrayList<OneRun>();
        list.add(new OneRun(48.409523, 15.602394, 48.375466, 15.72916, 12574, 169));
        list.add(new OneRun(48.410061, 15.63951, 48.411386, 15.604899, 3101, 94));
        list.add(new OneRun(48.412294, 15.62007, 48.398306, 15.609667, 3965, 95));

        runAlgo(testCollector, "files/krems.osm.gz", "target/krems-gh",
                list, "MTB", true, "MTB", "fastest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());

        runAlgo(testCollector, "files/krems.osm.gz", "target/krems-gh",
                list, "BIKE,MTB", false, "MTB", "fastest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    List<OneRun> createAndorra()
    {
        List<OneRun> list = new ArrayList<OneRun>();
        list.add(new OneRun(42.56819, 1.603231, 42.571034, 1.520662, 17708, 524));
        list.add(new OneRun(42.529176, 1.571302, 42.571034, 1.520662, 11408, 305));
        return list;
    }

    @Test
    public void testAndorra()
    {
        runAlgo(testCollector, "files/andorra.osm.gz", "target/andorra-gh",
                createAndorra(), "CAR", true, "CAR", "shortest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testAndorraPbf()
    {
        runAlgo(testCollector, "files/andorra.osm.pbf", "target/andorra-gh",
                createAndorra(), "CAR", true, "CAR", "shortest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testAndorraFoot()
    {
        List<OneRun> list = createAndorra();
        list.get(0).setDistance(1, 16354);
        list.get(0).setLocs(1, 648);
        list.get(1).setDistance(1, 12701);
        list.get(1).setLocs(1, 431);

        runAlgo(testCollector, "files/andorra.osm.gz", "target/andorra-gh",
                list, "FOOT", true, "FOOT", "shortest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testCampoGrande()
    {
        // test not only NE quadrant of earth!

        // bzcat campo-grande.osm.bz2 
        //   | ./bin/osmosis --read-xml enableDateParsing=no file=- --bounding-box top=-20.4 left=-54.6 bottom=-20.6 right=-54.5 --write-xml file=- 
        //   | bzip2 > campo-grande.extracted.osm.bz2
        List<OneRun> list = new ArrayList<OneRun>();
        list.add(new OneRun(-20.4, -54.6, -20.6, -54.54, 25516, 271));
        list.add(new OneRun(-20.43, -54.54, -20.537, -54.674, 18009, 237));
        runAlgo(testCollector, "files/campo-grande.osm.gz", "target/campo-grande-gh", list,
                "CAR", false, "CAR", "shortest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testMonacoVia()
    {
        OneRun oneRun = new OneRun();
        oneRun.add(43.730729, 7.42135, 0, 0);
        oneRun.add(43.727697, 7.419199, 2581, 110);
        oneRun.add(43.726387, 7.4, 3001, 90);

        List<OneRun> list = new ArrayList<OneRun>();
        list.add(oneRun);

        runAlgo(testCollector, "files/monaco.osm.gz", "target/monaco-gh",
                list, "CAR", true, "CAR", "shortest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testHarsdorf()
    {
        List<OneRun> list = new ArrayList<OneRun>();
        // choose Unterloher Weg and the following residential + cycleway
        list.add(new OneRun(50.004333, 11.600254, 50.044449, 11.543434, 6931, 184));
        runAlgo(testCollector, "files/harsdorf.osm.pbf", "target/harsdorf-gh",
                list, "bike", true, "bike", "fastest", false);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    @Test
    public void testNeudrossenfeld()
    {
        List<OneRun> list = new ArrayList<OneRun>();
        // choose cycleway (Dreschenauer Stra??e)
        list.add(new OneRun(49.987132, 11.510496, 50.018839, 11.505024, 3985, 106));

        runAlgo(testCollector, "files/neudrossenfeld.osm.pbf", "target/neudrossenfeld-gh",
                list, "bike", true, "bike", "fastest", true);

        runAlgo(testCollector, "files/neudrossenfeld.osm.pbf", "target/neudrossenfeld2-gh",
                list, "bike2", true, "bike2", "fastest", true);
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
    }

    void runAlgo( TestAlgoCollector testCollector, String osmFile,
            String graphFile, List<OneRun> forEveryAlgo, String importVehicles,
            boolean ch, String vehicle, String weightCalcStr, boolean is3D )
    {
        AlgorithmPreparation tmpPrepare = null;
        OneRun tmpOneRun = null;
        try
        {
            Helper.removeDir(new File(graphFile));
            GraphHopper hopper = new GraphHopper().
                    setInMemory(true).
                    // avoid that path.getDistance is too different to path.getPoint.calcDistance
                    setWayPointMaxDistance(0).
                    setOSMFile(osmFile).
                    disableCHShortcuts().
                    setGraphHopperLocation(graphFile).
                    setEncodingManager(new EncodingManager(importVehicles));
            if (is3D)
                hopper.setElevationProvider(new SRTMProvider().setCacheDir(new File("./files")));

            hopper.importOrLoad();

            FlagEncoder encoder = hopper.getEncodingManager().getEncoder(vehicle);            
            Weighting weighting = hopper.createWeighting(weightCalcStr, encoder);
            
            Collection<Entry<AlgorithmPreparation, LocationIndex>> prepares = RoutingAlgorithmSpecialAreaTests.
                    createAlgos(hopper.getGraph(), hopper.getLocationIndex(), encoder, ch, weighting, hopper.getEncodingManager());
            EdgeFilter edgeFilter = new DefaultEdgeFilter(encoder);
            for (Entry<AlgorithmPreparation, LocationIndex> entry : prepares)
            {
                tmpPrepare = entry.getKey();
                LocationIndex idx = entry.getValue();
                for (OneRun oneRun : forEveryAlgo)
                {
                    tmpOneRun = oneRun;
                    List<QueryResult> list = oneRun.getList(idx, edgeFilter);
                    testCollector.assertDistance(tmpPrepare, list, oneRun);
                }
            }
        } catch (Exception ex)
        {
            if (tmpPrepare == null)
                throw new RuntimeException("cannot handle file " + osmFile, ex);

            throw new RuntimeException("cannot handle " + tmpPrepare.toString() + ", for " + tmpOneRun
                    + ", file " + osmFile, ex);
        } finally
        {
            // Helper.removeDir(new File(graphFile));
        }
    }

    @Test
    public void testPerformance() throws IOException
    {
        int N = 10;
        int noJvmWarming = N / 4;

        Random rand = new Random(0);
        EncodingManager eManager = new EncodingManager("CAR");
        FlagEncoder encoder = eManager.getEncoder("CAR");
        Graph graph = new GraphBuilder(eManager).create();

        String bigFile = "10000EWD.txt.gz";
        new PrinctonReader(graph).setStream(new GZIPInputStream(PrinctonReader.class.getResourceAsStream(bigFile), 8 * (1 << 10))).read();
        Collection<Entry<AlgorithmPreparation, LocationIndex>> prepares = RoutingAlgorithmSpecialAreaTests.
                createAlgos(graph, null, encoder, false, new ShortestWeighting(), eManager);
        for (Entry<AlgorithmPreparation, LocationIndex> entry : prepares)
        {
            AlgorithmPreparation prepare = entry.getKey();
            StopWatch sw = new StopWatch();
            for (int i = 0; i < N; i++)
            {
                int node1 = Math.abs(rand.nextInt(graph.getNodes()));
                int node2 = Math.abs(rand.nextInt(graph.getNodes()));
                RoutingAlgorithm d = prepare.createAlgo();
                if (i >= noJvmWarming)
                    sw.start();

                Path p = d.calcPath(node1, node2);
                // avoid jvm optimization => call p.distance
                if (i >= noJvmWarming && p.getDistance() > -1)
                    sw.stop();

                // System.out.println("#" + i + " " + name + ":" + sw.getSeconds() + " " + p.nodes());
            }

            float perRun = sw.stop().getSeconds() / ((float) (N - noJvmWarming));
            System.out.println("# " + getClass().getSimpleName() + " " + prepare.createAlgo().getName()
                    + ":" + sw.stop().getSeconds() + ", per run:" + perRun);
            assertTrue("speed to low!? " + perRun + " per run", perRun < 0.08);
        }
    }

    @Test
    public void testMonacoParallel() throws IOException
    {
        System.out.println("testMonacoParallel takes a bit time...");
        String graphFile = "target/monaco-gh";
        Helper.removeDir(new File(graphFile));
        final EncodingManager encodingManager = new EncodingManager("CAR");
        GraphHopper hopper = new GraphHopper().
                setInMemory(true).
                setEncodingManager(encodingManager).
                disableCHShortcuts().
                setWayPointMaxDistance(0).
                setOSMFile("files/monaco.osm.gz").setGraphHopperLocation(graphFile).
                importOrLoad();
        final Graph g = hopper.getGraph();
        final LocationIndex idx = hopper.getLocationIndex();
        final List<OneRun> instances = createMonacoCar();
        List<Thread> threads = new ArrayList<Thread>();
        final AtomicInteger integ = new AtomicInteger(0);
        int MAX = 100;
        FlagEncoder carEncoder = encodingManager.getEncoder("CAR");

        // testing if algorithms are independent. should be. so test only two algorithms. 
        // also the preparing is too costly to be called for every thread
        int algosLength = 2;
        Weighting weighting = new ShortestWeighting();
        final EdgeFilter filter = new DefaultEdgeFilter(carEncoder);
        for (int no = 0; no < MAX; no++)
        {
            for (int instanceNo = 0; instanceNo < instances.size(); instanceNo++)
            {
                RoutingAlgorithm[] algos = new RoutingAlgorithm[]
                {
                    new AStar(g, carEncoder, weighting),
                    new DijkstraBidirectionRef(g, carEncoder, weighting)
                };
                for (final RoutingAlgorithm algo : algos)
                {
                    // an algorithm is not thread safe! reuse via clear() is ONLY appropriated if used from same thread!
                    final int instanceIndex = instanceNo;
                    Thread t = new Thread()
                    {
                        @Override
                        public void run()
                        {
                            OneRun oneRun = instances.get(instanceIndex);
                            testCollector.assertDistance(new NoOpAlgorithmPreparation()
                            {
                                @Override
                                public RoutingAlgorithm createAlgo()
                                {
                                    return algo;
                                }

                                @Override
                                public String toString()
                                {
                                    return algo.toString();
                                }

                            }, oneRun.getList(idx, filter), oneRun);
                            integ.addAndGet(1);
                        }
                    };
                    t.start();
                    threads.add(t);
                }
            }
        }

        for (Thread t : threads)
        {
            try
            {
                t.join();
            } catch (InterruptedException ex)
            {
                throw new RuntimeException(ex);
            }
        }

        assertEquals(MAX * algosLength * instances.size(), integ.get());
        assertEquals(testCollector.toString(), 0, testCollector.errors.size());
        hopper.close();
    }
}
