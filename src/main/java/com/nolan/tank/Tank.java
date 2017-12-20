package com.nolan.tank;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;

import java.util.Optional;

public class Tank {
    private static final double LENGTH = 20;
    private static final double WIDTH = 10;

    public Point2D position = new Point2D(50, 50);
    public double angle = 0;
    public double speed = 0.05;
    public double sensorLength = 50;

    public Tank(Point2D position, double angle, double speed, double sensorLength) {
        this.position = position;
        this.angle = angle;
        this.speed = speed;
        this.sensorLength = sensorLength;
    }

    public Tank(Point2D position) {
        this.position = position;
    }

    public Tank() {
    }

    public Point2D getForward() {
        return new Point2D(Math.cos(angle), Math.sin(angle));
    }

    public Segment[] getSensors() {
        Point2D a = new Point2D(-LENGTH / 2, -WIDTH / 2);
        Point2D b = new Point2D(LENGTH / 2, -WIDTH / 2);
        Point2D c = new Point2D(LENGTH / 2, WIDTH / 2);
        Point2D d = new Point2D(-LENGTH / 2, WIDTH / 2);
        Segment[] tankBorders = new Segment[] {
                new Segment(a, b),
                new Segment(b, c),
                new Segment(c, d),
                new Segment(d, a),
        };
        Segment[] sensors = new Segment[8];
        for (int i = 0; i < 8; ++i) {
            double sensorAngle = i * Math.PI / 4;
            Point2D direction = new Point2D(Math.cos(sensorAngle), Math.sin(sensorAngle));
            Segment ray = new Segment(Point2D.ZERO, direction.multiply(Math.max(LENGTH, WIDTH)));
            Point2D begin = null;
            for (Segment border : tankBorders) {
                Optional<Segment.Intersection> intersection = border.intersect(ray);
                if (!intersection.isPresent()) continue;
                begin = intersection.get().point;
            }
            assert begin != null;
            Point2D end = begin.add(direction.multiply(sensorLength));
            sensors[i] = new Segment(begin, end);
        }
        Affine transformation = new Affine(Transform.rotate(angle * 180 / Math.PI, 0, 0));
        transformation.prepend(Transform.translate(position.getX(), position.getY()));
        for (int i = 0; i < sensors.length; ++i) {
            Point2D newA = transformation.transform(sensors[i].a);
            Point2D newB = transformation.transform(sensors[i].b);
            sensors[i] = new Segment(newA, newB);
        }
        return sensors;
    }

    public void rotate(double delta) {
        angle += delta;
    }

    public void move(double time) {
        double distance = time * speed;
        Point2D forward = new Point2D(Math.cos(angle), Math.sin(angle));
        position = position.add(forward.multiply(distance));
    }

    public void draw(GraphicsContext graphics) {
        Affine savedState = graphics.getTransform();
        graphics.translate(position.getX(), position.getY());
        graphics.rotate(angle * 180 / Math.PI);
        graphics.strokeRect(-LENGTH / 2, -WIDTH / 2, LENGTH, WIDTH);
        graphics.setTransform(savedState);
    }
}
