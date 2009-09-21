/*
 * VoiceUpdateListener.java
 *
 * Created on 21. September 2009, 10:27
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package marytts.tools.install;

/**
 *
 * @author marc
 */
public interface VoiceUpdateListener
{
    /**
     * Take note of the fact that the current language has changed and the list of voices needs updating.
     */
    public void updateVoices(LanguageComponentDescription currentLanguage, boolean forceUpdate);
    
}
