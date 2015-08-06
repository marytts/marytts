#!/usr/bin/env ruby
#
# A basic mary client in Ruby,
# kindly donated to the MARY TTS project
# by Hugh Sasse. Thanks Hugh!


# Ruby client for the MARY TTS HTTP server.
# This is for Windows only, and relies on 
# the Win32-Sound gem to access the audio.
#
#

require 'rubygems'
require 'net/http'
require 'uri'

# A fairly minimal client class for the
# MARY TTS system.  This uses the modern
# HTTP interface to access the server.
# At present, this doesn't wrap the methods
# which provide documentation or lists of
# voices or features.
class MaryClient
  attr_accessor :host, :port
  attr_accessor :input_type, :output_type
  attr_accessor :locale, :audio, :voice

  # Set up the defaults for the MARY TTS
  # server, which is assumed to be running
  # on the local host, with British voices
  # installed.  These may be modified with 
  # the appropriate methods.
  # host = 127.0.0.1)
  # port = 59125
  # input_type = "TEXT" 
  # output_type = "AUDIO"
  # audio = "WAVE_FILE"
  # locale = "en_GB"
  # voice = "dfki-prudence-hsmm"
  def initialize
    @host = "127.0.0.1"	# The local machine
    @port = 59125
    @input_type = "TEXT"
    @output_type = "AUDIO"
    @locale = "en_GB"
    @audio = "WAVE_FILE"
    @voice = "dfki-prudence-hsmm"
  end

  # Process a text message, which with a
  # new client, will return the audio.
  # This is so that platform dependent parts
  # are kept separate.
  def generate(message)
    raw_params = {"INPUT_TEXT" => message,
      "INPUT_TYPE" => @input_type,
      "OUTPUT_TYPE" => @output_type,
      "LOCALE" => @locale,
      "AUDIO" => @audio,
      "VOICE" => @voice,
    }
    res = Net::HTTP.post_form(URI.parse("http://#{@host}:#{@port}/process"), raw_params)
    res.value # Throw an exception on failure
    #puts res.body
    return res.body
  end
end


# If this invoked as a program with no
# argumens, just give a greeting to show
# that it is working.  If arguments are
# supplied, process options to work out
# what to do with the arguments.
if __FILE__ == $0

  # These files are only loaded when this is
  # invoked as a program.
  require 'rbconfig'
  require 'getoptlong'

  # PLATFORM SPECIFIC CODE.
  # Needs more work [!]
  case Config::CONFIG['host_os']
  when /darwin/i
    raise NotImplementedError.new("Don't know how to play audio on a Mac")
  when /linux/i
    raise NotImplementedError.new("Far too many ways to play audio on Linux, you'll need to choose something")
  when /sunos|solaris/i
    raise NotImplementedError.new("Have not played audio on Suns for too long to implement this.")
  when /java/i
    raise NotImplementedError.new("Don't know how to play audio from Java ")
  when /win32|cygwin|mingw32/i
    # The various things that can use the Win32
    # sound gem
    require 'win32/sound'
    # Create a player class that will play the
    # sound that the Mary TTS system returns
    class Player

      # Play the audio passed in.
      # Possibly this should receive the audio
      # type so we can check that we can play it,
      # but at the moment that is the
      # responsibility of the user.
      def self.play(sound)
	Win32::Sound.play(sound, Win32::Sound::MEMORY)
      end
    end
  else
    raise NotImplementedError.new("Haven't thought how to support this OS yet")
  end


  client = nil
  split = ""

  if ARGV.size.zero?
    client = MaryClient.new()
    sound = client.generate("Hello from Mary Text to Speech with Ruby.")
      Player.play(sound)
  else
    args_mode = :words
    stdout_mode = :absorb
    opts = GetoptLong::new(
      ["--audio", "-a", GetoptLong::REQUIRED_ARGUMENT],
      ["--echo", "-e", GetoptLong::NO_ARGUMENT],
      ["--help", "-h", GetoptLong::NO_ARGUMENT],
      ["--host", "-H", GetoptLong::REQUIRED_ARGUMENT],
      ["--input-type", "-i", GetoptLong::REQUIRED_ARGUMENT],
      ["--locale", "-l", GetoptLong::REQUIRED_ARGUMENT],
      ["--read", "-r", GetoptLong::NO_ARGUMENT],

      ["--split", "-s", GetoptLong::REQUIRED_ARGUMENT],
      ["--output-type", "-o", GetoptLong::REQUIRED_ARGUMENT],
      ["--port", "-P", GetoptLong::REQUIRED_ARGUMENT],
      ["--tee", "-t", GetoptLong::NO_ARGUMENT],
      ["--voice", "-v", GetoptLong::REQUIRED_ARGUMENT]
    )

    opts.each do |opt, arg|
      unless ["--help", "-h"].include?(opt)
	# skip if we are only getting help
	client ||= MaryClient.new()
      end
      case opt
      when "--help", "-h"
	puts <<-EOHELP
