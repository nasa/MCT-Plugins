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
package gov.nasa.arc.mct.scenario.component;

/**
 * Exposed as a capability by objects which serve as a 
 * central (or at least locatable) repository for other 
 * objects, based on the capabilities exposed by those 
 * objects. An example is the Tag Repositories, 
 * User Tags and Mission Tags.
 */
public interface RepositoryCapability {
	// All components in a repository are expected to exhibit some specific capability
	public Class<?> getCapabilityClass();
	
	// Repositories may be specific to users, to groups, or global
	public String getUserScope();
}
