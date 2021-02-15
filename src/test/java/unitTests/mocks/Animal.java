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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Animal animal = (Animal) o;
        return id == animal.id &&
                Objects.equals(name, animal.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Animal {" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
