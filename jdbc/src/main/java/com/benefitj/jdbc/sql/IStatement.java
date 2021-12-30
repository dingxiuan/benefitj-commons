package com.benefitj.jdbc.sql;

import com.alibaba.fastjson.JSONObject;
import com.benefitj.core.functions.IFunction;
import com.benefitj.core.functions.IRunnable;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public interface IStatement<T extends IStatement<T>> {

  default T self() {
    return (T)this;
  }

  Connection getConnection();

  boolean execute(String sql);

  ResultSet executeQuery(String sql);

  /**
   * try{} catch(e){}
   */
  default <T> T tryThrow(Callable<T> call) {
    return SqlUtils.tryThrow(call);
  }

  /**
   * try{} catch(e){}
   */
  default void tryThrow(IRunnable r) {
    SqlUtils.tryThrow(r);
  }


  default StatementRoot getRoot() {
    return (StatementRoot) this;
  }

  /**
   * 开启事务
   *
   * @param call 回调
   * @param open 是否开始
   * @param <T>  结果类型
   * @return 返回结果
   */
  default <T> T transactional(Callable<T> call, boolean open) {
    return tryThrow(() -> {
      if (open) {
        Savepoint savepoint;
        synchronized (this) {
          savepoint = getRoot().getSavepoint();
          if (savepoint == null) {
            // 开启事务
            getRoot().setSavepoint(savepoint = getConnection().setSavepoint());
          }
        }
        try {
          T result = call.call();
          getConnection().releaseSavepoint(savepoint);
          return result;
        } catch (Exception e) {
          getConnection().rollback(savepoint);
          throw new IllegalStateException(e);
        } finally {
          getRoot().removeSavepoint();
        }
      } else {
        return call.call();
      }
    });
  }

  /**
   * 开启事务
   *
   * @param r    回调
   * @param open 是否开始
   */
  default void transactional(IRunnable r, boolean open) {
    tryThrow(() -> {
      if (open) {
        Savepoint savepoint = getRoot().getSavepoint();
        if (savepoint == null) {
          // 开启事务
          getRoot().setSavepoint(savepoint = getConnection().setSavepoint());
        }
        try {
          r.run();
          getConnection().releaseSavepoint(savepoint);
        } catch (Exception e) {
          getConnection().rollback(savepoint);
          throw new IllegalStateException(e);
        } finally {
          getRoot().removeSavepoint();
        }
      } else {
        r.run();
      }
    });
  }

  /**
   * 查询
   */
  default <T> T query(String sql, IFunction<ResultSet, T> mappedFunc) {
    return tryThrow(() -> {
      try (final ResultSet set = executeQuery(sql);) {
        return mappedFunc.apply(set);
      }
    });
  }

  /**
   * 查询
   */
  default List<JSONObject> queryList(String sql) {
    return query(sql, SqlUtils::getRecords);
  }

  /**
   * 执行 Statement 语句
   *
   * @param sql SQL语句
   */
  default boolean execute(String sql, boolean transactional) {
    return transactional(() -> execute(sql), transactional);
  }

  /**
   * 获取使用中的数据库
   */
  default String getUsingDatabase() {
    return queryList("SELECT database() AS db;")
        .stream()
        .findFirst()
        .map(json -> json.getString("db"))
        .orElse(null);
  }

  /**
   * 切换数据库
   *
   * @param db 数据库
   */
  default T use(String db) {
    execute("USE `" + db + "`;", false);
    return self();
  }

  /**
   * 切换数据库
   */
  default T useIfNotEmpty(String db) {
    if (StringUtils.isNotBlank(db)) {
      use(db);
    }
    return self();
  }

  /**
   * 获取全部的数据库
   */
  default List<String> getDatabases(boolean ignoreSchemas) {
    return queryList("SHOW DATABASES;")
        .stream()
        .map(json -> json.getString("Database"))
        .filter(name -> !ignoreSchemas || !DatabaseConnector.SCHEMAS.contains(name))
        .collect(Collectors.toList());
  }

  /**
   * 获取全部的数据库
   */
  default List<String> getDatabases() {
    return getDatabases(true);
  }

  /**
   * 创建数据库
   *
   * @param name 数据库名称
   */
  default T createDatabase(String name) {
    List<String> databases = getDatabases(false);
    if (!databases.contains(name)) {
      execute("CREATE DATABASE `" + name + "`");
    }
    return self();
  }

  /**
   * 删除数据库
   *
   * @param db 数据库名
   */
  default T dropDatabase(String db) {
    execute("DROP DATABASE `" + db + "`;", false);
    return self();
  }

  /**
   * 获取表状态
   */
  default List<TableStatus> showTableStatus(String db) {
    useIfNotEmpty(db);
    return queryList("SHOW TABLE STATUS;")
        .stream()
        .map(json -> json.toJavaObject(TableStatus.class))
        .collect(Collectors.toList());
  }

  /**
   * 获取表状态
   */
  default List<TableStatus> showTableStatus() {
    return showTableStatus(null);
  }

  /**
   * 获取表
   */
  default List<String> showTables(String db) {
    useIfNotEmpty(db);
    return queryList("SHOW TABLES;")
        .stream()
        .flatMap(json -> json.values().stream().map(o -> o != null ? o.toString() : null))
        .collect(Collectors.toList());
  }

  /**
   * 获取表
   */
  default List<String> showTables() {
    return showTables(null);
  }


  /**
   * 清空数据表
   *
   * @param tables 表名
   */
  default void clearTables(String... tables) {
    clearTables(tables, true);
  }

  /**
   * 清空数据表
   *
   * @param tables        表名
   * @param transactional 是否开启事务
   */
  default void clearTables(String[] tables, boolean transactional) {
    transactional(() -> {
      for (String table : tables) {
        //execute("DELETE FROM `" + table + "`;");
        execute("TRUNCATE TABLE `" + table + "`;");
      }
    }, transactional);
  }

  /**
   * 删除数据表
   *
   * @param tables 表名
   * @return 删除的表数量
   */
  default int dropTables(String... tables) {
    return dropTables(tables, true);
  }

  /**
   * 删除数据表
   *
   * @param tables        表名
   * @param transactional 是否开启事务
   * @return 删除的表数量
   */
  default int dropTables(String[] tables, boolean transactional) {
    final AtomicInteger count = new AtomicInteger(0);
    transactional(() -> {
      for (String table : tables) {
        execute("DROP TABLES IF EXISTS `" + table + "`;");
        count.incrementAndGet();
      }
    }, transactional);
    return count.get();
  }

  /**
   * 查询表的DDL
   *
   * @param db    数据库
   * @param table 表名
   * @return 返回表的DDL
   */
  default TableDDL showTableDDL(@Nullable String db, String table) {
    useIfNotEmpty(db);
    return queryList("SHOW create table `" + table + "`;")
        .stream()
        .map(json -> {
          if (json.containsKey("Table")) {
            return new TableDDL(json.getString("Table"), json.getString("Create Table"));
          }
          TableDDL ddl = new TableDDL();
          ddl.setName(json.getString("View"));
          ddl.setDdl(json.getString("Create View"));
          ddl.setView(true);
          ddl.setCharacterSetClient(json.getString("character_set_client"));
          ddl.setCollationConnection(json.getString("collation_connection"));
          return ddl;
        })
        .findFirst()
        .orElse(null);
  }

  /**
   * 查询数据库所有表的DDL
   *
   * @param db 数据库
   * @return 返回表的DDL
   */
  default List<TableDDL> showTableDDLs(String db) {
    return showTableDDLs(db, true);
  }

  /**
   * 查询数据库所有表的DDL
   *
   * @param db         数据库
   * @param ignoreView 是否忽略视图
   * @return 返回表的DDL
   */
  default List<TableDDL> showTableDDLs(String db, boolean ignoreView) {
    return showTables(db)
        .stream()
        .map(name -> showTableDDL(null, name))
        .filter(tableDDL -> !(ignoreView && tableDDL.isView()))
        .collect(Collectors.toList());
  }

  /**
   * 添加主键
   *
   * @param table  表名
   * @param column 主键列
   * @return 返回执行结果
   */
  default boolean addPrimaryKey(String table, String column) {
    return execute("ALTER TABLE `" + table + "` ADD PRIMARY KEY (`" + column + "`);", true);
  }

  /**
   * 添加约束主键
   *
   * @param table   表名
   * @param name    主键名
   * @param columns 主键列
   * @return 返回执行结果
   */
  default boolean addConstraintPrimaryKey(String table, String name, String[] columns) {
    String str = SqlUtils.joint(Arrays.asList(columns), ", ");
    return execute("ALTER TABLE `" + table + "` ADD CONSTRAINT `" + name + "` PRIMARY KEY (`" + str + "`);", true);
  }

  /**
   * 删除主键
   *
   * @param table 表名
   * @return 返回执行结果
   */
  default boolean dropPrimaryKey(String table) {
    return execute("ALTER TABLE `" + table + "` DROP  PRIMARY KEY;", true);
  }

  /**
   * 删除约束主键
   *
   * @param table      表名
   * @param constraint 关联
   * @return 返回执行结果
   */
  default boolean dropConstraintPrimaryKey(String table, String constraint) {
    return execute("ALTER TABLE `" + table + "` DROP CONSTRAINT `" + constraint + "`;", true);
  }

  /**
   * 删除唯一
   *
   * @param table 表名
   * @param index 索引名
   * @return 返回执行结果
   */
  default boolean addUnique(String table, String index) {
    return execute("ALTER TABLE `" + table + "` ADD UNIQUE `" + index + "`", true);
  }

  /**
   * 创建索引
   *
   * @param table  表名
   * @param index  索引名
   * @param unique 是否唯一
   * @return 返回执行结果
   */
  default boolean createIndex(String table, String index, boolean unique) {
    return execute("CREATE " + (unique ? " UNIQUE " : "") + " INDEX " + index + " ON `" + table + "`;", true);
  }

  /**
   * 删除索引
   *
   * @param table 表名
   * @param index 索引名
   * @return 返回执行结果
   */
  default boolean dropIndex(String table, String index) {
    return execute("DROP INDEX `" + index + "` ON `" + table + "`;", true);
  }

  /**
   * 添加列
   *
   * @param table     表名
   * @param columnSQL 列SQL
   * @return 返回执行结果
   */
  default boolean addColumn(String table, String columnSQL) {
    return execute("ALTER TABLE `" + table + "` ADD COLUMN " + columnSQL + ";", true);
  }

  /**
   * 删除列
   *
   * @param table     表名
   * @param columnSQL 列SQL
   * @return 返回执行结果
   */
  default boolean dropColumn(String table, String columnSQL) {
    return execute("ALTER TABLE `" + table + "` DROP COLUMN " + columnSQL + ";", true);
  }

  /**
   * 修改列
   *
   * @param table     表名
   * @param columnSQL 列SQL
   * @return 返回执行结果
   */
  default boolean alterColumn(String table, String columnSQL) {
    return execute("ALTER TABLE `" + table + "` ALTER COLUMN " + columnSQL + ";", true);
  }

}
