# Tcl/Tk MARY TTS client.

# This has been tested on Windows, and because
# of the use of sound there will be portability
# issues.  However, there should be enough here
# for a reasonable start at a client, for any
# platform that supports Tcl/Tk.  The platform
# specific code has, as far as possible, been
# isolated in the part of the code that detects
# whether this is being run as a program.

# Notes:
# More work will need to be done with this,
# in order to make the code clean.  It should
# probably be wrapped in a package, to solve
# any namespace issues.  There are a lot of
# global variables.  It seems that some of
# these are necessary for the menus to work.
# Handling of temporary files could be improved.

# TODO:
# Create modifier sliders.
# Extend the query proc to make use of them.
# Create the Help menu contents.
# Create the actions for the Edit menu.
# Handle obtaining different server data
#  Can do that, but can't reset properly: how
#  to handle the cancel button?  Where do
#  command procedures get their scope?  Make
#  it all global?  ghastly.
# from the user, and resetting all the globals
# and menus as a result.
# How do we ensure temporary files are removed
# in the event of a problem?  if {catch {}} ...?
# Maybe leaving them around is diagnostic info?


package require Tk
package require http

set mary_tts_default_host "127.0.0.1"
set mary_tts_default_port 59125

# Actual host and port, and global old
# copies to allow revert on cancel in the
# dialogues.  Apparently upvar #0 is the
# norm for that sort of thing [Tcl Wiki]
set mary_tts_host $mary_tts_default_host
set old_mary_tts_host $mary_tts_host
set mary_tts_port $mary_tts_default_port
set old_mary_tts_port $mary_tts_port

# Informational URLs
set informational_urls [ list \
version datatypes voices \
audioformats audioeffects ] 

#######

# Obtain a static page from the server, i.e.
# no parameters are needed to get it.
proc get_page { relative_url } {
  global mary_tts_host mary_tts_port
  set url http://$mary_tts_host:$mary_tts_port/$relative_url
  set result [::http::geturl $url]
  return [::http::data $result]
}

proc list_of_lines {str} {
  return [ split $str "\n" ]
}


# We will need to collect this information
# when we have the server and port chosen.
proc get_audioeffects {} {
  return [list_of_lines [get_page audioeffects] ] 
}

proc get_audioformats {} {
  return [list_of_lines [get_page audioformats]  ]
}

proc get_datatypes {} {
  return [ list_of_lines [get_page datatypes] ]
}


proc get_voices {} {
  return [list_of_lines [get_page voices] ]
}

# Handling post queries.

# Submit the query to the server, using the
# http POST method.
proc make_query {url encoded_params} {
  set http  [::http::geturl $url -query $encoded_params]
  set result [::http::data $http]
  return $result
}

# Get the text from the input text area
proc get_input_text {} {
  return [.io.inp.input_area get 1.0 end]
}

# Get the text from the output text area
proc get_output_text {} {
  return [.io.out.output_area get 1.0 end]
}

# Pushes the query to the server and gets
# the results back, displaying or playing
# them.
# Probably needs refactoring as it does
# rather too much.
proc generate {text_to_process} {
  global mary_tts_host mary_tts_port
  global inputtype outputtype locales
  global audioformat voice 
  set url "http://$mary_tts_host:$mary_tts_port/process"
  # ::http::formatQuery converts a list of
  # key value # pairs into the correct format
  # for http POST.
  set params [::http::formatQuery INPUT_TEXT $text_to_process INPUT_TYPE $inputtype OUTPUT_TYPE $outputtype LOCALE $locales($voice) AUDIO $audioformat VOICE $voice ]
  set result [make_query $url $params]
  if {$outputtype eq "AUDIO"} {
    # call the platform dependent implementation.
    play $result
  } else {
    clear_output
    add_message $result
  }
  # Return the result so we can save it if
  # the user requires it.
  return $result
}


# These next procs are for handling the
# lists of data one gets back from the server
# which possibly have several words per line,
# separated by spaces.

# If the first word of each listed line is
# significant, extract the list of first words.
proc collect_first_words_of_phrase_list {a_list} {
  for {set i 0} {$i < [llength $a_list]} {incr i} {
    set data [lindex $a_list $i ]
    set word [ lindex [split $data " "] 0 ]
    lappend words $word
  }
  return $words
}


