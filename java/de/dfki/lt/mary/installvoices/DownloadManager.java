package de.dfki.lt.mary.installvoices;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import de.dfki.lt.mary.installvoices.InstallableVoice.Status;

// The Download Manager.
public class DownloadManager extends JFrame
        implements Observer {
    
    private String maryBase;
    
    // Download table's data model.
    private DownloadsTableModel tableModel;
    
    // Table listing downloads.
    private JTable table;
    
    // These are the buttons for managing the selected download.
    private JButton downloadButton;
    //private JButton pauseButton, resumeButton;
    private JButton cancelButton;
    private JButton installButton;
    private JButton removeButton;
    
    // Currently selected download.
    private InstallableVoice selectedVoice;
    
    // Flag for whether or not table selection is being cleared.
    private boolean clearing;
    
    private boolean exitOnClose;
    
    // Constructor for Download Manager.
    public DownloadManager(List<InstallableVoice> voices, boolean exitOnClose) {
        this.exitOnClose = exitOnClose;
        
        // Set application title.
        setTitle("MARY Voice Installer");
        
        // Set window size.
        setSize(800, 600);
        
        // Handle window closing events.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });
        
        // Set up file menu.
/*        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Exit",
                KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionExit();
            }
        });
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
 */      
       
        // Set up Downloads table.
        tableModel = new DownloadsTableModel();
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(new
                ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                tableSelectionChanged();
            }
        });
        // Allow only one row at a time to be selected.
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Set up ProgressBar as renderer for progress column.
        ProgressRenderer renderer = new ProgressRenderer(0, 100);
        renderer.setStringPainted(true); // show progress text
        table.setDefaultRenderer(JProgressBar.class, renderer);
        
        // Set table's row height large enough to fit JProgressBar.
        table.setRowHeight(
                (int) renderer.getPreferredSize().getHeight());
        
        // Set up downloads panel.
        JPanel downloadsPanel = new JPanel();
        downloadsPanel.setBorder(
                BorderFactory.createTitledBorder("Available MARY voices"));
        downloadsPanel.setLayout(new BorderLayout());
        downloadsPanel.add(new JScrollPane(table),
                BorderLayout.CENTER);
        
        // Set up buttons panel.
        JPanel buttonsPanel = new JPanel();
        downloadButton = new JButton("Download");
        downloadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionDownload();
            }
        });
        downloadButton.setEnabled(false);
        buttonsPanel.add(downloadButton);
/*        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionPause();
            }
        });
        pauseButton.setEnabled(false);
        buttonsPanel.add(pauseButton);
        resumeButton = new JButton("Resume");
        resumeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionResume();
            }
        });
        resumeButton.setEnabled(false);
        buttonsPanel.add(resumeButton);
*/
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionCancel();
            }
        });
        cancelButton.setEnabled(false);
        buttonsPanel.add(cancelButton);
        installButton = new JButton("Install");
        installButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionInstall();
            }
        });
        installButton.setEnabled(false);
        buttonsPanel.add(installButton);
        removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionRemove();
            }
        });
        removeButton.setEnabled(false);
        buttonsPanel.add(removeButton);
        
        // Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
        
        
        for (InstallableVoice v: voices) {
            tableModel.addDownload(v);
        }
    }
    
    // Exit this program.
    private void actionExit() {
        if (exitOnClose) {
            System.exit(0);
        } else {
            setVisible(false);
        }
    }
    
    // Verify download URL.
    private URL verifyUrl(String url) {
        // Only allow HTTP URLs.
        if (!url.toLowerCase().startsWith("http://"))
            return null;
        
        // Verify format of URL.
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }
        
        // Make sure URL specifies a file.
        if (verifiedUrl.getFile().length() < 2)
            return null;
        
        return verifiedUrl;
    }
    
    // Called when table row selection changes.
    private void tableSelectionChanged() {
    /* Unregister from receiving notifications
       from the last selected download. */
        if (selectedVoice != null)
            selectedVoice.deleteObserver(DownloadManager.this);
        
    /* If not in the middle of clearing a download,
       set the selected download and register to
       receive notifications from it. */
        if (!clearing) {
            selectedVoice = tableModel.getDownload(table.getSelectedRow());
            if (selectedVoice != null)
                selectedVoice.addObserver(DownloadManager.this);
            updateButtons();
        }
    }

    private void actionDownload() {
        System.out.println("Downloading "+selectedVoice);
        selectedVoice.download();
    }
    
    // Pause the selected download.
    private void actionPause() {
        selectedVoice.pause();
        updateButtons();
    }
    
    // Resume the selected download.
    private void actionResume() {
        selectedVoice.resume();
        updateButtons();
    }
    
    // Cancel the selected download.
    private void actionCancel() {
        selectedVoice.cancel();
        updateButtons();
    }
    
    // Install the selected voice.
    private void actionInstall() {
        System.out.println("Installing voice "+selectedVoice);
        try {
            selectedVoice.install();
        } catch (Exception e) {
            System.err.println("Cannot install voice:");
            e.printStackTrace();
        }
        updateButtons();
    }
    
    private void actionRemove() {
        clearing = true;
        System.out.println("Removing voice "+selectedVoice);
        if (selectedVoice.uninstall()) {
            if (selectedVoice.getStatus() == Status.AVAILABLE
                    || selectedVoice.getStatus() == Status.DOWNLOADED) {
                // do nothing
            } else { // remove entry from table
                tableModel.clearDownload(table.getSelectedRow());
                selectedVoice = null;
            }
        }
        clearing = false;
        updateButtons();
    }
    
  /* Update each button's state based off of the
     currently selected download's status. */
    private void updateButtons() {
        if (selectedVoice != null) {
            Status status = selectedVoice.getStatus();
            switch (status) {
                case DOWNLOADING:
                    downloadButton.setEnabled(false);
//                    pauseButton.setEnabled(true);
//                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    installButton.setEnabled(false);
                    removeButton.setEnabled(false);
                    break;
/*                case Download.PAUSED:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    installButton.setEnabled(false);
                    removeButton.setEnabled(false);
                    break;
*/
                case ERROR:
                      downloadButton.setEnabled(false);
//                    pauseButton.setEnabled(false);
//                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    installButton.setEnabled(false);
                    removeButton.setEnabled(false);
                    break;
                case DOWNLOADED:
                    downloadButton.setEnabled(false);
//                  pauseButton.setEnabled(false);
//                  resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    installButton.setEnabled(true);
                    removeButton.setEnabled(false);
                    break;
                case INSTALLING:
                    downloadButton.setEnabled(false);
//                  pauseButton.setEnabled(false);
//                  resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    installButton.setEnabled(false);
                    removeButton.setEnabled(false);
                    break;
                case INSTALLED:
                    downloadButton.setEnabled(false);
//                  pauseButton.setEnabled(false);
//                  resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    installButton.setEnabled(false);
                    removeButton.setEnabled(true);
                  break;
                default: // AVAILABLE
                    downloadButton.setEnabled(true);
//                    pauseButton.setEnabled(false);
//                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    installButton.setEnabled(false);
                    removeButton.setEnabled(false);
            }
        } else {
            // No download is selected in table.
            downloadButton.setEnabled(false);
//            pauseButton.setEnabled(false);
//            resumeButton.setEnabled(false);
            cancelButton.setEnabled(false);
            installButton.setEnabled(false);
            removeButton.setEnabled(false);
        }
    }
    
  /* Update is called when a Download notifies its
     observers of any changes. */
    public void update(Observable o, Object arg) {
        // Update buttons if the selected download has changed.
        if (selectedVoice != null && selectedVoice.equals(o))
            updateButtons();
    }
    
}


