/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Main.java
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

package rx;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.vehicles.Vehicle;
import rx.observables.ConnectableObservable;
import rx.observables.GroupedObservable;
import rx.subjects.PublishSubject;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        PublishSubject<Event> events = PublishSubject.create();
        Observable<Event> eternalEvents = ObservableUtils.fromEventsManager(eventsManager);
        eternalEvents.subscribe(events);

        subscribePersonExample(events);
        subscribeLinkExample(events);

        events.take(5).count().subscribe(System.out::println);
        eternalEvents.take(5).count().subscribe(System.out::println);

        new MatsimEventsReader(eventsManager).readFile("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/regimes/uncongested3/output-berlin/ITERS/it.0/2kW.15.0.events.xml.gz");
        events.onCompleted();
    }

    private static void subscribeLinkExample(PublishSubject<Event> events) {
        Observable<Event> linkEvents = events.filter(e -> e instanceof LinkEnterEvent || e instanceof LinkLeaveEvent || e instanceof PersonArrivalEvent).doOnError(System.out::println);
        linkEvents.count().subscribe(n -> System.out.printf("Number of link events: %d\n", n));


        Observable<GroupedObservable<Id<Vehicle>, Event>> byVehicle = linkEvents.groupBy(e -> {
            if (e instanceof LinkEnterEvent) {
                return ((LinkEnterEvent) e).getVehicleId();
            } else if (e instanceof LinkLeaveEvent) {
                return ((LinkLeaveEvent) e).getVehicleId();
            } else {
                return Id.create(((PersonArrivalEvent) e).getPersonId(), Vehicle.class);
            }
        });
        byVehicle.flatMap(vehicle -> {
            Observable<Double> maybeOneTravelTime = vehicle.lift(subscriber -> new Subscriber<Event>() {
                LinkEnterEvent lastEnter = null;

                @Override
                public void onCompleted() {
                    subscriber.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onError(e);
                }

                @Override
                public void onNext(Event event) {
                    if (!subscriber.isUnsubscribed()) {
                        if (event instanceof LinkEnterEvent) {
                            lastEnter = (LinkEnterEvent) event;
                        } else if (event instanceof LinkLeaveEvent) {
                            if (lastEnter != null) {
                                LinkLeaveEvent arrival = (LinkLeaveEvent) event;
                                subscriber.onNext(arrival.getTime() - lastEnter.getTime());
                            }
                            subscriber.onCompleted();
                            unsubscribe();
                        } else {
                            subscriber.onCompleted();
                            unsubscribe();
                        }
                    }
                }
            });
            return maybeOneTravelTime;
        }).count().subscribe(n -> System.out.printf("Got %d travel time events.\n",n));


    }

    private static void subscribePersonExample(PublishSubject<Event> events) {
        Observable<Event> personEvents = events.filter(e -> e instanceof HasPersonId).doOnError(System.out::println);
        personEvents.count().subscribe(n -> System.out.printf("Number of person events: %d\n", n));
        personEvents.count().subscribe(n -> System.out.printf("Number of person events: %d\n", n));
        Observable<GroupedObservable<Id<Person>, Event>> byPerson = personEvents.groupBy(event -> ((HasPersonId) event).getPersonId());
        byPerson.count().subscribe(n -> System.out.printf("Number of persons: %d\n", n));
        byPerson.subscribe(o -> {
            ConnectableObservable<Event> p = o.publish();
            p.filter(e -> e instanceof PersonDepartureEvent).count().subscribe(n -> System.out.printf("Person %s got %d departure events.\n", o.getKey(), n));
            p.filter(e -> e instanceof PersonArrivalEvent).count().subscribe(n -> System.out.printf("Person %s got %d arrival events.\n", o.getKey(), n));
            Observable<Double> ttonePerson1 = Observable.zip(p.filter(e -> e instanceof PersonDepartureEvent), p.filter(e -> e instanceof PersonArrivalEvent), (d, a) -> a.getTime() - d.getTime());
            Observable<Double> ttonePerson2 = p.lift(subscriber -> new Subscriber<Event>() {
                PersonDepartureEvent lastDeparture = null;
                @Override
                public void onCompleted() {
                    subscriber.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onError(e);
                }

                @Override
                public void onNext(Event event) {
                    if (!subscriber.isUnsubscribed()) {
                        if (event instanceof PersonDepartureEvent) {
                            lastDeparture = (PersonDepartureEvent) event;
                        } else if (event instanceof PersonArrivalEvent) {
                            PersonArrivalEvent arrival = (PersonArrivalEvent) event;
                            subscriber.onNext(arrival.getTime() - lastDeparture.getTime());
                            lastDeparture = null;
                        }
                    }
                }
            });
            ttonePerson1.count().subscribe(n -> System.out.printf("Person %s got %d travel-time events.\n", o.getKey(), n));
            ttonePerson2.count().subscribe(n -> System.out.printf("Person %s got %d travel-time events.\n", o.getKey(), n));
            p.count().subscribe(n -> System.out.printf("Person %s got %d events.\n", o.getKey(), n));
            p.connect();
        });
    }

}
