package unitTests.mocks;

import com.revature.annotations.Attr;
import com.revature.annotations.FK;
import com.revature.annotations.PK;
import com.revature.annotations.Table;

import java.util.Objects;

@Table(tableName = "weightlifters")
public class Weightlifter {
    @PK(columnName = "weightlifter_id")
    private int id;

    @Attr(columnName = "firstname")
    private String firstName;

    @Attr(columnName = "lastname")
    private String lastName;

    @Attr(columnName = "weight")
    private double weight;

    @Attr(columnName = "height")
    private double height;

    @FK(columnName = "country_id")
    private int countryId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public int getCountryId() {
        return countryId;
    }

    public void setCountryId(int countryId) {
        this.countryId = countryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Weightlifter that = (Weightlifter) o;
        return id == that.id &&
                Double.compare(that.weight, weight) == 0 &&
                Double.compare(that.height, height) == 0 &&
                countryId == that.countryId &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, weight, height, countryId);
    }

    @Override
    public String toString() {
        return "Weightlifter {" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", weight=" + weight +
                ", height=" + height +
                ", countryId=" + countryId +
                '}';
    }
}

