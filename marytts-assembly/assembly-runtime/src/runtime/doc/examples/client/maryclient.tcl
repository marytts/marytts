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
# Create modifier sliders, for the effects.
# Extend the query proc to make use of them.
# Turn the Help menu into something more useful.
# Debug the actions for the Edit menu.
# Provide a means of getting example inputs
# from the server.
# Provide a means of re-loading all the
# dynamically collected information when the
# server is changed from the menu.  This means
# that we need to delete the existing menu
# entries in order to add them correctly.
# How do we ensure temporary files are removed
# in the event of a problem?  if {catch {}} ...?
# Maybe leaving them around is diagnostic info?
# Make that an option?
# Add error handling code for network and disk
# failures likely to beset such clients.
# Add sensible defaults for things the user must
# always set at startup, but these will be
# platform spacific.  Always default to Audio
# output for example, or is it possible that
# people have no voices installed?


# This is a GUI, so:
package require Tk

# We are communicating with the Mary server
# with HTTP.
package require http

# Use the local machine in preference to the
# one in Germany.
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

# Collect the audio data from the server.
proc collect_audio_data {text_to_process} {
  global mary_tts_host mary_tts_port
  global inputtype outputtype locales
  global audioformat voice 
  set url "http://$mary_tts_host:$mary_tts_port/process"
  # ::http::formatQuery converts a list of
  # key value pairs into the correct format
  # for http POST.
  set params [::http::formatQuery INPUT_TEXT $text_to_process INPUT_TYPE $inputtype OUTPUT_TYPE $outputtype LOCALE $locales($voice) AUDIO $audioformat VOICE $voice ]
  set result [make_query $url $params]
  return $result
}

