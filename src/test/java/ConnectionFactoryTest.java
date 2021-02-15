import com.revature.utils.ConnectionFactory;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

import static org.junit.Assert.*;

public class ConnectionFactoryTest {
    @Test
    public void connectionFactoryShouldCreateOnlyOneFactoryInstance() {
        ConnectionFactory c1 = ConnectionFactory.getInstance();
        ConnectionFactory c2 = ConnectionFactory.getInstance();
        assertSame(c1, c2);
    }

    @Test
    public void connectionFactoryShouldCreateOnlyOneConnectionInstance() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application2.properties"));
        ConnectionFactory.addCredentials(props);
        Connection conn1 = ConnectionFactory.getInstance().getConnection();
        Connection conn2 = ConnectionFactory.getInstance().getConnection();

        assertSame(conn1, conn2);
    }

    @Test
    public void connectionFactoryShouldThrowAnErrorIfBadCredentialsAreProvided() throws IOException {
        Properties props = new Properties();
        props.load(new FileReader("src/main/resources/application2.properties"));
        ConnectionFactory.addCredentials(props);
        Connection conn = ConnectionFactory.getInstance().getConnection();

        assertThrows(Exception.class, () -> conn.prepareStatement("select * from weightlifters"));
    }

    @Test
    public void connectionFactoryShouldThrowAnErrorIfPropertiesFileIsNotFound() {
        Properties props = new Properties();
        assertThrows(FileNotFoundException.class, () -> props.load(new FileReader("src/main/resources/application3.properties")));
    }
}
