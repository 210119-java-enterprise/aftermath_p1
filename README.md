# Aftermath ORM
This is a custom ORM created by Jonathan "Jay" Norman. It uses a wrapper around JDBC for the purpose of simplifying CRUD operations between a Java application and a SQL database (e.g. Postgres, Maria, MySQL, Oracle). This ORM takes influences from ORMs across languages, such as Hiberate (Java) and Mongoose (Node.js).
## Origin of the name
We assure you that our ORM has no connection to Dr. Dre and Aftermath Entertainment! Aftermath is a pun on the SQL acronym; SQL is typically pronounced "sequel", and a synonym of "sequel" is "aftermath".
## Setup
Aftermath uses annotations to map POJO/Java Beans to a database meta model. The following annotations Aftermath provides are:
1. @PK - Not PK Fire [:^)](https://www.youtube.com/watch?v=HglT7sTcuv8); Primary key
2. @FK - Foreign key
3. @Table - Database table
4. @Attr - Table column/attribute

You will annotate your fields and methods in your POJOs/Java Beans based on what they represent in the database. Make sure that you are using traditional getters and setters for your POJOs in order to integrate with Aftermath correctly. All the annotations have a name parameter so that you can mirror the name that is in the database. For instance, if you type @PK(columnName = "accountId"), this tells Aftermath that the annotated member is associated with an accountId that exists on your database. After the creation of your POJOs/Java Beans, add the class representation of your model by instantiating the CrudModel<T> class, like this:

```java
CrudModel<ModelClass> mm = new CrudModel<>(ModelClass.class);
```

The CrudModel<T> class serves as the bridge between Aftermath and your database; the CrudModel<T> contains the CRUD methods necessary to build and execute CRUD operations (i.e. SELECT, INSERT, UPDATE, DELETE);

You will use an application.properties file to contain your database credentials; it's not recommended to post your credentials on Github in plain text. We also recommend you to include the path to your application.properties file in your .gitignore file. The structure you will use for your application.properties file is:

```
url=<link to your database>
user=<username in your database>
password=<your database account password>
currentSchema=<name of the schema you're working in>
```

It should be noted that application.properties files have no quotes for their strings, so type your string values without quotes. In order to load the application.properties file into Aftermath, use the ConnectionFactory.addCredentials static method, like this:

```java
try {
   Properties props = new Properties();
   ConnectionFactory.addCredentials(props.load(new FileReader("<url to your application.properties file>")));
} catch (IOException e) {
   // handle exception
}
```

## CRUD methods
Currently, there are 4 main CRUD methods: grab(), add(), change(), and remove(), which corresponds to select, insert, update, and delete respectively. These methods can be called off a CRUDModel<> object. 

### grab()
Call this method if you want to build a select statement. If you pass no arguments to grab(), it will retrieve all the columns/attributes on a table (so it's essentially a select * from table statement). The grab() method can take in a String array to select for particular columns, like this:

```java
CrudModel<Artist> artists = new CrudModel<Artist>(Artist.class);
artist.grab(new String[] {"firstname","lastname","age"});
```

Alternatively, you can add the string arguments directly into the grab method:

```java
CrudModel<Artist> artists = new CrudModel<Artist>(Artist.class);
artist.grab("firstname","lastname","age");
```

#### where(), or(), and(), not()
These methods are used in conjunction with the grab(), change(), and remove() methods; as you might expect, they are used to construct a where clause. For the and(), or(), and not() methods, you can specify a condition by using the Conditions enum. The conditions enum has the following values:

```java
public enum Conditions {
    EQUALS,   
    NOT_EQUALS, 
    GT, // greater than
    LT, // less than
    GTE, // greater than or equal to
    LTE // less than or equal to
}
```

To use the conditions enum with the where clase methods, pass in an enum values in and(), or(), and not(), like this:

```java
CrudModel<Artist> artists = new CrudModel<Artist>(Artist.class);
artist.grab("firstname","lastname","age")
   .where(Conditions.EQUALS, "firstname", "Bob")
   .and(Conditions.EQUALS, "lastname", "Ross");
```

If you want to use a negation operation on and() and or(), call and() or or() with zero arguments, then call not() afterwards with the first argument being the Condition enum value you want to use, followed by the column name and value. Like this:

```java
CrudModel<Artist> artists = new CrudModel<Artist>(Artist.class);
artist.grab("firstname","lastname","age")
   .where(Conditions.EQUALS, "firstname", "Bob")
   .and()
   .not(Conditions.EQUALS, "lastname", "Ross");
```

#### runGrab()
This method returns an ArrayList of your model. Example:
```java
CrudModel<Artist> artists = new CrudModel<Artist>(Artist.class);
ArrayList<Artist> artist.grab("firstname","lastname","age")
   .where(Conditions.EQUALS, "firstname", "Bob")
   .and(Conditions.EQUALS, "lastname", "Ross")
   .runGrab();
```

### add(), addValues(), and runAdd()
Call this method to create insert statements. Example:

```java
CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

int rowsAffected = weightlifter.add("firstname", "lastname", "weight", "height", "country_id")
                .addValues("Tatiana", "Kashirina", "108", "177", String.valueOf(Country.Russia.ordinal() + 1))
                .addValues("Lasha", "Talakhadze", "168", "197", String.valueOf(Country.Georgia.ordinal() + 1))
                .addValues("Kendrick", "Farris", "97", "175", String.valueOf(Country.USA.ordinal() + 1))
                .addValues("Jacques", "Demers", "75", "175", String.valueOf(Country.Canada.ordinal() + 1))
                .addValues("Lidia", "Valentín", "78.80", "169", String.valueOf(Country.Canada.ordinal() + 1))
                .addValues("Meredith", "Alwine", "71", "172", String.valueOf(Country.USA.ordinal() + 1))
                .runAdd();
```
As one can see, call the add() method of a CrudModel object with the columns you want to insert values for. Then call addValues() off of either add() or another addValues() method; the arguments for addValues() correspond to the values associated with a column. If you establish an order on add(), that order must be applied to the addValues() argument. Call runAdd() to execute an insert statement; the returned value is the number of rows inserted. 

### change() and set()
Call change() to build an update statement; the arguments are the columns to update. Call set() to adjust the values to the columns specified in change(). Example:

```java
CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

int rowsAffected = weightlifter.change("lastname", "firstname", "weight")
                .set("Tani", "Kashiri", "144")
                .where(Conditions.EQUALS, "firstname","Tatiana")
                .runChange();
```

### remove(), and runRemove()
Does what you expect: removes rows based on the criteria set by where(). Example:

```java
CrudModel<Weightlifter> weightlifter = new CrudModel<>(Weightlifter.class);

int rowsAffected = weightlifter.remove()
                .where(Conditions.EQUALS, "weightlifter_id","6")
		.runRemove();
```

## Transactions
Sometimes, you don't want a CRUD operation to fully take place until other CRUD operations take place. To start off with transactions, turn off auto commit by calling turnOffAutoCommit() before you make your CRUD operations. 
You can create savepoints by calling addSavepoint() and pass the name of your savepoint as an argument. To rollback, call the rollback() method and pass it the name of a savepoint you have created. Example:

```java
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
weightlifters.turnOnAutoCommit();
```
- [x] Basic documentation of the annotations and the xml config file
- [x] CrudModel<T> containment implementation
- [x] CRUD features

