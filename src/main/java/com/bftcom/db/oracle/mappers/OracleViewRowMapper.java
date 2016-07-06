package com.bftcom.db.oracle.mappers;

import com.bftcom.db.core.mappers.RowMapper;
import com.bftcom.db.core.model.View;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.sql.Row;

public class  OracleViewRowMapper<T extends View> extends RowMapper<T> {
  public static String TABLE = "USER_VIEWS";//table/view name in DB

  private Class<T> clazz;

  /**
   * VIEW_NAME VARCHAR2(30)	NOT NULL
   * TEXT_LENGTH	NUMBER	 	Length of the view text
   * TEXT	LONG	 	View text
   * TYPE_TEXT_LENGTH	NUMBER	 	Length of the type clause of the typed view
   * TYPE_TEXT	VARCHAR2(4000)	 	Type clause of the typed view
   * OID_TEXT_LENGTH	NUMBER	 	Length of the WITH OID clause of the typed view
   * OID_TEXT	VARCHAR2(4000)	 	WITH OID clause of the typed view
   * VIEW_TYPE_OWNER	VARCHAR2(30)	 	Owner of the type of the view if the view is a typed view
   * VIEW_TYPE	VARCHAR2(30)	 	Type of the view if the view is a typed view
   * SUPERVIEW_NAME	VARCHAR2(30)	 	Name of the superview
   */
  public enum COLS {
    VIEW_NAME, TEXT_LENGTH, TEXT, TYPE_TEXT_LENGTH, TYPE_TEXT, OID_TEXT_LENGTH, OID_TEXT, VIEW_TYPE_OWNER, VIEW_TYPE, SUPERVIEW_NAME;
  }

  public static String COL_NAMES = StringUtils.join(COLS.values(), ",");

  public OracleViewRowMapper(Class clazz) {
    super(clazz);
  }

  @Override
  public T mapRow(Row row) {
    T i = getInstance();
    i.setViewName(row.getString(COLS.VIEW_NAME.name()));
    i.setSql(row.getString(COLS.TEXT.name()));
    return i;
  }
}
