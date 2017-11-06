/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.modules.nlp;

import java.util.Locale;
import marytts.data.Utterance;
import marytts.modules.MaryModule;

import marytts.config.MaryProperties;
import marytts.data.SupportedSequenceType;

import marytts.MaryException;

import org.apache.logging.log4j.core.Appender;
/**
 * Dummy modules to support new language (for phone durations and phone f0)
 *
 * @author Sathish Pammi
 */

public class DummyTokens2Words extends MaryModule {
    public DummyTokens2Words() {
        this((Locale) null);
    }

    /**
     * Constructor to be called with instantiated objects.
     *
     * @param locale
     *            locale
     */
    public DummyTokens2Words(String locale) {
        super("DummyTokens2Words", new Locale(locale));
    }

    /**
     * Constructor to be called with instantiated objects.
     *
     * @param locale
     *            locale
     */
    public DummyTokens2Words(Locale locale) {
        super("DummyTokens2Words", locale);
    }

    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public void checkInput(Utterance utt) throws MaryException {
        if (!utt.hasSequence(SupportedSequenceType.WORD)) {
            throw new MaryException("Word sequence is missing", null);
        }
    }

    /**
     * Perform this module's processing on abstract "Utterance" input
     * <code>d</code>. Subclasses need to make sure that the
     * <code>process()</code> method is thread-safe, because in server-mode, it
     * will be called from different threads at the same time. A sensible way to
     * do this seems to be not to use any global or static variables, or to use
     * them read-only.
     * <p>
     *
     * @return A Utterance object of type <code>outputType()</code> encapsulating
     *         the processing result.
     *         <p>
     *         This method just returns its input. Subclasses should override
     *         this.
     */
    public Utterance process(Utterance utt, MaryProperties configuration, Appender app) throws Exception {
        return utt; // just return input.
    }

}
