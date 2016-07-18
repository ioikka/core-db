package com.bftcom.db.postgresql.model;

import com.bftcom.db.core.model.View;

/**
 * @author ikka
 * @date: 03.07.2016.
 */
public class PostgresqlView extends View {
  private final static String CREATE_TEMPLATE = "CREATE OR REPLACE VIEW %s (%s) AS \n%s";//

  public PostgresqlView(View view) {
    super(view);
  }

  @Override
  public String getCreateSql() {
    String result = "";
    if (columnsAsString != null && sql != null) {
      result = String.format(CREATE_TEMPLATE, name, columnsAsString, sql.replaceAll("SYSDATE", "CURRENT_DATE"));//Oracle SYSDATE. And if not Oracle? todo
    }
    return result;
  }
}
