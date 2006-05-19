; Portions Copyright 2004 Sun Microsystems, Inc.
; Portions Copyright 1999-2003 Language Technologies Institute,
; Carnegie Mellon University.
; All Rights Reserved.  Use is subject to license terms.
;
; See the file "license.terms" for information on usage and
; redistribution of this file, and for a DISCLAIMER OF ALL
; WARRANTIES.

; Dumps phone duration stats to stdout.
;
; Expects duration_ph_info to be defined.
;
(define (dump_dur_stat)
  (let ((dur_lines duration_ph_info))
    (while dur_lines
	   (let ((dur_line (car dur_lines)))
	     (format t "%s %f %f\n" 
		     (car dur_line) 
		     (cadr dur_line)
		     (caddr dur_line)))
	   (set! dur_lines (cdr dur_lines)))))
