package com.bftcom.db.oracle.model;

import com.bftcom.db.core.model.View;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OracleView extends View {

  public OracleView() {
    super();
  }

  public OracleView(View view) {
    super(view);
  }

  public static void fillDdl(DataSource dataSource, View view) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT dbms_metadata.get_ddl('VIEW','" + view.getViewName() + "') FROM DUAL")) {
        if (resultSet.next()) {
          String ddl = resultSet.getString(1);
          view.setDdl(ddl);
          if (ddl != null) {
            String createViewAs = ddl.split(" AS \n")[0];
            view.setColumnsAsString(createViewAs.substring(createViewAs.indexOf("(") + 1, createViewAs.lastIndexOf(")")).replaceAll("\"", ""));
          }
        }
      }
    }
  }
}
