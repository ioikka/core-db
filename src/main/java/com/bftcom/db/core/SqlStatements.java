package com.bftcom.db.core;

import com.bftcom.db.core.model.Check;
import com.bftcom.db.core.model.Constraint;
import com.bftcom.db.core.model.FK;
import com.bftcom.db.core.model.View;
import com.bftcom.db.core.sql.KeyWords;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.symmetric.model.Sequence;

public interface SqlStatements {
  String getAllSequences();

  static String buildSelectStatement(String tableName, String... columnNames) {
    return KeyWords.SELECT + " " + StringUtils.join(columnNames, ",") + " " +
        KeyWords.FROM + " " + tableName;
  }

  default String createSequence(Sequence sequence, boolean continueNumbering) {
    throw new NotImplementedException();
  }

  default String dropSequence(Sequence sequence) {
    return "DROP SEQUENCE " + sequence.getSequenceName();
  }


  default String createView(View view) {
    throw new NotImplementedException();
  }

  default String createView(String sql) {
    throw new NotImplementedException();
  }

  default String convertTo(String sql) {
    throw new NotImplementedException();
  }
}
