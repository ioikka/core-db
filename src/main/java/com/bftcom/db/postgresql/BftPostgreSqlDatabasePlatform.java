package com.bftcom.db.postgresql;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.postgresql.PostgreSqlDatabasePlatform;
import org.jumpmind.db.platform.postgresql.PostgreSqlDdlBuilder;
import org.jumpmind.db.platform.postgresql.PostgreSqlJdbcSqlTemplate;
import org.jumpmind.db.platform.postgresql.PostgresLobHandler;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class BftPostgreSqlDatabasePlatform extends PostgreSqlDatabasePlatform {
  private static Logger logger = LoggerFactory.getLogger(BftPostgreSqlDatabasePlatform.class);

  private static PostgresLobHandler postgresLobHandler = new PostgresLobHandler();
  private static SqlTemplateSettings sqlTemplateSettings = new SqlTemplateSettings();

  private PostgreSqlJdbcSqlTemplate postgreSqlJdbcSqlTemplate = new PostgreSqlJdbcSqlTemplate(dataSource, sqlTemplateSettings, postgresLobHandler, getDatabaseInfo()) {
    @Override
    public boolean supportsGetGeneratedKeys() {
      return false;
    }

    @Override
    public int update(String sql, Object... values) {
      logger.info("Excuting SQL: " + sql);
      return super.update(sql, values, null);
    }
  };

  public BftPostgreSqlDatabasePlatform(DataSource dataSource) {
    super(dataSource, sqlTemplateSettings);
  }

  @Override
  public ISqlTemplate getSqlTemplate() {
    return postgreSqlJdbcSqlTemplate;
  }

  @Override
  protected PostgreSqlDdlBuilder createDdlBuilder() {
    PostgreSqlDdlBuilder postgreSqlDdlBuilder = new PostgreSqlDdlBuilder() {
      private String[] quotedIdentifiers = {"LIMIT", "AS", "modify"};

      @Override
      protected void writePrimaryKeyStmt(Table table, Column[] primaryKeyColumns, StringBuilder ddl) {
        ddl.append("CONSTRAINT " + " PK_").append(table.getName()).append(" ");
        super.writePrimaryKeyStmt(table, primaryKeyColumns, ddl);
      }

      @Override
      protected String getDelimitedIdentifier(String identifier) {
        for (String quotedIdentifier : quotedIdentifiers) {
          if (quotedIdentifier.equalsIgnoreCase(identifier)) {
            return "\"" + identifier.toUpperCase() + "\"";
          }
        }
        return super.getDelimitedIdentifier(identifier);
      }

      @Override
      public boolean isDelimitedIdentifierModeOn() {
        return false;
      }

      @Override
      protected String getNativeDefaultValue(Column column) {
        String nativeDefaultValue = super.getNativeDefaultValue(column);
        return nativeDefaultValue.equals("sysdate") ? "CURRENT_DATE" : nativeDefaultValue;
      }
    };
    postgreSqlDdlBuilder.setDelimitedIdentifierModeOn(false);
    return postgreSqlDdlBuilder;
  }

}
