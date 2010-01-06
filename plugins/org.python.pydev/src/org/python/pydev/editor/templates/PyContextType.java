/*
 * Created on Aug 6, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.templates;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;

/**
 * @author Fabio Zadrozny
 */
public class PyContextType extends TemplateContextType {

    /**
     * Context type used for code-completions
     */
    public static final String PY_COMPLETIONS_CONTEXT_TYPE = "org.python.pydev.editor.templates.python";
    
    /**
     * Context type used for new modules (wizard)
     */
    public static final String PY_MODULES_CONTEXT_TYPE = "org.python.pydev.editor.templates.python.modules";

    private IPythonInterpreter interpreter;
    
    /**
     * Creates a new XML context type. 
     */
    public PyContextType() {
		interpreter = JythonPlugin.newPythonInterpreter();
        addGlobalResolvers();
        
    }

    private void addGlobalResolvers() {
        addResolver(new GlobalTemplateVariables.Cursor());
        addResolver(new GlobalTemplateVariables.WordSelection());
        addResolver(new GlobalTemplateVariables.LineSelection());
        addResolver(new GlobalTemplateVariables.Dollar());
        addResolver(new GlobalTemplateVariables.Date());
        addResolver(new GlobalTemplateVariables.Year());
        addResolver(new GlobalTemplateVariables.Time());
        addResolver(new GlobalTemplateVariables.User());
        
        HashMap<String, Object> locals = new HashMap<String, Object>();
        locals.put("py_context_type", this);
        
        //execute all the files that start with 'pytemplate' that are located beneath
        //the org.python.pydev.jython/jysrc directory and some user specified dir (if any).
        JythonPlugin.execAll(locals, "pytemplate", interpreter); 
        
    }
    



}
