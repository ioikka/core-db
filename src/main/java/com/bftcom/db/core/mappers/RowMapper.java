package com.bftcom.db.core.mappers;

import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class RowMapper<T> implements ISqlRowMapper<T> {
  private static Logger logger = LoggerFactory.getLogger(RowMapper.class);

  private Class<T> clazz;

  public RowMapper(Class clazz) {
    //noinspection unchecked
    this.clazz = clazz;
  }

  protected T getInstance() {
    try {
      return clazz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      logger.error("", e);
    }
    return null;
  }

  @Override
  public T mapRow(Row row) {
    return getInstance();
  }
}
