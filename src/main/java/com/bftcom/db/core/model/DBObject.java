package com.bftcom.db.core.model;

import org.apache.commons.lang.NotImplementedException;

abstract public class DBObject {
  private String name;

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getCreateSql() {
    throw new NotImplementedException();
  }

  public String getDropSql() {
    throw new NotImplementedException();
  }

  public String getAlterSql() {
    throw new NotImplementedException();
  }
}
