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
package gov.nasa.arc.mct.scenario;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.platform.spi.PersistenceProvider;
import gov.nasa.arc.mct.platform.spi.Platform;
import gov.nasa.arc.mct.platform.spi.PlatformAccess;
import gov.nasa.arc.mct.scenario.component.TagRepositoryComponent;
import gov.nasa.arc.mct.services.internal.component.ComponentInitializer;
import gov.nasa.arc.mct.services.internal.component.User;

import java.util.Collection;
import java.util.Collections;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		Platform platform = PlatformAccess.getPlatform();
		PersistenceProvider persistence = platform.getPersistenceProvider();
		Collection<AbstractComponent> bootstraps = platform.getBootstrapComponents();
		String user = platform.getCurrentUser().getUserId();
		String wild = "*";
		String compType = TagRepositoryComponent.class.getName();
		
		if (!contains(bootstraps, TagRepositoryComponent.class, user)) {
			AbstractComponent repo = platform.getComponentRegistry().newInstance(compType);
			repo.setDisplayName("User Tags");
			persistence.persist(Collections.singleton(repo));
			persistence.tagComponents("bootstrap:creator", Collections.singleton(repo));
		}
		
		if (!contains(bootstraps, TagRepositoryComponent.class, wild)) {
			AbstractComponent repo = platform.getComponentRegistry().newInstance(compType);
			repo.setDisplayName("Mission Tags");
			repo.getCapability(ComponentInitializer.class).setCreator(wild);
			persistence.persist(Collections.singleton(repo));
			persistence.tagComponents("bootstrap:admin", Collections.singleton(repo));
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
	}

	private boolean contains(Collection<AbstractComponent> components, 
			Class<? extends AbstractComponent> componentClass, 
			String creator) {
		for (AbstractComponent ac : components) {
			if (componentClass.isAssignableFrom(ac.getClass())) {
				if (ac.getCreator().equals(creator)) {
					return true;
				}
			}
		}
		return false;
	}
}
