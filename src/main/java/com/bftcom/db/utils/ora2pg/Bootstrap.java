package com.bftcom.db.utils.ora2pg;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Считываем конфигурационный файл. Первый вызов программы должен быть
 * Bootstrap.bootstrap();
 */
public class Bootstrap {
  private static Logger logger = LoggerFactory.getLogger(Bootstrap.class);
  public static final String PROJECT_PROPERTIES_FILENAME = "project.properties";

  static {
    Properties props = new Properties();
    try (FileInputStream inStream = new FileInputStream(PROJECT_PROPERTIES_FILENAME)) {
      props.load(inStream);
      for (Object o : props.keySet()) {
        String s = (String) o;
        System.setProperty(s, String.valueOf(props.get(s)));
      }
    } catch (IOException e) {
      logger.warn("", e);
    }
  }

  private Bootstrap() {
  }

  public static void bootstrap() {
    //dummy. считываем конфигурационный файл
  }
}
