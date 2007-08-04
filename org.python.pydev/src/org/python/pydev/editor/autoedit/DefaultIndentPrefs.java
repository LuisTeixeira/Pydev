/*
 * Created on May 5, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.autoedit;

import org.python.pydev.core.cache.PyPreferencesCache;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;

public class DefaultIndentPrefs extends AbstractIndentPrefs {
    /** 
     * Cache for indentation string 
     */
    private String indentString = null;

    private boolean useSpaces;

    private int tabWidth;
    
	private static PyPreferencesCache cache;

    private static IIndentPrefs indentPrefs;
    
    public synchronized static IIndentPrefs get() {
        if(indentPrefs == null){
            indentPrefs = new DefaultIndentPrefs();
        }
        return indentPrefs;
    }
	
	private PyPreferencesCache getCache(){
    	if(cache == null){
    		cache = new PyPreferencesCache(PydevPlugin.getDefault().getPreferenceStore());
    	}
    	return cache;
	}
	
	/**
	 * Not singleton (each pyedit may force to use tabs or not).
	 */
	DefaultIndentPrefs(){
		PyPreferencesCache c = getCache();
		useSpaces = c.getBoolean(PydevPrefs.SUBSTITUTE_TABS);
		tabWidth = c.getInt(PydevPrefs.TAB_WIDTH, 4);
	}

    public boolean getUseSpaces() {
        PyPreferencesCache c = getCache();
		if(useSpaces != c.getBoolean(PydevPrefs.SUBSTITUTE_TABS)){
            useSpaces = c.getBoolean(PydevPrefs.SUBSTITUTE_TABS);
            regenerateIndentString();
        }
        return useSpaces;
    }

    public static int getStaticTabWidth(){
        PydevPlugin default1 = PydevPlugin.getDefault();
        if(default1 == null){
            return 4;
        }
        int w = default1.getPluginPreferences().getInt(PydevPrefs.TAB_WIDTH);
        if(w <= 0){ //tab width should never be 0 or less (in this case, let's make the default 4)
            w = 4;
        }
        return w;
    }
    
    public int getTabWidth() {
        PyPreferencesCache c = getCache();
        if(tabWidth != c.getInt(PydevPrefs.TAB_WIDTH, 4)){
            tabWidth = c.getInt(PydevPrefs.TAB_WIDTH, 4);
            regenerateIndentString();
        }
        return tabWidth;
    }

    public void regenerateIndentString(){
    	PyPreferencesCache c = getCache();
        c.clear(PydevPrefs.TAB_WIDTH);
    	c.clear(PydevPrefs.SUBSTITUTE_TABS);
        indentString = super.getIndentationString();
    }
    /**
     * This class also puts the indentation string in a cache and redoes it 
     * if the preferences are changed.
     * 
     * @return the indentation string. 
     */
    public String getIndentationString() {
        if (indentString == null){
            regenerateIndentString();
        }

        return indentString;
    }

    /** 
     * @see org.python.pydev.editor.autoedit.IIndentPrefs#getAutoParentesis()
     */
    public boolean getAutoParentesis() {
        return getCache().getBoolean(PydevPrefs.AUTO_PAR);
    }
    
    public boolean getIndentToParLevel() {
    	return getCache().getBoolean(PydevPrefs.AUTO_INDENT_TO_PAR_LEVEL);
    }

    public boolean getAutoColon() {
        return getCache().getBoolean(PydevPrefs.AUTO_COLON);
    }

    public boolean getAutoBraces() {
        return getCache().getBoolean(PydevPrefs.AUTO_BRACES);
    }

    public boolean getAutoWriteImport() {
        return getCache().getBoolean(PydevPrefs.AUTO_WRITE_IMPORT_STR);
    }

    public boolean getSmartIndentPar() {
    	return getCache().getBoolean(PydevPrefs.SMART_INDENT_PAR);
    }

	public boolean getAutoAddSelf() {
		return getCache().getBoolean(PydevPrefs.AUTO_ADD_SELF);
	}

    public boolean getAutoDedentElse() {
        return getCache().getBoolean(PydevPrefs.AUTO_DEDENT_ELSE);
    }


}