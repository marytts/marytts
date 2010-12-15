; Portions Copyright 2004 Sun Microsystems, Inc.
; Portions Copyright 1999-2003 Language Technologies Institute,
; Carnegie Mellon University.
; All Rights Reserved.  Use is subject to license terms.
;
; See the file "license.terms" for information on usage and
; redistribution of this file, and for a DISCLAIMER OF ALL
; WARRANTIES.

; Dumps the target f0 mean and range to stdout.
; 
; Expects int_lr_params to be defined.
;
(define (dump_f0_terms)
  (format t 
	  "F0_MEAN=%d\n" 
	  (cadr (assoc 'target_f0_mean int_lr_params)))
  (format t 
	  "F0_RANGE=%d\n" 
	  (cadr (assoc 'target_f0_std int_lr_params)))
)
