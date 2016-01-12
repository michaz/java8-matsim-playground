package fx;/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LineChartSample.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.matsim.analysis.LegHistogram;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LegHistogramApp extends Application {

    @Override
    public void start(Stage stage) {
        List<LegHistogram> legHistograms = new ArrayList<>();
        for (int iter : Arrays.asList(0 /*, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100*/)) {
            LegHistogram legHistogram = readLegHistogram(iter);
            legHistograms.add(legHistogram);

        }


        stage.setTitle("Leg Histogram");


        Scene scene = new Scene(LegHistogramPane.createAnimatedChart(legHistograms), 800, 600);
        scene.getStylesheets().add("fx/leghistogram.css");

        stage.setScene(scene);
        stage.show();
    }


    private LegHistogram readLegHistogram(int iter) {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        LegHistogram legHistogram = new LegHistogram(300);
        eventsManager.addHandler(legHistogram);
        new MatsimEventsReader(eventsManager).readFile(getParameters().getNamed().get("runDir")+"/ITERS/it." + iter + "/" + iter + ".events.xml.gz");
        return legHistogram;
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
