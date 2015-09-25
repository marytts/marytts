#Define textgrid, label folders, and utterance file name (without extension - expects .TextGrid)
form Select location
	text folder /home/sahil/maus_web/textgrid
	text labFolder /home/sahil/maus_web/lab
	text file cw_0001
	positive tier 1
endform

system_nocheck mkdir -p 'labFolder$'
Read from file... 'folder$'/'file$'.TextGrid
#Open TextGrid file in Praat
select TextGrid 'file$'
#Get MAU tier 
Extract one tier... tier

#Delete TextGrid object from Praat
select TextGrid 'file$'
Remove

#Select MAU TextGrid
select TextGrid MAU

#Convert MAU TextGrid to tabular format
Down to Table... no 6 no no

#Remove MAU TextGrid object from Praat 
select TextGrid MAU
Remove

#Remove tmin column from MAU table
select Table MAU
Remove column... tmin

# Iterate over all rows in table and create a .lab file with the columns [tmax 125 phone]
numRows = Get number of rows
labFile$ = "'labFolder$'/'file$'.lab"
deleteFile("'labFile$'")
fileappend 'labFile$' # 'newline$'
echo Saving 'labFile$' 'newline$'

Create Table with column names... table numRows # num text

for n from 1 to numRows
	select Table MAU
	f_tmax = Get value... n tmax
	s_text$ = Get value... n text
	line$ = "'f_tmax:6' 125 's_text$''newline$'"
	line$ = replace$ (line$,"<p:>","_",0)
	 line$ >> 'labFile$'
	select Table table
endfor

#Remove all created objects from Praat
select Table table
Remove
select Table MAU
Remove
