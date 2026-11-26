package hu.unideb.inf.javafx.model;

import java.util.ArrayList;
import java.util.List;

public class Item {
    private String name;
    private int id;
    private int count;
    private final List<String> locations = new ArrayList<>();

    public Item(int id, String name) {
        this.id = id;
        this.name = name;
        this.count = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCount() {
        this.count = locations.size();
        return count;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void addLocation(String location) {
        if (location != null && !location.isBlank()) {
            locations.add(location);
        }
    }
}