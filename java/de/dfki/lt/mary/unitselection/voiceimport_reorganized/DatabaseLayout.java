/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.File;
import java.util.Properties;

import de.dfki.lt.mary.MaryProperties;

/**
 * The DatabaseLayout class registers the base directory of a voice database,
 * as well as the various subdirectories where the various voice database
 * components should be stored or read from.
 * 
 * @author sacha
 *
 */
public class DatabaseLayout 
{   
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /**
     * Constructor for a new database layout.
     * 
     */
    public DatabaseLayout() {
        initDefaultProps();
    }
    
    /**
     * Initializes a default database layout.
     *
     */
    private void initDefaultProps() {
        
        /* baseName : the name of the root directory for the database */
        setIfDoesntExist( "db.baseName", "." );
        
        setIfDoesntExist( "db.wavSubDir", "wav" );
        setIfDoesntExist( "db.wavExtension", ".wav" );
        
        setIfDoesntExist( "db.lpcSubDir", "lpc" );
        setIfDoesntExist( "db.lpcExtension", ".lpc" );
        
        setIfDoesntExist( "db.pitchmarksSubDir", "pm" );
        setIfDoesntExist( "db.pitchmarksExtension", ".pm" );
        
        setIfDoesntExist( "db.melcepSubDir", "mcep" );
        setIfDoesntExist( "db.melcepExtension", ".mcep" );
        
        setIfDoesntExist( "db.timelineSubDir", "mary_timelines" );
        setIfDoesntExist( "db.timelineExtension", ".bin" );
        
        setIfDoesntExist( "db.lpcTimelineBaseName", "timeline_quantized_lpc+res" );
        setIfDoesntExist( "db.melcepTimelineBaseName", "timeline_mcep" );
        
        setIfDoesntExist( "db.featuresSubDir", "mary_features" );
        setIfDoesntExist( "db.featuresExtension", ".bin" );
        
        setIfDoesntExist( "db.cartsSubDir", "mary_carts" );
        
        setIfDoesntExist( "db.targetFeaturesBaseName", "targetFeatures" );
        setIfDoesntExist( "db.joinCostFeaturesBaseName", "joinCostFeatures" );
        setIfDoesntExist( "db.unitFileBaseName", "units" );
    }
    
    /**
     * Sets a property if this property has not been set before. This is used to preserve
     * user overrides if they were produced before the instanciation of the databaseLayout.
     * 
     * @param propertyName The property name.
     * @param propertyVal The property value.
     */
    public static void setIfDoesntExist( String propertyName, String propertyVal ) {
        if ( System.getProperty( propertyName ) == null ) System.setProperty( propertyName, propertyVal );
    }
    
    /*****************/
    /* OTHER METHODS */
    /*****************/
    
    /* Various accessors and absolute path makers: */

    public String baseName() { return( System.getProperty( "db.baseName") ); }
    
    public String wavDirName() { return( System.getProperty( "db.baseName" ) + System.getProperty( "file.separator" )
            + System.getProperty( "db.wavSubDir" ) + System.getProperty( "file.separator" ) ); }
    public String wavExt() { return( System.getProperty( "db.wavExtension") ); }
    
    
    public String lpcDirName() { return( System.getProperty( "db.baseName" ) + System.getProperty( "file.separator" )
            + System.getProperty( "db.lpcSubDir" ) + System.getProperty( "file.separator" ) ); }
    public String lpcExt() { return( System.getProperty( "db.lpcExtension") ); }
    
    public String timelineDirName() { return( System.getProperty( "db.baseName" ) + System.getProperty( "file.separator" )
            + System.getProperty( "db.timelineSubDir" ) + System.getProperty( "file.separator" ) ); }
    public String timelineExt() { return( System.getProperty( "db.timelineExtension") ); }
    
    public String pitchmarksDirName() { return( System.getProperty( "db.baseName" ) + System.getProperty( "file.separator" )
            + System.getProperty( "db.pitchmarksSubDir" ) + System.getProperty( "file.separator" ) ); }
    public String pitchmarksExt() { return( System.getProperty( "db.pitchmarksExtension") ); }
    
    public String melcepDirName() { return( System.getProperty( "db.baseName" ) + System.getProperty( "file.separator" )
            + System.getProperty( "db.melcepSubDir" ) + System.getProperty( "file.separator" ) ); }
    public String melcepExt() { return( System.getProperty( "db.melcepExtension") ); }
    
    public String cartsDirName() { return( System.getProperty( "db.baseName" ) + System.getProperty( "file.separator" )
            + System.getProperty( "db.cartsSubDir") ); }
    
    /* File name for the LPC+residual timeline */
    public String lpcTimelineFileName() {
        String ret = System.getProperty( "db.lpcTimelineFileName" );
        if ( ret != null ) return( ret );
        /* else: */
        return( timelineDirName() + System.getProperty( "db.lpcTimelineBaseName" ) + timelineExt() );
    }
    
    /* File name for the mel cepstrum timeline */
    public String melcepTimelineFileName() {
        String ret = System.getProperty( "db.melcepTimelineFileName" );
        if ( ret != null ) return( ret );
        /* else: */
        return( timelineDirName() + System.getProperty( "db.melcepTimelineBaseName" ) + timelineExt() );
    }
    
    /* File name for the target features file */
    public String targetFeaturesFileName() {
        String ret = System.getProperty( "db.targetFeaturesFileName" );
        if ( ret != null ) return( ret );
        /* else: */
        return( System.getProperty( "db.baseName" ) + System.getProperty( "file.separator" )
                + System.getProperty( "db.featuresSubDir" ) + System.getProperty( "file.separator" )
                + System.getProperty( "db.targetFeaturesBaseName" ) + System.getProperty( "db.featuresExtension" ) );
    }
    
    /* File name for the join cost features file */
    public String joinCostFeaturesFileName() {
        String ret = System.getProperty( "db.joinCostFeaturesFileName" );
        if ( ret != null ) return( ret );
        /* else: */
        return( System.getProperty( "db.baseName" ) + System.getProperty( "file.separator" )
                + System.getProperty( "db.featuresSubDir" ) + System.getProperty( "file.separator" )
                + System.getProperty( "db.joinCostFeaturesBaseName" ) + System.getProperty( "db.featuresExtension" ) );
    }

    /* File name for the unit file */
    public String unitFileName() {
        String ret = System.getProperty( "db.unitFileName" );
        if ( ret != null ) return( ret );
        /* else: */
        return( System.getProperty( "db.baseName" ) + System.getProperty( "file.separator" )
                + System.getProperty( "db.featuresSubDir" ) + System.getProperty( "file.separator" )
                + System.getProperty( "db.unitFileBaseName" ) + System.getProperty( "db.featuresExtension" ) );
    }
    
}
