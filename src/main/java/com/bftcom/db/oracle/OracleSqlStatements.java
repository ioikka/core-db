package com.bftcom.db.oracle;

import com.bftcom.db.core.SqlStatements;
import com.bftcom.db.oracle.mappers.OracleSequenceRowMapper;
import org.apache.commons.lang.StringUtils;

public class OracleSqlStatements implements SqlStatements {

  private static final String GET_ALL_SEQUENCES = SqlStatements.buildSelectStatement(OracleSequenceRowMapper.TABLE, StringUtils.join(OracleSequenceRowMapper.COLS.values(), ","));

  @Override
  public String getAllSequences() {
    return GET_ALL_SEQUENCES;
  }
}
