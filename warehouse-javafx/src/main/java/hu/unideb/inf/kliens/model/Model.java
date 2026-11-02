package hu.unideb.inf.kliens.model;

public class Model
{
    private Item item;

    public Model() {
        item = new Item(0067,"Fogkefe");
    }

    public Item getItem() {return item;}
}
