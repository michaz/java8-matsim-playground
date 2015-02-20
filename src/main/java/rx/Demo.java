/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Demo.java
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
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import rx.observables.ConnectableObservable;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;

public class Demo {

    public static void main(String[] args) throws InterruptedException {
        ConnectableObservable<PersonDepartureEvent> events = Observable.<PersonDepartureEvent>create(subscriber -> {
            for(int i=0;i<100;i++) {
                subscriber.onNext(new PersonDepartureEvent(i, Id.createPersonId("1"), Id.createLinkId("5"), "car"));
                subscriber.onNext(new PersonDepartureEvent(i, Id.createPersonId("2"), Id.createLinkId("5"), "car"));
                subscriber.onNext(new PersonDepartureEvent(i, Id.createPersonId("3"), Id.createLinkId("5"), "car"));
                subscriber.onNext(new PersonDepartureEvent(i, Id.createPersonId("4"), Id.createLinkId("6"), "car"));
                subscriber.onNext(new PersonDepartureEvent(i, Id.createPersonId("5"), Id.createLinkId("6"), "car"));
                subscriber.onNext(new PersonDepartureEvent(i, Id.createPersonId("6"), Id.createLinkId("6"), "car"));
                subscriber.onNext(new PersonDepartureEvent(i, Id.createPersonId("7"), Id.createLinkId("6"), "car"));
            }
            subscriber.onCompleted();
        }).doOnError(Throwable::printStackTrace).publish();

//        events.observeOn(Schedulers.computation()).doOnEach(System.out::println).map(e -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e1) {
//                throw new RuntimeException(e1);
//            }
//            return 7;
//        }).subscribe(System.out::println);
//
//        events.observeOn(Schedulers.computation()).doOnEach(System.out::println).map(e -> {
//            try {
//                Thread.sleep(200);
//            } catch (InterruptedException e1) {
//                throw new RuntimeException(e1);
//            }
//            return 8;
//        }).subscribe(System.out::println);

        events.onBackpressureBuffer().observeOn(Schedulers.computation()).doOnEach(System.out::println).subscribe(System.out::println);

        Observable<GroupedObservable<Double, PersonDepartureEvent>> byLink = events.onBackpressureBuffer().observeOn(Schedulers.computation()).groupBy(e -> Math.random());
        byLink.doOnEach(System.out::println).flatMap(link -> link.first()).subscribe(System.out::println);


        Observable<GroupedObservable<Double, PersonDepartureEvent>> byLink2 = events.onBackpressureBuffer().observeOn(Schedulers.computation()).groupBy(e -> Math.random());
        Observable.merge(byLink2.doOnEach(System.out::println)).subscribe(System.out::println);
        events.connect();
        Thread.sleep(7000);
    }

}
