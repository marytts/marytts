; Portions Copyright 2004 Sun Microsystems, Inc.
; Portions Copyright 1999-2003 Language Technologies Institute,
; Carnegie Mellon University.
; All Rights Reserved.  Use is subject to license terms.
;
; See the file "license.terms" for information on usage and
; redistribution of this file, and for a DISCLAIMER OF ALL
; WARRANTIES.

; Dumps intonation accent cart to stdout.
;
; Expects int_accent_cart_tree to be defined.
;
(define (dump_int_accent_cart) 
  (set! current_node 0)
  (set! nodes (print_cart_nodes int_accent_cart_tree))
  (format t "TOTAL %d\n" current_node)
  (format t "%s" nodes))

(defvar cart_operators
  '(("is" "=")
    ("in" "IN")
    ("<" "<")
    (">" ">")
    ("matches" "REGEX")
    ("=" "EQUALS"))) ; CST_CART_OP_EQUALS not handled in
                     ;    Flite->FreeTTS Conversion
                     ; May cause problems.

(define (print_cart_list l)
    (cond
        ((null? l))
        ((cdr l) (format nil "%f,%s" (car l) (print_cart_list (cdr l))))
        (t (format nil "%f" (car l)))))

(define (print_cart_nodes tree)
  (set! current_node (+ 1 current_node))
  (cond
   ((cdr tree) ;node (non-leaf)
    (let ((operator (cadr (assoc_string (cadr (car tree)) cart_operators)))
	  (val (nth 2 (car tree))))
      (let ((type (cond
		   ((string-equal operator "=") (format nil "String(%s)" val))
		   ((string-equal operator "REGEX") (format nil "Integer(%d)" val))
		   ((number? val) (format nil "Float(%f)" val))
		   ((consp val) (format stderr "List vals not supported here yet\n")
		    (error val))
		   (t (format nil "String(%s)" val))
		   )))
	(let ((left_val (print_cart_nodes (car (cdr tree)))))
          (let ((this_node_val (format nil "NODE %s %s %s %d\n"
				       (caar tree) ;feat
				       operator
				       type
				       current_node)))
	    (let ((right_val (print_cart_nodes (car (cdr (cdr tree))))))
	      (string-append this_node_val left_val right_val))))
        )))
   (t (format nil "LEAF String(%s)\n" (caar (cdr (car tree)))))))
