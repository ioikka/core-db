package com.bftcom.db.oracle.mappers;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.Row;
import org.jumpmind.symmetric.model.Sequence;

public class OracleSequenceRowMapper implements ISqlRowMapper<Sequence> {
  public static String TABLE = "USER_SEQUENCES";//table/view name in DB

  public enum COLS {
    SEQUENCE_NAME,
    MIN_VALUE,
    MAX_VALUE,
    INCREMENT_BY,
    LAST_NUMBER,
  }

  public static String COL_NAMES = StringUtils.join(COLS.values(), ",");

  @Override
  public Sequence mapRow(Row row) {
    Sequence seq = new Sequence();
    seq.setSequenceName(row.getString(COLS.SEQUENCE_NAME.name()));
    seq.setMaxValue(row.getLong(COLS.MAX_VALUE.name()));
    seq.setMinValue(row.getLong(COLS.MIN_VALUE.name()));
    seq.setCurrentValue(row.getLong(COLS.LAST_NUMBER.name()));
    return seq;
  }
}
