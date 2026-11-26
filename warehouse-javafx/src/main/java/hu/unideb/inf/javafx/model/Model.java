package hu.unideb.inf.javafx.model;

public class Model
{
    private Item item;

    public Model() {
        item = new Item(67,"Fogkefe");
    }

    public Item getItem() {return item;}
}
