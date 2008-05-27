/*
*    jETeL/Clover - Java based ETL application framework.
*    Copyright (C) 2005-06  Javlin Consulting <info@javlinconsulting.cz>
*    
*    This library is free software; you can redistribute it and/or
*    modify it under the terms of the GNU Lesser General Public
*    License as published by the Free Software Foundation; either
*    version 2.1 of the License, or (at your option) any later version.
*    
*    This library is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU    
*    Lesser General Public License for more details.
*    
*    You should have received a copy of the GNU Lesser General Public
*    License along with this library; if not, write to the Free Software
*    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
*/

package org.jetel.connection.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.jetel.connection.jdbc.specific.DBConnectionInstance;
import org.jetel.connection.jdbc.specific.JDBCSpecific.AutoGeneratedKeysType;
import org.jetel.data.DataRecord;
import org.jetel.exception.JetelException;
import org.jetel.util.string.StringUtils;

/**
 * This class is prepared statement with CopySQLData[] object prepared to work with concrete record.
 * 
 * @author avackova (agata.vackova@javlinconsulting.cz) ; 
 * (c) JavlinConsulting s.r.o.
 *  www.javlinconsulting.cz
 *
 * @since Nov 2, 2007
 *
 */
//Doesn't work on select query yet!!!!
public class SQLCloverStatement {
	
	private QueryAnalyzer analyzer;
	private String query;
	private DBConnectionInstance connection;
	private PreparedStatement preparedStatement;
	private CopySQLData[] transMap;
	private String[] cloverFields;
	private String[] dbFields;
	private String[] autoGeneratedColumn;
	private String[] autoGeneratedColumnToClover;
	private DataRecord record;
	
	private AutoKeyGenerator autoKeyGenerator;
	private ResultSet generatedKeys;
	
	private String tableName;
	private Log logger;
	
	private final static String INSERT_KEY_WORD = "insert";
	
