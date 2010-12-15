; Portions Copyright 2004 Sun Microsystems, Inc.
; Portions Copyright 1999-2003 Language Technologies Institute,
; Carnegie Mellon University.
; All Rights Reserved.  Use is subject to license terms.
;
; See the file "license.terms" for information on usage and
; redistribution of this file, and for a DISCLAIMER OF ALL
; WARRANTIES.

; Dumps the guess_pos part of speech tagger to stdout.
; 
; Expects guess_pos to be defined.
;
(define (dump_pos)
  (let ((pos_list guess_pos))
    (while pos_list
	   (let ((pos (car (car pos_list)))
		 (words (cdr (car pos_list))))
	     (while words
		    (format t "%s %s\n" (car words) pos)
		    (set! words (cdr words))))
	   (set! pos_list (cdr pos_list))))
)
