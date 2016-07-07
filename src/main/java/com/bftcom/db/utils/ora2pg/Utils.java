package com.bftcom.db.utils.ora2pg;

import com.bftcom.db.core.SqlStatements;
import com.bftcom.db.core.model.Check;
import com.bftcom.db.core.model.Constraint;
import com.bftcom.db.core.model.FK;
import com.bftcom.db.core.model.UQ;
import com.bftcom.db.core.model.View;
import com.bftcom.db.oracle.mappers.OracleCheckRowMapper;
import com.bftcom.db.oracle.mappers.OracleFKRowMapper;
import com.bftcom.db.oracle.mappers.OracleSequenceRowMapper;
import com.bftcom.db.oracle.OracleSqlStatements;
import com.bftcom.db.oracle.mappers.OracleUQRowMapper;
import com.bftcom.db.oracle.model.OracleFK;
import com.bftcom.db.oracle.model.OracleView;
import com.bftcom.db.oracle.mappers.OracleViewRowMapper;
import com.bftcom.db.postgresql.PSQLState;
import com.bftcom.db.postgresql.PostgresqlSqlStatements;
import com.bftcom.db.postgresql.model.PostgresqlView;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Database;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.IDdlBuilder;
import org.jumpmind.db.platform.oracle.OracleDatabasePlatform;
import org.jumpmind.db.platform.oracle.OracleDdlReader;
import org.jumpmind.db.platform.postgresql.PostgreSqlDatabasePlatform;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.SqlException;
import org.jumpmind.symmetric.io.data.DbExport;
import org.jumpmind.symmetric.model.Sequence;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * <p>
 * date: 29.06.2016
 */
public class Utils {
  private static final Logger logger = LoggerFactory.getLogger(Utils.class);
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String URL = "url";

  private Utils() {
  }

  public static Properties getSrcProperties() {
    Properties srcProperties = new Properties();
    srcProperties.setProperty(USERNAME, System.getProperty(ConfigConstants.SRC_DB_USER));
    srcProperties.setProperty(PASSWORD, System.getProperty(ConfigConstants.SRC_DB_PASSWORD));
    srcProperties.setProperty(URL, System.getProperty(ConfigConstants.SRC_DB_URL));
    return srcProperties;
  }

  public static Properties getDstProperties() {
    Properties dstProperties = new Properties();
    dstProperties.setProperty(USERNAME, System.getProperty(ConfigConstants.DST_DB_USER));
    dstProperties.setProperty(PASSWORD, System.getProperty(ConfigConstants.DST_DB_PASSWORD));
    dstProperties.setProperty(URL, System.getProperty(ConfigConstants.DST_DB_URL));
    return dstProperties;
  }

  public static Table[] createTables(IDatabasePlatform srcPlatform, IDatabasePlatform dstPlatform) throws CloneNotSupportedException {
    OracleDdlReader srcDdlReader = new OracleDdlReader(srcPlatform);

    logger.info("A long operation is ahead of us. Stay patient.");
    long startTimeInMs = System.currentTimeMillis();

    IDdlBuilder srcDdlBuilder = srcPlatform.getDdlBuilder();
    srcDdlBuilder.setCaseSensitive(false);

    Table[] srcTables = new Table[0];

    Database database = srcDdlReader.readTables(null, System.getProperty(ConfigConstants.SRC_DB_USER), new String[]{"TABLE"});
    srcTables = database.getTables();
    logger.info(String.format("Read tables in %d ms", System.currentTimeMillis() - startTimeInMs));

    Table[] dstTables = new Table[srcTables.length];

    int cnt = 0;
    for (Table table : srcTables) {
      logger.debug("Cloning table " + table.getName() + " without indices and FKs");
      Table clone = (Table) table.clone();

      clone.removeAllIndices();
      clone.removeAllForeignKeys();
      clone.setSchema(null);
      for (Column column : clone.getColumns()) {
        column.setDescription(null);
      }
      dstTables[cnt++] = clone;
    }
    dstPlatform.createTables(false, true, dstTables);
    String[] tableNames = new String[srcTables.length];

    cnt = 0;
    for (Table srcTable : srcTables) {
      tableNames[cnt++] = srcTable.getName();
    }

    return dstTables;
  }

  public static void exportToCsv(DbExport dbExport, String... tableNames) {
    dbExport.setFormat(DbExport.Format.CSV);
    dbExport.setNoCreateInfo(true);
    dbExport.setUseQuotedIdentifiers(false);
    dbExport.setUseVariableForDates(false);
    dbExport.setDir(System.getProperty(ConfigConstants.CSV_DIR));

    for (String tableName : tableNames) {
      try {
        long startTime = System.currentTimeMillis();
        dbExport.exportTables(new String[]{tableName});
        logger.info(String.format("Table %s exported to %s%s.csv file in %d ms", tableName, dbExport.getDir(), tableName, System.currentTimeMillis() - startTime));
      } catch (IOException e) {
        logger.error(StringUtils.EMPTY, e);
      }
    }
  }

