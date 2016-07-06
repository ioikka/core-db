package com.bftcom.db.core.model;

import org.apache.commons.lang.StringUtils;

/**
 * @author ikka
 * @date: 06.07.2016.
 */
public class UQ extends Constraint {
  private String cols[];

  public String[] getCols() {
    return cols;
  }

  public void setCols(String[] cols) {
    this.cols = cols;
  }

  @Override
  public String getAddConstraintSql() {
    String cols = StringUtils.join(getCols(), ",");
    return String.format("ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (%s)", getTableName(), getName(), cols);
  }
}
