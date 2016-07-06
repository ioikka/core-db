package com.bftcom.db.core.model;

import org.apache.commons.lang.NotImplementedException;

/**
 * @author ikka
 * @date: 05.07.2016.
 */
public abstract class Constraint {
  /**
   * Name of the constraint definition
   */
  private String name;
  /**
   * Type of constraint definition:
   */
  private Type type;
  /**
   * Name associated with the table (or view) with constraint definition
   */
  private String tableName;

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

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

  public enum Type {
    C, //(check constraint on a table)
    P, //(primary key)
    U, //(unique key)
    R, // (referential integrity)
    V ,//(with check option, on a view)
    O, //(with read only, on a view)
    ;
  }

  public String getAddConstraintSql(){
    throw new NotImplementedException();
  }

  public String getDropConstraintSql(){
    return String.format("ALTER TABLE %s DROP CONSTRAINT %s", getTableName(), getName());
  }
}
