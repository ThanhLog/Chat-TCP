module ChatApp {
  requires java.rmi;
  requires java.desktop;
  requires org.mongodb.driver.sync.client;
  requires java.xml;
  requires org.mongodb.bson;
  requires org.mongodb.driver.core;
  requires com.google.gson;
  exports chat ;
}
