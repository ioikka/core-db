package com.bftcom.db.oracle.mappers;

import com.bftcom.db.core.model.Check;
import com.bftcom.db.core.model.Constraint;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.Row;

public class OracleCheckRowMapper<T extends Check> extends OracleConstraintRowMapper<T> {
  public static String SELECT = "SELECT constraint_name, table_name, search_condition, constraint_type FROM user_constraints WHERE constraint_type = 'C'";

  public enum COLS {
    CONSTRAINT_NAME, TABLE_NAME, SEARCH_CONDITION, CONSTRAINT_TYPE
  }


  public OracleCheckRowMapper(Class clazz) {
    super(clazz);
  }

  @Override
  public T mapRow(Row row) {
    T i = super.mapRow(row);
    i.setSearchCondition(row.getString(COLS.SEARCH_CONDITION.name()));
    return i;
  }
}
