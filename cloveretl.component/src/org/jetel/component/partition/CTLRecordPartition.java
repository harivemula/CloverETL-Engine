/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.component.partition;

import java.nio.ByteBuffer;
import java.util.Properties;

import org.jetel.ctl.CTLAbstractTransform;
import org.jetel.ctl.CTLEntryPoint;
import org.jetel.ctl.TransformLangExecutorRuntimeException;
import org.jetel.data.DataRecord;
import org.jetel.data.RecordKey;
import org.jetel.exception.ComponentNotReadyException;
import org.jetel.exception.TransformException;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.util.ExceptionUtils;
import org.jetel.util.bytes.CloverBuffer;

/**
 * Base class of all Java transforms generated by CTL-to-Java compiler from CTL transforms in the Patition component.
 *
 * @author Michal Tomcanyi, Javlin a.s. &lt;michal.tomcanyi@javlin.cz&gt;
 * @author Martin Janik, Javlin a.s. &lt;martin.janik@javlin.eu&gt;
 *
 * @version 17th June 2010
 * @created 2nd April 2009
 *
 * @see PartitionFunction
 */
public abstract class CTLRecordPartition extends CTLAbstractTransform implements PartitionFunction {

	/** Input data record used for partitioning, or <code>null</code> if not accessible. */
	private DataRecord inputRecord = null;

	@Override
	@Deprecated
	public void init(int numPartitions, RecordKey partitionKey) throws ComponentNotReadyException {
		init(numPartitions, partitionKey, null, null);
	}
	
	@Override
	public final void init(int numPartitions, RecordKey partitionKey, Properties parameters, DataRecordMetadata metadata) throws ComponentNotReadyException {
		globalScopeInit();
		initDelegate(numPartitions);
	}

	/**
	 * Called by {@link #init(int, RecordKey)} to perform user-specific initialization defined in the CTL transform.
	 * The default implementation does nothing, may be overridden by the generated transform class.
	 *
	 * @param partitionCount the number of partitions
	 *
	 * @throws ComponentNotReadyException if the initialization fails
	 */
	@CTLEntryPoint(name = PartitionTL.INIT_FUNCTION_NAME, parameterNames = {
			PartitionTL.PARTITION_COUNT_PARAM_NAME }, required = false)
	protected void initDelegate(int partitionCount) throws ComponentNotReadyException {
		// does nothing by default, may be overridden by generated transform classes
	}

	@Override
	public final boolean supportsDirectRecord() {
		return false;
	}

	@Override
	public final int getOutputPort(DataRecord record) throws TransformException {
		int result = 0;

		// only input record is accessible within the getOutputPort() function
		inputRecord = record;

		try {
			result = getOutputPortDelegate();
		} catch (ComponentNotReadyException exception) {
			// the exception may be thrown by lookups, sequences, etc.
			throw new TransformException("Generated transform class threw an exception!", exception);
		}

		// make the input record inaccessible again
		inputRecord = null;

		return result;
	}

	/**
	 * Called by {@link #getOutputPort(DataRecord)} to perform user-specific partitioning defined in the CTL transform.
	 * Has to be overridden by the generated transform class.
	 *
	 * @throws ComponentNotReadyException if some internal initialization failed
	 * @throws TransformException if an error occurred
	 */
	@CTLEntryPoint(name = PartitionTL.GET_OUTPUT_PORT_FUNCTION_NAME, required = true)
	protected abstract int getOutputPortDelegate() throws ComponentNotReadyException, TransformException;

	@Override
	public final int getOutputPortOnError(Exception exception, DataRecord record) throws TransformException {
		return getOutputPortOnError(exception);
	}

	@Override
	public final int getOutputPortOnError(Exception exception, CloverBuffer buffer) throws TransformException {
		return getOutputPortOnError(exception);
	}

	private int getOutputPortOnError(Exception exception) throws TransformException {
		int result = 0;

		try {
			result = getOutputPortOnErrorDelegate(ExceptionUtils.getMessage(null, exception), ExceptionUtils.stackTraceToString(exception));
		} catch (UnsupportedOperationException ex) {
			// no custom error handling implemented, throw an exception so the transformation fails
			throw new TransformException("Partitioning failed!", exception);
		} catch (ComponentNotReadyException ex) {
			// the exception may be thrown by lookups, sequences, etc.
			throw new TransformException("Generated transform class threw an exception!", ex);
		}

		// make the input record inaccessible again
		inputRecord = null;

		return result;
	}

	/**
	 * Called by {@link #getOutputPortOnError(Exception, DataRecord)} to perform user-specific partitioning defined
	 * in the CTL transform. May be overridden by the generated transform class.
	 * Throws <code>UnsupportedOperationException</code> by default.
	 *
	 * @param errorMessage an error message of the error message that occurred
	 * @param stackTrace a stack trace of the error message that occurred
	 *
	 * @throws ComponentNotReadyException if some internal initialization failed
	 * @throws TransformException if an error occurred
	 */
	@CTLEntryPoint(name = PartitionTL.GET_OUTPUT_PORT_ON_ERROR_FUNCTION_NAME, parameterNames = {
			PartitionTL.ERROR_MESSAGE_PARAM_NAME, PartitionTL.STACK_TRACE_PARAM_NAME }, required = false)
	protected int getOutputPortOnErrorDelegate(String errorMessage, String stackTrace)
			throws ComponentNotReadyException, TransformException {
		throw new UnsupportedOperationException();
	}

	@Override
	public final int getOutputPort(CloverBuffer directRecord) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public int getOutputPort(ByteBuffer directRecord) throws TransformException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	@Deprecated
	public int getOutputPortOnError(Exception exception, ByteBuffer directRecord) throws TransformException {
		return getOutputPortOnError(exception);
	}
	
	@Override
	protected final DataRecord getInputRecord(int index) {
		if (inputRecord == null) {
			throw new TransformLangExecutorRuntimeException(INPUT_RECORDS_NOT_ACCESSIBLE);
		}

		if (index != 0) {
			throw new TransformLangExecutorRuntimeException(new Object[] { index }, INPUT_RECORD_NOT_DEFINED);
		}

		return inputRecord;
	}

	@Override
	protected final DataRecord getOutputRecord(int index) {
		throw new TransformLangExecutorRuntimeException(OUTPUT_RECORDS_NOT_ACCESSIBLE);
	}

}
