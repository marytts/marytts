; Portions Copyright 2004 Sun Microsystems, Inc.
; Portions Copyright 1999-2003 Language Technologies Institute,
; Carnegie Mellon University.
; All Rights Reserved.  Use is subject to license terms.
;
; See the file "license.terms" for information on usage and
; redistribution of this file, and for a DISCLAIMER OF ALL
; WARRANTIES.

; Dumps join weights to stdout.
;
; Expects clunit_params to be defined.
;
(define (dump_join_weights)
  (format t 
	  "JOIN_WEIGHTS %d" 
	  (length (cadr (assoc 'join_weights clunits_params))))

  (let ((join_weights (cadr (assoc 'join_weights clunits_params))))
    (while join_weights
	   (format t " %d" (* 65536 (car join_weights)))
	   (set! join_weights (cdr join_weights))))

  (format t "\n")
)