	private final static Pattern TABLE_NAME_PATTERN = Pattern.compile("(?:from|into|update)\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
	
	/**
	 * Constructor for sql query containing db - clover mapping
	 * @see QueryAnalyzer
	 * 
	 * @param connection connection to db
	 * @param query sql query
	 * @param record record which will be filled by results from executing query
	 */
	public SQLCloverStatement(DBConnectionInstance connection, String query, DataRecord record){
		this.connection = connection;
		analyzer = new QueryAnalyzer(query);
		this.record = record;
	}
	
	/**
	 * Constructor for sql query containing question marks
	 * 
	 * @param connection connection to db
	 * @param query sql query
	 * @param record record which will be filled by results from executing above query
	 * @param cloverFields clover fields to populate 
	 * @param dbFields database fields
	 * 
	 * @see org.jetel.component.DBOutputTable
	 */
	public SQLCloverStatement(DBConnectionInstance connection, String query, DataRecord record, 
			String[] cloverFields, String[] dbFields){
		this(connection, query, record);
		this.cloverFields = cloverFields;
		this.dbFields = dbFields;
	}

	/**
	 * Constructor for sql query containing question marks
	 * 
	 * @param connection connection to db
	 * @param query sql query
	 * @param record record which will be filled by results from executing above query
	 * @param cloverFields clover fields to populate 
	 * @param dbFields database fields
	 * @param autoGeneratedColumns auto generated columns to be returned
	 */
	public SQLCloverStatement(DBConnectionInstance connection, String query, DataRecord record, 
			String[] cloverFields, String[] dbFields, String[] autoGeneratedColumns){
		this(connection, query, record, cloverFields, dbFields);
		this.autoGeneratedColumn = autoGeneratedColumns;
	}
	
	/**
	 * Prepares statement for database and transMap for result set and data record
	 * 
	 * @return true if successful, false if not 
	 * @throws SQLException
	 * @throws JetelException
	 */
	public void prepareUpdateStatement() throws SQLException, JetelException{
		//get query in form acceptable for PreparedStatement
		if (analyzer.getSource().trim().toLowerCase().startsWith(INSERT_KEY_WORD)) {
			query = analyzer.getInsertQuery();
		}else{
			query = analyzer.getUpdateDeleteQuery();
		}
		List<String[]> dbCloverMap = analyzer.getDbCloverFieldMap();
		if (cloverFields == null && dbFields == null) {
			List<String> cFields = new ArrayList<String>(dbCloverMap.size());
			List<String> dFields = new ArrayList<String>(dbCloverMap.size());
			for (Iterator iterator = dbCloverMap.iterator(); iterator.hasNext();) {
				String[] mapping = (String[]) iterator.next();
				if (mapping[1] != null){//some dbFields can be mapped to constant
					cFields.add(mapping[1]);
					dFields.add(mapping[0]);
				}
				
			}
			cloverFields = cFields.toArray(new String[0]);
			dbFields = dFields.toArray(new String[0]);
		}else if (cloverFields !=null && dbFields != null) {//maybe we have to change order due to order in query
			List<String> cFields = new ArrayList<String>(dbCloverMap.size());
			List<String> dFields = new ArrayList<String>(dbCloverMap.size());
			int index;
			String dbField;
			for (int i = 0; i < dbCloverMap.size(); i++) {
				dbField = dbCloverMap.get(i)[0];
				index = StringUtils.findString(dbField, dbFields);
				if (index > -1) {
					dFields.add(dbField);
					cFields.add(cloverFields[index]);
				}
			}
			cloverFields = cFields.toArray(new String[0]);
			dbFields = dFields.toArray(new String[0]);
		}
		List<Integer> dbFieldTypes;
		if (tableName != null) {
			if (dbFields != null && dbFields.length > 0) {
				dbFieldTypes = SQLUtil.getFieldTypes(connection.getSqlConnection().getMetaData(), tableName, dbFields);
			}else{
				dbFieldTypes = SQLUtil.getFieldTypes(connection.getSqlConnection().getMetaData(), tableName);
			}
		}else{
			if (cloverFields != null && cloverFields.length > 0) {
				dbFieldTypes= SQLUtil.getFieldTypes(record.getMetadata(), cloverFields, connection.getJdbcSpecific());
			} else {
				dbFieldTypes= SQLUtil.getFieldTypes(record.getMetadata(), connection.getJdbcSpecific());
			}
		}
		//prepare trans map
		if (cloverFields != null && cloverFields.length > 0) {
			transMap = CopySQLData.jetel2sqlTransMap(dbFieldTypes, record, cloverFields);
		} else {
			transMap = CopySQLData.jetel2sqlTransMap(dbFieldTypes, record);
		}
		//get auto generated columns from query
		if (analyzer != null) {
			List<String[]> cloverDbMap = analyzer.getCloverDbFieldMap();
			if (cloverDbMap.size() > 0) {
				List<String> cFields = new ArrayList<String>(cloverDbMap.size());
				List<String> dFields = new ArrayList<String>(cloverDbMap.size());
				for (Iterator iterator = cloverDbMap.iterator(); iterator.hasNext();) {
					String[] mapping = (String[]) iterator.next();
					if (mapping[0] != null && mapping[1] != null){
						cFields.add(mapping[0]);
						dFields.add(mapping[1]);
					}
					
				}
				autoGeneratedColumn = dFields.toArray(new String[0]);
				autoGeneratedColumnToClover = cFields.toArray(new String[0]);
			}
		}
		//create sql statement
		autoKeyGenerator = new AutoKeyGenerator(connection, query, autoGeneratedColumn);
		autoKeyGenerator.setLogger(logger);
		autoKeyGenerator.setFillFields(autoGeneratedColumnToClover);
		preparedStatement = autoKeyGenerator.prepareStatement();
	}
	
	/**
	 * Resets this object. For complete reset method setInRecord must be called too.
	 * 
	 * @throws SQLException
	 */
	public void reset() throws SQLException{
		preparedStatement = autoKeyGenerator.reset();
	}
	
	public void setInRecord(DataRecord inRecord) {
		this.record = inRecord;
		CopySQLData.resetDataRecord(transMap, record);
	}
	
	/**
	 * Fills prepared statements with data obtained from input record and executes update on database
	 * 
	 * @return
	 * @throws SQLException
	 */
	public int executeUpdate() throws SQLException{
		for (int i = 0; i < transMap.length; i++) {
			transMap[i].jetel2sql(preparedStatement);
		}
		return preparedStatement.executeUpdate();
	}
	
	/**
	 * Adds set of parameters to batch
	 * 
	 * @throws SQLException
	 */
	public void addBatch() throws SQLException{
		for (int i = 0; i < transMap.length; i++) {
			transMap[i].jetel2sql(preparedStatement);
		}
		preparedStatement.addBatch();
	}
	
	/**
	 * Submits a batch of commands to the database for execution and if all commands execute successfully, returns an 
	 * array of update counts.
	 * 
	 * @return an array of update counts containing one element for each command in the batch. The elements of the 
	 * array are ordered according to the order in which commands were added to the batch.
	 * @throws SQLException
	 */
	public int[] executeBatch() throws SQLException{
		return preparedStatement.executeBatch();
	}
	
	/**
	 * Empties this Statement object's current list of SQL commands.
	 * 
	 * @throws SQLException
	 */
	public void clearBatch() throws SQLException{
		preparedStatement.clearBatch();
	}
	 
	/**
	 * Fills record by data received from database by calling getGeneratedKeys() method
	 * 
	 * @param keyRecord record to fill 
	 * @throws SQLException
	 */
	public void fillKeyRecord(DataRecord keyRecord) throws SQLException{
		if (autoKeyGenerator.getAutoKeyType() == AutoGeneratedKeysType.NONE) {
			return;
		}
		
		generatedKeys = preparedStatement.getGeneratedKeys();
		if (generatedKeys.next()) {
			autoKeyGenerator.fillKeyRecord(record, keyRecord, generatedKeys);
		}					
	}
	
	/**
	 * @param batchUpdate
	 */
	public void setBatchUpdate(boolean batchUpdate){
        CopySQLData.setBatchUpdate(transMap,batchUpdate);
	}
	
	/**
	 * Releases this Statement object's database and JDBC resources immediately instead of waiting for this to happen 
	 * when it is automatically closed.
	 * 
	 * @throws SQLException
	 */
	public void close() throws SQLException{
		if (preparedStatement != null) {
			preparedStatement.close();
		}
	}
	
	/**
	 * @return underlying PreparedStatement object
	 */
	public PreparedStatement getStatement(){
		return preparedStatement;
	}

	/**
	 * @return sql query
	 */
	public String getQuery(){
		return query;
	}
	
	/**
	 * @return sql query as has been set by user
	 */
	public String getQuerySource(){
		return analyzer != null ? analyzer.getSource() : query;
	}
	
	/**
	 * @return logger
	 */
	public Log getLogger() {
		return logger;
	}

	/**
	 * Sets logger
	 * 
	 * @param logger
	 */
	public void setLogger(Log logger) {
		this.logger = logger;
	}

	/**
	 * @return table name from query
	 */
	public String getTableName() {
		if (tableName != null) {
			return tableName;
		}else{
			Matcher matcher = TABLE_NAME_PATTERN.matcher(query);
			if (matcher.find()) {
				tableName = matcher.group(1);
			}
		}
		return tableName;
	}

	/**
	 * Sets table name
	 * 
	 * @param tableName
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
}