Usage: #{$0} [options] [arguments]
--audio -a
	Audio format. Defualt: WAVE_FILE
--echo -e
	Act as an echo command and send output
	arguments to the synthesizer only (not
	to standard output.
	Turns off --read|-r
--help -h
	Print this help, then exit.
--host -H
	The host which is the server.
	Default: 127.0.0.1
--input-type -i
	The type of the input supplied to the
	TTS system. Default: TEXT
--locale -l
	The locale of the input. Default: en_GB
--output-type -o
	The output type from the TTS system.
	Default: AUDIO
--port -P
	The port for the TTS server
	Default: 59125
--read -r
	Read the files passed as arguments.
	Turns off --echo|-e
--split -s (lines|paragraphs)
        When reading files, split the input
	into lines or paragraphs. Paragraphs
	mean reading up to the next double 
	newline. Note, the argument is literally
	"lines" or "paragraphs" (or some 
	abbreviation of those) without the
	quotes.
	Default is paragraphs.
--tee -t
	Act as tee: send the output to the TTS
	system, and to standard output.
--voice -v
	The voice to use.
	Default: dfki-prudence-hsmm
	EOHELP
	exit(0)
      when "--audio", "-a"
	client.audio = arg
      when "--echo", "-e"
	args_mode = :words
      when "--host", "-H"
	client.host = arg
      when "--input-type", "-i"
	client.input_type = arg
      when "--locale", "-l"
	client.locale = arg
      when "--output-type", "-o"
	client.output_type = arg
      when "--port", "-P"
	client.port = arg.to_i
      when "--read", "-r"
	args_mode = :files
      when "--split", "-s"
	case arg
	when /^p/i
	  split = ""
	when /^l/i
	  split = $/
	end
      when "--tee", "-t"
	stdout_mode = :emit
      when "--voice", "-v"
	client.voice = arg
      end
    end

    client ||= MaryClient.new()
    case args_mode
    when :words
      input_text = ARGV.join(" ") 
      unless input_text =~ /\A\s*\Z/m
	sound = client.generate(input_text)
	if client.output_type == "AUDIO"
	  Player.play(sound)
	end
      end
      if stdout_mode == :emit
	puts input_text
      end
    when :files
      # Slurp in paragraphs so sentences
      # don't get broken in stupid places.
      $/ = split # paragraph mode 
      ARGF.each do |paragraph|
	begin
	  unless paragraph =~ /\A\s*\Z/m
	    sound = client.generate(paragraph)
            if client.output_type == "AUDIO"
	      # and client.audio == "WAVE_FILE"
	      Player.play(sound)
	    end
	  end
	rescue Exception => e
	  puts "got error #{e} while trying to say #{paragraph.inspect}"
	  raise
	end
	if stdout_mode == :emit
	  puts paragraph
	end # end if
      end # end ARGF.each
    end # end case
  end # if ARGV.size.zero?
end

