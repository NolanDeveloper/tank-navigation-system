package com.nolan.tank;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;

import java.util.Optional;

public class Segment {
    public static class Intersection {
        public final double t;
        public final double u;
        public final Point2D point;

        public Intersection(double t, double u, Point2D point) {
            this.t = t;
            this.u = u;
            this.point = point;
        }
    }

    public final Point2D a;
    public final Point2D b;

    public Segment(Point2D a, Point2D b) {
        this.a = a;
        this.b = b;
    }

    public void draw(GraphicsContext g) {
        g.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
    }

    /* https://stackoverflow.com/a/565282/4626533
     * Collinear segments are not considered intersecting.
     */
    public Optional<Intersection> intersect(Segment w) {
        Point2D p = a;
        Point2D r = b.subtract(a);
        Point2D q = w.a;
        Point2D s = w.b.subtract(w.a);
        double d = r.crossProduct(s).getZ();
        if (0 == d) return Optional.empty();
        Point2D qMinusP = q.subtract(p);
        double t = qMinusP.crossProduct(s).getZ() / d;
        double u = qMinusP.crossProduct(r).getZ() / d;
        if (0 <= t && t <= 1 && 0 <= u && u <= 1) {
            Point2D intersectionPoint = p.add(r.multiply(t));
            return Optional.of(new Intersection(t, u, intersectionPoint));
        } else {
            return Optional.empty();
        }
    }
}