# If the second word of each listed line is
# significant, extract the list of second words.
proc collect_second_words_of_phrase_list {a_list} {
  for {set i 0} {$i < [llength $a_list]} {incr i} {
    set data [lindex $a_list $i ]
    set word [ lindex [split $data " "] 1 ]
    lappend words $word
  }
  return $words
}


# The list of datatypes must be separated into
# input data types and output data types so that
# interactions with the server make sense.
# This handles the inputs.
proc collect_first_words_of_input_types {a_list} {
  for {set i 0} {$i < [llength $a_list]} {incr i} {
    set data [lindex $a_list $i ]
    if {[ string match -nocase "*input*" $data ]} {
      set word [ lindex [split $data " "] 0 ]
      lappend words $word
    }
  }
  return $words
}


# The list of datatypes must be separated into
# input data types and output data types so that
# interactions with the server make sense.
# This handles the outputs.
proc collect_first_words_of_output_types {a_list} {
  for {set i 0} {$i < [llength $a_list]} {incr i} {
    set data [lindex $a_list $i ]
    if {[string match -nocase "*output*" $data]} {
      set word [ lindex [split $data " "] 0 ]
      lappend words $word
    }
  }
  return $words
}

# setup all the variables to hold voices,
# audio options, etc., based on what the
# server can do.
proc setup_globals {} {
  global audioeffects audioformats voices
  global inputtypes outputtypes audioformat voice
  global inputtype outputtype locales

  set audioeffects [get_audioeffects]
  set audioformats [get_audioformats]
  set audioformat [lindex $audioformats 0 ]
  set datatypes_data [get_datatypes]
  set inputtypes [collect_first_words_of_input_types $datatypes_data]
  set inputtype [lindex $inputtypes 0]
  set outputtypes [collect_first_words_of_output_types $datatypes_data]
  set outputtype [lindex $outputtypes 0]
  set voices_data [get_voices]
  set voices [collect_first_words_of_phrase_list $voices_data]
  set locales_list [collect_second_words_of_phrase_list $voices_data ]
  for {set i 0} {$i < [llength $voices]} {incr i} {
    set locales([lindex $voices $i]) [lindex $locales_list $i]
  }
  set voice [lindex $voices 0]
}

# A general procedure for filling in the 
# elements of a listbox from a list.
# At present this is unused, but it could
# be useful later. 
proc add_listbox_items {a_var a_widget} {
  upvar $a_var var
  foreach item $var  {
    $a_widget insert end $item
  }
}

# Create the menubuttons along the top.
# Usual File, Edit and Help menus plus
# those to set attributes.
proc create_menubuttons {} {
  set buttons [ list file File edit Edit \
  server "Server" \
  inputtype "Input type" outputtype "Output type" \
  voice Voice \
  audioformat "Audio format" help Help ]

  set count 1
  foreach { menu_tag string_tag} $buttons {
    menubutton .menus.$menu_tag -text $string_tag -menu .menus.${menu_tag}.menu
    menu .menus.${menu_tag}.menu -tearoff true
    grid .menus.$menu_tag -in .menus -row 1 -column $count -sticky w
    incr count
  }
}

# Get the contents of a text file for reading
# or loading into a text widget, etc.
proc text_file_contents {what_for} {
  set a_file [tk_getOpenFile -title $what_for ]
  set the_text ""

  if {$a_file != ""} {
    set a_stream [open $a_file r ]
    set the_text [read $a_stream]
    close $a_stream
  }

  return $the_text
}

# Save the_text to a text file specified
# by the user, for the given reason (what_for).
proc save_text_file {the_text, what_for} {
  set a_file [tk_getSaveFile -title $what_for -message $what_for]
  if {$a_file != ""} {
    set a_stream [open $a_file w ]
    puts $a_stream $the_text 
    close $a_stream
  }
}

