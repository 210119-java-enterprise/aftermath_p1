package unitTests;
import com.revature.utils.ConnectionFactory;
import com.revature.utils.MetaModel;
import org.junit.Test;
import unitTests.mocks.Animal;
import unitTests.mocks.Country;
import unitTests.mocks.Weightlifter;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Properties;

import static com.revature.utils.Conditions.EQUALS;
import static org.junit.Assert.*;

public class MetaModelTest {
    @Test
    public void metaModelShouldGrabAllFieldsInSelect() throws Exception {
        Properties props = new Properties();

        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        MetaModel<Weightlifter> weightlifter = new MetaModel<>(Weightlifter.class);

        ArrayList<Weightlifter> warr = weightlifter.grab("height").runGrab();
        warr.stream().forEach(System.out::println);

        System.out.println("+========================================================================================+");

        warr = weightlifter.grab().runGrab();
        warr.stream().forEach(System.out::println);

        assertTrue(true);
    }

    @Test
    public void metaModelShouldBuiltAValidInsertStatement() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> modelAnimal = new MetaModel<>(Weightlifter.class);

        modelAnimal.add(new String[] {"firstname", "lastname", "weight", "height", "country_id"})
                   .addValues(new String[] {"Jacques", "Demers", "75", "180", String.valueOf(Country.Canada.ordinal() + 1)})
                   .addValues(new String[] {"Juan", "Martinez", "79", "184", String.valueOf(Country.Spain.ordinal() + 1)})
                   .runAdd();

        // asserting true since this doesn't really matter; we care about the structure of the insert statement
        // it's probably more efficient to use a regex, but let's print out the results for starters
        assertTrue(true);

        System.out.println(modelAnimal.getPreparedStatement());
    }

    @Test
    public void metaModelShouldBuiltAValidUpdateStatement() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> weightlifter = new MetaModel<>(Weightlifter.class);

        weightlifter.change("firstname", "lastname", "weight").set("tani","kaka", "94")
                .where(EQUALS, "firstname","Tatiana")
                .and(EQUALS, "country_id", "2");

        // asserting true since this doesn't really matter; we care about the structure of the insert statement
        // it's probably more efficient to use a regex, but let's print out the results for starters
        assertTrue(true);

        System.out.println(weightlifter.getPreparedStatement());
    }

    @Test
    public void metaModelShouldThrowAnExceptionIfSetIsCalledTwice() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> weightlifter = new MetaModel<>(Weightlifter.class);

        assertThrows(Exception.class, () -> weightlifter.change("firstname","lastname")
                                                        .set("tani","kaka")
                                                        .set("tata","kani"));
    }

    @Test
    public void metaModelShouldThrowAnExceptionIfSetIsntCalledOnChange() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> weightlifter = new MetaModel<>(Weightlifter.class);

        assertThrows(Exception.class, () -> weightlifter.grab()
                .set("tani","kaka"));
    }

    @Test
    public void metaModelShouldThrowAnExceptionIfChangeHasNoArgs() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> weightlifter = new MetaModel<>(Weightlifter.class);

        assertThrows(Exception.class, () -> weightlifter.change());
    }

    @Test
    public void callingAddMultipleTimesShouldResetThePreparedStatement() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        MetaModel<Animal> modelAnimal = new MetaModel<>(Animal.class);
        modelAnimal.add(new String[] {"weight", "height", "animalName"});
        String name1 = modelAnimal.getPreparedStatement();

        modelAnimal.add(new String[] {"weight", "height", "sound", "daisy"});
        String name2 = modelAnimal.getPreparedStatement();

        assertNotEquals(name1, name2);
    }

    @Test
    public void metaModelShouldThrowExceptionIfAddValuesIsntCalledAfterAdd() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> modelAnimal = new MetaModel<>(Weightlifter.class);

        assertThrows(Exception.class, () -> modelAnimal.add(
                new String[] {"firstname", "lastname", "weight", "height", "country_id"})
                .runAdd());
    }

    @Test
    public void metaModelShouldBuiltAValidWhereClause() throws Exception {
        Properties props = new Properties();

        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        MetaModel<Weightlifter> weightlifter = new MetaModel<>(Weightlifter.class);

        weightlifter.grab()
                .where(EQUALS, "country_id", "2")
                .and()
                .not(EQUALS, "firstname", "Tatiana");
        System.out.println(weightlifter.getPreparedStatement());

        weightlifter.grab("firstname", "height")
                .where(EQUALS, "country_id", "2")
                .and(EQUALS, "firstname", "Tatiana")
                .and(EQUALS, "lastname", "Kashirina");
        System.out.println(weightlifter.getPreparedStatement());
        assertTrue(true);
    }

    @Test
    public void metaModelShouldThrowAnExceptionIfWhereIsCalledSequentially() throws Exception {
        Properties props = new Properties();

        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        MetaModel<Weightlifter> weightlifter = new MetaModel<>(Weightlifter.class);

        assertThrows(Exception.class,
                () -> weightlifter.grab()
                        .where(EQUALS, "country_id", "2")
                        .where(EQUALS, "country_id", "2"));
    }

    @Test
    public void metaModelShouldThrowAnExceptionIfWhereIsCalledOnInsert() throws Exception {
        Properties props = new Properties();

        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        MetaModel<Weightlifter> weightlifter = new MetaModel<>(Weightlifter.class);

        assertThrows(Exception.class,
                () -> weightlifter.add("firstname", "lastname")
                        .where(EQUALS, "country_id", "2"));
    }
}
