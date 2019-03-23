package com.mitdbg.modeldb;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.mitdbg.modeldb.entities.ArtifactEntity;
import com.mitdbg.modeldb.entities.ExperimentEntity;
import com.mitdbg.modeldb.entities.ExperimentRunEntity;
import com.mitdbg.modeldb.entities.FeatureEntity;
import com.mitdbg.modeldb.entities.KeyValueEntity;
import com.mitdbg.modeldb.entities.ProjectEntity;
import com.mitdbg.modeldb.entities.TagsMapping;
import io.grpc.protobuf.StatusProto;

public class ModelDBHibernateUtil {
  private static final Logger LOGGER = Logger.getLogger(ModelDBHibernateUtil.class.getName());
  private static StandardServiceRegistry registry;
  private static SessionFactory sessionFactory;
  public static Map<String, Object> databasePropMap;

  private ModelDBHibernateUtil() {}

  public static SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      try {

        Map<String, Object> rDBPropMap =
            (Map<String, Object>) databasePropMap.get("RdbConfiguration");

        String databaseName = (String) rDBPropMap.get("RdbDatabaseName");
        String rDBDriver = (String) rDBPropMap.get("RdbDriver");
        String rDBUrl = (String) rDBPropMap.get("RdbUrl");
        String rDBDialect = (String) rDBPropMap.get("RdbDialect");
        String configUsername = (String) rDBPropMap.get("RdbUsername");
        String configPassword = (String) rDBPropMap.get("RdbPassword");

        // Hibernate settings equivalent to hibernate.cfg.xml's properties
        Configuration configuration = new Configuration();

        Properties settings = new Properties();

        String connectionString =
            rDBUrl
                + "/"
                + databaseName
                + "?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8";
        settings.put(Environment.DRIVER, rDBDriver);
        settings.put(Environment.URL, connectionString);
        settings.put(Environment.USER, configUsername);
        settings.put(Environment.PASS, configPassword);
        settings.put(Environment.DIALECT, rDBDialect);
        settings.put(Environment.HBM2DDL_AUTO, "update");
        settings.put(Environment.SHOW_SQL, "false");
        configuration.setProperties(settings);

        configuration.addAnnotatedClass(ProjectEntity.class);
        configuration.addAnnotatedClass(ExperimentEntity.class);
        configuration.addAnnotatedClass(ExperimentRunEntity.class);
        configuration.addAnnotatedClass(KeyValueEntity.class);
        configuration.addAnnotatedClass(ArtifactEntity.class);
        configuration.addAnnotatedClass(FeatureEntity.class);
        configuration.addAnnotatedClass(TagsMapping.class);

        // Create registry builder
        StandardServiceRegistryBuilder registryBuilder =
            new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());

        // Create registry
        registry = registryBuilder.build();
        sessionFactory = configuration.buildSessionFactory(registry);
      } catch (Exception e) {
        e.printStackTrace();
        LOGGER.log(Level.WARNING, "ModelDBHibernateUtil getSessionFactory() getting error ", e);
        if (registry != null) {
          StandardServiceRegistryBuilder.destroy(registry);
        }
        Status status =
            Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(e.getMessage()).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    }
    return sessionFactory;
  }

  public static void shutdown() {
    if (registry != null) {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }
}
