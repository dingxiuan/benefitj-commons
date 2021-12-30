package com.benefitj.jdbc.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLWarning;
import java.sql.Statement;

public interface EnhanceStatement extends Statement, IStatement<EnhanceStatement> {

  @Override
  ResultSet executeQuery(String sql);

  @Override
  int executeUpdate(String sql);

  @Override
  void close();

  @Override
  int getMaxFieldSize();

  @Override
  void setMaxFieldSize(int max);

  @Override
  int getMaxRows();

  @Override
  void setMaxRows(int max);

  @Override
  void setEscapeProcessing(boolean enable);

  @Override
  int getQueryTimeout();

  @Override
  void setQueryTimeout(int seconds);

  @Override
  void cancel();

  @Override
  SQLWarning getWarnings();

  @Override
  void clearWarnings();

  @Override
  void setCursorName(String name);

  @Override
  boolean execute(String sql);

  @Override
  ResultSet getResultSet();

  @Override
  int getUpdateCount();

  @Override
  boolean getMoreResults();

  @Override
  void setFetchDirection(int direction);

  @Override
  int getFetchDirection();

  @Override
  void setFetchSize(int rows);

  @Override
  int getFetchSize();

  @Override
  int getResultSetConcurrency();

  @Override
  int getResultSetType();

  @Override
  void addBatch(String sql);

  @Override
  void clearBatch();

  @Override
  int[] executeBatch();

  @Override
  Connection getConnection();

  @Override
  boolean getMoreResults(int current);

  @Override
  ResultSet getGeneratedKeys();

  @Override
  int executeUpdate(String sql, int autoGeneratedKeys);

  @Override
  int executeUpdate(String sql, int[] columnIndexes);

  @Override
  int executeUpdate(String sql, String[] columnNames);

  @Override
  boolean execute(String sql, int autoGeneratedKeys);

  @Override
  boolean execute(String sql, int[] columnIndexes);

  @Override
  boolean execute(String sql, String[] columnNames);

  @Override
  int getResultSetHoldability();

  @Override
  boolean isClosed();

  @Override
  void setPoolable(boolean poolable);

  @Override
  boolean isPoolable();

  @Override
  void closeOnCompletion();

  @Override
  boolean isCloseOnCompletion();

  @Override
  long getLargeUpdateCount();

  @Override
  void setLargeMaxRows(long max);

  @Override
  long getLargeMaxRows();

  @Override
  long[] executeLargeBatch();

  @Override
  long executeLargeUpdate(String sql);

  @Override
  long executeLargeUpdate(String sql, int autoGeneratedKeys);

  @Override
  long executeLargeUpdate(String sql, int[] columnIndexes);

  @Override
  long executeLargeUpdate(String sql, String[] columnNames);

  @Override
  String enquoteLiteral(String val);

  @Override
  String enquoteIdentifier(String identifier, boolean alwaysQuote);

  @Override
  boolean isSimpleIdentifier(String identifier);

  @Override
  String enquoteNCharLiteral(String val);

  @Override
  <T> T unwrap(Class<T> iface);

  @Override
  boolean isWrapperFor(Class<?> iface);

}
