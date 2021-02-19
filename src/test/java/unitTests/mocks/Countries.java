package unitTests.mocks;

import com.revature.annotations.Attr;
import com.revature.annotations.PK;
import com.revature.annotations.Table;

import java.util.Objects;

@Table(tableName="countries")
public class Countries {
    @PK(columnName="country_id")
    @Attr(columnName="country_id")
    int id;

    @Attr(columnName = "name")
    String name;

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
        Countries countries = (Countries) o;
        return id == countries.id &&
                Objects.equals(name, countries.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Countries {" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
