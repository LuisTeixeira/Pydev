/*
 * Created on 05/08/2005
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.service.environment.Constants;
import org.python.pydev.core.REF;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.IPythonPathNature;
import org.python.pydev.plugin.nature.PythonNature;

public abstract class SimpleRunner {

    /**
     * Just execute the string. Does nothing else.
     */
    public Process createProcess(String executionString, File workingDir) throws IOException {
        return Runtime.getRuntime().exec(executionString, null, workingDir);
    }

    /**
     * THIS CODE IS COPIED FROM org.eclipse.debug.internal.core.LaunchManager
     * 
     * changed so that we always set the PYTHONPATH in the environment
     * 
     * @return the system environment with the PYTHONPATH env variable added for a given project (if it is null, return it with the
     * default PYTHONPATH added).
     */
    public String[] getEnvironment(IProject project) throws CoreException {
        PythonNature pythonNature = PythonNature.getPythonNature(project);
        if(pythonNature == null){ //no associated nature in the project... just get the default env
            return getDefaultSystemEnvAsArray();
        }
        
        
        String pythonPathEnvStr = "";
    	try {
            
            if (PydevPlugin.getInterpreterManager(pythonNature).hasInfoOnDefaultInterpreter(pythonNature)){ //check if we have a default interpreter.
                pythonPathEnvStr = makePythonPathEnvString(project);
            }
        } catch (Exception e) {
            return null; //we cannot get it
        }
    
    	DebugPlugin defaultPlugin = DebugPlugin.getDefault();
    	if(defaultPlugin != null){
            Map<String,String> env = getDefaultSystemEnv(defaultPlugin);		
    
    		env.put("PYTHONPATH", pythonPathEnvStr); //put the environment
    		return getMapEnvAsArray(env);
    	}else{
    	    return null;
    	}
    }

    /**
     * @return an array with the env variables for the system with the format xx=yy  
     */
    public String[] getDefaultSystemEnvAsArray() throws CoreException {
        Map defaultSystemEnv = getDefaultSystemEnv();
        if(defaultSystemEnv != null){
            return getMapEnvAsArray(defaultSystemEnv);
        }
        return null;
    }

    /**
     * @return a map with the env variables for the system  
     */
    public Map getDefaultSystemEnv() throws CoreException {
        DebugPlugin defaultPlugin = DebugPlugin.getDefault();
        return getDefaultSystemEnv(defaultPlugin);
    }

    /**
     * @return a map with the env variables for the system  
     */
    private Map<String,String> getDefaultSystemEnv(DebugPlugin defaultPlugin) throws CoreException {
        if(defaultPlugin != null){
            ILaunchManager launchManager = defaultPlugin.getLaunchManager();
    
            // build base environment
            Map<String,String> env = new HashMap<String,String>();
            env.putAll(launchManager.getNativeEnvironment());
            
            // Add variables from config
            boolean win32= isWindowsPlatform();
            for(Iterator iter= env.entrySet().iterator(); iter.hasNext(); ) {
            	Map.Entry entry= (Map.Entry) iter.next();
            	String key= (String) entry.getKey();
            	if (win32) {
            		// Win32 vars are case insensitive. Uppercase everything so
            		// that (for example) "pAtH" will correctly replace "PATH"
            		key= key.toUpperCase();
            	}
            	String value = (String) entry.getValue();
            	// translate any string substitution variables
            	String translated = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
            	env.put(key, translated);
            }
            return env;
        }
        return null;
    }

    /**
     * @return whether we are in windows or not
     */
    public static boolean isWindowsPlatform() {
        try {
            return Platform.getOS().equals(Constants.OS_WIN32);
        } catch (NullPointerException e) {
            String env = System.getenv("os");
            if(env.toLowerCase().indexOf("win") != -1){
                return true;
            }
            return false;
        }
    }

    /**
     * Formats the script to the windows environment (if needed), adding '"' to the start and end of the parameter
     * 
     * @param param the parameter that might be formatted
     * 
     * @return the formatted parameter
     */
    protected String formatParamToExec(String param) {
        if(isWindowsPlatform()){ //in windows, we have to put python "path_to_file.py"
            if(param.startsWith("\"") == false){
                param = "\""+param+"\"";
            }
        }
        return param;
    }

    /**
     * Creates a string that can be passed as the PYTHONPATH 
     * 
     * @param project the project we want to get the settings from. If it is null, the system pythonpath is returned 
     * @return a string that can be used as the PYTHONPATH env variable
     */
    public String makePythonPathEnvString(IProject project) {
        List paths;
        if(project == null){
            return ""; //no pythonpath can be gotten (set to empty, so that the default is gotten)
        }
        
        //if we have a project, get its complete pythonpath
        IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
    	paths = pythonPathNature.getCompleteProjectPythonPath();
    
        String separator = getPythonPathSeparator();
    	StringBuffer pythonpath = new StringBuffer();
        for (int i = 0; i < paths.size(); i++) {
    		if (i > 0){
    			pythonpath.append(separator);
    		}
    		pythonpath.append(REF.getFileAbsolutePath(new File((String) paths.get(i))));
    	}
        return pythonpath.toString();
    }
    
    /**
     * @return the separator for the pythonpath variables (system dependent)
     */
    public String getPythonPathSeparator(){
        boolean win32= isWindowsPlatform();
        String separator = ";";
        if(!win32){
            separator = ":"; //system dependent
        }
        return separator;
    }

    /**
     * @param env a map that will have its values formatted to xx=yy, so that it can be passed in an exec
     * @return an array with the formatted map
     */
    private String[] getMapEnvAsArray(Map env) {
        List<String> strings= new ArrayList<String>(env.size());
    	for(Iterator iter= env.entrySet().iterator(); iter.hasNext(); ) {
    		Map.Entry entry = (Map.Entry) iter.next();
    		StringBuffer buffer= new StringBuffer((String) entry.getKey());
    		buffer.append('=').append((String) entry.getValue());
    		strings.add(buffer.toString());
    	}
    	
    	return strings.toArray(new String[strings.size()]);
    }

    /**
     * shortcut
     */
    public String runAndGetOutput(String executionString, File workingDir, IProgressMonitor monitor) {
        return runAndGetOutput(executionString, workingDir, null, monitor);
    }
    
    /**
     * shortcut
     */
    public String runAndGetOutput(String executionString, File workingDir) {
        return runAndGetOutput(executionString, workingDir, null, new NullProgressMonitor());
    }
    
    /**
     * shortcut
     */
    public String runAndGetOutput(String executionString, File workingDir, IProject project) {
        return runAndGetOutput(executionString, workingDir, project, new NullProgressMonitor());
    }

    /**
     * shortcut
     */
    public String runAndGetOutput(String script, String args, File workingDir) {
        return runAndGetOutput(script, args, workingDir, null);
    }

    /**
     * This is the method that actually does the running (all others are just 'shortcuts' to this one).
     * 
     * @param executionString this is the string that will be executed
     * @param workingDir this is the directory where the execution will happen
     * @param project this is the project that is related to the run (it is used to get the environment for the shell we are going to
     * execute with the correct pythonpath environment variable).
     * @param monitor this is the monitor used to communicate the progress to the user
     * 
     * @return the string that is the output of the process (stdout).
     */
    public abstract String runAndGetOutput(String executionString, File workingDir, IProject project, IProgressMonitor monitor);

    /**
     * Execute the script specified with the interpreter for a given project 
     * 
     * @param script the script we will execute
     * @param args the arguments to pass to the script
     * @param workingDir the working directory
     * @param project the project that is associated to this run
     * 
     * @return a string with the output of the process (stdout)
     */
    public abstract String runAndGetOutput(String script, String args, File workingDir, IProject project);

}
