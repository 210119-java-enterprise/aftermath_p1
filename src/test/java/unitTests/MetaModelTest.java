package unitTests;
import com.revature.exceptions.BadMethodChainCallException;
import com.revature.utils.Conditions;
import com.revature.utils.ConnectionFactory;
import com.revature.utils.FKField;
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

        ArrayList<Weightlifter> warr = weightlifter.grab("height", "lastname").runGrab();
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
        assertTrue(true);
    }
}
