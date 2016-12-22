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
package marytts.client;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * GUI for a set of audio effects.
 * 
 * @author Oytun T&uuml;rk
 */
public class AudioEffectsBoxGUI {
	private AudioEffectsBoxData data;
	public AudioEffectControlGUI[] effectControls;

	public JPanel mainPanel;
	public JLabel effectsBoxLabel;
	public JScrollPane scrollPane;
	public JPanel effectControlsPanel;

	public AudioEffectsBoxGUI(String availableEffects) {
		data = new AudioEffectsBoxData(availableEffects);

		if (availableEffects != null && !availableEffects.equals("")) {
			mainPanel = new JPanel();
			effectsBoxLabel = new JLabel("Audio Effects:");
			effectControlsPanel = new JPanel();

			if (data.getTotalEffects() > 0) {
				effectControls = new AudioEffectControlGUI[data.getTotalEffects()];

				for (int i = 0; i < effectControls.length; i++)
					effectControls[i] = new AudioEffectControlGUI(data.getControlData(i));
			} else
				effectControls = null;
		} else
			effectControls = null;
	}

	public AudioEffectsBoxData getData() {
		return data;
	}

	public boolean hasEffects() {
		return data.hasEffects();
	}

	public void show() {
		mainPanel.removeAll();
		mainPanel.validate();

		effectControlsPanel.removeAll();
		effectControlsPanel.validate();

		GridBagLayout g = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		mainPanel.setLayout(g);

		c.fill = GridBagConstraints.VERTICAL;
		g.setConstraints(mainPanel, c);

		c.gridx = 0;
		c.gridy = 0;
		c.ipadx = 200;
		c.ipady = 20;
		c.fill = GridBagConstraints.CENTER;
		g.setConstraints(effectsBoxLabel, c);
		mainPanel.add(effectsBoxLabel);

		c.gridx = 0;
		c.gridy = 1;
		c.ipadx = 0;
		c.ipady = 0;
		g.setConstraints(effectControlsPanel, c);
		mainPanel.add(effectControlsPanel);

		if (effectControls != null && effectControls.length > 0) {
			effectControlsPanel.setLayout(g);

			c.gridx = 0;
			c.fill = GridBagConstraints.BOTH;

			int totalShown = 0;
			for (int i = 0; i < effectControls.length; i++) {
				if (effectControls[i].getVisible()) {
					c.gridy = totalShown;
					g.setConstraints(effectControls[i].mainPanel, c);
					effectControlsPanel.add(effectControls[i].mainPanel);
					effectControls[i].show();

					totalShown++;
				}
			}
		}

		// Add the scroll pane
		c.gridx = 0;
		c.gridy = 1;
		c.ipadx = 300;
		c.ipady = 105;
		scrollPane = new JScrollPane(effectControlsPanel);
		scrollPane.setViewportView(effectControlsPanel);
		g.setConstraints(scrollPane, c);
		mainPanel.add(scrollPane);
		effectControlsPanel.validate();
		mainPanel.validate();
	}
}
