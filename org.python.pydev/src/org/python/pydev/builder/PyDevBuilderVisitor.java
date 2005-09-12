/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Visitors within pydev should be subclasses of this class.
 * 
 * They should be prepared for being reused to, as they are instantiated and reused for visiting many resources.
 * 
 * @author Fabio Zadrozny
 */
public abstract class PyDevBuilderVisitor {

    public static final int MAX_TO_VISIT_INFINITE = -1;

    /**
     * This field acts like a memory. 
     * 
     * It is set before a given resource is visited, and is maintained 
     * for each visitor for a class. 
     * 
     * In this way, we can keep from having to recreate some info (such as the ast) each time over and over. 
     */
    public HashMap<String, Object> memo;

    
    /**
     * Method to return whether a resource is an __init__
     * 
     * this is needed because when we create an __init__, all sub-folders 
     * and files on the same folder become valid modules.
     * 
     * @return whether the resource is an init resource
     */
    protected boolean isInitFile(IResource resource){
        return resource.getName().startsWith("__init__.");
    }
    
    /**
     * @param resource the resource we want to know about
     * @return true if it is in the pythonpath
     */
    protected boolean isInPythonPath(IResource resource){
        IProject project = resource.getProject();
        PythonNature nature = PythonNature.getPythonNature(project);
        if(project != null && nature != null){
            ICodeCompletionASTManager astManager = nature.getAstManager();
            if(astManager != null){
                ProjectModulesManager modulesManager = astManager.getProjectModulesManager();
                return modulesManager.isInPythonPath(resource, project);
            }
        }

        return false;
    }
    
    /**
     * @param initResource
     * @return all the IFiles that are below the folder where initResource is located.
     */
    protected IResource[] getInitDependents(IResource initResource){
        
        List<IResource> toRet = new ArrayList<IResource>();
        IContainer parent = initResource.getParent();
        
        try {
            fillWithMembers(toRet, parent);
            return toRet.toArray(new IResource[0]);
        } catch (CoreException e) {
            //that's ok, it might not exist anymore
            return new IResource[0];
        }
    }
    
	/**
     * @param toRet
     * @param parent
     * @throws CoreException
     */
    private void fillWithMembers(List<IResource> toRet, IContainer parent) throws CoreException {
        IResource[] resources = parent.members();
        
        for (int i = 0; i < resources.length; i++) {
            if(resources[i].getType() == IResource.FILE){
                toRet.add(resources[i]);
            }else if(resources[i].getType() == IResource.FOLDER){
                fillWithMembers(toRet, (IFolder)resources[i]);
            }
        }
    }



	/**
	 * 
	 * @return the maximun number of resources that it is allowed to visit (if this
	 * number is higher than the number of resources changed, this visitor is not called).
     */
    public int maxResourcesToVisit() {
        return MAX_TO_VISIT_INFINITE;
    }
	
    /**
     * if all the files below a folder that has an __init__.py just added or removed should 
     * be visited, this method should return true, otherwise it should return false 
     * 
     * @return false by default, but may be reimplemented in subclasses. 
     */
    public boolean shouldVisitInitDependency(){
        return false;
    }

    /**
     * Called when a resource is changed
     * 
     * @param resource to be visited.
     */
    public abstract boolean visitChangedResource(IResource resource, IDocument document);

    
    /**
     * Called when a resource is added. Default implementation calls the same method
     * used for change.
     * 
     * @param resource to be visited.
     */
    public boolean visitAddedResource(IResource resource, IDocument document){
        return visitChangedResource(resource, document);
    }

    /**
     * Called when a resource is removed
     * 
     * @param resource to be visited.
     */
    public abstract boolean visitRemovedResource(IResource resource, IDocument document);
    
}
