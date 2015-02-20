/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PairsOfThingsWithStreams.java
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

package java8;

import java.util.stream.Stream;

public class PairsOfThingsWithStreams {

    static class Pair<T> {
        T first;
        T second;
        private Pair(T first, T second) {
            this.first = first;
            this.second = second;
        }
    }

    static class PairsWithBorders<T> {
        T left;
        Stream<Pair<T>> pairs;
        T right;
        private PairsWithBorders(T left, Stream<Pair<T>> pairs, T right) {
            this.left = left;
            this.pairs = pairs;
            this.right = right;
        }
    }

    private static <T> Stream<Pair<T>> pairs(Stream<T> things) {
        return things
                .map(thing -> new PairsWithBorders<T>(thing, Stream.empty(), thing))
                // Doesn't work because Stream.concat doesn't work into arbitrary depths.
                // According to StackOverflow, making pairs is not efficiently supported with streams at all.
                // (But I can accumulate into a List, of course.)
                .reduce((a, b) -> new PairsWithBorders<T>(a.left, Stream.concat(
                        a.pairs,
                        Stream.concat(Stream.of(new Pair<T>(a.right, b.left)), b.pairs)), b.right))
                .get()
                .pairs;
    }

    public static void main(String[] args) {
        pairs(Stream.iterate(0,n->n+1)).forEach(p -> System.out.printf("(%d,%d)",p.first, p.second));
    }

}
