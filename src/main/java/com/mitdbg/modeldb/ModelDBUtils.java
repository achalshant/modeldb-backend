package com.mitdbg.modeldb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

public class ModelDBUtils {

  private static final Logger LOGGER = Logger.getLogger(ModelDBUtils.class.getName());

  private ModelDBUtils() {}

  public static Map<String, Object> readYamlProperties() throws IOException {

    // --------------- Start reading properties from config.yaml file --------------------------

    String configFilePath = System.getenv("VERTA_MODELDB_CONFIG");
    LOGGER.log(Level.SEVERE, "Config file path: {0} ", configFilePath);

    InputStream inputStream = new FileInputStream(new File(configFilePath));
    Yaml yaml = new Yaml();
    Map<String, Object> prop = (Map<String, Object>) yaml.load(inputStream);
    LOGGER.log(Level.SEVERE, "Properties map: {0} ", prop);
    // --------------- Finish reading properties from config.yaml file --------------------------

    return prop;
  }

  public static String getStringFromProtoObject(MessageOrBuilder object)
      throws InvalidProtocolBufferException {
    return JsonFormat.printer().preservingProtoFieldNames().print(object);
  }

  public static Message.Builder getProtoObjectFromString(
      String jsonStringData, Message.Builder builder) throws InvalidProtocolBufferException {
    JsonFormat.parser().merge(jsonStringData, builder);
    return builder;
  }

  public static boolean isValidEmail(String email) {
    String emailRegex =
        "^[a-zA-Z0-9_+&*-]+(?:\\."
            + "[a-zA-Z0-9_+&*-]+)*@"
            + "(?:[a-zA-Z0-9-]+\\.)+[a-z"
            + "A-Z]{2,7}$";

    Pattern pat = Pattern.compile(emailRegex);
    if (email == null) return false;
    return pat.matcher(email).matches();
  }
}