# Create the menu for File operations
proc create_menu_file {} { 
  set fmenu .menus.file.menu
  $fmenu add command -label "New" -command {
    .io.inp.input_area delete 1.0 end 
  }
  # Replace the contents of the input text
  # widget by the data from the open file.
  # <FIXME>YAGNI, but is there any reason
  # to allow inserting a file, rather than
  # replacing the text with file contents?
  # </FIXME>
  $fmenu add command -label "Open" -command {
    set the_text [text_file_contents "File to load"]
    if {$the_text != ""} {
      .io.inp.input_area delete 1.0 end 
      .io.inp.input_area insert end $the_text
    }
  }

  $fmenu add command -label "Read" -command {
    generate [text_file_contents "File to read"]
  }
  $fmenu add command -label "Save Input" -command { }
  $fmenu add command -label "Save Output" -command { }
}

# Create the menu for edit operations
proc create_menu_edit {} {
  set emenu .menus.edit.menu
  $emenu add command -label "Select All" -command {}
  # Add specific editing commands here later.
  # For example, we would like to be able to 
  # add whole tags to the XML based formats,
  # wrap matching tags around selected text.
  # Also we need to find out what happens with
  # copy cut and paste, given that X Windows
  # is different from MS Windows.
  # It would be useful to provide an option which
  # allows the insertion of the default text
  # which the server can supply for each mode.
  # However, my thinking is that this should not
  # be forced as it is in the Java application,
  # because this can rub out edits when swtiching
  # voices, and this can be annoying when
  # exploring the system.
}

# Set the server properties, mostly just
# host and port.  Maybe later protocol will
# be possible for https connections?
proc create_menu_server {} {
  set smenu .menus.server.menu
  $smenu add command -label "host" -command {
    create_entry_dialog "MARY TTS server name" "hostname/IP Address" mary_tts_host
  }
  $smenu add command -label "port" -command {
    create_entry_dialog "MARY TTS server port" "pott number" mary_tts_port
  }
}

# Create the menu for Help
proc create_menu_help {} {
  # This clearly ought to be here, but I am
  # not sure what docs to put in at this stage.
  # This is all pretty much wet paint anyway,
  # so maybe "it is too early to say".
  set hmenu .menus.help.menu
  $hmenu add command -label "Introduction" -command {
    tk_messageBox -message "This is a basic Tcl/Tk
client for the MARY TTS system. Most of the options
are reached through the menus on the top.  Some
facilities are presently lacking.  Most of the
interface should be self-explanatory.  In the
File menu, Read will read a given file aloud
(or at least take it as input for the present
form of processing), whereas Open will load it
into the input area.  Save input and Save output
refer to the contents of the text windows." -type ok
  }
  $hmenu add command -label "About" -command {}
}

# We need to create menus for the available
# voices and audio formats, etc.
# When we have the data for these menus from
# the server, create them by using the global
# lists of information.
proc create_radio_menu_from_list {what} {
  global $what 
  set plural "${what}s"
  upvar 1 $plural var
  foreach item $var {
    .menus.${what}.menu add radiobutton -label $item -variable $what \
    -value $item
  }
}

proc reset_entry_and_var {a_variable} {
  upvar #0 $a_variable var
  upvar #0 old_$a_variable old_var
  set var $old_var 
  destroy .entry_dialogue 
}
# Create the toplevel for choosing a host
# or port, something taken from an entry.
proc create_entry_dialog {a_message a_label a_variable} {
  upvar #0 $a_variable var
  upvar #0 old_$a_variable old_var
  toplevel .entry_dialogue 
  label .entry_dialogue.the_message -text $a_message
  label .entry_dialogue.the_label -text $a_label
  entry .entry_dialogue.the_entry -textvariable $a_variable
  button .entry_dialogue.ok -text "OK" -command {
    destroy .entry_dialogue 
  }
  button .entry_dialogue.cancel -text "Cancel" -command "reset_entry_and_var $a_variable" 
  
  grid .entry_dialogue.the_message -row 1 -column 1
  grid .entry_dialogue.the_label -row 2 -column 1
  grid .entry_dialogue.the_entry -row 2 -column 2
  grid .entry_dialogue.ok -row 3 -column 1
  grid .entry_dialogue.cancel -row 3 -column 2
}

# Add a message to the end of the output
# text widget.
proc add_message {a_message} {
  .io.out.output_area configure -state normal
  .io.out.output_area insert end $a_message
  .io.out.output_area configure -state disabled
}


