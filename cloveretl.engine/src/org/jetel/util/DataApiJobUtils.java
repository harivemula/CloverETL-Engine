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
package org.jetel.util;

/**
 * @author Jiri Musil (info@cloveretl.com)
 *         (c) Javlin, a.s. (www.cloveretl.com)
 *
 * @created Mar 28, 2017
 */
public class DataApiJobUtils {
	
	public static final String DATA_API_JOB_INPUT_TYPE = "DATAAPI_INPUT";
	public static final String DATA_API_JOB_OUTPUT_TYPE = "DATAAPI_OUTPUT";
	
	public static boolean isDataApiJobInputComponent(String componentType) {
		return DATA_API_JOB_INPUT_TYPE.equals(componentType);
	}
	
	public static boolean isDataApiJobOutputComponent(String componentType) {
		return DATA_API_JOB_OUTPUT_TYPE.equals(componentType);
	}
	
	public static boolean isDataApiJobInputOutputComponent(String componentType) {
		return isDataApiJobInputComponent(componentType) || isDataApiJobOutputComponent(componentType);
	}
	
}
