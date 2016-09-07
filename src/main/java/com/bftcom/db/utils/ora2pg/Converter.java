package com.bftcom.db.utils.ora2pg;

import com.bftcom.db.core.SqlStatements;
import com.bftcom.db.core.model.*;
import com.bftcom.db.oracle.OracleSqlStatements;
import com.bftcom.db.oracle.mappers.*;
import com.bftcom.db.oracle.model.OracleFK;
import com.bftcom.db.oracle.model.OracleView;
import com.bftcom.db.postgresql.BftPostgreSqlDatabasePlatform;
import com.bftcom.db.postgresql.PSQLState;
import com.bftcom.db.postgresql.PostgresqlSqlStatements;
import com.bftcom.db.postgresql.model.PostgresqlView;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Database;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.oracle.OracleDatabasePlatform;
import org.jumpmind.db.platform.oracle.OracleDdlReader;
import org.jumpmind.db.platform.postgresql.PostgreSqlDatabasePlatform;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.SqlException;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.symmetric.io.data.DbExport;
import org.jumpmind.symmetric.model.Sequence;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.*;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ikka
 * @date: 08.07.2016.
 */
public class Converter {
  public static final String TRUE = "true";
  public static final String SRC_TABLES = "SRC_TABLES";

  private static Logger logger = LoggerFactory.getLogger(Converter.class);

  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String URL = "url";

  private Properties props;
  private String[] excludedTables;
  private String[] includedTables;
  private IDatabasePlatform srcPlatform;
  private IDatabasePlatform dstPlatform;
  private Properties srcProperties;
  private Properties dstProperties;

  public Converter(Properties props) {
    this.props = props;

    try {
      config();
    } catch (Exception e) {
      logger.error("", e);
    }
  }

  public void convert() {
    Table[] dstTables = new Table[0];
    try {
      dstTables = createTablesInDstDatabase();

      List<String> tableNames = Stream.of(dstTables).map(Table::getName).collect(Collectors.toList());

      DbExport dbExport = new DbExport(srcPlatform);
      String[] tNames = tableNames.toArray(new String[0]);

      //Move data
      exportToCsv(dbExport, tNames);
      importFromCsv(Arrays.asList(excludedTables), Arrays.asList(includedTables), dstPlatform, tNames);

      //create constraints. they are not copied during tables creation phase
      createSequences();
      createViews();
      createUQ();
      createFK();
      createCheck();

      convertBlobs(dstPlatform, dstTables);//should be executed once todo add check if a conversion operation makes sense
    } catch (CloneNotSupportedException e) {
      logger.error("", e);
    }
  }

  private void config() throws Exception {
    excludedTables = props.getProperty(Configuration.EXCLUDED_TABLES).trim().split(",");
    includedTables = props.getProperty(Configuration.INCLUDED_TABLES).split(",");


    srcProperties = dbPropertiesBuilder(props, Configuration.SRC_DB_USER, Configuration.SRC_DB_PASSWORD, Configuration.SRC_DB_URL);
    dstProperties = dbPropertiesBuilder(props, Configuration.DST_DB_USER, Configuration.DST_DB_PASSWORD, Configuration.DST_DB_URL);

    srcPlatform = new OracleDatabasePlatform(BasicDataSourceFactory.createDataSource(srcProperties), new SqlTemplateSettings());
//    srcPlatform = new FirebirdDatabasePlatform(BasicDataSourceFactory.createDataSource(srcProperties), new SqlTemplateSettings());
    dstPlatform = new BftPostgreSqlDatabasePlatform(BasicDataSourceFactory.createDataSource(dstProperties));
  }

  public Properties getSrcProperties() {
    return srcProperties;
  }


  public Properties getDstProperties() {
    return dstProperties;
  }


