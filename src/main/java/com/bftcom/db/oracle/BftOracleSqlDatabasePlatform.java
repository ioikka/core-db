package com.bftcom.db.oracle;

import org.jumpmind.db.platform.oracle.OracleDatabasePlatform;
import org.jumpmind.db.platform.oracle.OracleDdlBuilder;
import org.jumpmind.db.sql.SqlTemplateSettings;

import javax.sql.DataSource;

/**
 * @author ikka
 * @date: 08.07.2016.
 */
public class BftOracleSqlDatabasePlatform extends OracleDatabasePlatform {
  @Override
  protected OracleDdlBuilder createDdlBuilder() {
    OracleDdlBuilder ddlBuilder = super.createDdlBuilder();
    ddlBuilder.setCaseSensitive(false);
    return ddlBuilder;
  }

  public BftOracleSqlDatabasePlatform(DataSource dataSource) {
    super(dataSource, new SqlTemplateSettings());


  }
}
