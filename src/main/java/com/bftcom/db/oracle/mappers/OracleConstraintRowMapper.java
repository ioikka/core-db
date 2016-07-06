package com.bftcom.db.oracle.mappers;

import com.bftcom.db.core.mappers.RowMapper;
import com.bftcom.db.core.model.Constraint;
import com.bftcom.db.core.model.View;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.Row;

public class OracleConstraintRowMapper<T extends Constraint> extends RowMapper<T> {
  public static String TABLE = "USER_CONSTRAINTS";//table/view name in DB

  public enum COLS {
    VIEW_RELATED,
    INVALID,
    INDEX_NAME,
    INDEX_OWNER,
    LAST_CHANGE,
    RELY,
    BAD,
    GENERATED,
    VALIDATED,
    DEFERRED,
    DEFERRABLE,
    STATUS,
    DELETE_RULE,
    R_CONSTRAINT_NAME,
    R_OWNER,
    SEARCH_CONDITION,
    TABLE_NAME,
    CONSTRAINT_TYPE,
    CONSTRAINT_NAME,
    OWNER,
  }

  public static String COL_NAMES = StringUtils.join(COLS.values(), ",");

  public OracleConstraintRowMapper(Class clazz) {
    super(clazz);
  }



  @Override
  public T mapRow(Row row) {
    T i = super.getInstance();
    i.setName(row.getString(COLS.CONSTRAINT_NAME.name()));
    i.setTableName(row.getString(COLS.TABLE_NAME.name()));
    i.setType(Constraint.Type.valueOf(row.getString(COLS.CONSTRAINT_TYPE.name())));
    return i;
  }
}
