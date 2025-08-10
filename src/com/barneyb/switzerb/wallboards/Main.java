package com.barneyb.switzerb.wallboards;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;

public class Main {

    private static final double WALL_WIDTH = 90;
    private static final double WALL_HEIGHT = 92;
    private static final double BOARD_WIDTH = 4.75;
    private static final double COURSE_COUNT = Math.floor(WALL_HEIGHT / BOARD_WIDTH);
    private static final List<Double> SPANS = List.of(13.75, 16.0, 16.0, 16.0, 16.0, 12.25);
    private static final Map<Double, Integer> AVAIL_BOARDS = Map.of(
            67.0, 6,
            50.0, 7,
            48.0, 7,
            42.0, 1,
            35.0, 13,
            32.0, 6,
            27.0, 4,
            19.0, 8,
            16.0, 2);

    static {
        double totalSpan = SPANS.stream().mapToDouble(s -> s).sum();
        if (totalSpan != WALL_WIDTH) {
            throw new RuntimeException(String.format(
                    "your spans (%f) don't match the wall (%f)",
                    totalSpan,
                    WALL_WIDTH));
        }
    }

    public static void main(String[] args) {
        new Main().solve();
    }

    private final NavigableMap<Double, Integer> pool = new TreeMap<>(AVAIL_BOARDS);
    private final List<List<Cut>> courses = new ArrayList<>();
    private final NavigableMap<Double, Integer> on_wall = new TreeMap<>();

    private void solve() {
        printInfo();

        for (int crs = 0; crs < COURSE_COUNT; crs++) {
            List<Cut> course = new ArrayList<>();
            courses.add(course);
            Iterator<Double> spans = SPANS.iterator();
            while (spans.hasNext()) {
                double longest = pool.lastKey();
                double target = 0;
                while (target < longest && spans.hasNext()) {
                    double peek = spans.next();
                    if (target + peek < longest) {
                        target += peek;
                        continue;
                    }
                    course.add(takeCut(target));
                    longest = pool.lastKey();
                    target = peek;
                }
                course.add(takeCut(target));
            }
        }
        printCourses();
        printPool("Left Over", pool);
        printPool("On Wall", on_wall);
    }

    private record Cut(double board, double len) {

        double rest() {
            return board - consumed();
        }

        double consumed() {
            return len + 0.75;
        }

    }

    private Cut takeCut(double target) {
        if (target < 5) throw new RuntimeException("target of " + target);
        try {
            Double board = pool.tailMap(target + 0.5)
                    .keySet()
                    .iterator()
                    .next();
            Cut c = new Cut(board, target);
            pool.compute(c.board(),
                         (key, count) -> count > 1 ? count - 1 : null);
            on_wall.compute(c.len(),
                            (key, count) -> count == null ? 1 : count + 1);
            if (c.rest() > 0) {
                pool.compute(c.rest(),
                             (k, v) -> v == null ? 1 : v + 1);
            }
            return c;
        } catch (NoSuchElementException e) {
            System.err.printf("No more boards for a %.2f cut?!%n", target);
            printCourses();
            printPool("Pool", pool);
            throw e;
        }
    }

    private void printInfo() {
        System.out.printf("Course Count: %.0f%n", COURSE_COUNT);
        System.out.printf("Spans: %s%n", SPANS);
        printPool("Pool", pool);
        printCourses();
    }

    private void printPool(String label, Map<Double, Integer> pool) {
        double avail = pool.entrySet()
                .stream()
                .mapToDouble(e -> e.getKey() * e.getValue())
                .sum();
        System.out.printf("%s (%.0f\"):%n", label, avail);
        pool.forEach((l, n) -> {
            System.out.printf("  %5.2f\":%3d|", l, n);
            if (l >= 1)
                System.out.print(" ".repeat(l.intValue() - 1));
            System.out.println('|');
        });
    }

    private void printCourses() {
        if (courses.isEmpty()) return;
        courses.add(SPANS.stream().map(l -> new Cut(l, l)).toList());
        System.out.println("Courses:");
        char delim = '|';
        for (int i = 0, l = courses.size(); i < l; i++) {
            if (i == l - 1) delim = '.';
            List<Cut> cs = courses.get(i);
            System.out.printf("%2d ", l - i - 1);
            for (Cut c : cs) {
                System.out.print(delim);
                System.out.print(" ".repeat((int) c.len() - 1));
            }
            if (delim == '.') {
                System.out.println(delim);
            } else {
                for (Cut c : cs) {
                    System.out.printf("%c %.2f / %.2f ", delim, c.len(), c.board());
                }
            }
            System.out.println();
        }
    }

}
