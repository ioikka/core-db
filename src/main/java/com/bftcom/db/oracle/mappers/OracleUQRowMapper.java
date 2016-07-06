package com.bftcom.db.oracle.mappers;

import com.bftcom.db.core.model.Constraint;
import com.bftcom.db.core.model.FK;
import com.bftcom.db.core.model.UQ;
import com.bftcom.db.oracle.model.OracleFK;
import org.jumpmind.db.sql.Row;

public class OracleUQRowMapper<T extends UQ> extends OracleConstraintRowMapper<T> {
  public static String SELECT = "SELECT c.constraint_name," +
      "c.table_name," +
      "(SELECT LISTAGG(cc.column_name, ',') WITHIN GROUP (ORDER BY cc.constraint_name, cc.position) FROM user_cons_columns cc WHERE c.constraint_name=cc.constraint_name) AS cols " +
      "FROM user_constraints c " +
      "LEFT JOIN user_constraints r ON c.r_constraint_name = r.constraint_name " +
      "WHERE c.constraint_type = 'U'";

  public enum COLS {
    CONSTRAINT_NAME, TABLE_NAME, COLS
  }

  public OracleUQRowMapper(Class clazz) {
    super(clazz);
  }

  @Override
  public T mapRow(Row row) {
    T i = getInstance();
    i.setTableName(row.getString(COLS.TABLE_NAME.name()));
    i.setName(row.getString(COLS.CONSTRAINT_NAME.name()));
    i.setCols(row.getString(COLS.COLS.name()).split(","));
    i.setType(Constraint.Type.U);
    return i;
  }
}
