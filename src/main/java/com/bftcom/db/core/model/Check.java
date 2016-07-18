package com.bftcom.db.core.model;

public class Check  extends Constraint {
  private String searchCondition;

  public String getSearchCondition() {
    return searchCondition;
  }

  public void setSearchCondition(String searchCondition) {
    this.searchCondition = searchCondition;
  }

  @Override
  public String getAddConstraintSql() {
    //might not work for every RDBMS, removes quotes around automatically generated identifiers
    String searchCondition = getSearchCondition().replaceAll("\"", "");
    return String.format("ALTER TABLE %s ADD CONSTRAINT %s CHECK (%s)", getTableName(), getName(), searchCondition);
  }
}