  public Table[] createTablesInDstDatabase() throws CloneNotSupportedException {
    Table[] srcTables = deserializeSrcMetadata();

    if (srcTables.length == 0) {
      srcTables = readTables();
    }

    Table[] dstTables = new Table[srcTables.length];

    int cnt = 0;
    for (Table table : srcTables) {
      logger.debug("Cloning table " + table.getName() + " without indices and FKs");
      Table dstTable = (Table) table.clone();

      dstTable.removeAllIndices();

      dstTable.removeAllForeignKeys();
      dstTable.setSchema(null);

      Arrays.stream(dstTable.getColumns()).forEach(col -> col.setDescription(null));
      dstTables[cnt++] = dstTable;
    }

    serializeSrcMetadata(srcTables);

    if (TRUE.equals(props.getProperty(Configuration.DST_CREATE_TABLES))) {
      dstPlatform.createTables(false, true, dstTables);//long operation ahead
    }
    return dstTables;
  }

  private void serializeSrcMetadata(Table[] srcTables) {
    if (TRUE.equals(props.get(Configuration.SRC_SERIALIAZE_METADATA))) {
      serializeMetadata(srcTables, SRC_TABLES);
    }
  }

  private void serializeMetadata(Object o, String fileName) {
    File outputFile = new File(fileName);
    try (FileOutputStream os = new FileOutputStream(outputFile); ObjectOutputStream in = new ObjectOutputStream(os)) {
      logger.info("serialiazing metadata from file");
      long start = System.currentTimeMillis();
      in.writeObject(o);
      in.flush();
      logger.info("Metadata serialized in " + (System.currentTimeMillis() - start) + " ms" + "( see file " + outputFile.getAbsolutePath() + " )");
    } catch (IOException e) {
      logger.warn("", e);
    }

  }

