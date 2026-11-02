package hu.unideb.inf.kliens.model;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Item
{
    private String Name;
    private int Id;
    private int Count;
    private ArrayList<String> Locations;

    public Item(int id, String name)
    {
        Id = id;
        Name = name;
        Count = 0;
    }

    public String getName()
    {
        return Name;
    }

    public void setName(String name)
    {
        Name = name;
    }

    public int getId()
    {
        return Id;
    }

    public void setId(int id)
    {
        Id = id;
    }

    public int getCount()
    {
        Count = Locations.size();
        return Count;
    }

    public ArrayList<String> getLocations()
    {
        return Locations;
    }

    public void addLocation(String location)
    {
        Locations.add(location);
    }

}
