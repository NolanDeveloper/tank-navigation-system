package com.nolan.tank;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.nolan.tank.Logic.*;

public class MainWindow extends Application {

    private static final double POINT_SIZE = 4;

    private final GridPane grid = new GridPane();
    private final GridPane leftPane = new GridPane();
    private final VBox rightPane = new VBox();
    private final ToggleGroup activeObjectToggleGroup = new ToggleGroup();
    private final RadioButton tankRadioButton = new RadioButton("Танк");
    private final RadioButton obstaclesRadioButton = new RadioButton("Препятствия");
    private final RadioButton targetRadioButton = new RadioButton("Цель");
    private final Button startButton = new Button("Пуск");
    private final Canvas canvas = new Canvas(640, 480);
    private final Scene scene = new Scene(grid);

    private final AnimationTimer redrawTimer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            GraphicsContext graphics = canvas.getGraphicsContext2D();
            graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            tank.draw(graphics);
            for (Segment sensor : tank.getSensors()) {
                sensor.draw(graphics);
                Point2D nearest = obstacles.stream()
                        .map(obstacle -> obstacle.intersect(sensor).map(x -> x.point))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .min((left, right) -> {
                            int a = (int)left.subtract(tank.position).magnitude();
                            int b = (int)right.subtract(tank.position).magnitude();
                            return a - b;
                        }).orElse(sensor.b);
                drawPoint(graphics, nearest);
            }
            for (Segment obstacle : obstacles) {
                obstacle.draw(graphics);
            }
            drawPoint(graphics, target);
        }
    };

    private final AnimationTimer moveTimer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double distanceToTarget = target.subtract(tank.position).magnitude();
            if (distanceToTarget < 30) moveTimer.stop();

            Point2D toTarget = target.subtract(tank.position).normalize();
            Point2D toForward = tank.getForward();
            double angle = Math.atan2(toTarget.crossProduct(toForward).getZ(), toTarget.dotProduct(toForward));

            // Логические переменные. Значения от 0 до 1.
            double targetIsBehind = Math.abs(angle) / Math.PI;
            double targetIsFront = not(targetIsBehind);
            targetIsFront *= targetIsFront;
            double targetIsRight = Math.abs((angle + Math.PI + 3 * Math.PI / 2) % (2 * Math.PI) - Math.PI) / Math.PI;
            double targetIsLeft = not(targetIsRight);
            List<Double> sensorData = Stream.of(tank.getSensors())
                    .map(sensor ->
                            1 - obstacles.stream().flatMap(obstacle ->
                                    sensor.intersect(obstacle)
                                            .map(x -> Stream.of(x.t))
                                            .orElse(Stream.empty()))
                                    .mapToDouble(x -> x).min().orElse(1))
                    .collect(Collectors.toList());
            double leftObstacle = or(or(sensorData.get(5), sensorData.get(6)), sensorData.get(7));
            double rightObstacle = or(or(sensorData.get(1), sensorData.get(2)), sensorData.get(3));
            double frontObstacle = or(or(sensorData.get(7), sensorData.get(0)), sensorData.get(1));
            double needToSpeedUp = and(not(targetIsBehind), not(frontObstacle));
            double needToTurnLeft = and(targetIsLeft, not(leftObstacle));
            double needToTurnRight = and(targetIsRight, not(rightObstacle));
            double needToMoveForward = and(targetIsFront, not(frontObstacle));
            if (needToTurnLeft < needToMoveForward && needToTurnRight < needToMoveForward)
                tank.rotate(0);
            else if (needToTurnLeft < needToTurnRight && needToMoveForward < needToTurnRight)
                tank.rotate(0.01);
            else
                tank.rotate(-0.01);
            tank.speed = needToSpeedUp * 0.05;
            tank.move(20);
        }
    };

    private enum ActiveObject {
        TANK, OBSTACLES, TARGET
    }

    private ActiveObject activeObject = ActiveObject.TANK;

    private Tank tank = new Tank();
    private List<Segment> obstacles = new ArrayList<>();
    private Point2D target = new Point2D(100, 100);

    private static void drawPoint(GraphicsContext graphics, Point2D point) {
        graphics.fillRect(point.getX() - POINT_SIZE / 2, point.getY() - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
    }

    public void addObstacle(Point2D center) {
        Random r = new Random();
        int n = 3 + r.nextInt(10);
        Point2D[] points = new Point2D[n];
        for (int i = 0; i < n; ++i)
        {
            double d = r.nextDouble() * 20 + 10;
            double angle = i * 2 * Math.PI / n;
            points[i] = center.add(new Point2D(Math.cos(angle), Math.sin(angle)).multiply(d));
        }
        for (int i = 0; i < n - 1; ++i)
            obstacles.add(new Segment(points[i], points[i + 1]));
        obstacles.add(new Segment(points[0], points[n - 1]));
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        leftPane.add(canvas, 0, 0);
        tankRadioButton.setToggleGroup(activeObjectToggleGroup);
        obstaclesRadioButton.setToggleGroup(activeObjectToggleGroup);
        targetRadioButton.setToggleGroup(activeObjectToggleGroup);
        rightPane.getChildren().addAll(tankRadioButton, obstaclesRadioButton, targetRadioButton, startButton);
        tankRadioButton.setSelected(true);
        activeObjectToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            RadioButton activeRadioButton = (RadioButton)newValue;
            if (activeRadioButton == tankRadioButton) {
                activeObject = ActiveObject.TANK;
            } else if (activeRadioButton == obstaclesRadioButton) {
                activeObject = ActiveObject.OBSTACLES;
            } else if (activeRadioButton == targetRadioButton) {
                activeObject = ActiveObject.TARGET;
            }
        });
        grid.add(leftPane, 0, 0);
        grid.add(rightPane, 1, 0);
        primaryStage.setTitle("Навигационная система");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        canvas.setOnMouseClicked(event -> {
            Point2D mousePosition = new Point2D(event.getX(), event.getY());
            switch (activeObject) {
                case TANK:
                    tank = new Tank(mousePosition);
                    break;
                case OBSTACLES:
                    addObstacle(mousePosition);
                    break;
                case TARGET:
                    target = mousePosition;
                    break;
            }
        });
        startButton.setOnAction(event -> moveTimer.start());
        redrawTimer.start();
    }

    public static void main(final String... args) {
        launch(args);
    }
}
