package com.strongloop.android.remoting.adapters;

/**
 * A single item within a larger SLRESTContract, encapsulation a single route's
 * verb and pattern, e.g. GET /widgets/:id.
 */
public class RestContractItem {

    private final String pattern;
    private final String verb;
    private final boolean isMultipart;

    /**
     * Creates a new item encapsulating the given pattern and the default verb,
     * <code>"POST"</code>.
     * @param pattern The pattern corresponding to this route, e.g.
     * <code>"/widgets/:id"</code>.
     */
    public RestContractItem(String pattern) {
        this(pattern, "POST");
    }

    /**
     * Creates a new item encapsulating the given pattern and verb.
     * @param pattern The pattern corresponding to this route, e.g.
     * <code>"/widgets/:id"</code>.
     * @param verb The verb corresponding to this route, e.g.
     * <code>"GET"</code>.
     */
    public RestContractItem(String pattern, String verb, boolean isMultipart) {
        this.pattern = pattern;
        this.verb = verb;
        this.isMultipart = isMultipart;
    }

    /**
     * Creates a new item encapsulating the given pattern and verb.
     * @param pattern The pattern corresponding to this route, e.g.
     * <code>"/widgets/:id"</code>.
     * @param verb The verb corresponding to this route, e.g.
     * <code>"GET"</code>.
     */
    public RestContractItem(String pattern, String verb) {
        this.pattern = pattern;
        this.verb = verb;
        this.isMultipart = false;
    }
    
    /**
     * Gets the pattern corresponding to this route, e.g.
     * <code>"/widgets/:id"</code>.
     * @return the pattern.
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Gets the verb corresponding to this route, e.g. <code>"GET"</code>.
     * @return the verb.
     */
    public String getVerb() {
        return verb;
    }
    
    /**
     * Gets a boolean that indicates if the item is a multipart form mime type.
     * @return true if the item is multipart
     */
    public boolean getIsMultipart() {
        return isMultipart;
    }
}
