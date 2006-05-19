; Portions Copyright 2004 Sun Microsystems, Inc.
; Portions Copyright 1999-2003 Language Technologies Institute,
; Carnegie Mellon University.
; All Rights Reserved.  Use is subject to license terms.
;
; See the file "license.terms" for information on usage and
; redistribution of this file, and for a DISCLAIMER OF ALL
; WARRANTIES.

; Dumps the phoneset to stdout.
; 
; Expects Phoneset.description to be defined.
;
(define (dump_phoneset)
  (phoneset_to_text (PhoneSet.description nil))
)

(define (dump_phone_line feature_schema phone_line)
  (let ((feature_names feature_schema)
	(phone_name (car phone_line))
	(phone_attributes (cdr phone_line)))
    (while feature_names
	   (let ((feature_name (caar feature_names))
		 (phone_attribute (car phone_attributes)))
	     (format t "%s %s %s\n" phone_name feature_name phone_attribute)
	     (set! feature_names (cdr feature_names))
	     (set! phone_attributes (cdr phone_attributes))
	     ))))

(define (phoneset_to_text phoneset)
  (let ((feature_schema (car (cdr (car (cdr  phoneset)))))
	(phone_lines    (car (cdr (car (cddr phoneset))))))
    (while phone_lines
	   (dump_phone_line feature_schema (car phone_lines))
	   (set! phone_lines (cdr phone_lines)))
    (format t "silence symbol %s\n" (car (cadr (car (PhoneSet.description '(silences))))))
))
