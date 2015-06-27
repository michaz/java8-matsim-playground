/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CreateODDemand.java
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

package bluetoothpaper;

import cadyts.CadytsModule;
import cadyts.MeasurementLoader;
import cadyts.interfaces.defaults.BasicMeasurementLoader;
import cdr.TrajectoryReRealizerModule;
import clones.ClonesConfigGroup;
import clones.ClonesModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import matrices.MatricesModule;
import matrices.TimedMatrices;
import matrices.TimedMatrix;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class CreateODDemand extends Application {

    static class MatrixGUI {
        TableView<Map.Entry<String, ArrayList<Entry>>> table = new TableView<>();
    }

    static class MatrixGUIUpdater implements StartupListener, IterationEndsListener {

        @Inject
        TimedMatrices matrices;

        @Inject
        MatrixGUI gui;

        @Override
        public void notifyStartup(StartupEvent event) {
            FXUtils.runAndWait(() -> {
                for (TimedMatrix matrix : matrices.getMatrices()) {
                    TableColumn<Map.Entry<String, ArrayList<Entry>>, String> origin = new TableColumn<>("Origin");
                    origin.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getKey()));
                    gui.table.getColumns().add(origin);
                    for (Map.Entry<String, ArrayList<Entry>> column : matrix.getMatrix().getToLocations().entrySet()) {
                        TableColumn<Map.Entry<String, ArrayList<Entry>>, Double> tableColumn = new TableColumn<>(column.getKey());
                        tableColumn.setCellValueFactory(param -> {
                            Entry entry = matrix.getMatrix().getEntry(param.getValue().getKey(), column.getKey());
                            if (entry != null) {
                                return new ReadOnlyObjectWrapper<>(entry.getValue());
                            } else {
                                return new ReadOnlyObjectWrapper<>(0.0);
                            }
                        });
                        gui.table.getColumns().add(tableColumn);
                    }
                }
            });
        }

        @Override
        public void notifyIterationEnds(IterationEndsEvent event) {
            FXUtils.runAndWait(() -> {
                gui.table.getItems().clear();
                for (TimedMatrix matrix : matrices.getMatrices()) {
                    for (Map.Entry<String, ArrayList<Entry>> row : matrix.getMatrix().getFromLocations().entrySet()) {
                        gui.table.getItems().add(row);
                    }
                }
            });
        }

    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("O/D Matrix");
        stage.setWidth(300);
        stage.setHeight(500);
        final VBox vbox = new VBox();
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(10, 0, 0, 10));
        final Label label = new Label("O/D Matrix");
        label.setFont(new Font("Arial", 20));
        MatrixGUI matrixGUI = new MatrixGUI();
        vbox.getChildren().addAll(label, matrixGUI.table);
        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();
        final Config config = createConfig();
        final Scenario scenario = createScenario(config);
        final TimedMatrix matrix = createMatrix();
        Controler controler = createControler(scenario, matrix, matrixGUI);
        new Thread(controler::run).start();
    }

    private Controler createControler(final Scenario scenario, final TimedMatrix matrix, final MatrixGUI matrixGUI) {
        final Controler controler = new Controler(scenario);
        CharyparNagelCadytsScoringFunctionFactory scoringFunctionFactory = new CharyparNagelCadytsScoringFunctionFactory();
        controler.setScoringFunctionFactory(scoringFunctionFactory);
        controler.setModules(
                new ControlerDefaultsModule(),
                new CadytsModule(),
                new ClonesModule(),
                new MatricesModule(),
                new TrajectoryReRealizerModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        bind(TimedMatrices.class).toInstance(() -> Collections.singleton(matrix));
                        bind(MatrixGUI.class).toInstance(matrixGUI);
                        Multibinder<MeasurementLoader<Link>> measurementLoaderBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<MeasurementLoader<Link>>() {
                        });
                        measurementLoaderBinder.addBinding().toInstance(calibrator -> {
                            String measurementFileName = "/Users/michaelzilske/IDEAcheckout/linked-against-maven-matsim/java8-matsim-playground/src/main/resources/bluetoothpaper/multilink-measurements.xml";
                            new BasicMeasurementLoader<Link>(calibrator) {
                                @Override
                                protected Link label2link(String label) {
                                    return scenario.getNetwork().getLinks().get(Id.createLinkId(label));
                                }
                            }.load(measurementFileName);
                        });
                        addControlerListenerBinding().to(MatrixGUIUpdater.class);
                    }
                });
        return controler;
    }

    private Scenario createScenario(Config config) {
        final Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario).parse(CreateODDemand.class.getResourceAsStream("network.xml"));

        final Counts counts = new Counts();
        scenario.addScenarioElement(Counts.ELEMENT_NAME, counts);
        scenario.addScenarioElement("calibrationCounts", counts);
        return scenario;
    }

    private TimedMatrix createMatrix() {
        final TimedMatrix matrix = new TimedMatrix();
        matrix.setStartTime(9 * 60 * 60);
        matrix.setEndTime(10 * 60 * 60);

        // Antoniou et al, 2000. Switched 8 to 2, I think it is a typo in the paper.
        load(matrix.getMatrix(), 2, 7, 250);
        load(matrix.getMatrix(), 1, 7, 250);
        load(matrix.getMatrix(), 1, 10, 250);
        load(matrix.getMatrix(), 2, 4, 250);
        load(matrix.getMatrix(), 1, 4, 250);
        load(matrix.getMatrix(), 9, 7, 250);
        return matrix;
    }

    private Config createConfig() {
        Config config = ConfigUtils.createConfig();
        PlanCalcScoreConfigGroup.ActivityParams sightingParam = new PlanCalcScoreConfigGroup.ActivityParams("sighting");
        sightingParam.setTypicalDuration(30.0 * 60);
        config.planCalcScore().addActivityParams(sightingParam);
        config.planCalcScore().setTraveling_utils_hr(0);
        config.planCalcScore().setPerforming_utils_hr(0);
        config.planCalcScore().setTravelingOther_utils_hr(0);
        config.planCalcScore().setConstantCar(0);
        config.planCalcScore().setMonetaryDistanceCostRateCar(0);

        final int LAST_ITERATION = 100;
        config.controler().setLastIteration(LAST_ITERATION);
        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(Id.create(1, StrategyConfigGroup.StrategySettings.class));
            stratSets.setStrategyName("SelectExpBeta");
            stratSets.setWeight(0.7);
            config.strategy().addStrategySettings(stratSets);
        }
        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(Id.create(2, StrategyConfigGroup.StrategySettings.class));
            stratSets.setStrategyName("ReRealize");
            stratSets.setWeight(0.3);
            stratSets.setDisableAfter((int) (LAST_ITERATION * 0.8));
            config.strategy().addStrategySettings(stratSets);
        }
        ClonesConfigGroup clonesConfig = ConfigUtils.addOrGetModule(config, ClonesConfigGroup.NAME, ClonesConfigGroup.class);
        clonesConfig.setCloneFactor(2.0);
        return config;
    }

    private static void load(Matrix matrix, int origin, int destination, int count) {
        matrix.createEntry(Integer.toString(origin), Integer.toString(destination), count);
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
