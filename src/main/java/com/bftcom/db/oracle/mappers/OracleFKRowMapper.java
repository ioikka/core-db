package com.bftcom.db.oracle.mappers;

import com.bftcom.db.core.model.FK;
import org.jumpmind.db.sql.Row;

public class OracleFKRowMapper<T extends FK> extends OracleUQRowMapper<T> {
  public static String SELECT = "select C.CONSTRAINT_NAME," +
      "C.TABLE_NAME," +
      "(select listagg(cc.column_name, ',') within group (order by cc.constraint_name, cc.position) from user_cons_columns cc where C.CONSTRAINT_NAME=cc.constraint_name)as COLS," +
      "R.TABLE_NAME REF_TABLE_NAME," +
      "(select listagg(cc.column_name, ',') within group (order by cc.constraint_name, cc.position) from user_cons_columns cc where C.R_CONSTRAINT_NAME=cc.constraint_name)as REF_COLS " +
      "from USER_CONSTRAINTS C " +
      "left join user_constraints r on C.R_CONSTRAINT_NAME = R.CONSTRAINT_NAME " +
      "where C.CONSTRAINT_TYPE = 'R'";

  public enum COLS {
    CONSTRAINT_NAME, TABLE_NAME, COLS, REF_TABLE_NAME, REF_COLS
  }

  public OracleFKRowMapper(Class clazz) {
    super(clazz);
  }

  @Override
  public T mapRow(Row row) {
    T i = super.mapRow(row);
    i.setRefTableName(row.getString(COLS.REF_TABLE_NAME.name()));
    i.setCols(row.getString(COLS.COLS.name()).split(","));
    i.setRefCols(row.getString(COLS.REF_COLS.name()).split(","));
    return i;
  }
}
