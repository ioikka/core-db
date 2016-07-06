package com.bftcom.db.postgresql;

import com.bftcom.db.core.sql.KeyWords;
import com.bftcom.db.core.SqlStatements;
import org.apache.commons.lang.NotImplementedException;
import org.jumpmind.symmetric.model.Sequence;

public class PostgresqlSqlStatements implements SqlStatements{

  @Override
  public String getAllSequences() {
    throw new NotImplementedException();
  }

  /**
   * https://www.postgresql.org/docs/current/static/sql-createsequence.html
   *  CREATE [ TEMPORARY | TEMP ] SEQUENCE [ IF NOT EXISTS ] name [ INCREMENT [ BY ] increment ]
   *  [ MINVALUE minvalue | NO MINVALUE ] [ MAXVALUE maxvalue | NO MAXVALUE ]
   *  [ START [ WITH ] start ] [ CACHE cache ] [ [ NO ] CYCLE ]
   *  [ OWNED BY { table_name.column_name | NONE } ]
   *
   * @return sql statement that creates a sequence
   */
  @Override
  public String createSequence(Sequence sequence, boolean continueNumbering) {
    String startWith = continueNumbering ? " " + KeyWords.START + " " + KeyWords.WITH + " " + sequence.getCurrentValue() : "";
    return KeyWords.CREATE + " " + KeyWords.SEQUENCE + " "  + sequence.getSequenceName() + " " + startWith;
  }
}
