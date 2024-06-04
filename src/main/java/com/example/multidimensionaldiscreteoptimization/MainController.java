package com.example.multidimensionaldiscreteoptimization;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainController {

    @FXML
    private ComboBox<Integer> cityCountComboBox;

    @FXML
    private TableView<Distance> distanceTable;

    @FXML
    private TableColumn<Distance, String> city1Column;

    @FXML
    private TableColumn<Distance, String> city2Column;

    @FXML
    private TableColumn<Distance, Double> distanceColumn;

    @FXML
    private TextArea resultTextArea;

    @FXML
    private Canvas mapCanvas;

    private List<City> cities = new ArrayList<>();
    private double[][] distances;
    private ObservableList<Distance> distanceList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cityCountComboBox.setItems(FXCollections.observableArrayList(5, 10, 15, 20));
        cityCountComboBox.setValue(10);

        city1Column.setCellValueFactory(new PropertyValueFactory<>("city1"));
        city2Column.setCellValueFactory(new PropertyValueFactory<>("city2"));
        distanceColumn.setCellValueFactory(new PropertyValueFactory<>("distance"));

        distanceTable.setItems(distanceList);
    }

    @FXML
    public void generateMap() {
        int cityCount = cityCountComboBox.getValue();
        generateCities(cityCount);
        calculateDistances();
        drawMap();
        updateDistanceTable();
    }

    @FXML
    public void calculateRoute() {
        // Запуск муравьиного алгоритма
        AntColonyOptimization aco = new AntColonyOptimization(cities, distances);
        aco.run();
        List<Integer> bestTour = aco.getBestTour();
        double bestLength = aco.getBestTourLength();

        resultTextArea.setText("Лучший маршрут: " + bestTour + "\nДлина: " + bestLength);
        drawRoute(bestTour);
    }

    private void generateCities(int count) {
        cities.clear();
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            cities.add(new City(rand.nextDouble() * mapCanvas.getWidth(), rand.nextDouble() * mapCanvas.getHeight()));
        }
    }

    private void calculateDistances() {
        int size = cities.size();
        distances = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                distances[i][j] = distances[j][i] = cities.get(i).distanceTo(cities.get(j));
            }
        }
    }

    private void drawMap() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());
        gc.setFill(Color.RED);
        for (City city : cities) {
            gc.fillOval(city.getX() - 5, city.getY() - 5, 10, 10);
        }
    }

    private void drawRoute(List<Integer> route) {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(2);
        for (int i = 0; i < route.size() - 1; i++) {
            City city1 = cities.get(route.get(i));
            City city2 = cities.get(route.get(i + 1));
            gc.strokeLine(city1.getX(), city1.getY(), city2.getX(), city2.getY());
        }
        // Замыкаем маршрут
        City lastCity = cities.get(route.get(route.size() - 1));
        City firstCity = cities.get(route.get(0));
        gc.strokeLine(lastCity.getX(), lastCity.getY(), firstCity.getX(), firstCity.getY());
    }

    private void updateDistanceTable() {
        distanceList.clear();
        int size = cities.size();
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                distanceList.add(new Distance("Город " + i, "Город " + j, distances[i][j]));
            }
        }
    }

    // Дополнительные классы
    private static class City {
        private double x, y;

        public City(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double distanceTo(City other) {
            return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
        }
    }

    public static class Distance {
        private String city1;
        private String city2;
        private double distance;

        public Distance(String city1, String city2, double distance) {
            this.city1 = city1;
            this.city2 = city2;
            this.distance = distance;
        }

        public String getCity1() {
            return city1;
        }

        public String getCity2() {
            return city2;
        }

        public double getDistance() {
            return distance;
        }
    }

    // Класс муравьиного алгоритма
    private static class AntColonyOptimization {
        private List<City> cities;
        private double[][] distances;
        private double[][] pheromones;
        private List<Integer> bestTour;
        private double bestTourLength;
        private Random rand = new Random();

        private static final int NUM_ANTS = 100;
        private static final int NUM_ITERATIONS = 1000;
        private static final double ALPHA = 1.0; // Влияние феромонов
        private static final double BETA = 2.0; // Влияние расстояния
        private static final double EVAPORATION_RATE = 0.5; // Коэффициент испарения феромонов
        private static final double Q = 100; // Константа для обновления феромонов

        public AntColonyOptimization(List<City> cities, double[][] distances) {
            this.cities = cities;
            this.distances = distances;
            this.pheromones = new double[cities.size()][cities.size()];
            for (int i = 0; i < cities.size(); i++) {
                Arrays.fill(pheromones[i], 1.0);
            }
            this.bestTour = new ArrayList<>();
            this.bestTourLength = Double.MAX_VALUE;
        }

        public void run() {
            for (int iter = 0; iter < NUM_ITERATIONS; iter++) {
                List<List<Integer>> allTours = new ArrayList<>();
                for (int ant = 0; ant < NUM_ANTS; ant++) {
                    List<Integer> tour = generateTour();
                    allTours.add(tour);
                    double tourLength = calculateTourLength(tour);
                    if (tourLength < bestTourLength) {
                        bestTour = new ArrayList<>(tour);
                        bestTourLength = tourLength;
                    }
                }
                updatePheromones(allTours);
            }
        }

        private List<Integer> generateTour() {
            List<Integer> tour = new ArrayList<>();
            boolean[] visited = new boolean[cities.size()];
            int currentCity = rand.nextInt(cities.size());
            tour.add(currentCity);
            visited[currentCity] = true;

            for (int i = 1; i < cities.size(); i++) {
                currentCity = selectNextCity(currentCity, visited);
                tour.add(currentCity);
                visited[currentCity] = true;
            }
            return tour;
        }

        private int selectNextCity(int currentCity, boolean[] visited) {
            double[] probabilities = new double[cities.size()];
            double sum = 0.0;

            for (int i = 0; i < cities.size(); i++) {
                if (!visited[i]) {
                    probabilities[i] = Math.pow(pheromones[currentCity][i], ALPHA) * Math.pow(1.0 / distances[currentCity][i], BETA);
                    sum += probabilities[i];
                }
            }

            double randValue = rand.nextDouble() * sum;
            for (int i = 0; i < cities.size(); i++) {
                if (!visited[i]) {
                    randValue -= probabilities[i];
                    if (randValue <= 0) {
                        return i;
                    }
                }
            }

            // В случае если выбор не был сделан, возвращаем первый не посещённый город (страховочный вариант)
            for (int i = 0; i < cities.size(); i++) {
                if (!visited[i]) {
                    return i;
                }
            }

            return -1; // Если все города посещены (не должно произойти)
        }

        private void updatePheromones(List<List<Integer>> allTours) {
            // Испарение феромонов
            for (int i = 0; i < cities.size(); i++) {
                for (int j = 0; j < cities.size(); j++) {
                    pheromones[i][j] *= (1 - EVAPORATION_RATE);
                }
            }

            // Добавление новых феромонов
            for (List<Integer> tour : allTours) {
                double tourLength = calculateTourLength(tour);
                for (int i = 0; i < tour.size() - 1; i++) {
                    int city1 = tour.get(i);
                    int city2 = tour.get(i + 1);
                    pheromones[city1][city2] += Q / tourLength;
                    pheromones[city2][city1] += Q / tourLength; // Обратный путь
                }
                // Замыкаем маршрут
                int lastCity = tour.get(tour.size() - 1);
                int firstCity = tour.get(0);
                pheromones[lastCity][firstCity] += Q / tourLength;
                pheromones[firstCity][lastCity] += Q / tourLength; // Обратный путь
            }
        }

        private double calculateTourLength(List<Integer> tour) {
            double length = 0.0;
            for (int i = 0; i < tour.size() - 1; i++) {
                length += distances[tour.get(i)][tour.get(i + 1)];
            }
            length += distances[tour.get(tour.size() - 1)][tour.get(0)]; // Замыкаем маршрут
            return length;
        }

        public List<Integer> getBestTour() {
            return bestTour;
        }

        public double getBestTourLength() {
            return bestTourLength;
        }
    }
}
