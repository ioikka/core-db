package com.bftcom.db.utils.ora2pg;

/**
 */
abstract public class Configuration {
  public static final String SRC_DB_USER = "src.db.user";
  public static final String SRC_DB_PASSWORD = "src.db.password";
  public static final String SRC_DB_URL = "src.db.url";
  public static final String DST_DB_USER = "dst.db.user";
  public static final String DST_DB_PASSWORD = "dst.db.password";
  public static final String DST_DB_URL = "dst.db.url";
  public static final String DST_DB_QUOTED_COLS =  "dst.db.quoted.cols";
  public static final String DST_CREATE_TABLES = "dst.create.tables";

  public static final String EXCLUDED_TABLES = "excluded.tables";
  public static final String INCLUDED_TABLES = "included.tables";
  public static final String EXPORT_TO_CSV = "export.to.csv";
  public static final String DST_CONVERT_BLOBS = "dst.convert.blobs";
  public static final String DST_CONVERT_VIEWS = "dst.convert.views";
  public static final String DST_CREATE_CHECKS = "dst.create.checks";
  public static final String DST_CREATE_FKS = "dst.create.fks";
  public static final String DST_CREATE_UQS = "dst.create.uqs";
  public static final String DST_CREATE_SEQUENCES = "dst.create.sequences";
  public static final String IMPORT_FROM_CSV = "import.from.csv";

  public static final String CSV_DIR = "csv.dir";
  public static final String SRC_SERIALIAZE_METADATA = "src.serialiaze.metadata";

}
