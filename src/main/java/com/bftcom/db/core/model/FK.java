package com.bftcom.db.core.model;

import org.apache.commons.lang.StringUtils;

/**
 * @author ikka
 * @date: 05.07.2016.
 */
public class FK extends UQ {
  //ALTER TABLE tableName ADD CONSTRAINT name FOREIGN KEY (tableCols[]) references refConstraintTableName (refConstraintTableCols[])
  private String refConstraintName;
  private String refCols[];
  private String refTableName;

  public String getRefConstraintName() {
    return refConstraintName;
  }

  public void setRefConstraintName(String refConstraintName) {
    this.refConstraintName = refConstraintName;
  }

  public String[] getRefCols() {
    return refCols;
  }

  public void setRefCols(String[] refCols) {
    this.refCols = refCols;
  }

  public String getRefTableName() {
    return refTableName;
  }

  public void setRefTableName(String refTableName) {
    this.refTableName = refTableName;
  }

  @Override
  public String getAddConstraintSql() {
    String cols = StringUtils.join(getCols(), ",");
    String refCols = StringUtils.join(getRefCols(), ",");
    return String.format("ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)", getTableName(), getName(), cols, getRefTableName(), refCols);
  }
}
