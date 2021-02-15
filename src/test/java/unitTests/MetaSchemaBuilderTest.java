package unitTests;
import com.revature.utils.ConnectionFactory;
import com.revature.utils.MetaSchemaBuilder;
import org.junit.Test;
import unitTests.mocks.Animal;
import java.io.FileReader;
import java.util.Properties;

import static org.junit.Assert.*;

public class MetaSchemaBuilderTest
{
    @Test
    public void metaSchemaBuilderShouldAddAModelToTheMapIfClassExists() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        MetaSchemaBuilder<Animal> animal = new MetaSchemaBuilder<>();
        animal.addModel(Animal.class);
        assertNotNull(animal.getModel("Animal"));
    }

    @Test
    public void metaSchemaBuilderShouldntAllowdDuplicateModels() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application.properties"));
        ConnectionFactory.addCredentials(props);

        MetaSchemaBuilder<Animal> animal = new MetaSchemaBuilder<>();
        animal.addModel(Animal.class);
        animal.addModel(Animal.class);
        assertEquals(1, animal.count());
    }
}
