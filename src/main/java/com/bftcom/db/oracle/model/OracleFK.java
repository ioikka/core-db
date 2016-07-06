package com.bftcom.db.oracle.model;

import com.bftcom.db.core.model.FK;

public class OracleFK extends FK {
  public static String GET_DDL_SQL_TEMPLATE = "select DBMS_METADATA.GET_DDL('REF_CONSTRAINT', '%s') from dual";
}
