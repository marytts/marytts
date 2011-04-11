#!/usr/bin/wish

package require snack

snack::sound s

s read [lindex $argv 0]

set fd [open [lindex $argv 1] w]
puts $fd [join [s pitch -method esps -maxpitch [lindex $argv 2] -minpitch [lindex $argv 3]] \n]
close $fd

exit 