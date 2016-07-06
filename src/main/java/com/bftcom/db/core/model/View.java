package com.bftcom.db.core.model;

import org.apache.commons.lang.NotImplementedException;

import java.util.List;

/**
 * @author ikka
 * @date: 02.07.2016.
 */
public class View {
  /**
   * a substring from the original ddl statement (order of col names is kept to the original order).
   */
  protected String columnsAsString;
  protected String viewName;
  protected List<String> columnNames;//all cols' names
  protected String statement;
  protected String ddl;//complete ddl with create view part
  protected String sql;//after as
  protected String createSql;

  public View() {
  }

  public View(View view){
    this.viewName = view.viewName;
    this.columnsAsString = view.columnsAsString;
    this.columnNames = view.columnNames;
    this.statement = view.statement;
    this.ddl = view.ddl;
    this.sql = view.sql;
    this.createSql = view.createSql;
  }

  public String getViewName() {
    return viewName;
  }

  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

  public List<String> getColumnNames() {
    return columnNames;
  }

  public void setColumnNames(List<String> columnNames) {
    this.columnNames = columnNames;
  }

  public String getStatement() {
    return statement;
  }

  public void setStatement(String statement) {
    this.statement = statement;
  }

  public String getDdl() {
    return ddl;
  }

  public void setDdl(String ddl) {
    this.ddl = ddl;
  }

  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }


  public String getColumnsAsString() {
    return columnsAsString;
  }

  public void setColumnsAsString(String columnsAsString) {
    this.columnsAsString = columnsAsString;
  }

  public String getCreateSql() {
    throw new NotImplementedException();
  }
}
