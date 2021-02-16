package unitTests.mocks;

import com.revature.annotations.Attr;
import com.revature.annotations.PK;
import com.revature.annotations.Table;

import java.util.Objects;

@Table(tableName="animals")
public class Animal {
    @PK(columnName="animal_id")
    private int id;

    @Attr(columnName="animalName")
    private String name;

    @Attr(columnName = "sound")
    private String sound;

    @Attr(columnName="weight")
    private double weight;

    @Attr(columnName="height")
    private double height;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Animal animal = (Animal) o;
        return id == animal.id &&
                Double.compare(animal.weight, weight) == 0 &&
                Double.compare(animal.height, height) == 0 &&
                Objects.equals(name, animal.name) &&
                Objects.equals(sound, animal.sound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, sound, weight, height);
    }

    @Override
    public String toString() {
        return "Animal {" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sound='" + sound + '\'' +
                ", weight=" + weight +
                ", height=" + height +
                '}';
    }
}