  private Table[] deserializeMetadata(String filename) {
    Table[] srcTables = new Table[0];
    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(filename)))) {

      logger.info("restoring metadata from file");
      long start = System.currentTimeMillis();
      srcTables = (Table[]) in.readObject();
      logger.info("Metadata restored in " + (System.currentTimeMillis() - start) + " ms");

    } catch (IOException | ClassNotFoundException e) {
      logger.warn("", e);
    }
    return srcTables;
  }

  private Table[] deserializeSrcMetadata() {
    if (TRUE.equals(props.get(Configuration.SRC_SERIALIAZE_METADATA))) {
      return deserializeMetadata(SRC_TABLES);
    }
    return new Table[0];
  }

  private Table[] readTables() {
    logger.info("A long operation is ahead of us. Stay patient.");
    OracleDdlReader srcDdlReader = new OracleDdlReader(srcPlatform);
//    FirebirdDdlReader srcDdlReader = new FirebirdDdlReader(srcPlatform);

    long startTimeInMs = System.currentTimeMillis();
    Database database = srcDdlReader.readTables(null, System.getProperty(Configuration.SRC_DB_USER), new String[]{"TABLE"});
    logger.info(String.format("Read tables in %d ms", System.currentTimeMillis() - startTimeInMs));

    return database.getTables();
  }

  private void exportToCsv(DbExport dbExport, String... tableNames) {
    if (TRUE.equals(props.getProperty(Configuration.EXPORT_TO_CSV))) {
      dbExport.setFormat(DbExport.Format.CSV);
      dbExport.setNoCreateInfo(true);
      dbExport.setUseQuotedIdentifiers(false);
      dbExport.setUseVariableForDates(false);
      dbExport.setDir(props.getProperty(Configuration.CSV_DIR));

      for (String tableName : tableNames) {
        List<String> includedTableList = Arrays.asList(this.includedTables);
        try {
          if (!includedTableList.isEmpty() && !includedTableList.contains(tableName)) {
            continue;
          }
          long startTime = System.currentTimeMillis();
          dbExport.exportTables(new String[]{tableName});
          logger.info(String.format("Table %s exported to %s%s.csv file in %d ms", tableName, dbExport.getDir(), tableName, System.currentTimeMillis() - startTime));
        } catch (IOException e) {
          logger.error("", e);
        }
      }
    }
  }

  public void createViews() {
    if (TRUE.equals(props.getProperty(Configuration.DST_CONVERT_VIEWS))) {
      try {
        List<? extends View> views = null;

        //Read data
        DataSource srcDataSource;
        srcDataSource = BasicDataSourceFactory.createDataSource(getSrcProperties());
        ISqlTemplate sqlTemplate = srcPlatform.getSqlTemplate();
        String selectSql;

        if (srcPlatform instanceof OracleDatabasePlatform) {
          selectSql = SqlStatements.buildSelectStatement(OracleViewRowMapper.TABLE, OracleViewRowMapper.COL_NAMES.split(","));
          views = sqlTemplate.query(selectSql, new OracleViewRowMapper<OracleView>(OracleView.class));
          for (View view : views) {
            try {
              OracleView.fillDdl(srcDataSource, view);//using static call to fill ddl //todo revise
            } catch (SQLException e) {
              logger.error("", e);
            }
          }
        }

        //Write data
        List<View> viewList = new ArrayList<>();
        if (dstPlatform instanceof PostgreSqlDatabasePlatform) {
          if (views != null) {
            viewList = views.stream().map(PostgresqlView::new).collect(Collectors.toList());
          }
        }
        List<String> createSqlList = viewList.stream().map(View::getCreateSql).collect(Collectors.toList());
        List<String> failedSqlList = createViews(dstPlatform, createSqlList, new ArrayList<>());

        if (failedSqlList.size() > 0) {
          logger.warn("Not all views have been created. Consult log files for details.");
          for (String failedCreateSql : failedSqlList) {
            logger.error("Failed to execute " + failedCreateSql);
          }
        }
      } catch (Exception e) {
        logger.error("", e);
      }
    }
  }

  private List<String> createViews(IDatabasePlatform dstPlatform, List<String> createSqlList, List<String> failedCreateSqlList) {
    boolean atLeastOneExecutedSuccessfully = false;
    for (String createSql : createSqlList) {
      if (logger.isTraceEnabled()) {
        logger.trace("Creating view with sql: " + createSql);
      }
      try {
        dstPlatform.getSqlTemplate().update(createSql);
        atLeastOneExecutedSuccessfully = true;
      } catch (Exception e) {
        failedCreateSqlList.add(createSql);
      }
    }
    if (atLeastOneExecutedSuccessfully && failedCreateSqlList.size() > 0) {
      createViews(dstPlatform, failedCreateSqlList, new ArrayList<>());
    }
    return failedCreateSqlList;
  }

  public void createCheck() {
    if (TRUE.equals(props.getProperty(Configuration.DST_CREATE_CHECKS))) {
      long start = System.currentTimeMillis();
      try {
        List<? extends Constraint> constraintList = null;

        // /Reading source
        if (srcPlatform instanceof OracleDatabasePlatform) {
          constraintList = srcPlatform.getSqlTemplate().query(OracleCheckRowMapper.SELECT, new OracleCheckRowMapper<>(Check.class));
        }

        //Writing to destination
        if (constraintList != null) {
          for (Constraint c : constraintList) {
            createConstraint(c);
          }
        }
      } catch (Exception e) {
        logger.error("", e);
      } finally {
        logger.info("created Checks in " + (System.currentTimeMillis() - start) + " ms");
      }
    }
  }

  public void createFK() {
    if (TRUE.equals(props.getProperty(Configuration.DST_CREATE_FKS))) {
      long start = System.currentTimeMillis();
      try {
        List<? extends FK> constraintList = null;
        ISqlTemplate sqlTemplate = srcPlatform.getSqlTemplate();

        //Reading source
        if (srcPlatform instanceof OracleDatabasePlatform) {
          constraintList = sqlTemplate.query(OracleFKRowMapper.SELECT, new OracleFKRowMapper<OracleFK>(OracleFK.class));
        }

        //Writing to destination
        if (constraintList != null) {
          for (FK c : constraintList) {
            createConstraint(c);
          }
        }
      } catch (Exception e) {
        logger.error("", e);
      } finally {
        logger.info("created FKs in " + (System.currentTimeMillis() - start) + " ms");
      }
    }
  }

  public void createUQ() {
    if (TRUE.equals(props.getProperty(Configuration.DST_CREATE_UQS))) {
      long start = System.currentTimeMillis();
      try {
        List<? extends Constraint> constraintList = null;

        //Reading source
        ISqlTemplate sqlTemplate = srcPlatform.getSqlTemplate();
        if (srcPlatform instanceof OracleDatabasePlatform) {
          constraintList = sqlTemplate.query(OracleUQRowMapper.SELECT, new OracleUQRowMapper<>(UQ.class));
        }

        //Writing to destination
        if (constraintList != null) {
          for (Constraint c : constraintList) {
            createConstraint(c);
          }
        }
      } catch (Exception e) {
        logger.error("", e);
      } finally {
        logger.info("created UQs in " + (System.currentTimeMillis() - start) + " ms");
      }
    }
  }

  private void createConstraint(Constraint c) {
    ISqlTemplate dstSqlTemplate = dstPlatform.getSqlTemplate();
    String addConstraintSql = c.getAddConstraintSql();
    try {
      dstSqlTemplate.update(addConstraintSql);
    } catch (Exception e) {
      if (e.getCause() instanceof PSQLException) {
        String sqlState = ((PSQLException) e.getCause()).getSQLState();
        if (PSQLState.DUPLICATE_OBJECT.getState().equals(sqlState) || PSQLState.DUPLICATE_TABLE.getState().equals(sqlState)) {
          logger.warn("Duplicate constraint. Let's drop it first and then try to create it.");
          dstSqlTemplate.update(c.getDropConstraintSql());
          dstSqlTemplate.update(addConstraintSql);
        }
      } else {
        logger.error("", e);
      }
    }
  }

  public void createSequences() {
    if (TRUE.equals(props.getProperty(Configuration.DST_CREATE_SEQUENCES))) {
      SqlStatements srcSqlStatements = null;
      SqlStatements dstSqlStatements = null;

      if (srcPlatform instanceof OracleDatabasePlatform) {
        srcSqlStatements = new OracleSqlStatements();
      } else if (srcPlatform instanceof PostgreSqlDatabasePlatform) {
        srcSqlStatements = new PostgresqlSqlStatements();
      }

      if (dstPlatform instanceof OracleDatabasePlatform) {
        srcSqlStatements = new OracleSqlStatements();
      } else if (dstPlatform instanceof PostgreSqlDatabasePlatform) {
        dstSqlStatements = new PostgresqlSqlStatements();
      }

      if (srcSqlStatements == null || dstSqlStatements == null) {
        logger.warn("Destination or Source type is unknown");
        return;
      }

      List<Sequence> seqs = srcPlatform.getSqlTemplate().query(srcSqlStatements.getAllSequences(), new OracleSequenceRowMapper());

      for (Sequence seq : seqs) {
        String createSeqSql = dstSqlStatements.createSequence(seq, true);
        String dropSeqSql = dstSqlStatements.dropSequence(seq);
        try {
          dstPlatform.getSqlTemplate().update(dropSeqSql);
        } catch (SqlException e) {
          logger.warn("Sequence " + seq.getSequenceName() + " could not be dropped.", e);
        }
        try {
          dstPlatform.getSqlTemplate().update(createSeqSql);
        } catch (SqlException e) {
          logger.error("Sequence " + seq.getSequenceName() + " could not be created.", e);
        }
      }
    }
  }

  public void importFromCsv(List<String> excludedTableList, List<String> includedTableList, IDatabasePlatform dstPlatform, String[] tableNames) {
    if (TRUE.equals(props.getProperty(Configuration.IMPORT_FROM_CSV))) {
      String includedTables = props.getProperty(Configuration.INCLUDED_TABLES);

      logger.info("Temporary disabling all contraints");
      for (String tableName : tableNames) {
        if (excludedTableList.contains(tableName)) {
          continue;
        }
        dstPlatform.getSqlTemplate().update("ALTER TABLE " + tableName + " DISABLE TRIGGER ALL");
      }


      for (String tableName : tableNames) {
        if (excludedTableList.contains(tableName)) {
          continue;
        }


        String deleteTableSql = "DELETE  FROM " + tableName;
        if (!includedTableList.isEmpty()) {
          if (includedTableList.contains(tableName)) {
            dstPlatform.getSqlTemplate().update(deleteTableSql);//only included tables are truncated
          }
        } else {
          dstPlatform.getSqlTemplate().update(deleteTableSql);//all tables are truncated
        }
      }

      for (String tableName : tableNames) {
        if (excludedTableList.contains(tableName)) {
          continue;
        }

        if (!includedTableList.isEmpty() && !includedTableList.contains(tableName)) {
          continue;
        }
        long start = System.currentTimeMillis();

        String header = null;
        String absoluteFileName = props.getProperty(Configuration.CSV_DIR) + tableName + ".csv";
        try (BufferedReader brTest = new BufferedReader(new FileReader(new File(absoluteFileName)))) {
          header = brTest.readLine();
        } catch (IOException e) {
          logger.error("", e);
        }

        String cols = "";
        if (header != null && header.length() > 0) {
          cols = header.replaceAll("\"", "");
          String[] headerCols = cols.split(",");
          String[] quotedCols = props.getProperty(Configuration.DST_DB_QUOTED_COLS).split(",");
          for (int i = 0; i < headerCols.length; i++) {
            boolean shouldQuote = false;
            for (String quotedCol : quotedCols) {
              if (headerCols[i].equalsIgnoreCase(quotedCol)) {
                shouldQuote = true;
                break;
              }
            }

            if (shouldQuote) {
              headerCols[i] = "\"" + headerCols[i] + "\"";
            }
          }
          cols = StringUtils.join(headerCols, ",");
        } else {
          logger.warn("Skipping data import for table " + tableName);
          continue;
        }

        String sql = String.format("COPY %s (%s) FROM '%s' WITH DELIMITER ',' CSV HEADER ESCAPE '\\' ENCODING 'WIN1251'", tableName, cols, absoluteFileName);
        dstPlatform.getSqlTemplate().update(sql);
        logger.info(String.format("Table %s imported in %d ms", tableName, System.currentTimeMillis() - start));
      }

      logger.info("Reeanbling all contraints");
      for (String tableName : tableNames) {
        if (excludedTableList.contains(tableName)) {
          continue;
        }
        dstPlatform.getSqlTemplate().update("ALTER TABLE " + tableName + " ENABLE TRIGGER ALL");
      }
    }
  }

  public void convertBlobs(IDatabasePlatform dstPlatform, Table[] dstTables) {
    if (TRUE.equals(props.getProperty(Configuration.DST_CONVERT_BLOBS))) {
      for (Table dstTable : dstTables) {
        for (Column col : dstTable.getColumns()) {
          if (col.getJdbcTypeCode() == Types.BLOB) {
            logger.info(dstTable + "." + col.getName());
            //tableName aliasOfTableName columnName columnName tableName 2ndAlias aliasOfTableName 2ndAlias
            String template = "update %s %s set %s = (select decode(convert_from(%s, 'UTF8'), 'hex') from %s %s where %s.id = %s.id)";

            String tName = dstTable.getName();
            String tAlias = "t";
            String cName = col.getName();
            String tAlias2 = "tt";

            String sqlRequest = String.format(template, tName, tAlias, cName, cName, tName, tAlias2, tAlias, tAlias2);
            dstPlatform.getSqlTemplate().update(sqlRequest);
          }
        }
      }
    }
  }

  private static Properties dbPropertiesBuilder(Properties props, String userKey, String pwdKey, String urlKey) {
    Properties result = new Properties();
    result.setProperty(USERNAME, props.getProperty(userKey));
    result.setProperty(PASSWORD, props.getProperty(pwdKey));
    result.setProperty(URL, props.getProperty(urlKey));
    return result;
  }
}
