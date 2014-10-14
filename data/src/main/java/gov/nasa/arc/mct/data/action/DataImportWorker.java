/*******************************************************************************
 * Mission Control Technologies, Copyright (c) 2009-2012, United States Government
 * as represented by the Administrator of the National Aeronautics and Space 
 * Administration. All rights reserved.
 *
 * The MCT platform is licensed under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations under 
 * the License.
 *
 * MCT includes source code licensed under additional open source licenses. See 
 * the MCT Open Source Licenses file included with this distribution or the About 
 * MCT Licenses dialog available at runtime from the MCT Help menu for additional 
 * information. 
 *******************************************************************************/
package gov.nasa.arc.mct.data.action;

import gov.nasa.arc.mct.api.feed.BufferFullException;
import gov.nasa.arc.mct.api.feed.FeedDataArchive;
import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.components.FeedProvider;
import gov.nasa.arc.mct.components.FeedProvider.RenderingInfo;
import gov.nasa.arc.mct.data.access.FeedDataArchiveAccess;
import gov.nasa.arc.mct.data.component.DataComponent;
import gov.nasa.arc.mct.data.component.DataTaxonomyComponent;
import gov.nasa.arc.mct.platform.spi.PlatformAccess;

import java.awt.Color;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingWorker;

/**
 * A SwingWorker responsible for managing the background 
 * activities of Data import. These include parsing data,
 * registering endTime in DataTaxonomyComponent (parent) 
 * and saving data to database.  
 * 
 * @author jdong
 *
 */
public class DataImportWorker extends SwingWorker<Boolean, Void> {
	private File file;
	private AbstractComponent parent;
	private FileNotFoundException fnfException;
	private BufferFullException bfException;
	private FeedDataArchive dataArchive = FeedDataArchiveAccess.getDataArchive();
	
	/**
	 * used to check whether lines of data belong to the same component,
	 * essentially used to find last time stamp.
	 */
	private String previousID, previousTime;
	
	/**
	 * monitor the process of reading data from file and writing to disk
	 */
	private Boolean success = true;
	
	public DataImportWorker(File file, AbstractComponent parent) {
		super();
		this.file = file;	
		this.parent = parent;
		if ((parent == null) && (file == null)) {
			throw new IllegalArgumentException();
		}
	}

	@Override
	protected Boolean doInBackground() throws Exception {
		setProgress(0);
		Boolean success = readFile(file);
		setProgress(100);
		
		return success & !isCancelled();
	}
	
	private Boolean readFile(File file) {
		Scanner fileScanner = null;
		
		try {
			fileScanner = new Scanner(file);
		} catch (FileNotFoundException e) {
			fnfException = e;
			success = false;
			e.printStackTrace();
		} finally {
			success = (fileScanner != null);
		}
		
		if (success) {
			long byteLength = file.length();
			int bytesRead = 0;
	        long totalBytes = 0;
			String line = fileScanner.nextLine();		
			readFirstLine(line);
			bytesRead = line.getBytes().length;

			while (success && fileScanner.hasNextLine()) {
				
				totalBytes += bytesRead;
				setProgress(Math.round(((float) totalBytes / (float) byteLength) * 100f));

				line = fileScanner.nextLine();
				saveData(line);
				bytesRead = line.getBytes().length;
			}	
			
			// process the last line of file
			setEndTime(previousID, previousTime);
			fileScanner.close();
		}		
		
		return success;
	}
	
	/**
	 * read first line to initialize previoudID and previousTime,
	 * and save the data.
	 * @param firstLine
	 */
	private void readFirstLine(String firstLine) {
		Scanner lineScanner = new Scanner(firstLine);
		lineScanner.useDelimiter(",");
		previousID = DataComponent.PREFIX + lineScanner.next();
		previousTime   = lineScanner.next();
		lineScanner.close();

		saveData(firstLine);
	}
	
    private void saveData(String line) {    	
    	Scanner lineScanner = new Scanner(line);
		lineScanner.useDelimiter(",");
		String feedID = DataComponent.PREFIX + lineScanner.next();
		String time   = lineScanner.next();
		String value  = lineScanner.next();
		lineScanner.close();
		
    	Map<String, String> datum = new HashMap<String, String>();
    	RenderingInfo ri = new RenderingInfo(value, Color.ORANGE, " ", Color.ORANGE, true);;
    	ri.setPlottable(true);
    	
    	// Fill in the normally expected key/value pairs
	    datum.put(FeedProvider.NORMALIZED_IS_VALID_KEY, Boolean.TRUE.toString());   
	    datum.put(FeedProvider.NORMALIZED_RENDERING_INFO, ri.toString());
	    datum.put(FeedProvider.NORMALIZED_TIME_KEY, String.valueOf(time));
	    datum.put(FeedProvider.NORMALIZED_VALUE_KEY, value);
	   
	    if (dataArchive != null) {
	    	try {
				dataArchive.putData(feedID, TimeUnit.MILLISECONDS, Long.parseLong(time), datum);
			} catch (BufferFullException e) {
				success = false;
				bfException = e;
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
	    }	
		
	    // check whether reading data for a new component
	    if (isEndTime(feedID)) setEndTime(feedID, time);
	    
	    previousID = feedID;
	    previousTime = time;
    }
    
    private Boolean isEndTime(String feedID) {
    	return !feedID.equals(previousID);
    }

    private void setEndTime(String id, String time) { 	
    	assert parent instanceof DataTaxonomyComponent;
    	((DataTaxonomyComponent)parent).setTimeStamp(id, time);
    	// since model is changed, needs to save into database
    	PlatformAccess.getPlatform().getPersistenceProvider().persist(Collections.singleton(parent));
    }

	public List<Exception> getException() {
		List<Exception> exceptions = new ArrayList<Exception> ();
		if (fnfException != null) exceptions.add(fnfException);
		if (bfException != null ) exceptions.add(bfException);
	    return exceptions;
	}
	
	public void safeClose(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

}
