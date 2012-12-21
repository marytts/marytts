##
# Copyright 2006-2010 DFKI GmbH.
# All Rights Reserved.  Use is subject to license terms.
#
# This file is part of MARY TTS.
#
# MARY TTS is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
##

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

# BEGIN process procedure
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
  # determine pitch curve:
  noprogress To Pitch... 0 minPitch maxPitch
  pitch = selected()
endif

# Get some debug info: 
min_f0 = Get minimum... 0 0 Hertz Parabolic
max_f0 = Get maximum... 0 0 Hertz Parabolic

# And convert to pitch marks:
plus sound
noprogress To PointProcess (cc)
pp = selected()

# convert pitch contour and pitch marks to PitchTier for easier access:
plus pitch
pt = To PitchTier

# Fill in 100 Hz pseudo-pitchmarks in unvoiced regions:
fill_f0 = 100 ; define constant

# get v/uv intervals:
select pp
# TODO tweak parameters?
tg = To TextGrid (vuv)... 0.02 0.01
# sanity check: are there even any unvoiced intervals?
ni = Get number of intervals... 1
if ni == 1
  i$ = Get label of interval... 1 1
  if i$ == "V"
    select pp
    goto writepp
  endif
endif
it = Extract tier... 1
uv = Down to TableOfReal... U

# iterate over uv intervals (two passes to avoid concurrent modification):
select pp
for row to Object_'uv'.nrow
  # get the fill start time:
  uv_start = Object_'uv'[row,"Start"]
  prev_pm_idx = Get low index... uv_start
  is_first_pm = prev_pm_idx == 0
  if is_first_pm
    fill_start = uv_start
  else
    prev_pm_time = Get time from index... prev_pm_idx
    prev_f0 = Object_'pt'[prev_pm_idx]
    fill_start = prev_pm_time + 1 / fill_f0
  endif

  # get the fill end time:
  uv_end = Object_'uv'[row,"End"]
  next_pm_idx = Get high index... uv_end
  is_last_pm = next_pm_idx > Object_'pt'.nx
  if is_last_pm
    fill_end = uv_end
  else
    next_pm_time = Get time from index... next_pm_idx
    next_f0 = Object_'pt'[next_pm_idx]
    fill_end = next_pm_time - 1 / fill_f0
  endif

  # adjust fill start and end times so that fill pitchmarks are centered in fill region:  
  fill_length = fill_end - fill_start
  fill_pm_remainder = fill_length mod (1 / fill_f0)
  if is_first_pm
    fill_start += fill_pm_remainder
  elsif is_last_pm
    fill_end -= fill_pm_remainder
  else
    fill_start += fill_pm_remainder / 2
    fill_end -= fill_pm_remainder / 2
  endif

  # store values
  fill_start_'row' = fill_start
  fill_end_'row' = fill_end
endfor

# do the fill:
for row to Object_'uv'.nrow
  time = fill_start_'row'
  while time <= fill_end_'row' + 1e-14 ; try to gracefully handle floating-point noise
    Add point... time
    time += 1 / fill_f0
  endwhile
endfor

label writepp
Write to short text file... 'pointpFile$'
printline 'basename$'   f0 range: 'min_f0:0' - 'max_f0:0' Hz ('percent$'%)

# cleanup:
plus wav
plus sound
plus pitch
plus pt
plus pp
plus tg
nocheck plus it
nocheck plus uv
Remove

endproc
# END process procedure
