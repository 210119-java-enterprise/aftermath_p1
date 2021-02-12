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

You will annotate your fields and methods in your POJOs/Java Beans based on what they represent in the database. All the annotations have a name parameter so that you can mirror the name that is in the database. For instance, if you type @PK(name = "accountId"), this tells Aftermath that the annotated member is associated with an accountId that exists on your database. After the creation of your POJOs/Java Beans, add the class representation of your models in MetaSchemaBuilder, like this:

```java
MetaSchemaBuilder.addModel(yourModelInstance.class);
```

Alternatively, you can write:

```java
Class<ModelClass>[] classes = new Class<ModelClass>[] { model1.class, model2.class };
MetaSchemaBuilder.addModels(classes);
```

You will use an application.properties file to contain your database credentials; it's not recommended to post your credentials on Github in plain text. We also recommend you to include the path to your application.properties file in your .gitignore file. The structure you will use for your application.properties file is:

```
url=<link to your database>
user=<username in your database>
password=<your database account password>
currentSchema=<name of the schema you're working in>
```

In order to load the application.properties file into Aftermath, use the MetaSchemaBuilder.addCredentials static method, like this:

```java
try {
   Properties props = new Properties();
   MetaSchemaBuilder.addCredentials(props.load(new FileReader("<url to your application.properties file>")));
} catch (IOException e) {
   // handle exception
}
```

- [x] Basic documentation of the annotations and the xml config file
- [ ] MetalModel containment implementation
- [ ] CRUD features

