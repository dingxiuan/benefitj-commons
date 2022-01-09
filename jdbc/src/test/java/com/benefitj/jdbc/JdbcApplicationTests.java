package com.benefitj.jdbc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.benefitj.core.TimeUtils;
import com.benefitj.jdbc.sql.DatabaseConnector;
import com.benefitj.jdbc.sql.EnhanceStatement;
import com.benefitj.jdbc.sql.TableDDL;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class JdbcApplicationTests {

  private DatabaseConnector connector;

  public DatabaseConnector getConnector() {
    DatabaseConnector c = this.connector;
    if (c == null) {
      synchronized (this) {
        if ((c = this.connector) == null) {
          this.connector = new DatabaseConnector()
              .setUrl("jdbc:mysql://192.168.1.203:53306")
              .setUser("root")
              .setPassword("hsrg8888")
//              .setUrl("jdbc:mysql://192.168.19.129:3306")
//              .setUser("root")
//              .setPassword("admin")
              .setDriver("com.mysql.cj.jdbc.Driver")
              //.setCatalog("hsrg")
              .setAutoCommit(true)
              .setReadOnly(false)
              .setClientProperties(prop -> {
                prop.setProperty("useUnicode", "true");
                prop.setProperty("characterEncoding", "utf-8");
                prop.setProperty("serverTimezone", "Asia/Shanghai");
                prop.setProperty("allowMultiQueries", "true");
              });
          c = this.connector;
        }
      }
    }
    return c;
  }

  @Before
  public void before() throws SQLException {
    final Connection c = getConnector().getConnection();

    System.err.println("schema: " + c.getSchema()
        + ", catalog: " + c.getCatalog()
        + ", autoCommit: " + c.getAutoCommit()
        + ", holdability: " + c.getHoldability()
        + ", networkTimeout: " + c.getNetworkTimeout()
        + ", transactionIsolation: " + c.getTransactionIsolation()
        + ", typeMap: " + c.getTypeMap()
        + ", clientInfo: " + c.getClientInfo()
    );
    System.err.println();
  }

  @Test
  public void testDatabase() {
    System.err.println();
    EnhanceStatement stmt = getConnector().createStmt();
    System.err.println(stmt.getDatabases());
    System.err.println();

//    stmt.createDatabase("test");
//    stmt.use("test");
//
//    //stmt.dropDatabase("test");
//    stmt.execute("CREATE TABLE IF NOT EXISTS `ru3noob_tbl`(\n" +
//            "   `runoob_id` INT UNSIGNED AUTO_INCREMENT,\n" +
//            "   `runoob_title` VARCHAR(100) NOT NULL,\n" +
//            "   `runoob_author` VARCHAR(40) NOT NULL,\n" +
//            "   `submission_date` DATE,\n" +
//            "   PRIMARY KEY ( `runoob_id` )\n" +
//            ")ENGINE=InnoDB DEFAULT CHARSET=utf8;"
//        , false);
    stmt.close();
  }

  @Test
  public void testTables() {
    EnhanceStatement stmt = getConnector().createStmt();
    stmt.use("quartz");
    System.err.println(stmt.showTables());
    System.err.println(JSON.toJSONString(stmt.showTableStatus()));
    stmt.close();
  }

  @Test
  public void testShowColumns() {
    EnhanceStatement stmt = getConnector().createStmt();
    stmt.use("hsrg");
    List<JSONObject> records = stmt.queryList("show columns from `HS_PERSON`");
    System.err.println(records);
    stmt.close();
  }

  @Test
  public void testShowCreateTableDDL() {
    EnhanceStatement stmt = getConnector().createStmt();

    long start = TimeUtils.now();
    List<TableDDL> ddls = stmt.showTableDDLs("jeecg-boot");
    System.err.println(JSON.toJSONString(ddls));

//    // 创建表
//    stmt.use("test");
//    stmt.transactional(() -> {
//      TableDDL ddl = ddls.get(0);
////      for (TableDDL ddl : ddls) {
////
////      }
//      System.err.println("\n------------------------------\n");
//      System.err.println("create table: " + ddl.getDdl());
//      System.err.println("\n------------------------------\n");
//      stmt.dropTables(new String[]{ddl.getName()}, false);
//      stmt.execute(ddl.getDdl());
//    }, false);

    stmt.close();
    System.err.println("耗时: " + TimeUtils.diffNow(start));
  }

  @Test
  public void testQuery() {
    long start1 = TimeUtils.now();
    EnhanceStatement stmt = getConnector().createStmt();
    stmt.use("hsrg");
    long start2 = TimeUtils.now();
    List<JSONObject> json = stmt.queryList("SELECT count(t.type) AS count, t.type\n" +
        "FROM (SELECT DISTINCT hrt.zid AS reportZid, hrt.person_zid AS personZid, hrt.type AS type\n" +
        "      FROM HS_REPORT_TASK AS hrt\n" +
        "      LEFT JOIN HS_INPATIENT AS hi ON hi.org_zid = hrt.org_zid\n" +
        "      LEFT JOIN HS_ORG AS ho ON ho.zid = hi.org_zid\n" +
        "      WHERE hrt.`status` in ('FINISH') AND hrt.type in ('activity', 'jsyl', 'mse', 'hrv', 'physical', 'sleepStageAhi', 'holter') \t\t        AND (hi.org_zid = '0' OR (ho.auto_code LIKE concat( ( SELECT auto_code FROM HS_ORG WHERE zid = '0' LIMIT 1 ), ':%')))\n" +
        "\t) AS t\n" +
        "GROUP BY t.type");

    System.err.println(json);

    // 关闭
    stmt.close();

    System.err.println("耗时1: " + TimeUtils.diffNow(start1));
    System.err.println("耗时2: " + TimeUtils.diffNow(start2));
  }

}
