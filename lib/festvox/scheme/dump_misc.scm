; Portions Copyright 2004 Sun Microsystems, Inc.
; Portions Copyright 1999-2003 Language Technologies Institute,
; Carnegie Mellon University.
; All Rights Reserved.  Use is subject to license terms.
;
; See the file "license.terms" for information on usage and
; redistribution of this file, and for a DISCLAIMER OF ALL
; WARRANTIES.

; Dumps miscellaneous terms to stdout.
; 
; Expects clunits_params to be defined.
;
(define (dump_misc)
  (format t 
	  "CONTINUITY_WEIGHT %d\n" 
	  (cadr (assoc 'continuity_weight clunits_params)))

  (format t 
	  "OPTIMAL_COUPLING %d\n" 
	  (cadr (assoc 'optimal_coupling clunits_params)))

  (format t 
	  "EXTEND_SELECTIONS %d\n" 
	  (cadr (assoc 'extend_selections clunits_params)))
)
