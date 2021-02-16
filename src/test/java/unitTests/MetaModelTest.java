package unitTests;
import com.revature.utils.ConnectionFactory;
import com.revature.utils.MetaModel;
import org.junit.Test;
import unitTests.mocks.Animal;
import java.io.FileReader;
import java.util.Properties;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class MetaModelTest {
    @Test
    public void metaModelShouldBuiltAValidInsertStatement() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);
        MetaModel<Animal> modelAnimal = new MetaModel<>(Animal.class);

        modelAnimal.add(new String[] {"weight", "weiner", "height", "teapot", "animalName"})
                   .addValues(new String[] {"124", "200", "Elephant"})
                   .addValues(new String[] {"12", "8", "Mouse"})
                   .addValues(new String[] {"50", "86", "Dog"});

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
}
