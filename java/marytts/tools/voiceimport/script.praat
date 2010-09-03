# use Praat form to provide ARGV (NOTE: these must be explicitly given during call to Praat!)
form Provide arguments
  sentence basenameList ../basenames.lst
  sentence wavDir ../wav
  sentence pmDir ../pm
  real minPitch 75
  real maxPitch 600
endform

# Get basename array (taking into account that some have a header line):
Read Strings from raw text file... 'basenameList$'
numLines = Get number of strings
numBasenames = 0
for l to numLines
  line$ = Get string... l
  if !startsWith(line$, "FROM:")
    numBasenames += 1
    basenames_'numBasenames'$ = line$
  endif
endfor
Remove

# iterate over all basenames:
lastpercent$ = ""
for b to numBasenames
  basename$ = basenames_'b'$
  wavFile$ = "'wavDir$'/'basename$'.wav"
  pitchFile$ = "'pmDir$'/'basename$'.Pitch"
  pointpFile$ = "'pmDir$'/'basename$'.PointProcess"

  # provide current progress as text file "percent":
  percent = floor(b / numBasenames * 100)
  percent$ = "'percent'"
  if percent$ != lastpercent$
    percent$ > percent
    lastpercent$ = percent$
  endif

  call process

endfor
filedelete percent

procedure process
wav = Read from file... 'wavFile$'

# Remove DC offset, if present:
Subtract mean

# First, low-pass filter the speech signal to make it more robust against noise
# (i.e., mixed noise+periodicity regions treated more likely as periodic)
sound = Filter (pass Hann band)... 0 1000 100

# check for available Pitch file:
if fileReadable(pitchFile$)
  pitch = Read from file... 'pitchFile$'
else
# Then determine pitch curve:
  noprogress To Pitch... 0 minPitch maxPitch
  pitch = selected()
endif

# Get some debug info: 
min_f0 = Get minimum... 0 0 Hertz Parabolic
max_f0 = Get maximum... 0 0 Hertz Parabolic

# And convert to pitch marks:
plus sound
noprogress To PointProcess (cc)

# Fill in 100 Hz pseudo-pitchmarks in unvoiced regions:
Voice... 0.01 0.02000000001
Write to short text file... 'pointpFile$'
printline 'basename$'   f0 range: 'min_f0:0' - 'max_f0:0' Hz ('percent$'%)

# cleanup:
plus wav
plus sound
plus pitch
Remove

endproc