# Pushes the query to the server and gets
# the results back, displaying or playing
# them.
proc generate_output {text_to_process} {
  global outputtype 
  set result [collect_audio_data $text_to_process]
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
# be useful later.  [It took a while to
# figure out so I'm not ready to kill it
# with YAGNI.]
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
  audioformat "Audio format" \
  textstyle "Text style" help Help ]

  set count 1
  foreach { menu_tag string_tag} $buttons {
    menubutton .menus.$menu_tag -text $string_tag \
      -menu .menus.${menu_tag}.menu -underline 0 -font ClientFont
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
# At the moment there is no error handling
# for this (disk full, write protected, etc).
proc save_text_file {the_text what_for} {
  set a_file [tk_getSaveFile -title $what_for -parent .]
  if {$a_file != ""} {
    set a_stream [open $a_file w ]
    puts $a_stream $the_text 
    close $a_stream
  }
}

# Save the_data to a binary file specified
# by the user, for the given reason (what_for),
# a text string.
# At the moment there is no error handling
# for this (disk full, write protected, etc).
proc save_binary_file {the_data what_for} {
  set a_file [tk_getSaveFile -title $what_for -parent .]
  if {$a_file != ""} {
    set a_stream [open $a_file w ]
    fconfigure $a_stream -translation binary
    puts -nonewline $a_stream $the_data 
    close $a_stream
  }
}

# Create the menu for File operations
proc create_menu_file {} { 
  set fmenu .menus.file.menu
  $fmenu add command -label "New" \
    -font ClientFont -command {
    .io.inp.input_area delete 1.0 end 
  }
  # Replace the contents of the input text
  # widget by the data from the open file.
  # <FIXME>YAGNI, but is there any reason
  # to allow inserting a file, rather than
  # replacing the text with file contents?
  # </FIXME>
  $fmenu add command -label "Open" \
    -font ClientFont -command {
    set the_text [text_file_contents "File to load"]
    if {$the_text != ""} {
      .io.inp.input_area delete 1.0 end 
      .io.inp.input_area insert end $the_text
    }
  }

  $fmenu add command -label "Read" \
    -font ClientFont -command {
    generate_output [text_file_contents "File to read"]
  }
  # How to make these disabled for now?
  $fmenu add command -label "Save Input" \
    -font ClientFont -command {
    set the_text [get_input_text]
    save_text_file $the_text "Save Input"
  }
  $fmenu add command -label "Save Output" \
    -font ClientFont -command {
    set the_text [get_output_text]
    save_text_file $the_text "Save Output"
  }
}

# Create the menu for edit operations
proc create_menu_edit {} {
  set emenu .menus.edit.menu
  $emenu add command -label "Select All from Input Area" \
    -font ClientFont -command {
    # This code says copy the selection as well.
    # May be wrong for some platforms, but is
    # it more useful?
    .io.inp.input_area tag add sel 1.0 end
    event generate .io.inp.input_area <<copy>>
}
  $emenu add command -label "Select All from Output Area" \
    -font ClientFont -command {
    # This code says copy the selection as well.
    # May be wrong for some platforms, but is
    # it more useful?
    .io.out.output_area tag add sel 1.0 end
    event generate .io.out.output_area <<Copy>>
}
  $emenu add command -label "Copy from Input Area" \
    -font ClientFont -command {
    # this appears not to work. FIXME
    event generate .io.inp.input_area <<Copy>>
  }
  $emenu add command -label "Copy from Output Area" \
    -font ClientFont -command {
    # this appears not to work. FIXME
    event generate .io.out.output_area <<copy>>
  }
  $emenu add command -label "Paste into Input Area" \
    -font ClientFont -command {
    # this appears not to work. FIXME
    event generate .io.inp.input_area <<Paste>>
  }
  $emenu add command \
    -font ClientFont -label "Insert example text into Input Area"\
    -command {
  }
  # Add specific editing commands here later.
  # For example, we would like to be able to 
  # add whole tags to the XML based formats,
  # wrap matching tags around selected text.
  # Also we need to find out what happens with
  # copy cut and paste, given that X Windows
  # is different from MS Windows.
  # Allow example text to be inserted.
  # However, my thinking is that this should not
  # overwrite as it is in the Java application,
  # because this rubs out edits when switching
  # voices, and this can be annoying when
  # exploring the system.
}

# Set the server properties, mostly just
# host and port.  Maybe later protocol will
# be possible for https connections?
proc create_menu_server {} {
  set smenu .menus.server.menu
  $smenu add command -label "host" -font ClientFont -command {
    create_entry_dialog "MARY TTS server name" "hostname/IP Address" mary_tts_host
  }
  $smenu add command -label "port" -font ClientFont -command {
    create_entry_dialog "MARY TTS server port" "pott number" mary_tts_port
  }
}

# setup the fonts for the various areas on the dipslay.
proc setup_font {family size} {
  foreach win {.io .controls .entry.dialogue } {
   font configure ClientFont -family $family -size $size 
  }
}

# Create the menu for changing the text size.
proc create_menu_textstyle {} {
  set tmenu .menus.textstyle.menu

  $tmenu add cascade -label "Courier" -underline 0 -menu \
    $tmenu.courier -font ClientFont
  $tmenu add cascade -label "Times" -underline 0 -menu \
    $tmenu.times -font ClientFont
  $tmenu add cascade -label "Helvetica" -underline 0 -menu \
    $tmenu.helvetica -font ClientFont
  foreach {name family} [list $tmenu.courier Courier \
    $tmenu.times Times $tmenu.helvetica Helvetica ] {
    set m1 [menu $name]
    foreach pts {6 7 8 9 10 12 14 16 18 20 24 28 32 36} {
      $m1 add command -label "$pts" -font ClientFont\
        -command [list setup_font $family $pts ]
    }
  }
} 



# Create the menu for Help
proc create_menu_help {} {
  # This is all pretty much "wet paint"
  # Is there enough to merit separate menus?
  set hmenu .menus.help.menu
  $hmenu add command -label "Introduction" -font ClientFont\
    -command {
    tk_messageBox -message "This is a basic Tcl/Tk
client for the MARY TTS system. Most of the options
are reached through the menus on the top.  Some
facilities are presently lacking.

Most of the interface should be self-explanatory.
In the File menu, Read will read a given file aloud
(or at least take it as input for the present
form of processing), whereas Open will load it
into the input area.  Save input and Save output
refer to the contents of the text windows. The
save button next to the play button will save
the output to a file; this is assumed to be a
text file, unless the output is audio, in which
case it is a binary file. 

The Edit menu has cut and paste facilities,
but these don't seem to work reliably.  The
default key bindings for text areas should
be useable.

You will need to set the input and output types
and the audio format before pressing play.
Code does not yet exist to figure out sensible
defaults for your platform.

This does not have support for the effects, yet.

Contributions from developers welcome." -type ok
  }
  $hmenu add command -label "About" -command {} -font ClientFont
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
      -value $item -font ClientFont
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
  label .entry_dialogue.the_message -text $a_message \
    -font ClientFont
  label .entry_dialogue.the_label -text $a_label -font ClientFont
  entry .entry_dialogue.the_entry -textvariable $a_variable \
    -font ClientFont
  button .entry_dialogue.ok -text "OK" -font ClientFont -command {
    destroy .entry_dialogue 
  }
  button .entry_dialogue.cancel -text "Cancel" -font ClientFont \
    -command "reset_entry_and_var $a_variable" 
  
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

# Graphical stuff.  

# In order to be able to scale the font, define a font.
font create ClientFont -family [font actual TkDefaultFont -family] \
  -size [font actual TkDefaultFont -size]

frame .menus
create_menubuttons
create_menu_file
create_menu_edit
create_menu_server
create_menu_textstyle
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
label .io.inp.input_label -text "Input Area" -font ClientFont
text .io.inp.input_area -height 10 -width 40 \
-xscrollcommand ".io.inp.input_x set" \
-yscrollcommand ".io.inp.input_y set"  -font ClientFont
scrollbar .io.inp.input_x -orient horizontal \
-command ".io.inp.input_area xview"
scrollbar .io.inp.input_y -orient vertical \
-command ".io.inp.input_area yview"

label .io.out.output_label -text "Output Area" -font ClientFont
text .io.out.output_area -height 10 -width 40 -state disabled \
-xscrollcommand ".io.out.output_x set" \
-yscrollcommand ".io.out.output_y set"  -font ClientFont
scrollbar .io.out.output_x -orient horizontal \
-command ".io.out.output_area xview"
scrollbar .io.out.output_y -orient vertical \
-command ".io.out.output_area yview"

grid .io.inp -in .io -row 1 -column 1
grid .io.out -in .io -row 1 -column 2
grid .io.inp.input_label -in .io.inp -row 1 -column 1
grid .io.inp.input_area -in .io.inp -row 2 -column 1
grid .io.inp.input_y -in .io.inp -row 2 -column 2 -sticky ns
grid .io.inp.input_x -in .io.inp -row 3 -column 1 -sticky ew

grid .io.out.output_label -in .io.out -row 1 -column 1
grid .io.out.output_area -in .io.out -row 2 -column 1
grid .io.out.output_y -in .io.out -row 2 -column 2 -sticky ns
grid .io.out.output_x -in .io.out -row 3 -column 1 -sticky ew

button .controls.play -text "play" -font ClientFont -command {
  generate_output [get_input_text]
}
grid .controls.play -in .controls -row 1 -column 1

button .controls.save -text "save" -font ClientFont -command {
  global outputtype
  set input_text [get_input_text]
  if { $outputtype eq "AUDIO" } {
    save_binary_file [collect_audio_data $input_text ] "Save audio file"
  } else {
    save_text_file [collect_audio_data $input_text ] "Save output to file"
  }
}

grid .controls.save -in .controls -row 1 -column 2

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
  # some way to reload all this.  But we need
  # to know how to delete the existing menu
  # entries to do that.
  setup_globals
  create_radio_menu_from_list inputtype
  create_radio_menu_from_list outputtype
  create_radio_menu_from_list voice
  create_radio_menu_from_list audioformat

  # Note, at the moment voices holds locales,
  # gender, and voice type

  # At the moment this is just diagnostic:
  ## add_message [ join $voices "\n"  ]
  # it tells us we have a basically working
  # system and the list of voices has been
  # picked up and manipulated correctly.
  # So it is commented out now.
}


