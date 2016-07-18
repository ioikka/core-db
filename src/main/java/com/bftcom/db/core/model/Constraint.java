package com.bftcom.db.core.model;

import org.apache.commons.lang.NotImplementedException;

public abstract class Constraint extends DBObject {
  public enum Type {
    C, //(check constraint on a table)
    P, //(primary key)
    U, //(unique key)
    R, // (referential integrity)
    V ,//(with check option, on a view)
    O, //(with read only, on a view)
    ;
  }

  /**
   * Type of constraint definition:
   */
  private Type type;
  /**
   * Name associated with the table (or view) with constraint definition
   */
  private String tableName;

  public void setType(Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getAddConstraintSql(){
    throw new NotImplementedException();
  }

  public String getDropConstraintSql(){
    return String.format("ALTER TABLE %s DROP CONSTRAINT %s", getTableName(), getName());
  }
}
