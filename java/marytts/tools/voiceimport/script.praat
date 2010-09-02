# use Praat form to provide ARGV (NOTE: these must be explicitly given during call to Praat!)
form Provide arguments
  sentence wavFile input.wav
  sentence pointpFile output.PointProcess
  real minPitch 75
  real maxPitch 600
endform

Read from file... 'wavFile$'

# Remove DC offset, if present:
Subtract mean

# First, low-pass filter the speech signal to make it more robust against noise
# (i.e., mixed noise+periodicity regions treated more likely as periodic)
sound = Filter (pass Hann band)... 0 1000 100

# check for available Pitch file:
pitchFile$ = pointpFile$ - "PointProcess" + "Pitch"
if fileReadable(pitchFile$)
  pitch = Read from file... 'pitchFile$'
else
# Then determine pitch curve:
  pitch = To Pitch... 0 minPitch maxPitch
endif

# Get some debug info: 
min_f0 = Get minimum... 0 0 Hertz Parabolic
max_f0 = Get maximum... 0 0 Hertz Parabolic

# And convert to pitch marks:
plus sound
To PointProcess (cc)

# Fill in 100 Hz pseudo-pitchmarks in unvoiced regions:
Voice... 0.01 0.02000000001
Write to short text file... 'pointpFile$'
lastSlash = rindex(wavFile$, "/")
baseName$ = right$(wavFile$, length(wavFile$) - lastSlash) - ".wav"
printline 'baseName$'   f0 range: 'min_f0:0' - 'max_f0:0' Hz
