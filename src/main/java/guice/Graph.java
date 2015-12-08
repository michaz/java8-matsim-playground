/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Graph.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package guice;

import com.google.inject.Guice;
import com.google.inject.grapher.graphviz.GraphvizGrapher;
import com.google.inject.grapher.graphviz.GraphvizModule;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class Graph {

    public static void main(String[] args) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final PrintWriter out = new PrintWriter(baos);
        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory(args[0]);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        Injector matsimInjector = Injector.createInjector(config,
                new ControlerDefaultsModule(),
                new ControlerDefaultCoreListenersModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        bind(Scenario.class).toInstance(ScenarioUtils.createScenario(config));
                        bind(OutputDirectoryHierarchy.class).asEagerSingleton();
                        bind(IterationStopWatch.class).asEagerSingleton();
                        bind(ControlerI.class).to(Controler.class).asEagerSingleton();
                    }
                });
        matsimInjector.getInstance(ControlerI.class);
        try {

            MyGrapher renderer = new MyGrapher();
            renderer.setRankdir("LR");
            renderer.setOut(out);
            renderer.graph(matsimInjector.getInstance(com.google.inject.Injector.class));

            File file = new File(args[0]+"/guice.dot");
            PrintWriter fileOut = new PrintWriter(file);
            String s = baos.toString("UTF-8");
            s = fixGrapherBug(s);
            s = hideClassPaths(s);
            fileOut.write(s);
            fileOut.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String hideClassPaths(String s) {
        s = s.replaceAll("\\w[a-z\\d_\\.]+\\.([A-Z][A-Za-z\\d_]*)", "");
        return s;
    }

    public static String fixGrapherBug(String s) {
        s = s.replaceAll("style=invis", "style=solid");
        return s;
    }

}