  public static void createViews(IDatabasePlatform srcPlatform, IDatabasePlatform dstPlatform) {
    try {
      List<? extends View> views = null;

      //Read data
      DataSource srcDataSource;
      srcDataSource = BasicDataSourceFactory.createDataSource(Utils.getSrcProperties());
      ISqlTemplate sqlTemplate = srcPlatform.getSqlTemplate();
      String selectSql;

      if (srcPlatform instanceof OracleDatabasePlatform) {
        selectSql = SqlStatements.buildSelectStatement(OracleViewRowMapper.TABLE, OracleViewRowMapper.COL_NAMES.split(","));
        views = sqlTemplate.query(selectSql, new OracleViewRowMapper<OracleView>(OracleView.class));
        for (View view : views) {
          try {
            OracleView.fillDdl(srcDataSource, view);//using static call to fill ddl
          } catch (SQLException e1) {
            logger.error("", e1);
          }
        }
      }

      //Write data
      if (dstPlatform instanceof PostgreSqlDatabasePlatform) {
        List<PostgresqlView> postgresqlViewList = new ArrayList<>();
        if (views != null) {
          for (View view : views) {
            postgresqlViewList.add(new PostgresqlView(view));
          }
        }
        for (PostgresqlView postgresqlView : postgresqlViewList) {
          String createSql = postgresqlView.getCreateSql();
          if (logger.isTraceEnabled()) {
            logger.trace("Creating view with sql: " + createSql);
          }
          dstPlatform.getSqlTemplate().update(createSql);
        }
      }
    } catch (Exception e) {
      logger.error("", e);
    }
  }

  public static void createCheck(IDatabasePlatform srcPlatform, IDatabasePlatform dstPlatform) {
    long start = System.currentTimeMillis();
    try {
      List<? extends Check> constraintList = null;

      ISqlTemplate sqlTemplate = srcPlatform.getSqlTemplate();


      //Reading source
      if (srcPlatform instanceof OracleDatabasePlatform) {
        constraintList = sqlTemplate.query(OracleCheckRowMapper.SELECT, new OracleCheckRowMapper<>(Check.class));
      }

      //Writing to destination
      if (dstPlatform instanceof PostgreSqlDatabasePlatform) {
        if (constraintList != null) {
          for (Check c : constraintList) {
            createConstraint(dstPlatform, c);
          }
        }
      }
    } catch (Exception e) {
      logger.error("", e);
    } finally {
      logger.info("created Checks in " + (System.currentTimeMillis() - start) + " ms");
    }
  }

  public static void createFK(IDatabasePlatform srcPlatform, IDatabasePlatform dstPlatform) {
    long start = System.currentTimeMillis();
    try {
      List<? extends FK> constraintList = null;
      ISqlTemplate sqlTemplate = srcPlatform.getSqlTemplate();


      //Reading source
      if (srcPlatform instanceof OracleDatabasePlatform) {
        constraintList = sqlTemplate.query(OracleFKRowMapper.SELECT, new OracleFKRowMapper<OracleFK>(OracleFK.class));
      }

      //Writing to destination
      if (dstPlatform instanceof PostgreSqlDatabasePlatform) {
        if (constraintList != null) {
          for (FK c : constraintList) {
            createConstraint(dstPlatform, c);
          }
        }
      }
    } catch (Exception e) {
      logger.error("", e);
    } finally {
      logger.info("created FKs in " + (System.currentTimeMillis() - start) + " ms");
    }
  }

  public static void createUQ(IDatabasePlatform srcPlatform, IDatabasePlatform dstPlatform) {
    long start = System.currentTimeMillis();
    try {
      List<? extends UQ> constraintList = null;

      ISqlTemplate sqlTemplate = srcPlatform.getSqlTemplate();


      //Reading source
      if (srcPlatform instanceof OracleDatabasePlatform) {
        constraintList = sqlTemplate.query(OracleUQRowMapper.SELECT, new OracleUQRowMapper<>(UQ.class));
      }

      //Writing to destination
      if (dstPlatform instanceof PostgreSqlDatabasePlatform) {
        if (constraintList != null) {
          for (UQ c : constraintList) {
            createConstraint(dstPlatform, c);
          }
        }
      }
    } catch (Exception e) {
      logger.error("", e);
    } finally {

      logger.info("created UQs in " + (System.currentTimeMillis() - start) + " ms");
    }
  }

  private static void createConstraint(IDatabasePlatform dstPlatform, Constraint c) {
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

  public static void createSequences(IDatabasePlatform srcPlatform, IDatabasePlatform dstPlatform) {
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

  public static void copyData(List<String> excludedTablesList, IDatabasePlatform dstPlatform, String[] tableNames) {
    for (String tableName : tableNames) {
      if (excludedTablesList.contains(tableName)) {
        continue;
      }
      long start = System.currentTimeMillis();
      dstPlatform.getSqlTemplate().update("TRUNCATE TABLE " + tableName);
      String absoluteFileName = System.getProperty(ConfigConstants.CSV_DIR) + tableName + ".csv";

      String sql = String.format("COPY %s FROM '%s' WITH DELIMITER ',' CSV HEADER ESCAPE '\\' ENCODING 'WIN1251'", tableName, absoluteFileName);
      System.out.println(sql);
      dstPlatform.getSqlTemplate().update(sql);
      logger.info(String.format("Table %s imported in %d ms", tableName, System.currentTimeMillis() - start));
    }
  }

  public static void convertBlobs(IDatabasePlatform dstPlatform, Table[] dstTables) {
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
