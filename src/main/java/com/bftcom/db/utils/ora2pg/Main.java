package com.bftcom.db.utils.ora2pg;

import com.bftcom.db.postgresql.BftPostgreSqlDatabasePlatform;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.oracle.OracleDatabasePlatform;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.symmetric.common.SystemConstants;
import org.jumpmind.symmetric.io.data.DbExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * date: 30.06.2016
 */
public class Main {
  private static Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    Bootstrap.bootstrap();//first call
    Converter converter = new Converter(System.getProperties());
    converter.convert();
  }
}

