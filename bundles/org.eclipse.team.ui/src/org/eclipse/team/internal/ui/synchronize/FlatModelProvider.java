/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.synchronize.*;

/**
 * Provides a flat layout
 */
public class FlatModelProvider extends SynchronizeModelProvider {

	public static class FlatModelProviderDescriptor implements ISynchronizeModelProviderDescriptor {
		public static final String ID = TeamUIPlugin.ID + ".modelprovider_flat"; //$NON-NLS-1$
		public String getId() {
			return ID;
		}		
		public String getName() {
			return Policy.bind("FlatModelProvider.0"); //$NON-NLS-1$
		}		
		public ImageDescriptor getImageDescriptor() {
			return TeamImages.getImageDescriptor(ITeamUIImages.IMG_FLAT);
		}
	}
	private static final FlatModelProviderDescriptor flatDescriptor = new FlatModelProviderDescriptor();
	
	private static final String P_LAST_RESOURCESORT = TeamUIPlugin.ID + ".P_LAST_RESOURCE_SORT"; //$NON-NLS-1$

	private int sortCriteria = FlatSorter.PATH;
	
	/* *****************************************************************************
	 * Model element for the resources in this layout. They are displayed with filename and path
	 * onto the same line.
	 */
	public static class FullPathSyncInfoElement extends SyncInfoModelElement {
		public FullPathSyncInfoElement(IDiffContainer parent, SyncInfo info) {
			super(parent, info);
		}
		public String getName() {
			IResource resource = getResource();
			return resource.getName() + " - " + resource.getFullPath().toString(); //$NON-NLS-1$
		}
	}
	
	/**
	 * Sorter that sorts flat path elements using the criteria specified
	 * when the sorter is created
	 */
	public class FlatSorter extends ViewerSorter {
		
		private int resourceCriteria;
		
		// Resource sorting options
		public final static int NAME = 1;
		public final static int PATH = 2;
		public final static int PARENT_NAME = 3;
		
		public FlatSorter(int resourceCriteria) {
			this.resourceCriteria = resourceCriteria;
		}
		
		protected int classComparison(Object element) {
			if (element instanceof FullPathSyncInfoElement) {
				return 0;
			}
			return 1;
		}
		
		protected int compareClass(Object element1, Object element2) {
			return classComparison(element1) - classComparison(element2);
		}
		
		protected int compareNames(String s1, String s2) {
			return collator.compare(s1, s2);
		}
		
		/* (non-Javadoc)
		 * Method declared on ViewerSorter.
		 */
		public int compare(Viewer viewer, Object o1, Object o2) {

			if (o1 instanceof FullPathSyncInfoElement && o2 instanceof FullPathSyncInfoElement) {
				IResource r1 = ((FullPathSyncInfoElement)o1).getResource();
				IResource r2 = ((FullPathSyncInfoElement)o2).getResource();
				if(resourceCriteria == NAME) 
					return compareNames(r1.getName(), r2.getName());
				else if(resourceCriteria == PATH)
					return compareNames(r1.getFullPath().toString(), r2.getFullPath().toString());
				else if(resourceCriteria == PARENT_NAME)
					return compareNames(r1.getParent().getName(), r2.getParent().getName());
				else return 0;
			} else if (o1 instanceof ISynchronizeModelElement)
				return 1;
			else if (o2 instanceof ISynchronizeModelElement)
				return -1;
			
			return 0;
		}

		public int getResourceCriteria() {
			return resourceCriteria;
		}
	}
	
	/* *****************************************************************************
	 * Action that allows changing the model providers sort order.
	 */
	private class ToggleSortOrderAction extends Action {
		private int criteria;
		protected ToggleSortOrderAction(String name, int criteria) {
			super(name, Action.AS_RADIO_BUTTON);
			this.criteria = criteria;
			update();	
		}

		public void run() {
			if (isChecked() && sortCriteria != criteria) {
			    sortCriteria = criteria;
				String key = getSettingsKey();
				IDialogSettings pageSettings = getConfiguration().getSite().getPageSettings();
				if(pageSettings != null) {
					pageSettings.put(key, criteria);
				}
				update();
				FlatModelProvider.this.firePropertyChange(P_VIEWER_SORTER, null, null);
			}
		}
		
		public void update() {
			setChecked(sortCriteria == criteria);			
		}
		
