/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ObservableUtils.java
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

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.BasicEventHandler;

public class ObservableUtils {

    /**
     * Wraps an EventsManager as an Observable.
     * The resulting Observable never finishes.
     */
    public static Observable<Event> fromEventsManager(EventsManager eventsManager) {
        return Observable.create(new Observable.OnSubscribe<Event>() {

            @Override
            public void call(final Subscriber<? super Event> subscriber) {
                final BasicEventHandler handler = new BasicEventHandler() {
                    @Override
                    public void handleEvent(Event event) {
                        if (subscriber.isUnsubscribed()) {
                            eventsManager.removeHandler(this);
                            return;
                        }
                        subscriber.onNext(event);
                    }

                    @Override
                    public void reset(int iteration) {

                    }
                };
                eventsManager.addHandler(handler);
            }
        });
    }

}
