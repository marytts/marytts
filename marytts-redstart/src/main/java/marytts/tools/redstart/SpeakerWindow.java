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
package marytts.tools.redstart;
import java.awt.Font;
import java.awt.Toolkit;

import javax.swing.ImageIcon;

/**
 *
 * @author  Mat Wilson <matwils@gmail.com>
 */
public class SpeakerWindow extends javax.swing.JFrame {
    
    private boolean showPromptCount;
    private Font defaultPromptFont;
    
    /** Creates new form SpeakerWindow */
    public SpeakerWindow() {
        
        initComponents();  // Auto-generated in NetBeans
        
        defaultPromptFont = jTextPane_PromptDisplay.getFont();
        
        // Set icon image in upper left corner to the 16 x 16 pixel image
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(IconSet.LOGO_16x16_URL));         
        
    }
    
    public void setShowPromptCount(boolean flag) {
        showPromptCount = flag;
        showOrHidePromptCount();
    }
   
    /** Updates the prompt display with the current prompt text
     *  @param promptText The current prompt text for the speaker to read
     */
    public void updatePromptDisplay(String text, String nextSentence, boolean redAlertMode) {
        jTextPane_PromptDisplay.setFont(defaultPromptFont);
        LookAndFeel.centerPromptText(this.jTextPane_PromptDisplay, text, redAlertMode);
        LookAndFeel.centerPromptText(this.jTextPane_nextSentence, nextSentence, redAlertMode);
    }

    
    /** Updates status icon in Speaker window
     *  @param statusIcon The icon to use (play, record, stop)
     */
    public void updateSessionStatus(ImageIcon statusIcon) {
        jLabel_SessionStatus.setText("");
        jLabel_SessionStatus.setIcon(statusIcon);
    }
    
    public void showOrHidePromptCount() {
        
        // Display accordingly
        this.jProgressBar_SpeakerProgress.setVisible(showPromptCount);
        this.jLabel_PromptCount.setVisible(showPromptCount);
        this.jLabel_PromptTotal.setVisible(showPromptCount);        
                
    }   
    
    public void updatePromptCount(int promptCount) {
                
        this.showOrHidePromptCount();
        
        // Update the count
        String countString = String.valueOf(promptCount);
        jLabel_PromptCount.setText(countString);
        
    }
    
    public void updatePromptTotal(int promptTotal) {
        String totalString = "/ " + String.valueOf(promptTotal);
        jLabel_PromptTotal.setText(totalString);
    }
    
    public void setupProgressBar(int promptTotal) {
        this.jProgressBar_SpeakerProgress.setMaximum(promptTotal);
        this.updatePromptTotal(promptTotal);
    }
        
    public void updateProgressBar(int promptCount) {
        this.jProgressBar_SpeakerProgress.setValue(promptCount);       
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanel_SpeakerWindow = new javax.swing.JPanel();
        jTextPane_PromptDisplay = new javax.swing.JTextPane();
        jLabel_SessionStatus = new javax.swing.JLabel();
        jProgressBar_SpeakerProgress = new javax.swing.JProgressBar();
        jLabel_PromptCount = new javax.swing.JLabel();
        jLabel_PromptTotal = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane_nextSentence = new javax.swing.JTextPane();

        setTitle("Redstart - Speaker Window");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jTextPane_PromptDisplay.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jTextPane_PromptDisplay.setEditable(false);
        jTextPane_PromptDisplay.setFont(new java.awt.Font("Tahoma", 0, 36));
        jTextPane_PromptDisplay.setText("This is a long and boring test sentence, the only purpose of which is to see how to break between lines without making any difference across the windows.");
        jTextPane_PromptDisplay.setAutoscrolls(false);

        jLabel_SessionStatus.setIcon(new javax.swing.ImageIcon(getClass().getResource("/marytts/tools/redstart/stopped_64x64.png")));

        jProgressBar_SpeakerProgress.setFocusable(false);

        jLabel_PromptCount.setFont(new java.awt.Font("Tahoma", 1, 24));
        jLabel_PromptCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel_PromptCount.setText("1999");
        jLabel_PromptCount.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jLabel_PromptCount.setPreferredSize(new java.awt.Dimension(64, 64));

        jLabel_PromptTotal.setFont(new java.awt.Font("Tahoma", 1, 24));
        jLabel_PromptTotal.setForeground(java.awt.Color.gray);
        jLabel_PromptTotal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel_PromptTotal.setText("/ 2012");
        jLabel_PromptTotal.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel_PromptTotal.setPreferredSize(new java.awt.Dimension(64, 64));

        jScrollPane1.setBorder(null);
        jTextPane_nextSentence.setBackground(new java.awt.Color(245, 245, 245));
        jTextPane_nextSentence.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jTextPane_nextSentence.setFont(new java.awt.Font("Tahoma", 0, 24));
        jTextPane_nextSentence.setForeground(new java.awt.Color(50, 50, 50));
        jTextPane_nextSentence.setText("This is a long and boring test sentence, the only purpose of which is to see how to break between lines without making any difference across the windows.");
        jScrollPane1.setViewportView(jTextPane_nextSentence);

        org.jdesktop.layout.GroupLayout jPanel_SpeakerWindowLayout = new org.jdesktop.layout.GroupLayout(jPanel_SpeakerWindow);
        jPanel_SpeakerWindow.setLayout(jPanel_SpeakerWindowLayout);
        jPanel_SpeakerWindowLayout.setHorizontalGroup(
            jPanel_SpeakerWindowLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_SpeakerWindowLayout.createSequentialGroup()
                .add(jPanel_SpeakerWindowLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel_SpeakerWindowLayout.createSequentialGroup()
                        .add(jProgressBar_SpeakerProgress, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 622, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 77, Short.MAX_VALUE)
                        .add(jLabel_PromptCount, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel_PromptTotal, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 86, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(21, 21, 21))
                    .add(jPanel_SpeakerWindowLayout.createSequentialGroup()
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 645, Short.MAX_VALUE)
                        .add(233, 233, 233)))
                .add(jLabel_SessionStatus))
            .add(jTextPane_PromptDisplay, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 942, Short.MAX_VALUE)
        );
        jPanel_SpeakerWindowLayout.setVerticalGroup(
            jPanel_SpeakerWindowLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_SpeakerWindowLayout.createSequentialGroup()
                .add(jTextPane_PromptDisplay, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_SpeakerWindowLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel_SessionStatus)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_SpeakerWindowLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel_SpeakerWindowLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel_PromptCount, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jLabel_PromptTotal, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jProgressBar_SpeakerProgress, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(28, 28, 28))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel_SpeakerWindow, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel_SpeakerWindow, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        //this.updatePromptDisplay(this.promptText);
    }//GEN-LAST:event_formComponentResized

    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // Call method to deselect Speaker Window in Window menu of Admin Window
        // How do I reference the instance of AdminWindow that created the Speaker Window object?
        // AdminWindow.deselectSpeakerWindow();
        // Or do we need to do this through a listener in AdminWindow?
    }//GEN-LAST:event_formWindowClosing
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SpeakerWindow().setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel_PromptCount;
    private javax.swing.JLabel jLabel_PromptTotal;
    private javax.swing.JLabel jLabel_SessionStatus;
    private javax.swing.JPanel jPanel_SpeakerWindow;
    private javax.swing.JProgressBar jProgressBar_SpeakerProgress;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane jTextPane_PromptDisplay;
    private javax.swing.JTextPane jTextPane_nextSentence;
    // End of variables declaration//GEN-END:variables
    
}

