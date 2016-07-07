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
      
      //Move data
      if ("true".equals(System.getProperty("export.to.csv"))) {
        Utils.exportToCsv(dbExport, tNames);//TO_CSV
      }
      if ("true".equals(System.getProperty("import.from.csv"))) {
        Utils.copyData(excludedTablesList, dstPlatform, tNames);//FROM_CSV
      }

      //create constraints. there were not copied during tables creation phase
      if ("true".equals(System.getProperty("dst.create.sequences"))) {
        Utils.createSequences(srcPlatform, dstPlatform);//SEQUENCES
      }
      if ("true".equals(System.getProperty("dst.convert.views"))) {
        Utils.createViews(srcPlatform, dstPlatform);//VIEWS
      }
      if ("true".equals(System.getProperty("dst.create.uqs"))) {
        Utils.createUQ(srcPlatform, dstPlatform);//UQ
      }
      if ("true".equals(System.getProperty("dst.create.fks"))) {
        Utils.createFK(srcPlatform, dstPlatform);//FK
      }
      if ("true".equals(System.getProperty("dst.create.checks"))) {
        Utils.createCheck(srcPlatform, dstPlatform);//CHECK
      }
      
      //convert blobs/ should be executed once
      if ("true".equals(System.getProperty("dst.convert.blobs"))) {
        Utils.convertBlobs(dstPlatform, dstTables);//BLOBS
      }

    } catch (Exception e) {
      logger.error("", e);
    }
  }
}

