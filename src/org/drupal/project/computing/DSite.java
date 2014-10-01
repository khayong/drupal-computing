package org.drupal.project.computing;

import java.util.List;
import java.util.logging.Logger;

/**
 * <p>This is the super class to any Drupal site. A Drupal Hybrid Computing application will use this class and
 * sub-classes to connect to Drupal sites. The suggested way to pass data between applications and Drupal site is
 * through the DRecord objects (maps to a Computing Entity). You would have your Drupal module
 * write input data to a record, and then your application read it and write results back to the output field in the
 * record, and your Drupal module can read data back to the database.</p>
 *
 * <p>It doesn't handle deleting of old records, which is handled by Drupal. Here we don't require DSite to offer
 * nodeLoad(), nodeSave(), or userLoad(), etc, because of security concerns. If your application needs to read nodes,
 * the Drupal part of your application will write the node info into the "input" field of computing_record, and then
 * your Java application can read it. However, we don't prevent subclasses to provide a nodeLoad() method. If you use
 * DDrushSite in particular, you can call any Drupal API with drush. Also, you can use DDatabase to access Drupal
 * database directly, but it's not recommended.</p>
 *
 * <p>A Drupal site doesn't need to know the DApplication, or DConfig.</p>
 *
 * <p>Some sub-class implementations:</p>
 * <ul>
 *     <li>DServicesSite: Connect to Drupal services endpoint. Recommended approach.</li>
 *     <li>DDrushSite: Connect to Drupal via Drush. Perhaps create security issues. But more powerful.</li>
 *     <li>DSqlSite (not implemented and not recommended): Connect to Drupal via direct database access (JDBC)</li>
 *     <li>DOrmSite (not implemented and not recommended): Connect to Drupal via direct database access on top of ORM layer (Hibernate/JPA)</li>
 * </ul>
 */
abstract public class DSite {

    protected Logger logger = DUtils.getInstance().getPackageLogger();


    /**
     * Get one available computing record from Drupal to process. Drupal will handle the logic of providing the record.
     *
     * @param appName
     * @return A computing record to handle, or NULL is none is found.
     * @throws DSiteException
     */
    abstract public DRecord claimRecord(String appName) throws DSiteException;


    /**
     * After agent finishes computation, return the results to Drupal.
     *
     * @param record
     * @throws DSiteException
     */
    abstract public void finishRecord(DRecord record) throws DSiteException;

    /**
     * The first active record for the application. Use this for the "first" running mode.
     * Sub-classes can use whatever logic to implement this method.
     *
     * @param appName
     * @return One active record for processing or null if no record is found.
     * @deprecated Don't use this. Use getRecord() instead.
     */
    public DRecord getNextRecord(String appName) throws DSiteException {
        return queryReadyRecords(appName).get(0);
    }

    /**
     * Active records are those without a "status" code. All handled records has a status code.
     * Usually, you would process the active records that has "RDY" control code.
     * Other records might have other control code, and would be processed differently.
     *
     * @return All active records that are not handled.
     * @param appName
     * @deprecated Don't use this. Use getRecord() instead.
     */
    abstract public List<DRecord> queryReadyRecords(String appName) throws DSiteException;

    /**
     * Save the updated record in the database.
     * @param record
     */
    abstract public void updateRecord(DRecord record) throws DSiteException;


    /**
     * Update only the specified field of the record.
     * @param record
     * @param fieldName
     */
    abstract public void updateRecordField(DRecord record, String fieldName) throws DSiteException;


    /**
     * Save the new record Drupal using the data in the parameter.
     *
     *
     * @param record The newly created record. record.isSave() has too be true.
     * @return The database record ID of the newly created record.
     */
    abstract public long createRecord(DRecord record) throws DSiteException;

    /**
     * Load one record according to its ID. Return null if there's no such a DRecord with the given id.
     *
     * @param id
     * @return
     */
    abstract public DRecord loadRecord(long id) throws DSiteException;


    /**
     * Check whether connection to Drupal site is established.
     *
     * @return true if the connection is valid. will not throw exceptions.
     */
    public boolean checkConnection() {
        try {
            String drupalVersion = getDrupalVersion();
            logger.info("Drupal version: " + drupalVersion);
            return true;
        } catch (DSiteException e) {
            logger.warning("Cannot connect to Drupal site");
            return false;
        } catch (DRuntimeException e) {
            logger.severe("Check connection unexpected error: " + e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * @return The version of Drupal for this site.
     */
    abstract public String getDrupalVersion() throws DSiteException;

    /**
     * Execute Drupal API "variable_get()".
     * Perhaps we should use generic method, but implementation is hard.
     * If defaultValue is a JsonElement, will output JsonElement too.
     *
     * @param name
     * @param defaultValue
     * @return
     */
    abstract public Object variableGet(String name, Object defaultValue) throws DSiteException;

    /**
     * Execute Drupal API "variable_set()"
     * @param name
     * @param value
     */
    abstract public void variableSet(String name, Object value) throws DSiteException;

    /**
     * @return Drupal site's current timestamp. Equivalent to PHP time().
     */
    abstract public long getTimestamp() throws DSiteException;
}