# Clear the text in the output text widget.
proc clear_output {} {
  .io.out.output_area configure -state normal
  .io.out.output_area delete 1.0 end 
  .io.out.output_area configure -state disabled
}

# Sound generation is platform dependent.
# This provides an "abstract" function to
# be overridden by the platform dependent
# code.  In this case it alerts the user
# in the output window that nothing is going
# to happen.
proc play {sound} {
  add_message \
  "play sound not implemented on this platform apparently"
}
# Graphical stuff.  First just a text area

frame .menus
create_menubuttons
create_menu_file
create_menu_edit
create_menu_server
create_menu_help
# Fill in the other menus at runtime.

# .io communicates text with the user, 
# through an input and output window.
frame .io
frame .io.inp
frame .io.out
# .controls will hold the play button and
# the effects controls.
frame .controls 

# Draw the controls in .io
text .io.inp.input_area -height 10 -width 40 \
-xscrollcommand ".io.inp.input_x set" \
-yscrollcommand ".io.inp.input_y set" 
scrollbar .io.inp.input_x -orient horizontal \
-command ".io.inp.input_area xview"
scrollbar .io.inp.input_y -orient vertical \
-command ".io.inp.input_area yview"

text .io.out.output_area -height 10 -width 40 -state disabled \
-xscrollcommand ".io.out.output_x set" \
-yscrollcommand ".io.out.output_y set" 
scrollbar .io.out.output_x -orient horizontal \
-command ".io.out.output_area xview"
scrollbar .io.out.output_y -orient vertical \
-command ".io.out.output_area yview"

grid .io.inp -in .io -row 1 -column 1
grid .io.out -in .io -row 1 -column 2
grid .io.inp.input_area -in .io.inp -row 1 -column 1
grid .io.inp.input_y -in .io.inp -row 1 -column 2 -sticky ns
grid .io.inp.input_x -in .io.inp -row 2 -column 1 -sticky ew

grid .io.out.output_area -in .io.out -row 1 -column 1
grid .io.out.output_y -in .io.out -row 1 -column 2 -sticky ns
grid .io.out.output_x -in .io.out -row 2 -column 1 -sticky ew

button .controls.play -text "play" -command {
  generate [get_input_text]
}
grid .controls.play -in .controls -row 1 -column 1


pack .menus .io .controls -in . -side top



# Detect whether this is the main program
# This test was taken from the Tcl Wiki, and
# seems to work OK. 

if {[info exists argv0] && [file tail [info script]] eq [file tail $argv0]} {

  # Try to find the temporary files directory.
  catch { set tmpdir "/tmp" }
  catch { set tmpdir $::env(TRASH_FOLDER) }
  catch { set tmpdir $::env(TMP) }
  catch { set tmpdir $::env(TEMP) }
  # <FIXME>This needs better handling of 
  # possible alternatives</FIXME>
  # This is needed for Windows sound only.

  # Do the platform dependent things.
  if {$tcl_platform(platform) eq "windows"} {
    package require twapi

    proc play {sound} {
      global tmpdir
      # Write sound to a temporary file
      set sndfile [file join $tmpdir "MARYTTS_sound.[pid].wav" ]
      set stream [open $sndfile w]
      # Make sure the file is binary:
      fconfigure $stream -translation binary
      puts -nonewline $stream $sound
      close $stream
      # Play the file.
      add_message "about to play $sndfile\n"
      ::twapi::play_sound $sndfile 
      # Remove the file.
      file delete $sndfile
    }
  }
  # Put other platforms here.

  # Setup the globals with reference to the
  # server, which is assumed to be working.
  # Since we have options to alter this with
  # menu items, there probably needs to be
  # some way to reload all this.
  setup_globals
  create_radio_menu_from_list inputtype
  create_radio_menu_from_list outputtype
  create_radio_menu_from_list voice
  create_radio_menu_from_list audioformat

  # Note, at the moment voices holds locales,
  # gender, and voice type

  # At the moment this is just diagnostic,
  # it tells us we have a basically working
  # system and the list of voices has been
  # picked up and manipulated correctly.
  add_message [ join $voices "\n"  ]
}


