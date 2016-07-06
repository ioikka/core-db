package com.bftcom.db.utils.ora2pg;

import com.bftcom.db.postgresql.BftPostgreSqlDatabasePlatform;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.oracle.OracleDatabasePlatform;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.symmetric.io.data.DbExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * <p>
 * date: 30.06.2016
 */
public class Main {
  private static Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    Bootstrap.bootstrap();//first call

    String[] excludedTables = System.getProperty(ConfigConstants.EXCLUDED_TABLES).trim().split(",");
    List<String> excludedTablesList = new ArrayList<>(Arrays.asList(excludedTables));

    try {
      DataSource srcDataSource;
      srcDataSource = BasicDataSourceFactory.createDataSource(Utils.getSrcProperties());
      IDatabasePlatform srcPlatform = new OracleDatabasePlatform(srcDataSource, new SqlTemplateSettings());

      DataSource dstDataSource;
      dstDataSource = BasicDataSourceFactory.createDataSource(Utils.getDstProperties());
      IDatabasePlatform dstPlatform = new BftPostgreSqlDatabasePlatform(dstDataSource);

      Table[] dstTables = Utils.createTables(srcPlatform, dstPlatform);
      List<String> tableNames = new ArrayList<>();
      Stream.of(dstTables).forEach(table -> tableNames.add(table.getName()));

      DbExport dbExport = new DbExport(srcPlatform);
      String[] tNames = tableNames.toArray(new String[0]);

      Utils.exportToCsv(dbExport, tNames);//TO_CSV
      Utils.copyData(excludedTablesList, dstPlatform, tNames);//FROM_CSV

      Utils.createSequences(srcPlatform, dstPlatform);//SEQUENCES
      Utils.createViews(srcPlatform, dstPlatform);//VIEWS
      Utils.createUQ(srcPlatform, dstPlatform);//UQ
      Utils.createFK(srcPlatform, dstPlatform);//FK
      Utils.createCheck(srcPlatform, dstPlatform);//CHECK
      Utils.convertBlobs(dstPlatform, dstTables);//BLOBS

    } catch (Exception e) {
      logger.error("", e);
    }
  }
}