		protected String getSettingsKey() {
		    return P_LAST_RESOURCESORT;
		}
	}
	
	/* *****************************************************************************
	 * Action group for this layout. It is added and removed for this layout only.
	 */
	public class FlatActionGroup extends SynchronizePageActionGroup {
		private MenuManager sortByResource;
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			sortByResource = new MenuManager(Policy.bind("FlatModelProvider.6"));	 //$NON-NLS-1$
			
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					ISynchronizePageConfiguration.SORT_GROUP, 
					sortByResource);
			
			// Ensure that the sort criteria of the provider is properly initialized
			FlatModelProvider.this.initialize(configuration);
			
			sortByResource.add( new ToggleSortOrderAction(Policy.bind("FlatModelProvider.8"), FlatSorter.PATH)); //$NON-NLS-1$
			sortByResource.add(new ToggleSortOrderAction(Policy.bind("FlatModelProvider.7"), FlatSorter.NAME)); //$NON-NLS-1$
			sortByResource.add(new ToggleSortOrderAction(Policy.bind("FlatModelProvider.9"), FlatSorter.PARENT_NAME)); //$NON-NLS-1$
		}

        /* (non-Javadoc)
		 * @see org.eclipse.team.ui.synchronize.SynchronizePageActionGroup#dispose()
		 */
		public void dispose() {
			sortByResource.dispose();
			sortByResource.removeAll();
			super.dispose();
		}
	}
	
    public FlatModelProvider(ISynchronizePageConfiguration configuration, SyncInfoSet set) {
        super(configuration, set);
        initialize(configuration);
    }

    public FlatModelProvider(AbstractSynchronizeModelProvider parentProvider, ISynchronizeModelElement modelRoot, ISynchronizePageConfiguration configuration, SyncInfoSet set) {
        super(parentProvider, modelRoot, configuration, set);
        initialize(configuration);
    }
    
    private void initialize(ISynchronizePageConfiguration configuration) {
		try {
			IDialogSettings pageSettings = getConfiguration().getSite().getPageSettings();
			if(pageSettings != null) {
				sortCriteria = pageSettings.getInt(P_LAST_RESOURCESORT);
			}
		} catch(NumberFormatException e) {
			// ignore and use the defaults.
		} 
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#createActionGroup()
     */
    protected SynchronizePageActionGroup createActionGroup() {
        return new FlatActionGroup();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider#getViewerSorter()
     */
    public ViewerSorter getViewerSorter() {
		return new FlatSorter(sortCriteria);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#buildModelObjects(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
     */
    protected IDiffElement[] buildModelObjects(ISynchronizeModelElement node) {
        if (node == getModelRoot());
        SyncInfo[] infos = getSyncInfoSet().getSyncInfos();
        List result = new ArrayList();
        for (int i = 0; i < infos.length; i++) {
            SyncInfo info = infos[i];
            result.add(createModelObject(node, info));
        }
        return (IDiffElement[]) result.toArray(new IDiffElement[result.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#handleResourceAdditions(org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent)
     */
    protected void handleResourceAdditions(ISyncInfoTreeChangeEvent event) {
        addResources(event.getAddedResources());
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#handleResourceRemovals(org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent)
     */
    protected void handleResourceRemovals(ISyncInfoTreeChangeEvent event) {
        IResource[] resources = event.getRemovedResources();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            removeFromViewer(resource);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider#getDescriptor()
     */
    public ISynchronizeModelProviderDescriptor getDescriptor() {
        return flatDescriptor;
    }

	protected void addResources(SyncInfo[] added) {
		for (int i = 0; i < added.length; i++) {
			SyncInfo info = added[i];
			ISynchronizeModelElement node = getModelObject(info.getLocal());
			if (node != null) {
				// Somehow the node exists. Remove it and read it to ensure
				// what is shown matches the contents of the sync set
				removeFromViewer(info.getLocal());
			}
			// Add the node to the root
			node = createModelObject(getModelRoot(), info);
		}
	}
	
	protected ISynchronizeModelElement createModelObject(ISynchronizeModelElement parent, SyncInfo info) {
	    SynchronizeModelElement newNode = new FullPathSyncInfoElement(parent, info);
		addToViewer(newNode);
		return newNode;
	}
}
