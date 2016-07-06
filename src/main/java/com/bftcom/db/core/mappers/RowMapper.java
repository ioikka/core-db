package com.bftcom.db.core.mappers;

import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.Row;

/**
 * @author ikka
 * @date: 06.07.2016.
 */
public class RowMapper<T> implements ISqlRowMapper<T> {

  protected Class<T> clazz;

  public RowMapper(Class clazz) {
    this.clazz = clazz;
  }

  public T getInstance() {
    try {
      return clazz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public T mapRow(Row row) {
    return getInstance();
  }
}
