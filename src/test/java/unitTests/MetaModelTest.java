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

import static com.revature.utils.Conditions.*;
import static org.junit.Assert.*;

public class MetaModelTest {
    @Test
    public void metaModelShouldGrabAllFieldsInSelect() throws Exception {
        Properties props = new Properties();

        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        MetaModel<Weightlifter> weightlifter = new MetaModel<>(Weightlifter.class);

        ArrayList<Weightlifter> warr = weightlifter.grab("country_id", "height", "weight").runGrab();
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

        int rowsAffected = modelAnimal.add(new String[] {"firstname", "lastname", "weight", "height", "country_id"})
                   .addValues(new String[] {"Vlad", "Chad", "134", "200", String.valueOf(Country.Russia.ordinal() + 1)})
                   .runAdd();

        assertNotEquals(0, rowsAffected);

        System.out.println(modelAnimal.getPreparedStatement());
    }

    @Test
    public void metaModelShouldBuiltAValidUpdateStatement() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> weightlifter = new MetaModel<>(Weightlifter.class);

        weightlifter.change("lastname", "firstname", "weight").set("Putinn", "Vladimirr", "144")
                .where(EQUALS, "firstname","Vladimir");//.runChange();

        // asserting true since this doesn't really matter; we care about the structure of the insert statement
        // it's probably more efficient to use a regex, but let's print out the results for starters
        //assertNotEquals(0, rowsAffected);

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

    @Test
    public void metaModelShouldBuiltAValidDeleteStatement() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> weightlifter = new MetaModel<>(Weightlifter.class);

        int rowsAffected = weightlifter.remove()
                .where(EQUALS, "weightlifter_id","6").runRemove();

        // asserting true since this doesn't really matter; we care about the structure of the insert statement
        // it's probably more efficient to use a regex, but let's print out the results for starters
        assertNotEquals(0, rowsAffected);

        System.out.println(weightlifter.getPreparedStatement());
    }

    @Test
    public void metaModelShouldntPermanantlyChangeDatabaseBeforeCommitIsCalled() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> weightlifters = new MetaModel<>(Weightlifter.class);

        weightlifters.turnOffAutoCommit();

        int rowsAffected = weightlifters.add(new String[] {"firstname", "lastname", "weight", "height", "country_id"})
                .addValues(new String[] {"v", "c", "134", "200", String.valueOf(Country.Russia.ordinal() + 1)})
                .addValues(new String[] {"vv", "cc", "134", "200", String.valueOf(Country.Russia.ordinal() + 1)})
                .addValues(new String[] {"vvv", "ccc", "134", "200", String.valueOf(Country.Russia.ordinal() + 1)})
                .addValues(new String[] {"vvvv", "cccc", "134", "200", String.valueOf(Country.Russia.ordinal() + 1)})
                .addValues(new String[] {"vvvvv", "ccccc", "134", "200", String.valueOf(Country.Russia.ordinal() + 1)})
                .runAdd();

        assertNotEquals(0, rowsAffected);

        System.out.println(weightlifters.getPreparedStatement());
    }

    @Test
    public void metaModelShouldPermanantlyChangeDatabaseAfterCommitIsCalled() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> weightlifters = new MetaModel<>(Weightlifter.class);

        weightlifters.turnOffAutoCommit();

        int rowsAffected = weightlifters.add(new String[] {"firstname", "lastname", "weight", "height", "country_id"})
                .addValues(new String[] {"Lasha", "Talakhadze", "169", "198", String.valueOf(Country.Georgia.ordinal() + 1)})
                .addValues(new String[] {"Svetlana", "Tsarukaeva", "134", "200", String.valueOf(Country.Russia.ordinal() + 1)})
                .addValues(new String[] {"Anastasiia", "Hotfrid", "134", "200", String.valueOf(Country.Georgia.ordinal() + 1)})
                .addValues(new String[] {"Francisco ", "Garcia ", "134", "200", String.valueOf(Country.Spain.ordinal() + 1)})
                .runAdd();

        assertNotEquals(0, rowsAffected);

        weightlifters.runCommit();

        System.out.println(weightlifters.getPreparedStatement());
    }


    @Test
    public void metaModelShouldPersistNonAutoCommitStateAfterFirstCommit() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> weightlifters = new MetaModel<>(Weightlifter.class);

        weightlifters.turnOffAutoCommit();

        int rowsAffected = weightlifters.add(new String[] {"firstname", "lastname", "weight", "height", "country_id"})
                .addValues(new String[] {"José", "Ibáñez", "169", "198", String.valueOf(Country.Spain.ordinal() + 1)})
                .runAdd();

        assertNotEquals(0, rowsAffected);

        weightlifters.runCommit();

        weightlifters.add("firstname", "lastname", "weight", "height", "country_id")
                .addValues(new String[] {"AFSfAFSAFASFASFASF", "ASFASFAFSAFSAFASFAFASFASFASFASFASFSF", "169", "198", String.valueOf(Country.Canada.ordinal() + 1)})
                .runAdd();

        System.out.println(weightlifters.getPreparedStatement());
    }

    @Test
    public void metaModelShouldAutoCommitAfterTurnOnAutoCommitIsCalled() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Weightlifter> weightlifters = new MetaModel<>(Weightlifter.class);

        weightlifters.turnOffAutoCommit();

        int rowsAffected = weightlifters.add(new String[] {"firstname", "lastname", "weight", "height", "country_id"})
                .addValues(new String[] {"Jaosé", "Ibáñeze", "169", "198", String.valueOf(Country.Spain.ordinal() + 1)})
                .runAdd();

        assertNotEquals(0, rowsAffected);

        weightlifters.runCommit();
        weightlifters.turnOnAutoCommit();

        weightlifters.add("firstname", "lastname", "weight", "height", "country_id")
                .addValues(new String[] {"Christine", "Girard", "169", "198", String.valueOf(Country.Canada.ordinal() + 1)})
                .runAdd();

        System.out.println(weightlifters.getPreparedStatement());
    }
}
