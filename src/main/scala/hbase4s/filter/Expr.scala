package hbase4s.filter

/**
  * Created by Volodymyr.Glushak on 11/05/2017.
  */
sealed trait Expr

case object Invalid extends Expr

case class Column(family: String, name: String)

case class And(l: Expr, r: Expr) extends Expr

case class Or(l: Expr, r: Expr) extends Expr

/**
  * Filter returns only the key component of each key-value.
  */
case object KeyOnly extends Expr

/**
  * Filter returns only the first key-value from each row.
  */
case object FirstKeyOnly extends Expr

/**
  * Filter returns only those key-values present in a row that starts with the specified row prefix
  * @param p a prefix of a row key
  */
case class RowPrefix(p: String) extends Expr

/**
  * Filter returns only those key-values present in a column that starts with the specified column prefix
  * @param cp a prefix of a column qualifier
  */
case class ColumnPrefix(cp: String) extends Expr

/**
  * Filter returns key-values that are present in a column that starts with any of the specified column prefixes.
  * @param cp list of column qualifier prefixes
  */
case class MultipleColumnPrefix(cp: Seq[String]) extends Expr

/**
  *  It returns the first limit number of columns in the table.
  * @param l limit
  */
case class ColumnCountGet(l: Int) extends Expr

/**
  * It returns page size number of rows from the table.
  * @param l page size
  */
case class Page(l: Int) extends Expr

/**
  * Filter returns all key-values present in rows up to and including the specified row.
  * @param rk a row key on which to stop scanning.
  */
case class InclusiveStop(rk: String) extends Expr


/**
  * It compares each qualifier name with the comparator using the compare operator and if the comparison returns true,
  * it returns all the key-values in that column.
  * @param cp compare operator
  * @param q comparator - column qualifier to compare
  */
case class Qualifier(cp: CompareOp, q: Column) extends Expr


/**
  * It compares each column value with the comparator using the compare operator and if the comparison returns true,
  * it returns all the key-values in that column.
  * @param cp compare operator
  * @param q comparator - column value to compare
  */
case class Value(cp: CompareOp, q: String) extends Expr

/**
  * If the column is found and the comparison with the comparator returns true, all the columns of the row will be emitted.
  * If the condition fails, the row will not be emitted.
  * @param col type that describes column family and name
  * @param op compare operator
  * @param value comparator - column value to compare
  * @param setFilterIfMissing - if false returns rows where column with such name missed, default is true
  */
case class SingleColVal(col: Column, op: CompareOp, value: String, setFilterIfMissing: Boolean = true) extends Expr
