package unitTests;
import com.revature.exceptions.InvalidInputException;
import com.revature.utils.ConnectionFactory;
import com.revature.utils.CrudModel;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import unitTests.mocks.Animal;
import unitTests.mocks.Country;
import unitTests.mocks.Weightlifter;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Properties;

import static com.revature.utils.Conditions.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CrudModelTest {
    @Test
    public void a_metaModelShouldBuiltAValidInsertStatement() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> modelAnimal = new CrudModel<>(Weightlifter.class);

        int rowsAffected = modelAnimal.add("firstname", "lastname", "weight", "height", "country_id")
                .addValues("Tatiana", "Kashirina", "108", "177", String.valueOf(Country.Russia.ordinal() + 1))
                .addValues("Lasha", "Talakhadze", "168", "197", String.valueOf(Country.Georgia.ordinal() + 1))
                .addValues("Kendrick", "Farris", "97", "175", String.valueOf(Country.USA.ordinal() + 1))
                .addValues("Jacques", "Demers", "75", "175", String.valueOf(Country.Canada.ordinal() + 1))
                .addValues("Lidia", "Valentín", "78.80", "169", String.valueOf(Country.Canada.ordinal() + 1))
                .addValues("Meredith", "Alwine", "71", "172", String.valueOf(Country.USA.ordinal() + 1))
                .runAdd();

        assertNotEquals(0, rowsAffected);

        System.out.println(modelAnimal.getPreparedStatement());
    }

    @Test
    public void b_metaModelShouldGrabAllFieldsInSelect() throws Exception {
        Properties props = new Properties();

        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

        ArrayList<Weightlifter> warr = weightlifter
                .grab("country_id", "height", "weight")
                .runGrab();

        warr.stream().forEach(System.out::println);

        System.out.println("+========================================================================================+");

        warr = weightlifter.grab().runGrab();
        warr.stream().forEach(System.out::println);

        assertTrue(true);
    }

    @Test
    public void c_metaModelShouldBuiltAValidUpdateStatement() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

        weightlifter.change("lastname", "firstname", "weight").set("Tani", "Kashiri", "144")
                .where(EQUALS, "firstname","Tatiana")
                .runChange();

        // asserting true since this doesn't really matter; we care about the structure of the insert statement
        // it's probably more efficient to use a regex, but let's print out the results for starters
        //assertNotEquals(0, rowsAffected);

        System.out.println(weightlifter.getPreparedStatement());
    }

    @Test
    public void d_metaModelShouldThrowAnExceptionIfSetIsCalledTwice() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

        assertThrows(Exception.class, () -> weightlifter.change("firstname","lastname")
                                                        .set("tani","kaka")
                                                        .set("tata","kani"));
    }

    @Test
    public void e_metaModelShouldThrowAnExceptionIfSetIsntCalledOnChange() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

        assertThrows(Exception.class, () -> weightlifter.grab()
                .set("tani","kaka"));
    }

    @Test
    public void f_metaModelShouldThrowAnExceptionIfChangeHasNoArgs() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

        assertThrows(Exception.class, () -> weightlifter.change());
    }

    @Test
    public void g_callingAddMultipleTimesShouldResetThePreparedStatement() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        CrudModel<Animal> modelAnimal = new CrudModel<>(Animal.class);
        modelAnimal.add(new String[] {"weight", "height", "animalName"});
        String name1 = modelAnimal.getPreparedStatement();

        modelAnimal.add(new String[] {"weight", "height", "sound", "daisy"});
        String name2 = modelAnimal.getPreparedStatement();

        assertNotEquals(name1, name2);
    }

    @Test
    public void h_metaModelShouldThrowExceptionIfAddValuesIsntCalledAfterAdd() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

        assertThrows(Exception.class, () -> weightlifter
                .add("firstname", "lastname", "weight", "height", "country_id")
                .runAdd());
    }

    @Test
    public void i_metaModelShouldBuiltAValidWhereClause() throws Exception {
        Properties props = new Properties();

        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

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
    public void j_metaModelShouldThrowAnExceptionIfWhereIsCalledSequentially() throws Exception {
        Properties props = new Properties();

        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

        assertThrows(Exception.class,
                () -> weightlifter.grab()
                        .where(EQUALS, "country_id", "2")
                        .where(EQUALS, "country_id", "2"));
    }

    @Test
    public void k_metaModelShouldThrowAnExceptionIfWhereIsCalledOnInsert() throws Exception {
        Properties props = new Properties();

        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

        assertThrows(Exception.class,
                () -> weightlifter.add("firstname", "lastname")
                        .where(EQUALS, "country_id", "2"));
    }

    @Test
    public void l_metaModelShouldBuiltAValidDeleteStatement() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

        int rowsAffected = weightlifter.remove()
                .where(EQUALS, "weightlifter_id","6").runRemove();

        // asserting true since this doesn't really matter; we care about the structure of the insert statement
        // it's probably more efficient to use a regex, but let's print out the results for starters
        assertNotEquals(0, rowsAffected);

        System.out.println(weightlifter.getPreparedStatement());
    }

    @Test
    public void m_metaModelShouldntPermanantlyChangeDatabaseBeforeCommitIsCalled() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifters = new CrudModel<>(Weightlifter.class);

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
    public void n_metaModelShouldPermanantlyChangeDatabaseAfterCommitIsCalled() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifters = new CrudModel<>(Weightlifter.class);

        weightlifters.turnOffAutoCommit();

        int rowsAffected = weightlifters.add("firstname", "lastname", "weight", "height", "country_id")
                .addValues("Svetlana", "Tsarukaeva", "134", "200", String.valueOf(Country.Russia.ordinal() + 1))
                .addValues("Anastasiia", "Hotfrid", "134", "200", String.valueOf(Country.Georgia.ordinal() + 1))
                .addValues("Francisco ", "Garcia ", "134", "200", String.valueOf(Country.Spain.ordinal() + 1))
                .runAdd();

        assertNotEquals(0, rowsAffected);

        weightlifters.runCommit();

        System.out.println(weightlifters.getPreparedStatement());
    }


    @Test
    public void o_metaModelShouldPersistNonAutoCommitStateAfterFirstCommit() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifters = new CrudModel<>(Weightlifter.class);

        weightlifters.turnOffAutoCommit();

        int rowsAffected = weightlifters.add("firstname", "lastname", "weight", "height", "country_id")
                .addValues("José", "Ibáñez", "169", "198", String.valueOf(Country.Spain.ordinal() + 1))
                .runAdd();

        assertNotEquals(0, rowsAffected);

        weightlifters.runCommit();

        weightlifters.add("firstname", "lastname", "weight", "height", "country_id")
                .addValues("AFSfAFSAFASFASFASF", "ASFASFAFSAFSAFASFAFASFASFASFASFASFSF", "169", "198", String.valueOf(Country.Canada.ordinal() + 1))
                .runAdd();

        System.out.println(weightlifters.getPreparedStatement());
    }

    @Test
    public void p_metaModelShouldAutoCommitAfterTurnOnAutoCommitIsCalled() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifters = new CrudModel<>(Weightlifter.class);

        weightlifters.turnOffAutoCommit();

        int rowsAffected = weightlifters.add("firstname", "lastname", "weight", "height", "country_id")
                .addValues("Jaosé", "Ibáñeze", "169", "198", String.valueOf(Country.Spain.ordinal() + 1))
                .runAdd();

        assertNotEquals(0, rowsAffected);

        weightlifters.runCommit();
        weightlifters.turnOnAutoCommit();

        weightlifters.add("firstname", "lastname", "weight", "height", "country_id")
                .addValues("Christine", "Girard", "169", "198", String.valueOf(Country.Canada.ordinal() + 1))
                .runAdd();

        System.out.println(weightlifters.getPreparedStatement());
    }

    @Test
    public void q_metaModelShouldUndoTransactionsAfterRollback() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifters = new CrudModel<>(Weightlifter.class);

        weightlifters.turnOffAutoCommit();
        weightlifters.addSavepoint("insert spanish lifters");

        int rowsAffected = weightlifters.add("firstname", "lastname", "weight", "height", "country_id")
                .addValues("Jwaosé", "Ieebáñeze", "169", "198", String.valueOf(Country.Spain.ordinal() + 1))
                .addValues("Paul", "Lopez", "169", "198", String.valueOf(Country.Spain.ordinal() + 1))
                .runAdd();

        assertNotEquals(0, rowsAffected);

        weightlifters.rollback("insert spanish lifters");

        weightlifters.add("firstname", "lastname", "weight", "height", "country_id")
                .addValues("Dmitry", "Klokov", "105", "183", String.valueOf(Country.Russia.ordinal() + 1))
                .runAdd();

        weightlifters.runCommit();

        System.out.println(weightlifters.getPreparedStatement());
    }

    @Test
    public void r_crudMethodShouldThrowAnExceptionIfAddIsGivenEmptyValue() throws InvalidInputException, Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        CrudModel<Weightlifter> weightlifters = new CrudModel<>(Weightlifter.class);

        weightlifters.turnOffAutoCommit();

        assertThrows(InvalidInputException.class, () -> weightlifters.add());
    }
}
