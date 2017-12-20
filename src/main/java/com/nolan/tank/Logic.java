package com.nolan.tank;

public class Logic {
    public static double and(double x, double y) {
        return x * y;
    }

    public static double or(double x, double y) {
        return x + y - x * y;
    }

    public static double not(double x) {
        return 1 - x;
    }
}
